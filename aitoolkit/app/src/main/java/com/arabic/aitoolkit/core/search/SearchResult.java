package com.arabic.aitoolkit.core.search;

import com.arabic.aitoolkit.core.database.entities.ExtractedText;

/**
 * يمثل نتيجة فردية من عملية البحث الدلالي.
 * يجمع النص المطابق ودرجة تشابهه مع الاستعلام.
 */
public class SearchResult {
    
    private final ExtractedText extractedText;
    private final float similarityScore; // درجة التشابه (بين 0.0 و 1.0)

    /**
     * منشئ لنتيجة البحث.
     * @param extractedText كيان النص المستخلص الذي تم العثور عليه.
     * @param similarityScore درجة التشابه بين متجه الاستعلام ومتجه النص.
     */
    public SearchResult(ExtractedText extractedText, float similarityScore) {
        this.extractedText = extractedText;
        this.similarityScore = similarityScore;
    }

    // المسندات (Getters)

    public ExtractedText getExtractedText() {
        return extractedText;
    }

    public float getSimilarityScore() {
        return similarityScore;
    }
}
