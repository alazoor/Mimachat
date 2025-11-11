package com.arabic.aitoolkit.core.chat;

public class ChatMessage {
    private String content;
    private boolean isUserMessage; // صحيح للمستخدم، خطأ للنظام/الذكاء الاصطناعي
    private String sourceReference; // مثال: "PRD: Page 1"
    private String sourceImagePath; // المسار المحلي للصورة التي تم استخلاص النص منها

    // منشئ لرسائل المستخدم (استعلام)
    public ChatMessage(String content, boolean isUserMessage) {
        this.content = content;
        this.isUserMessage = isUserMessage;
    }

    // منشئ لرسائل النظام (إجابة مع إسناد)
    public ChatMessage(String content, String sourceReference, String sourceImagePath) {
        this.content = content;
        this.isUserMessage = false;
        this.sourceReference = sourceReference;
        this.sourceImagePath = sourceImagePath;
    }

    // المسندات (Getters)
    public String getContent() {
        return content;
    }

    public boolean isUserMessage() {
        return isUserMessage;
    }

    public boolean hasSource() {
        return !isUserMessage && sourceImagePath != null && !sourceImagePath.isEmpty();
    }

    public String getSourceReference() {
        return sourceReference;
    }
    
    public String getSourceImagePath() {
        return sourceImagePath;
    }

    // يمكن إضافة مسندات ومعدلات (Setters) أخرى حسب الحاجة
}
