package com.arabic.aitoolkit.core.database.dao;

import androidx.room.Dao;
import androidx.room.Query;

import com.arabic.aitoolkit.core.search.TextWithVector;

import java.util.List;

/**
* واجهة وصول البيانات (DAO) المسؤولة عن جلب النصوص والمتجهات
* اللازمة لعملية البحث الدلالي (VectorSearchManager).
*/
@Dao
public interface SearchDao {
	
	/**
	* @Query لتحسين الأداء: تقوم بجلب جميع النصوص والمتجهات المطلوبة
	* في عملية واحدة (JOIN) لتقليل زمن الاستعلام (Latency).
	* * يفترض أن هناك جدول اسمه 'embedding_vectors' يربط المتجهات بالنصوص.
	*/
	@Query("SELECT " +
	"t.id, t.textContent, t.sourceImagePath, t.sourceReference, t.timestamp, " +
	"v.vector_data AS embeddingVectorBytes " +
	"FROM extracted_texts t " +
	"INNER JOIN embedding_vectors v ON t.id = v.textId")
	List<TextWithVector> getAllTextsWithVectors();
	
	/**
	* استعلام لجلب كيان نصي واحد بناءً على المعرف (ID).
	* @param textId معرّف النص
	* @return كيان TextWithVector.
	*/
	@Query("SELECT * FROM extracted_texts WHERE id = :textId")
	TextWithVector getTextWithVectorById(long textId);
}