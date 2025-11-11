package com.arabic.aitoolkit.core.database.entities;

import androidx.room.Entity;
import androidx.room.Index; // جديد: استيراد Index
import androidx.room.PrimaryKey;

/**
 * يمثل كيانًا لتخزين النصوص المستخلصة من OCR في قاعدة بيانات Room.
 */
@Entity(tableName = "extracted_texts",
        // جديد: إضافة فهرس (Index) لعمود sourceReference
        // هذا يحسن الأداء عندما نقوم بالبحث عن طريق مصدر معين.
        indices = {@Index(value = {"sourceReference"})} 
)
public class ExtractedText {
    
    @PrimaryKey(autoGenerate = true)
    private long id;
    
    private String textContent; // النص العربي المستخلص فعليًا
    private String sourceImagePath; // المسار المحلي للصورة التي جاء منها النص
    private String sourceReference; // مرجع سهل القراءة (مثل: "صورة 1" أو "PRD-Page-2")
    private long timestamp; // وقت الإدخال

    // المنشئ (Constructor)
    public ExtractedText(String textContent, String sourceImagePath, String sourceReference, long timestamp) {
        this.textContent = textContent;
        this.sourceImagePath = sourceImagePath;
        this.sourceReference = sourceReference;
        this.timestamp = timestamp;
    }

    // المسندات والمعدلات (Getters and Setters)

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTextContent() {
        return textContent;
    }

    public String getSourceImagePath() {
        return sourceImagePath;
    }
    
    public String getSourceReference() {
        return sourceReference;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
