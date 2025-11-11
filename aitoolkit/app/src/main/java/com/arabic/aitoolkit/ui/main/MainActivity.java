package com.arabic.aitoolkit.ui.main;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.arabic.aitoolkit.R;
import com.arabic.aitoolkit.ui.camera.CameraActivity;
import com.arabic.aitoolkit.ui.chat.ChatActivity;

// استيرادات المكونات الأساسية (التي تم دمجها حديثًا)
import com.arabic.aitoolkit.core.ocr.OCRManager;
import com.arabic.aitoolkit.core.database.AppDatabase;
import com.arabic.aitoolkit.core.database.dao.ExtractedTextDao; // يفترض وجوده
import com.arabic.aitoolkit.core.embeddings.EmbeddingManager;
import com.arabic.aitoolkit.core.models.ModelLoader;
import com.arabic.aitoolkit.utils.MemoryManager;

import java.io.FileNotFoundException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
	
	private static final String TAG = "MainActivity";
	private static final int PERMISSION_REQUEST_CODE = 100;
	private static final int SELECT_IMAGE_REQUEST = 101;
	
	// عناصر الواجهة
	private Button btnCaptureImage;
	private Button btnSelectImage;
	private Button btnStartChat;
	private ProgressBar progressBar;
	
	// مدراء الخدمات والموارد
	private ExecutorService backgroundExecutor;
	private MemoryManager memoryManager;
	private OCRManager ocrManager;
	private EmbeddingManager embeddingManager;
	private ModelLoader modelLoader;
	
	// -------------------------------------------------------------
	// دورة حياة النشاط (Activity Lifecycle)
	// -------------------------------------------------------------
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		initViews();
		initManagers();
		setupListeners();
		checkPermissions();
		
		// 5. التعامل مع URI القادم من CameraActivity أو onActivityResult
		handleIntentData(getIntent());
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		// التعامل مع URI من Intent جديد (عند العودة من الكاميرا)
		handleIntentData(intent);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (backgroundExecutor != null) {
			backgroundExecutor.shutdownNow();
		}
		// إغلاق موارد OCR و Embedding
		if (ocrManager != null) {
			ocrManager.close();
		}
		if (embeddingManager != null) {
			embeddingManager.unloadModel();
		}
	}
	
	// -------------------------------------------------------------
	// التهيئة (Initialization)
	// ----------------------------------------------------------------
	
	private void initViews() {
		btnCaptureImage = findViewById(R.id.btnCaptureImage);
		btnSelectImage = findViewById(R.id.btnSelectImage);
		btnStartChat = findViewById(R.id.btnStartChat);
		progressBar = findViewById(R.id.progressBar);
		
		Toast.makeText(this, "Android Arabic AI Toolkit جاهز.", Toast.LENGTH_SHORT).show();
	}
	
	private void initManagers() {
		backgroundExecutor = Executors.newSingleThreadExecutor();
		
		// 1. تهيئة الـ DAO وقاعدة البيانات
		AppDatabase db = AppDatabase.getInstance(getApplicationContext());
		// يفترض وجود extractedTextDao()
		ExtractedTextDao textDao = db.extractedTextDao();
		
		// 2. تهيئة المدراء المرتبطة بالنماذج
		modelLoader = new ModelLoader(getApplicationContext(), backgroundExecutor);
		embeddingManager = new EmbeddingManager(getApplicationContext(), backgroundExecutor, modelLoader);
		memoryManager = new MemoryManager(getApplicationContext());
		
		// 3. تهيئة OCRManager (يعتمد على EmbeddingManager)
		ocrManager = new OCRManager(getApplicationContext(), backgroundExecutor, textDao, embeddingManager);
	}
	
	private void setupListeners() {
		// 1. التقاط صورة جديدة (يفتح شاشة الكاميرا)
		btnCaptureImage.setOnClickListener(v -> {
			if (checkRequiredPermissions()) {
				startActivity(new Intent(MainActivity.this, CameraActivity.class));
				} else {
				Toast.makeText(this, "الرجاء منح أذونات الكاميرا والتخزين.", Toast.LENGTH_SHORT).show();
			}
		});
		
		// 2. اختيار صورة من المعرض
		btnSelectImage.setOnClickListener(v -> {
			if (checkRequiredPermissions()) {
				openImageChooser();
				} else {
				Toast.makeText(this, "الرجاء منح أذونات التخزين.", Toast.LENGTH_SHORT).show();
			}
		});
		
		// 3. الانتقال إلى شاشة الدردشة
		btnStartChat.setOnClickListener(v -> {
			startActivity(new Intent(MainActivity.this, ChatActivity.class));
		});
	}
	
	// -------------------------------------------------------------
	// الأذونات ومعالجة Intent (Permissions & Intent Handling)
	// -------------------------------------------------------------
	
	private void checkPermissions() {
		if (!checkRequiredPermissions()) {
			ActivityCompat.requestPermissions(
			this,
			new String[]{
				Manifest.permission.CAMERA,
				Manifest.permission.WRITE_EXTERNAL_STORAGE,
				Manifest.permission.READ_EXTERNAL_STORAGE
			},
			PERMISSION_REQUEST_CODE
			);
		}
	}
	
	private boolean checkRequiredPermissions() {
		return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
		ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
		ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
	}
	
	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == PERMISSION_REQUEST_CODE) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				Toast.makeText(this, "تم منح الأذونات بنجاح.", Toast.LENGTH_SHORT).show();
				} else {
				Toast.makeText(this, "لم يتم منح كل الأذونات المطلوبة. قد لا تعمل بعض الميزات.", Toast.LENGTH_LONG).show();
			}
		}
	}
	
	private void handleIntentData(Intent intent) {
		if (intent != null && intent.hasExtra("OCR_IMAGE_URI")) {
			String uriString = intent.getStringExtra("OCR_IMAGE_URI");
			Uri imageUri = Uri.parse(uriString);
			startOcrProcessing(imageUri);
			
			// مسح البيانات لتجنب المعالجة المكررة
			intent.removeExtra("OCR_IMAGE_URI");
		}
	}
	
	// -------------------------------------------------------------
	// اختيار الصورة (Image Selection)
	// -------------------------------------------------------------
	
	private void openImageChooser() {
		Intent intent = new Intent(Intent.ACTION_PICK);
		intent.setType("image/*");
		startActivityForResult(intent, SELECT_IMAGE_REQUEST);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == SELECT_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
			Uri imageUri = data.getData();
			if (imageUri != null) {
				startOcrProcessing(imageUri);
			}
		}
	}
	
	// -------------------------------------------------------------
	// بدء معالجة OCR (Core Logic)
	// -------------------------------------------------------------
	
	/**
	* وظيفة مساعدة لبدء عملية OCR على الصورة المحددة/الملتقطة.
	* @param imageUri المسار URI للصورة.
	*/
	private void startOcrProcessing(Uri imageUri) {
		if (ocrManager == null) {
			showToastOnUI("🚫 مدير OCR غير مهيأ. انتظر أو أعد تشغيل التطبيق.");
			return;
		}
		
		progressBar.setVisibility(View.VISIBLE);
		showToastOnUI("جاري معالجة OCR وتخزين المتجهات...");
		
		backgroundExecutor.execute(() -> {
			Bitmap optimizedBitmap = null;
			String resultMessage;
			try {
				// 1. تحسين الذاكرة وتحميل الصورة
				optimizedBitmap = memoryManager.loadOptimizedBitmap(imageUri);
				
				// 2. تمرير الصورة إلى OCRManager للمعالجة والتخزين
				String sourcePath = imageUri.toString();
				// نستخدم مرجعًا فريدًا لسهولة التتبع
				String sourceRef = "IMG-" + System.currentTimeMillis();
				
				resultMessage = ocrManager.processImage(optimizedBitmap, sourcePath, sourceRef);
				
				} catch (FileNotFoundException e) {
				Log.e(TAG, "File not found: " + e.getMessage());
				resultMessage = "🚫 فشل: لم يتم العثور على ملف الصورة.";
				} catch (Exception e) {
				Log.e(TAG, "Processing failed: " + e.getMessage());
				resultMessage = "❌ فشل غير متوقع: " + e.getMessage();
				} finally {
				// 3. تحرير الـ Bitmap
				if (optimizedBitmap != null) {
					optimizedBitmap.recycle();
				}
				
				// 4. تحديث الواجهة عند الانتهاء
				final String finalMessage = resultMessage;
				runOnUiThread(() -> {
					progressBar.setVisibility(View.GONE);
					Toast.makeText(this, finalMessage, Toast.LENGTH_LONG).show();
					
					// إذا نجحت المعالجة، انتقل إلى شاشة الدردشة
					if (finalMessage.startsWith("✅")) {
						startActivity(new Intent(MainActivity.this, ChatActivity.class));
					}
				});
			}
		});
	}
	
	// وظيفة مساعدة لعرض Toast من خيط الخلفية
	private void showToastOnUI(String message) {
		runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show());
	}
}