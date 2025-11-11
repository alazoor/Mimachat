package com.arabic.aitoolkit.ui.chat;

import android.content.Context;

import com.arabic.aitoolkit.core.chat.ChatMessage;
import com.arabic.aitoolkit.core.embedding.EmbeddingManager;
import com.arabic.aitoolkit.core.search.SearchResult;
import com.arabic.aitoolkit.core.search.VectorSearchManager;

import java.util.List;

public class QAManager {

    private final EmbeddingManager embeddingManager;
    private final VectorSearchManager vectorSearchManager;
    private final Context context;

    // معامل البحث الأدنى (Search Threshold) - يمكن تعديله في الإعدادات لاحقًا
    private static final float SIMILARITY_THRESHOLD = 0.65f; 
    // عدد النتائج القصوى التي نريد جلبها من البحث
    private static final int MAX_RESULTS = 3; 

    /**
     * منشئ QAManager.
     * @param context سياق التطبيق.
     * @param embeddingManager مُدير توليد المتجهات (Embedding).
     * @param vectorSearchManager مُدير البحث في المتجهات (Vector Search).
     */
    public QAManager(Context context, EmbeddingManager embeddingManager, VectorSearchManager vectorSearchManager) {
        this.context = context;
        this.embeddingManager = embeddingManager;
        this.vectorSearchManager = vectorSearchManager;
        // هنا يمكن إضافة منطق تهيئة إضافي إذا لزم الأمر
    }

    /**
     * معالجة استعلام المستخدم، والبحث عن الإجابة في قاعدة المعرفة.
     * @param queryText استعلام المستخدم باللغة العربية.
     * @return ChatMessage تحتوي على الإجابة والإسناد، أو رسالة "لم يتم العثور" في حالة الفشل.
     */
    public ChatMessage processQuery(String queryText) {
        try {
            // 1. توليد متجه (Embedding) للاستعلام
            float[] queryVector = embeddingManager.generateEmbedding(queryText);

            if (queryVector == null) {
                return createErrorMessage("فشل في توليد متجه البحث. يرجى التحقق من نموذج MiniLM.");
            }

            // 2. تنفيذ البحث الدلالي في قاعدة البيانات
            // نجلب أفضل 3 نتائج تطابق الحد الأدنى من التشابه
            List<SearchResult> results = vectorSearchManager.search(
                    queryVector, 
                    MAX_RESULTS, 
                    SIMILARITY_THRESHOLD
            );

            // 3. تحليل النتائج وتنسيق الإجابة
            if (results.isEmpty()) {
                return createNoMatchMessage();
            } else {
                return formatAnswer(results);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return createErrorMessage("حدث خطأ غير متوقع أثناء معالجة الاستعلام: " + e.getMessage());
        }
    }

    /**
     * تنسيق نتائج البحث المتعددة في رسالة دردشة واحدة ذات إسناد.
     * @param results قائمة نتائج البحث.
     * @return ChatMessage من نوع النظام تحتوي على الإجابة.
     */
    private ChatMessage formatAnswer(List<SearchResult> results) {
        // مبدأ الـ QAManager البسيط (MVP) هو تجميع النصوص ذات الصلة وذكر مصدر واحد (الأكثر صلة)
        StringBuilder answerBuilder = new StringBuilder();
        
        // استخدام النتيجة الأولى كمرجع أساسي (Source)
        SearchResult primarySource = results.get(0);
        
        // بناء الإجابة: دمج النصوص المستخلصة من النتائج
        answerBuilder.append("إليك المعلومات التي تم العثور عليها في المستندات:\n");
        int count = 1;
        for (SearchResult result : results) {
            // نضيف النص المطابق مع معامل التشابه
            answerBuilder.append("\n• ")
                         .append(result.getExtractedText().getTextContent())
                         .append(" (تشابه: ")
                         .append(String.format("%.2f", result.getSimilarityScore()))
                         .append(")");
            count++;
        }

        String content = answerBuilder.toString();
        String reference = "المصدر الرئيسي: " + primarySource.getExtractedText().getSourceReference();
        String imagePath = primarySource.getExtractedText().getSourceImagePath();

        // إنشاء رسالة الإجابة للنظام
        return new ChatMessage(content, reference, imagePath);
    }

    /**
     * إنشاء رسالة خطأ موحدة.
     */
    private ChatMessage createErrorMessage(String error) {
        // نستخدم constructor الرسالة البسيط للنظام
        return new ChatMessage("🚫 خطأ: " + error, false);
    }

     /**
     * إنشاء رسالة "لم يتم العثور على تطابق".
     */
    private ChatMessage createNoMatchMessage() {
        return new ChatMessage(
                "عذراً، لم أتمكن من العثور على معلومات ذات صلة باستعلامك ضمن النصوص المستخلصة. يرجى محاولة صياغة مختلفة أو التأكد من إدخال المزيد من المستندات.", 
                false
        );
    }
}
