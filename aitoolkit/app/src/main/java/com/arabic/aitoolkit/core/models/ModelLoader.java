package com.arabic.aitoolkit.core.models;

import android.content.Context;
import android.util.Log;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * فئة مسؤولة عن تحميل وتفريغ نماذج TensorFlow Lite (TFLite) و OCR (مثل Tesseract)
 * في الخلفية لإدارة موارد الذاكرة بكفاءة.
 */
public class ModelLoader {

    private static final String TAG = "ModelLoader";

    // أسماء ملفات نماذج TFLite
    public static final String EMBEDDING_MODEL_FILE = "miniLM_L6_v2_ar.tflite";
    public static final String OCR_DETECTION_MODEL_FILE = "ocr_detection.tflite";
    // Tesseract/OCR عادة لا يستخدم TFLite Loader، بل يستخدم ملفات بيانات OCR خام.

    private final Context context;
    private final ExecutorService executor;
    // Map لتخزين النماذج المُحمّلة
    private final Map<String, Interpreter> loadedTfLiteModels = new HashMap<>();

    public ModelLoader(Context context, ExecutorService executor) {
        this.context = context;
        this.executor = executor;
    }

    /**
     * تحميل نموذج TFLite معين بشكل غير متزامن.
     * @param modelName اسم ملف النموذج (مثل EMBEDDING_MODEL_FILE).
     * @param listener واجهة رد الاتصال للإبلاغ عن النجاح أو الفشل.
     */
    public void loadTfLiteModelAsync(final String modelName, final ModelLoadListener listener) {
        if (loadedTfLiteModels.containsKey(modelName)) {
            Log.d(TAG, modelName + " is already loaded.");
            listener.onModelLoaded(modelName, loadedTfLiteModels.get(modelName));
            return;
        }

        executor.execute(() -> {
            try {
                // قراءة ملف النموذج مباشرة من assets
                MappedByteBuffer modelBuffer = FileUtil.loadMappedFile(context, modelName);
                
                // إعداد المفسّر (Interpreter)
                Interpreter.Options options = new Interpreter.Options();
                // يمكن إضافة خيارات التسريع هنا (مثل XNNPACK أو GPU Delegate)
                // options.setNumThreads(Runtime.getRuntime().availableProcessors()); 

                Interpreter interpreter = new Interpreter(modelBuffer, options);
                
                loadedTfLiteModels.put(modelName, interpreter);
                Log.i(TAG, modelName + " loaded successfully.");
                
                // إرجاع النتيجة على خيط رد الاتصال (يفترض أن listener سيتعامل مع الـ Threading)
                listener.onModelLoaded(modelName, interpreter);
                
            } catch (IOException e) {
                Log.e(TAG, "Failed to load TFLite model: " + modelName, e);
                listener.onModelLoadFailed(modelName, e.getMessage());
            }
        });
    }

    /**
     * تفريغ نموذج TFLite معين من الذاكرة (إغلاق المفسّر).
     * @param modelName اسم ملف النموذج المراد تفريغه.
     */
    public void unloadTfLiteModel(String modelName) {
        Interpreter interpreter = loadedTfLiteModels.remove(modelName);
        if (interpreter != null) {
            interpreter.close();
            Log.i(TAG, modelName + " unloaded successfully.");
        }
    }

    /**
     * تفريغ جميع النماذج المُحمّلة.
     */
    public void unloadAllTfLiteModels() {
        for (Interpreter interpreter : loadedTfLiteModels.values()) {
            interpreter.close();
        }
        loadedTfLiteModels.clear();
        Log.i(TAG, "All TFLite models unloaded.");
    }

    // --------------- واجهة رد الاتصال (Listener Interface) ---------------
    
    public interface ModelLoadListener {
        void onModelLoaded(String modelName, Interpreter interpreter);
        void onModelLoadFailed(String modelName, String errorMessage);
    }
}
