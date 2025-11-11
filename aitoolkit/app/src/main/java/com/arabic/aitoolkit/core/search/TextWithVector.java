package com.arabic.aitoolkit.core.search;

import androidx.room.Embedded;
import com.arabic.aitoolkit.core.database.entities.ExtractedText;

/**
 * فئة POJO تجمع كيان ExtractedText مع متجه التضمين الخاص به.
 * تستخدم في Room لجلب البيانات اللازمة لـ VectorSearchManager.
 */
public class TextWithVector {
    
    // Embeds the fields of ExtractedText directly into this object
    @Embedded
    public ExtractedText text;
    
    // يفترض أن هذا هو المتجه المخزن كـ JSON أو BLOB (في التطبيق الفعلي)
    // أو كمرجع خارجي. هنا سنفترض أنه موجود كـ byte array (لأغراض Room/SQLite)
    public byte[] embeddingVectorBytes; 
    
    // المسندات (Getters) المطلوبة لعملية جلب البيانات (ليست ضرورية لـ Room POJO)
    // ولكن نتركها هنا لتوضيح كيفية استخدامها.

    public ExtractedText getExtractedText() {
        return text;
    }
    
    public byte[] getEmbeddingVectorBytes() {
        return embeddingVectorBytes;
    }
}
