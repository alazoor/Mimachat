package com.arabic.aitoolkit.core.search;

import android.util.Log;

import com.arabic.aitoolkit.core.database.dao.SearchDao;
import com.arabic.aitoolkit.core.embeddings.EmbeddingManager;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
* VectorSearchManager: يدير فهرسة المتجهات والبحث الدلالي.
* يحاكي وظيفة مكتبة البحث عن الأقرب (Nearest Neighbor Search).
*/
public class VectorSearchManager {
	
	private static final String TAG = "VectorSearchManager";
	private static final int EMBEDDING_DIMENSION = 384; // حجم متجه MiniLM
	private static final int MAX_RESULTS = 5; // عدد النتائج القصوى المراد إرجاعها
	
	private final EmbeddingManager embeddingManager;
	private final SearchDao searchDao;
	private final ExecutorService backgroundExecutor;
	
	// تمثيل مبسط لفهرس المتجهات (في التطبيق الحقيقي، سيكون هذا فهرس HNSWlib)
	private List<TextWithVector> knowledgeBaseCache = new ArrayList<>();
	
	public VectorSearchManager(EmbeddingManager embeddingManager, SearchDao searchDao, ExecutorService backgroundExecutor) {
		this.embeddingManager = embeddingManager;
		this.searchDao = searchDao;
		this.backgroundExecutor = backgroundExecutor;
		
		// تحميل قاعدة المعرفة عند التهيئة
		loadKnowledgeBaseAsync();
	}
	
	// -------------------------------------------------------------
	// إدارة قاعدة المعرفة
	// -------------------------------------------------------------
	
	/**
	* تحميل جميع النصوص والمتجهات من قاعدة البيانات إلى الذاكرة/الفهرس في الخلفية.
	*/
	private void loadKnowledgeBaseAsync() {
		backgroundExecutor.execute(() -> {
			try {
				// جلب النصوص والمتجهات معًا بفضل استعلام JOIN المحسن
				List<TextWithVector> data = searchDao.getAllTextsWithVectors();
				
				// تحويل بيانات المتجهات الثنائية (byte[]) إلى مصفوفة float[] في الذاكرة
				for (TextWithVector item : data) {
					item.embeddingVector = convertBytesToFloats(item.getEmbeddingVectorBytes());
				}
				
				knowledgeBaseCache = data;
				Log.i(TAG, "Knowledge base loaded successfully. Total documents: " + knowledgeBaseCache.size());
				} catch (Exception e) {
				Log.e(TAG, "Failed to load knowledge base: " + e.getMessage());
			}
		});
	}
	
	// -------------------------------------------------------------
	// وظيفة البحث الدلالي الرئيسية
	// -------------------------------------------------------------
	
	/**
	* يبحث عن النصوص ذات الصلة دلاليًا بسؤال المستخدم.
	* @param queryText سؤال المستخدم.
	* @return قائمة بالنتائج ذات الصلة (Top K results).
	*/
	public List<SearchResult> semanticSearch(String queryText) {
		if (knowledgeBaseCache.isEmpty()) {
			Log.w(TAG, "Knowledge base is empty. Cannot perform search.");
			return new ArrayList<>();
		}
		
		// 1. توليد متجه سؤال المستخدم
		float[] queryVector = embeddingManager.generateEmbedding(queryText);
		if (queryVector == null) {
			return new ArrayList<>();
		}
		
		// 2. محاكاة البحث عن أقرب جار (Nearest Neighbor Search)
		List<SearchResult> results = new ArrayList<>();
		
		for (TextWithVector document : knowledgeBaseCache) {
			float[] documentVector = document.embeddingVector;
			
			if (documentVector != null) {
				// حساب تشابه جيب التمام (Cosine Similarity)
				double similarity = calculateCosineSimilarity(queryVector, documentVector);
				
				// حفظ النتيجة إذا كانت ذات صلة
				results.add(new SearchResult(document.text.getTextContent(), similarity, document.text.getSourceReference()));
			}
		}
		
		// 3. فرز النتائج وإرجاع أفضل K نتيجة
		results.sort(Comparator.comparingDouble(SearchResult::getSimilarity).reversed());
		
		return results.subList(0, Math.min(results.size(), MAX_RESULTS));
	}
	
	// -------------------------------------------------------------
	// وظائف مساعدة رياضية (Math & Conversion)
	// -------------------------------------------------------------
	
	/**
	* يحسب تشابه جيب التمام (Cosine Similarity) بين متجهين.
	* Similarity = (A . B) / (||A|| * ||B||)
	*/
	private double calculateCosineSimilarity(float[] vectorA, float[] vectorB) {
		if (vectorA.length != vectorB.length) return 0;
		
		double dotProduct = 0.0;
		double normA = 0.0;
		double normB = 0.0;
		
		for (int i = 0; i < vectorA.length; i++) {
			dotProduct += vectorA[i] * vectorB[i];
			normA += vectorA[i] * vectorA[i];
			normB += vectorB[i] * vectorB[i];
		}
		
		if (normA == 0.0 || normB == 0.0) return 0.0;
		
		// تشابه جيب التمام
		return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
	}
	
	/**
	* يحول مصفوفة البايت (BLOB) إلى مصفوفة الأرقام العائمة (float[]).
	*/
	private float[] convertBytesToFloats(byte[] bytes) {
		if (bytes == null || bytes.length == 0 || bytes.length % 4 != 0) return null;
		
		FloatBuffer floatBuffer = ByteBuffer.wrap(bytes)
		.order(java.nio.ByteOrder.nativeOrder())
		.asFloatBuffer();
		float[] floats = new float[floatBuffer.remaining()];
		floatBuffer.get(floats);
		return floats;
	}
	
	// -------------------------------------------------------------
	// فئة POJO للنتيجة
	// -------------------------------------------------------------
	
	public static class SearchResult {
		private final String textContent;
		private final double similarity;
		private final String sourceReference;
		
		public SearchResult(String textContent, double similarity, String sourceReference) {
			this.textContent = textContent;
			this.similarity = similarity;
			this.sourceReference = sourceReference;
		}
		
		public String getTextContent() { return textContent; }
		public double getSimilarity() { return similarity; }
		public String getSourceReference() { return sourceReference; }
	}
}