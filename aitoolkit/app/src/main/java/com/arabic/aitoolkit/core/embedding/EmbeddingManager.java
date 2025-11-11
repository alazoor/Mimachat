package com.arabic.aitoolkit.core.embedding;

import android.content.Context;
import android.util.Log;

import com.arabic.aitoolkit.core.models.ModelLoader;

import org.tensorflow.lite.Interpreter;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
* EmbeddingManager: يدير تحميل نموذج TFLite MiniLM ويشغله لتحويل النصوص
* العربية إلى متجهات تضمين (Embeddings).
*/
public class EmbeddingManager implements ModelLoader.ModelLoadListener {
	
	private static final String TAG = "EmbeddingManager";
	private static final String MODEL_PATH = "all-mpnet-base-v2-ar.tflite"; // مثال لاسم نموذج تضمين
	private static final int MAX_SEQUENCE_LENGTH = 128; // الطول الأقصى للتسلسل
	private static final int EMBEDDING_DIMENSION = 384; // حجم متجه الإخراج (MiniLM-like)
	
	private final Context context;
	private final ExecutorService backgroundExecutor;
	private final ModelLoader modelLoader;
	private final ArabicTextNormalizer normalizer;
	
	private Interpreter embeddingInterpreter;
	private boolean isModelReady = false;
	
	// ------------------- المُنشئ -------------------
	
	public EmbeddingManager(Context context, ExecutorService backgroundExecutor, ModelLoader modelLoader) {
		this.context = context;
		this.backgroundExecutor = backgroundExecutor;
		this.modelLoader = modelLoader;
		this.normalizer = new ArabicTextNormalizer(); // تهيئة Normalizer
		
		// بدء عملية التحميل غير المتزامنة
		Log.i(TAG, "Attempting to load MiniLM model...");
		modelLoader.loadTfLiteModelAsync(MODEL_PATH, this);
	}
	
	// ------------------- callbacks تحميل النموذج -------------------
	
	@Override
	public void onModelLoaded(MappedByteBuffer modelBuffer) {
		backgroundExecutor.execute(() -> {
			try {
				// إعداد المترجم (Interpreter)
				Interpreter.Options options = new Interpreter.Options();
				options.setNumThreads(4); // استخدام 4 خيوط لتحسين الأداء
				embeddingInterpreter = new Interpreter(modelBuffer, options);
				
				isModelReady = true;
				Log.i(TAG, "Embedding model loaded and ready. Dims: " + EMBEDDING_DIMENSION);
				
				} catch (Exception e) {
				Log.e(TAG, "Failed to initialize TFLite Interpreter: " + e.getMessage());
				isModelReady = false;
			}
		});
	}
	
	@Override
	public void onModelLoadFailed(String errorMessage) {
		isModelReady = false;
		Log.e(TAG, "Failed to load Embedding model: " + errorMessage);
	}
	
	// ------------------- وظيفة الخدمة الرئيسية -------------------
	
	/**
	* يولد متجه تضمين لنص عربي محدد باستخدام نموذج TFLite.
	* @param text النص المراد تحويله.
	* @return مصفوفة float[] تمثل المتجه (أو null في حالة الفشل).
	*/
	public float[] generateEmbedding(String text) {
		if (!isModelReady) {
			Log.e(TAG, "Embedding model is not yet loaded or ready.");
			return null;
		}
		
		if (text == null || text.trim().isEmpty()) {
			return new float[EMBEDDING_DIMENSION]; // مصفوفة صفرية للنصوص الفارغة
		}
		
		// 1. الترميز والمعالجة المسبقة باستخدام Normalizer
		// تتوقع [inputIds, attentionMask, tokenTypeIds]
		List<float[]> inputs = normalizer.tokenize(text);
		
		if (inputs == null || inputs.size() != 3 || inputs.get(0).length != MAX_SEQUENCE_LENGTH) {
			Log.e(TAG, "Tokenization failed or returned incorrect length.");
			return null;
		}
		
		// يجب أن تكون المدخلات في مصفوفة ثنائية الأبعاد [1, MAX_SEQUENCE_LENGTH]
		Object[] inputsArray = new Object[] {
			new float[][] {inputs.get(0)}, // Input IDs
			new float[][] {inputs.get(1)}, // Attention Mask
			new float[][] {inputs.get(2)}  // Token Type IDs
		};
		
		// 2. إعداد مصفوفة الإخراج
		// الإخراج المتوقع: [1] (batch size) x [EMBEDDING_DIMENSION] (384)
		float[][] output = new float[1][EMBEDDING_DIMENSION];
		
		try {
			// 3. تنفيذ النموذج
			// نستخدم HashMap لربط مؤشر الإخراج بالمصفوفة التي تستقبله
			embeddingInterpreter.runForMultipleInputsOutputs(inputsArray, new HashMap<Integer, Object>() {{
					put(0, output); // نفترض أن المتجه يخرج من المؤشر 0
			}});
			
			// نأخذ الصف الأول والوحيد من الإخراج
			return output[0];
			
			} catch (Exception e) {
			Log.e(TAG, "Error running TFLite inference: ", e);
			return null;
		}
	}
	
	// ------------------- تنظيف الموارد -------------------
	
	public void unloadModel() {
		if (embeddingInterpreter != null) {
			embeddingInterpreter.close();
			embeddingInterpreter = null;
			isModelReady = false;
			Log.i(TAG, "Embedding Interpreter closed.");
		}
	}
}