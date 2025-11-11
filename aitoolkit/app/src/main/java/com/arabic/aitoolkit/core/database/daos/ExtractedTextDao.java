package com.arabic.aitoolkit.core.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.arabic.aitoolkit.core.database.entities.ExtractedText;

import java.util.List;

/**
* واجهة وصول البيانات (DAO) للنصوص المستخلصة (ExtractedText).
* تستخدم بواسطة OCRManager للتخزين و ChatActivity/SearchManager للجلب.
*/
@Dao
public interface ExtractedTextDao {
	
	/**
	* إدراج كيان نصي جديد.
	* @param text الكيان ExtractedText.
	* @return معرف (ID) الصف الجديد الذي تم إنشاؤه تلقائياً.
	*/
	@Insert(onConflict = OnConflictStrategy.REPLACE)
	long insert(ExtractedText text);
	
	/**
	* جلب جميع النصوص المستخلصة.
	* @return قائمة بجميع النصوص المستخلصة.
	*/
	@Query("SELECT * FROM extracted_texts ORDER BY timestamp DESC")
	List<ExtractedText> getAllTexts();
	
	/**
	* جلب نص واحد بناءً على المعرف (ID).
	*/
	@Query("SELECT * FROM extracted_texts WHERE id = :textId")
	ExtractedText getTextById(long textId);
	
	/**
	* مسح جميع النصوص.
	*/
	@Query("DELETE FROM extracted_texts")
	void clearAll();
}