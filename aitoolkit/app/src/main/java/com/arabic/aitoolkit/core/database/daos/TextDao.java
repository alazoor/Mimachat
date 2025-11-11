// المسار: com.arabic.aitoolkit.core.database.daos

package com.arabic.aitoolkit.core.database.daos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.arabic.aitoolkit.core.database.entities.ExtractedText;

import java.util.List;

/**
 * TextDao هي واجهة الوصول إلى بيانات النصوص.
 * تحدد العمليات الأساسية (CRUD) على كيان ExtractedText.
 */
@Dao
public interface TextDao {

    // ------------------- عمليات الإضافة والتحديث -------------------

    /**
     * إدخال نص مستخرج جديد في قاعدة البيانات.
     * @param text كائن ExtractedText لإدخاله.
     * @return ID الصف الذي تم إدخاله.
     */
    @Insert
    long insert(ExtractedText text);

    /**
     * تحديث صف موجود في قاعدة البيانات.
     * سيتم استخدامه لتحديث المتجهات (Embeddings) بعد إنشائها.
     * @param text كائن ExtractedText لتحديثه.
     * @return عدد الصفوف التي تم تحديثها.
     */
    @Update
    int update(ExtractedText text);


    // ------------------- عمليات الاستعلام (Query) -------------------

    /**
     * استرجاع جميع النصوص المستخرجة من قاعدة البيانات.
     * @return قائمة بجميع كائنات ExtractedText.
     */
    @Query("SELECT * FROM extracted_texts ORDER BY timestamp DESC")
    List<ExtractedText> getAllTexts();

    /**
     * استرجاع نص محدد باستخدام المعرف (ID).
     * @param id المعرف الخاص بـ ExtractedText.
     * @return كائن ExtractedText المطابق، أو null إذا لم يتم العثور عليه.
     */
    @Query("SELECT * FROM extracted_texts WHERE id = :id")
    ExtractedText getTextById(int id);


    /**
     * استرجاع النصوص التي لا تحتوي على متجهات تضمين بعد (embedding_vector_json هو NULL).
     * سيتم استخدام هذا لمهمة معالجة خلفية (Background Processing) لإنشاء المتجهات.
     * @return قائمة بالنصوص التي تحتاج إلى توليد متجهات.
     */
    @Query("SELECT * FROM extracted_texts WHERE embedding_vector_json IS NULL")
    List<ExtractedText> getTextsWithoutEmbeddings();


    // ------------------- عملية الحذف -------------------

    /**
     * حذف جميع النصوص من الجدول.
     */
    @Query("DELETE FROM extracted_texts")
    void deleteAllTexts();
}
