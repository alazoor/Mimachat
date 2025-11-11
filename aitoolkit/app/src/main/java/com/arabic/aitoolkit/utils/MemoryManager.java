package com.arabic.aitoolkit.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * مسؤول عن إدارة الذاكرة، خاصة تقليل حجم (downscaling) الصور
 * الكبيرة قبل تمريرها إلى OCRManager.
 */
public class MemoryManager {

    private static final String TAG = "MemoryManager";
    // الحجم الأقصى الموصى به للصورة (بالبكسل المربع)
    // 2048x2048 بكسل يعتبر حجمًا جيدًا لمعظم عمليات OCR.
    private static final int MAX_IMAGE_SIZE = 2048; 
    
    private final Context context;

    public MemoryManager(Context context) {
        this.context = context;
    }

    /**
     * يحمل صورة من Uri ويقلل حجمها إذا كانت أكبر من الحد الأقصى المحدد.
     * يستخدم inSampleSize لتقليل الذاكرة المستهلكة أثناء عملية التحميل.
     * * @param imageUri المسار URI للصورة.
     * @return كائن Bitmap مُحسن الذاكرة.
     * @throws FileNotFoundException إذا لم يتم العثور على الملف.
     */
    public Bitmap loadOptimizedBitmap(Uri imageUri) throws FileNotFoundException {
        // 1. تحديد حجم الصورة الأصلي دون تحميلها بالكامل إلى الذاكرة
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true; // تعيين هذا لـ true يعني أننا نحمل الحدود فقط
        
        try (InputStream inputStream = context.getContentResolver().openInputStream(imageUri)) {
            if (inputStream == null) {
                throw new FileNotFoundException("Cannot open input stream for URI: " + imageUri);
            }
            BitmapFactory.decodeStream(inputStream, null, options);
        } catch (Exception e) {
            Log.e(TAG, "Error determining image bounds: " + e.getMessage());
            throw new FileNotFoundException("Failed to read image metadata.");
        }

        // 2. حساب معامل التقليص (inSampleSize)
        options.inSampleSize = calculateInSampleSize(options, MAX_IMAGE_SIZE, MAX_IMAGE_SIZE);

        // 3. تحميل الصورة فعليًا مع معامل التقليص
        options.inJustDecodeBounds = false; // الآن نحمل البيانات الفعلية

        try (InputStream inputStream = context.getContentResolver().openInputStream(imageUri)) {
            if (inputStream == null) {
                throw new FileNotFoundException("Cannot open input stream for URI: " + imageUri);
            }
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
            
            if (bitmap == null) {
                 throw new IllegalStateException("Bitmap decoding failed.");
            }
            
            Log.d(TAG, String.format("Image loaded. Original size: %dx%d, Sample size: %d, Final size: %dx%d",
                options.outWidth, options.outHeight, options.inSampleSize, bitmap.getWidth(), bitmap.getHeight()));
            
            return bitmap;
            
        } catch (Exception e) {
             Log.e(TAG, "Error decoding bitmap with inSampleSize: " + e.getMessage());
             throw new FileNotFoundException("Failed to decode image into optimized bitmap.");
        }
    }

    /**
     * يحسب inSampleSize لتقليل حجم الصورة بحيث لا يتجاوز الحدود المطلوبة.
     * * @param options خيارات BitmapFactory مع تحديد الحدود.
     * @param reqWidth العرض المطلوب.
     * @param reqHeight الارتفاع المطلوب.
     * @return قيمة inSampleSize (عادةً تكون 1، 2، 4، 8...).
     */
    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // حساب أكبر inSampleSize بحيث يكون الارتفاع والعرض > المطلوب
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
}
