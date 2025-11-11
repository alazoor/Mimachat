// المسار: com.arabic.aitoolkit.utils

package com.arabic.aitoolkit.utils;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.util.Log;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * فئة مساعدة لتحميل ملفات TFLite من مجلد Assets إلى الذاكرة.
 * يتم استخدامها بواسطة OCRManager و EmbeddingManager.
 */
public class ModelLoader {

    private static final String TAG = "ModelLoader";

    /**
     * يقوم بتحميل ملف نموذج TFLite من مجلد Assets كـ MappedByteBuffer.
     * هذا هو الشكل المطلوب لتهيئة مُفسِّر TFLite.
     *
     * @param context سياق التطبيق (Context).
     * @param modelFileName المسار النسبي لملف TFLite داخل مجلد assets/models/.
     * مثال: "paddleocr/det_arabic.tflite"
     * @return MappedByteBuffer يحتوي على النموذج المحمّل، أو null في حالة الفشل.
     */
    public static MappedByteBuffer loadModelFile(Context context, String modelFileName) {
        try {
            // 1. فتح ملف Asset
            AssetFileDescriptor fileDescriptor = context.getAssets().openFd("models/" + modelFileName);

            // 2. استخدام FileInputStream و FileChannel للقراءة
            FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
            FileChannel fileChannel = inputStream.getChannel();

            // 3. تحديد الإزاحة والحجم
            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getDeclaredLength();

            // 4. تعيين الملف في الذاكرة (Memory Mapping)
            // هذا يتيح قراءة فعالة للنموذج من الذاكرة دون تحميل الملف بأكمله دفعة واحدة.
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);

        } catch (IOException e) {
            // تسجيل الخطأ إذا لم يتم العثور على الملف أو حدث خطأ في القراءة
            Log.e(TAG, "Failed to load model file: " + modelFileName, e);
            return null;
        }
    }
}
