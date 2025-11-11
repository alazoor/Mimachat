package com.arabic.aitoolkit.core.database.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
* يمثل كيانًا لتخزين متجه التضمين (Embedding Vector) المقابل لكل نص.
* يضمن هذا الفصل بين النص والمتجه لتحسين أداء الاستعلامات.
*/
@Entity(tableName = "embedding_vectors",
// ربط هذا الكيان بـ ExtractedText عبر textId (مفتاح خارجي)
foreignKeys = @ForeignKey(entity = ExtractedText.class,
parentColumns = "id", // عمود الـ ID في ExtractedText
childColumns = "textId", // عمود الربط في هذا الجدول
onDelete = ForeignKey.CASCADE), // حذف المتجه عند حذف النص
// إضافة فهرس على textId لتحسين عمليات JOIN (الربط)
indices = {@Index(value = {"textId"}, unique = true)}
)
public class EmbeddingVector {
	
	@PrimaryKey(autoGenerate = true)
	private long id;
	
	// المفتاح الخارجي (Foreign Key): يربط هذا المتجه بكيان ExtractedText محدد
	private long textId;
	
	// المتجه الفعلي المخزن كـ BLOB (مصفوفة بايت) في قاعدة بيانات SQLite
	// سيتم تحويل float[] إلى byte[] قبل التخزين والعكس أثناء الجلب.
	private byte[] vectorData;
	
	private long generationTimestamp; // وقت توليد المتجه
	
	// المنشئ (Constructor)
	public EmbeddingVector(long textId, byte[] vectorData, long generationTimestamp) {
		this.textId = textId;
		this.vectorData = vectorData;
		this.generationTimestamp = generationTimestamp;
	}
	
	// المسندات والمعدلات (Getters and Setters)
	
	public long getId() {
		return id;
	}
	
	public void setId(long id) {
		this.id = id;
	}
	
	public long getTextId() {
		return textId;
	}
	
	public void setTextId(long textId) {
		this.textId = textId;
	}
	
	public byte[] getVectorData() {
		return vectorData;
	}
	
	public void setVectorData(byte[] vectorData) {
		this.vectorData = vectorData;
	}
	
	public long getGenerationTimestamp() {
		return generationTimestamp;
	}
}