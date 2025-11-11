package com.arabic.aitoolkit.core.ocr;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.arabic.aitoolkit.core.database.dao.ExtractedTextDao;
import com.arabic.aitoolkit.core.database.entities.ExtractedText;
import com.arabic.aitoolkit.core.embedding.EmbeddingManager;
import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;

/**
 * OCRManager: يدير عملية التعرف البصري على الحروف (OCR) باستخدام Tesseract.
 * مسؤول عن تهيئة Tesseract، ومعالجة الصور، وتخزين النصوص والمتجهات.
 */
public class OCRManager {

    private static final String TAG = "OCRManager";
    private static final String TESS_DATA_DIR = "tessdata";
    private static final String LANG = "ara"; // اللغة العربية
    private static final String TESS_ASSET = LANG + ".traineddata";

    private final Context context;
    private final ExecutorService backgroundExecutor;
    private final ExtractedTextDao textDao;
    private final EmbeddingManager embeddingManager;
    
    private TessBaseAPI tessApi;
    private String dataPath;

    public OCRManager(Context context, ExecutorService backgroundExecutor, ExtractedTextDao textDao, EmbeddingManager embeddingManager) {
        this.context = context;
        this.backgroundExecutor = backgroundExecutor;
        this.textDao = textDao;
        this.embeddingManager = embeddingManager;
        this.dataPath = context.getFilesDir() + "/tesseract/"; 
        
        // يجب أن يتم تهيئة Tesseract في الخيط الخلفي
        initializeTesseract();
    }
    
    // -------------------------------------------------------------
    // التهيئة وتحميل البيانات
    // -------------------------------------------------------------

    private void initializeTesseract() {
        backgroundExecutor.execute(() -> {
            try {
                // 1. نسخ ملفات traineddata من assets إلى التخزين المحلي
                copyTessDataAssets();
                
                // 2. تهيئة Tesseract API
                tessApi = new TessBaseAPI();
                boolean success = tessApi.init(dataPath, LANG);

                if (success) {
                    Log.i(TAG, "Tesseract initialized successfully for language: " + LANG);
                } else {
                    Log.e(TAG, "Tesseract initialization failed.");
                    tessApi = null;
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to initialize Tesseract: " + e.getMessage(), e);
                tessApi = null;
            }
        });
    }

    private void copyTessDataAssets() throws IOException {
        File dataDir = new File(dataPath, TESS_DATA_DIR);
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }

        File dataFile = new File(dataDir, TESS_ASSET);
        if (!dataFile.exists()) {
            Log.d(TAG, "Copying traineddata file from assets...");
            
            try (InputStream in = context.getAssets().open(TESS_DATA_DIR + "/" + TESS_ASSET);
                 OutputStream out = new FileOutputStream(dataFile)) {
                
                byte[] buffer = new byte[1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                Log.d(TAG, TESS_ASSET + " copied successfully.");
            }
        } else {
            Log.d(TAG, TESS_ASSET + " already exists.");
        }
    }
    
    // -------------------------------------------------------------
    // معالجة الصور (الخدمة الرئيسية)
    // -------------------------------------------------------------

    /**
     * معالجة صورة (Bitmap) لاستخلاص النص منها وتخزينه مع متجه التضمين.
     * يجب أن يتم استدعاؤها في خيط الخلفية (backgroundExecutor).
     * * @param bitmap الصورة المُحسّنة الذاكرة (من MemoryManager).
     * @param sourcePath المسار الأصلي للصورة.
     * @param sourceRef مرجع سهل القراءة.
     * @return النص المستخلص أو رسالة خطأ.
     */
    public String processImage(Bitmap bitmap, String sourcePath, String sourceRef) {
        if (tessApi == null) {
            return "🚫 Tesseract غير مُهيأ. الرجاء المحاولة لاحقاً.";
        }

        long startTime = System.currentTimeMillis();

        try {
            // 1. إجراء OCR
            tessApi.setImage(bitmap);
            String extractedText = tessApi.getUTF8Text().trim();
            tessApi.clear();
            
            long ocrTime = System.currentTimeMillis() - startTime;
            Log.d(TAG, String.format("OCR completed in %d ms. Text length: %d", ocrTime, extractedText.length()));

            if (extractedText.isEmpty()) {
                return "⚠️ لم يتم العثور على أي نص في الصورة.";
            }
            
            // 2. توليد المتجه وتخزين النص
            storeExtractedTextAndEmbedding(extractedText, sourcePath, sourceRef);
            
            return "✅ تم استخلاص النص وتخزينه بنجاح. (الزمن الإجمالي: " + (System.currentTimeMillis() - startTime) + "ms)";

        } catch (Exception e) {
            Log.e(TAG, "OCR or Storage failed: " + e.getMessage(), e);
            return "❌ فشل غير متوقع أثناء المعالجة: " + e.getMessage();
        }
    }
    
    // -------------------------------------------------------------
    // التخزين وتوليد المتجهات
    // -------------------------------------------------------------

    private void storeExtractedTextAndEmbedding(String text, String sourcePath, String sourceRef) {
        // 1. إنشاء كيان النص المستخلص
        ExtractedText newText = new ExtractedText(text, sourcePath, sourceRef, System.currentTimeMillis());
        
        // 2. إدخال النص في قاعدة البيانات والحصول على الـ ID
        // (نفترض أن ExtractedTextDao لديها طريقة لإرجاع الـ ID المُنشأ تلقائيًا)
        long textId = textDao.insert(newText); 
        
        // 3. توليد متجه التضمين
        float[] embeddingVector = embeddingManager.generateEmbedding(text);
        
        if (embeddingVector != null) {
            // 4. تخزين المتجه المرتبط بـ textId
            // [TODO: يجب إنشاء EmbeddingVector Entity و DAO لتخزين المتجه الفعلي]
            // vectorDao.insertVector(textId, embeddingVector); 
            Log.i(TAG, String.format("Text (ID: %d) stored and embedding generated (Dim: %d).", textId, embeddingVector.length));
        } else {
            Log.e(TAG, "Failed to generate embedding for text ID: " + textId);
        }
    }

    public void close() {
        if (tessApi != null) {
            tessApi.end();
            Log.i(TAG, "Tesseract API closed.");
        }
    }
}
