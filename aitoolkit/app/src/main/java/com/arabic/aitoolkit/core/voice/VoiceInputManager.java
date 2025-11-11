package com.arabic.aitoolkit.core.voice;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;
import org.vosk.android.StorageService;

import java.io.IOException;
import java.io.File;

public class VoiceInputManager implements RecognitionListener {

    private static final String TAG = "VoiceInputManager";
    // اسم مجلد نموذج Vosk في assets/models/
    private static final String MODEL_NAME = "vosk-model-small-ar"; 

    private Model model;
    private SpeechService speechService;
    private final Context context;
    private final VoiceListener listener;

    /** واجهة رد الاتصال لإرجاع النتائج إلى ChatActivity */
    public interface VoiceListener {
        void onSpeechResult(String result);
        void onSpeechPartialResult(String partialResult);
        void onSpeechError(String errorMessage);
        void onSpeechStatusChange(boolean isListening);
    }

    public VoiceInputManager(Context context, VoiceListener listener) {
        this.context = context;
        this.listener = listener;
        LibVosk.setLogLevel(LogLevel.INFO); // إعداد مستوى سجل Vosk
    }

    /**
     * تهيئة نموذج Vosk بشكل غير متزامن.
     * يجب استدعاؤها مرة واحدة عند بدء تشغيل النشاط.
     */
    public void initModel() {
        // يتم نسخ النموذج من مجلد assets إلى التخزين المحلي
        StorageService.unpack(context, MODEL_NAME, "model", 
            (modelFile) -> {
                // النجاح في استخراج النموذج
                try {
                    model = new Model(modelFile.getAbsolutePath());
                    Log.d(TAG, "Vosk Model loaded successfully.");
                } catch (IOException e) {
                    listener.onSpeechError("فشل في تحميل نموذج Vosk: " + e.getMessage());
                }
            }, 
            (exception) -> {
                // فشل الاستخراج
                listener.onSpeechError("فشل في استخراج نموذج Vosk: " + exception.getMessage());
            }
        );
    }

    /**
     * بدء عملية الاستماع والتعرف على الكلام.
     */
    public void startListening() {
        if (model == null) {
            listener.onSpeechError("نموذج Vosk غير مُحمّل بعد.");
            return;
        }
        if (speechService != null) {
            speechService.stop();
            speechService = null;
        }

        try {
            // تهيئة المُعرّف
            Recognizer recognizer = new Recognizer(model, 16000.0f); // 16000.0f هو معدل العينة المطلوب لنموذج Vosk
            speechService = new SpeechService(recognizer, 16000.0f, this);
            speechService.startListening();
            listener.onSpeechStatusChange(true);
            Log.d(TAG, "Vosk listening started.");
        } catch (IOException e) {
            listener.onSpeechError("فشل في بدء خدمة الكلام: " + e.getMessage());
        }
    }

    /**
     * إيقاف خدمة الاستماع.
     */
    public void stopListening() {
        if (speechService != null) {
            speechService.stop();
            speechService = null;
        }
        listener.onSpeechStatusChange(false);
        Log.d(TAG, "Vosk listening stopped.");
    }
    
    /**
     * تدمير موارد Vosk.
     * يجب استدعاؤها في onDestroy() للنشاط.
     */
    public void destroy() {
        if (speechService != null) {
            speechService.cancel();
            speechService = null;
        }
        if (model != null) {
            model.close(); // تحرير الذاكرة
        }
    }

    // --------------- تطبيق واجهة RecognitionListener (ردود فعل Vosk) ---------------
    
    // يتم استدعاؤها عند التعرف على جملة كاملة (النتيجة النهائية)
    @Override
    public void onResult(String hypothesis) {
        try {
            JSONObject json = new JSONObject(hypothesis);
            String text = json.getString("text");
            // إيقاف الاستماع بعد الحصول على نتيجة كاملة
            stopListening(); 
            listener.onSpeechResult(text);
        } catch (JSONException e) {
            listener.onSpeechError("فشل في تحليل نتيجة Vosk النهائية.");
        }
    }

    // يتم استدعاؤها عند التعرف على جزء من الجملة (لإظهار التغذية الراجعة الفورية)
    @Override
    public void onPartialResult(String hypothesis) {
        try {
            JSONObject json = new JSONObject(hypothesis);
            String partial = json.getString("partial");
            listener.onSpeechPartialResult(partial);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse partial result.");
        }
    }
    
    @Override
    public void onTimeout() {
        Log.d(TAG, "Vosk Timeout.");
        stopListening();
    }

    @Override
    public void onError(Exception e) {
        listener.onSpeechError("خطأ في التعرف على الكلام: " + e.getMessage());
        stopListening();
    }
    
    @Override
    public void onFinalResult(String hypothesis) {
        // يتم التعامل مع النتيجة النهائية في onResult()
    }
}
