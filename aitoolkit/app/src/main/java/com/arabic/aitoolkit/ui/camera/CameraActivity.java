package com.arabic.aitoolkit.ui.camera;

import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.arabic.aitoolkit.R;
import com.arabic.aitoolkit.ui.main.MainActivity; // للعودة إلى الشاشة الرئيسية

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * نشاط مخصص لالتقاط صورة جديدة باستخدام CameraX.
 * يتم تخزين الصورة الملتقطة مؤقتًا لبدء عملية OCR.
 */
public class CameraActivity extends AppCompatActivity {

    private static final String TAG = "CameraActivity";

    private PreviewView previewView;
    private ImageButton captureButton;
    
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        previewView = findViewById(R.id.previewView);
        captureButton = findViewById(R.id.captureButton);

        cameraExecutor = Executors.newSingleThreadExecutor();
        
        // التحقق من إذن الكاميرا (تم التحقق منه مسبقًا في MainActivity، ولكن يُفضل التأكد)
        // إذا كان الإذن ممنوحًا، ابدأ الكاميرا
        startCamera();
        
        captureButton.setOnClickListener(v -> takePhoto());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
    
    // -------------------------------------------------------------
    // إعداد CameraX
    // -------------------------------------------------------------

    private void startCamera() {
        // تهيئة CameraProvider
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Camera initialization failed.", e);
                Toast.makeText(this, "فشل تهيئة الكاميرا: " + e.getMessage(), Toast.LENGTH_LONG).show();
                finish();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases(@NonNull ProcessCameraProvider cameraProvider) {
        // فك ربط أي حالات استخدام سابقة
        cameraProvider.unbindAll(); 

        // 1. حالة الاستخدام: المعاينة (Preview)
        Preview preview = new Preview.Builder()
                .build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // 2. حالة الاستخدام: التقاط الصورة (Image Capture)
        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        // 3. اختيار الكاميرا (الخلفية)
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        try {
            // ربط الحالات بدورة حياة النشاط
            cameraProvider.bindToLifecycle(
                    this, 
                    cameraSelector, 
                    preview, 
                    imageCapture
            );
        } catch(Exception exc) {
            Log.e(TAG, "Use case binding failed", exc);
            Toast.makeText(this, "فشل ربط حالات استخدام الكاميرا.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    // -------------------------------------------------------------
    // التقاط الصورة
    // -------------------------------------------------------------

    private void takePhoto() {
        if (imageCapture == null) {
            Toast.makeText(this, "الكاميرا غير جاهزة.", Toast.LENGTH_SHORT).show();
            return;
        }

        // إنشاء ملف الإخراج (سنستخدم MediaStore لحفظ الصورة)
        String fileName = "OCR_IMAGE_" + System.currentTimeMillis();
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ArabicAIToolkit");
        }

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(
                getContentResolver(), 
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, 
                contentValues
        ).build();

        // تنفيذ التقاط الصورة
        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this), // استخدام خيط الواجهة
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Uri savedUri = outputFileResults.getSavedUri();
                        if (savedUri != null) {
                            String msg = String.format(Locale.getDefault(), "تم حفظ الصورة: %s", savedUri.toString());
                            Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT).show();
                            
                            // بعد حفظ الصورة بنجاح، نرسل المسار إلى MainActivity لبدء عملية OCR
                            // ونغلق نشاط الكاميرا.
                            startOcrProcess(savedUri);
                            
                        } else {
                             onImageSaveError(new ImageCaptureException(0, "فشل غير معروف في حفظ الصورة.", null));
                        }
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exc) {
                        onImageSaveError(exc);
                    }
                }
        );
    }
    
    private void onImageSaveError(ImageCaptureException exc) {
        Log.e(TAG, "Photo capture failed: " + exc.getMessage(), exc);
        Toast.makeText(getBaseContext(), "فشل التقاط الصورة: " + exc.getMessage(), Toast.LENGTH_SHORT).show();
        // قد نعود إلى MainActivity إذا كان الخطأ خطيرًا
    }

    // -------------------------------------------------------------
    // الانتقال إلى المعالجة
    // -------------------------------------------------------------
    
    private void startOcrProcess(Uri imageUri) {
        // نعود إلى MainActivity ونمرر URI الصورة عبر Intent
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("OCR_IMAGE_URI", imageUri.toString());
        // نضيف علامات (Flags) لضمان عدم إنشاء نسخ متعددة من MainActivity
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish(); // إغلاق CameraActivity
    }
}
