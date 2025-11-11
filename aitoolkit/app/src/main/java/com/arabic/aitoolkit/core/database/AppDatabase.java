package com.arabic.aitoolkit.core.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.arabic.aitoolkit.core.database.dao.ExtractedTextDao;
import com.arabic.aitoolkit.core.database.dao.SearchDao;
import com.arabic.aitoolkit.core.database.entities.ExtractedText;
// يفترض وجود هذا الكيان لتخزين بيانات المتجهات الثنائية
import com.arabic.aitoolkit.core.database.entities.EmbeddingVector;

/**
* الفئة الرئيسية لقاعدة بيانات Room، المسؤولة عن توفير النسخة الوحيدة
* من قاعدة البيانات (Singleton).
*/
@Database(entities = {ExtractedText.class, EmbeddingVector.class}, // يجب إضافة جميع الكيانات هنا
version = 1,
exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
	
	private static final String DATABASE_NAME = "ai_toolkit_db";
	private static volatile AppDatabase INSTANCE;
	
	// ------------------- تعريف DAOs -------------------
	
	// DAO للنصوص المستخلصة (يستخدمه OCRManager)
	public abstract ExtractedTextDao extractedTextDao();
	
	// DAO للبحث (يستخدمه VectorSearchManager)
	public abstract SearchDao searchDao();
	
	// ------------------- آلية Singleton -------------------
	
	public static AppDatabase getInstance(final Context context) {
		if (INSTANCE == null) {
			synchronized (AppDatabase.class) {
				if (INSTANCE == null) {
					// إنشاء قاعدة البيانات
					INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
					AppDatabase.class, DATABASE_NAME)
					// لا نستخدم allowMainThreadQueries في تطبيق فعلي
					.build();
				}
			}
		}
		return INSTANCE;
	}
}