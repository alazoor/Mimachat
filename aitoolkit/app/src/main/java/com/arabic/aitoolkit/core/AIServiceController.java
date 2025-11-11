// المسار: com.arabic.aitoolkit.core

package com.arabic.aitoolkit.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.arabic.aitoolkit.core.database.AppDatabase;
import com.arabic.aitoolkit.core.database.daos.TextDao;
import com.arabic.aitoolkit.core.database.entities.ExtractedText;
import com.arabic.aitoolkit.core.embedding.EmbeddingManager;
import com.arabic.aitoolkit.core.ocr.OCRManager;
import com.arabic.aitoolkit.core.search.VectorSearchManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AIServiceController: يدير تدفق البيانات والعمليات بين OCR, Embeddings, Database, و Search.
 * هو نقطة الوصول الرئيسية للوظائف الشاملة في التطبيق.
 */
public class AIServiceController {

    private static final String TAG = "AIServiceController";
    private final TextDao textDao;
    private final OCRManager ocrManager;
    private final EmbeddingManager embeddingManager;
    private final VectorSearchManager searchManager;

    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    public AIServiceController(Context context) {
        // تهيئة الـ DAOs و المدراء
        this.textDao = AppDatabase.getInstance(context).textDao();
        this.ocrManager = OCRManager.getInstance(context);
        this.embeddingManager = EmbeddingManager.getInstance(context);
        this.searchManager = VectorSearchManager.getInstance(context);
    }

    // ------------------- 1. وظيفة OCR متكاملة (OCR-to-Database-to-Embedding) -------------------

    /**
     * يلتقط صورة، ينفذ OCR، ويخزن النتيجة، ويبدأ توليد المتجهات في الخلفية.
     * يحقق ربط OCR Text storage with embedding generation.
     *
     * @param imageBitmap الصورة المراد معالجتها.
     * @param sourceImagePath مسار الملف لتخزين الصورة المصدر.
     */
    public void processImageAndExtractText(Bitmap imageBitmap, String sourceImagePath) {
        ioExecutor.execute(() -> {
            try {
                // 1. تنفيذ OCR (سيرجع نصاً افتراضياً حتى ننفذ Story 2)
                String extractedText = ocrManager.extractTextFromImage(imageBitmap);

                if (extractedText != null && !extractedText.isEmpty()) {
                    // 2. تخزين النص في قاعدة البيانات (ExtractedText.java)
                    ExtractedText newText = new ExtractedText(
                            extractedText,
                            sourceImagePath,
                            System.currentTimeMillis()
                    );
                    long newId = textDao.insert(newText);
                    
                    if (newId > 0) {
                        Log.i(TAG, "OCR result stored with ID: " + newId);
                        
                        // 3. بدء توليد المتجهات وإضافتها للفهرس بشكل غير متزامن
                        generateAndIndexEmbeddingAsync((int) newId, extractedText);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error processing image and extracting text", e);
            }
        });
    }

    /**
     * وظيفة خلفية لتوليد المتجهات وتحديث القاعدة وإضافتها للفهرس.
     */
    private void generateAndIndexEmbeddingAsync(int textId, String textContent) {
        ioExecutor.execute(() -> {
            // 1. توليد المتجه
            float[] embedding = embeddingManager.generateEmbedding(textContent);
            
            if (embedding != null) {
                // 2. تحديث الكيان في قاعدة البيانات بالمتجه الجديد (يجب تحويله إلى String/JSON)
                // *ملاحظة: تحتاج إلى مُحوّل Room Type Converter لتحويل float[] إلى String أو JSON.*
                // مؤقتاً نفترض وجود دالة لتحويل float[] إلى JSON string:
                String embeddingJson = arrayToJson(embedding); 
                
                ExtractedText textToUpdate = textDao.getTextById(textId);
                if (textToUpdate != null) {
                    textToUpdate.setEmbeddingVectorJson(embeddingJson);
                    textDao.update(textToUpdate);
                    Log.d(TAG, "Text ID " + textId + " updated with embedding.");

                    // 3. إضافة المتجه إلى فهرس HNSWlib
                    searchManager.addVectorToIndex(embedding, textId);
                    searchManager.saveIndex(); // حفظ الفهرس بعد التحديث
                }
            }
        });
    }

    // ------------------- 2. وظيفة البحث الدلالي -------------------

    /**
     * تنفيذ استعلام البحث الدلالي الرئيسي.
     * @param query استعلام المستخدم (باللغة الطبيعية).
     * @param k عدد النتائج المطلوبة.
     * @return قائمة بالنصوص المستخرجة ذات الصلة.
     */
    public List<ExtractedText> performSemanticSearch(String query, int k) {
        if (!embeddingManager.isInitialized() || !searchManager.isIndexLoaded()) {
            Log.w(TAG, "Search components not ready.");
            return new ArrayList<>();
        }
        
        // 1. توليد متجه للاستعلام
        float[] queryEmbedding = embeddingManager.generateEmbedding(query);
        
        if (queryEmbedding == null) {
            Log.e(TAG, "Failed to generate embedding for query.");
            return new ArrayList<>();
        }
        
        // 2. البحث في فهرس المتجهات
        List<VectorSearchManager.SearchResult> searchResults = searchManager.search(queryEmbedding, k);

        // 3. استرجاع النصوص الكاملة من قاعدة البيانات
        List<ExtractedText> relevantTexts = new ArrayList<>();
        for (VectorSearchManager.SearchResult result : searchResults) {
            // استخدام ID النتيجة لجلب الكيان الكامل من Room
            ExtractedText text = textDao.getTextById(result.id);
            if (text != null) {
                // يمكن إضافة درجة التشابه إلى الكائن هنا إذا أردنا
                relevantTexts.add(text);
                Log.d(TAG, "Found relevant text with score: " + result.score);
            }
        }
        return relevantTexts;
    }

    // ------------------- 3. وظيفة مساعدة لتحويل float[] -------------------
    
    /**
     * (مؤقت) دالة وهمية لتحويل مصفوفة الأرقام العائمة إلى سلسلة نصية.
     */
    private String arrayToJson(float[] array) {
        // في التنفيذ الحقيقي، يجب استخدام مكتبة مثل GSON أو Jackson
        // للتحويل الصحيح إلى JSON.
        return array != null ? Arrays.toString(array) : "[]";
    }

    public void closeServices() {
        ocrManager.close();
        embeddingManager.close();
        searchManager.close();
        ioExecutor.shutdown();
    }
}
