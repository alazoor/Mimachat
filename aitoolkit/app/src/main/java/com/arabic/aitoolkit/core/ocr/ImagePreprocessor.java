// المسار: com.arabic.aitoolkit.core.ocr

package com.arabic.aitoolkit.core.ocr;

import android.graphics.Bitmap;
import android.util.Log;

// يجب إضافة مكتبات OpenCV (قد تحتاج إلى استيرادها بشكل صريح اعتمادًا على طريقة إضافتها)
// ملاحظة: قد تحتاج هذه الفئة إلى تهيئة OpenCV JNI بشكل صحيح في MainActivity
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * ImagePreprocessor: مسؤولة عن تحويل صور Bitmap إلى مصفوفات ByteBuffer
 * مناسبة كمدخل لنماذج TensorFlow Lite (OCR Detection Model).
 */
public class ImagePreprocessor {

    private static final String TAG = "ImagePreprocessor";
    
    // حجم الإدخال المتوقع لنموذج اكتشاف النص
    private static final int INPUT_SIZE = 640; 
    
    // عوامل التطبيع (Normalization) المطلوبة عادةً لنماذج TFLite
    // قيم افتراضية لنموذج PaddleOCR:
    private static final float NORM_MEAN = 127.5f;
    private static final float NORM_STD = 127.5f;
    
    // حجم البايتات لكل قناة لونية (3 قنوات: R, G, B)
    private static final int NUM_BYTES_PER_CHANNEL = 4; // float is 4 bytes

    /**
     * وظيفة المعالجة الرئيسية: تحويل Bitmap إلى ByteBuffer جاهز للإدخال في TFLite.
     * @param bitmap الصورة المصدر.
     * @return ByteBuffer يحتوي على بيانات الصورة المعالجة.
     */
    public static ByteBuffer preprocessForDetection(Bitmap bitmap) {
        if (bitmap == null) {
            Log.e(TAG, "Input bitmap is null.");
            return null;
        }

        // 1. تحويل Bitmap إلى مصفوفة OpenCV (Mat)
        Mat mat = new Mat();
        Utils.bitmapToMat(bitmap, mat);

        // 2. تغيير حجم الصورة (Resizing)
        // يجب أن يتناسب حجم الإدخال مع متطلبات النموذج (مثل 640x640)
        Mat resizedMat = new Mat();
        Imgproc.resize(mat, resizedMat, new Size(INPUT_SIZE, INPUT_SIZE));
        mat.release(); // تحرير الذاكرة

        // 3. تحويل الألوان والتطبيع (Normalization)
        
        // مساحة التخزين المطلوبة في الذاكرة: (العرض * الارتفاع * القنوات * حجم البايت)
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(
                INPUT_SIZE * INPUT_SIZE * 3 * NUM_BYTES_PER_CHANNEL);
        byteBuffer.order(ByteOrder.nativeOrder());

        // تحويل Mat إلى مصفوفة بايتات (عادةً ما يتم هذا كـ RGB أو BGR)
        // في نموذج PaddleOCR TFLite، غالبًا ما نحتاج إلى تطبيق تطبيع (Normalization).
        
        // ملاحظة: هذا الكود يقوم بعملية تطبيع بسيطة يدوياً للتبسيط. 
        // قد تتطلب نماذج TFLite المعقدة خطوات إضافية (مثل تحويل BGR إلى RGB).
        
        int[] pixels = new int[INPUT_SIZE * INPUT_SIZE];
        Bitmap resizedBitmap = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(resizedMat, resizedBitmap);
        resizedMat.release(); // تحرير الذاكرة

        resizedBitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE);

        // 4. تعبئة ByteBuffer وتطبيق التطبيع
        for (int pixel : pixels) {
            // استخراج قيم الألوان (R, G, B)
            float red = ((pixel >> 16) & 0xFF);
            float green = ((pixel >> 8) & 0xFF);
            float blue = (pixel & 0xFF);

            // تطبيق التطبيع: (القيمة - المتوسط) / الانحراف المعياري
            // (X - NORM_MEAN) / NORM_STD
            byteBuffer.putFloat((red - NORM_MEAN) / NORM_STD);
            byteBuffer.putFloat((green - NORM_MEAN) / NORM_STD);
            byteBuffer.putFloat((blue - NORM_MEAN) / NORM_STD);
        }

        return byteBuffer;
    }
}
