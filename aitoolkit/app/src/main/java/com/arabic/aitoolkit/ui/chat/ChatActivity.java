package com.arabic.aitoolkit.ui.chat;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.arabic.aitoolkit.R;
import com.arabic.aitoolkit.core.database.AppDatabase;
import com.arabic.aitoolkit.core.database.daos.SearchDao;
import com.arabic.aitoolkit.core.embedding.EmbeddingManager;
import com.arabic.aitoolkit.core.models.ModelLoader;
import com.arabic.aitoolkit.core.search.VectorSearchManager;
import com.arabic.aitoolkit.core.search.VectorSearchManager.SearchResult;
import com.arabic.aitoolkit.ui.main.MainActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
* نشاط شاشة الدردشة، حيث يتفاعل المستخدم مع نظام البحث الدلالي.
* يعرض الرسائل ويطلق عملية البحث باستخدام VectorSearchManager.
*/
public class ChatActivity extends AppCompatActivity {
	
	private static final String TAG = "ChatActivity";
	
	private RecyclerView recyclerView;
	private EditText messageInput;
	private ImageButton sendButton;
	
	private ChatAdapter chatAdapter;
	private List<ChatMessage> chatMessages;
	
	// مدراء الخدمات
	private ExecutorService backgroundExecutor;
	private VectorSearchManager searchManager;
	private EmbeddingManager embeddingManager;
	private ModelLoader modelLoader;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chat);
		
		// إعداد زر الرجوع إلى الشاشة الرئيسية (للسهولة)
		if (getSupportActionBar() != null) {
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
			getSupportActionBar().setTitle("الدردشة والبحث");
		}
		
		initViews();
		initManagers();
		setupListeners();
		
		// رسالة ترحيب أولية
		addMessage("مساعد البحث", "أهلاً! اطرح سؤالاً للبحث في الوثائق التي قمت بمسحها ضوئيًا.", false);
	}
	
	@Override
	public boolean onSupportNavigateUp() {
		// العودة إلى MainActivity
		finish();
		return true;
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (backgroundExecutor != null) {
			backgroundExecutor.shutdownNow();
		}
		// تفريغ موارد النماذج
		if (embeddingManager != null) {
			embeddingManager.unloadModel();
		}
	}
	
	// ------------------- التهيئة -------------------
	
	private void initViews() {
		recyclerView = findViewById(R.id.recyclerViewChat);
		messageInput = findViewById(R.id.editTextMessage);
		sendButton = findViewById(R.id.buttonSend);
		
		chatMessages = new ArrayList<>();
		chatAdapter = new ChatAdapter(chatMessages);
		recyclerView.setLayoutManager(new LinearLayoutManager(this));
		recyclerView.setAdapter(chatAdapter);
	}
	
	/**
	* تهيئة المدراء اللازمة للبحث الدلالي.
	*/
	private void initManagers() {
		backgroundExecutor = Executors.newSingleThreadExecutor();
		
		// 1. تهيئة الاعتماديات
		AppDatabase db = AppDatabase.getInstance(getApplicationContext());
		SearchDao searchDao = db.searchDao();
		
		// 2. تهيئة ModelLoader (ضروري لـ EmbeddingManager)
		modelLoader = new ModelLoader(getApplicationContext(), backgroundExecutor);
		
		// 3. تهيئة EmbeddingManager (ضروري للبحث)
		embeddingManager = new EmbeddingManager(
		getApplicationContext(),
		backgroundExecutor,
		modelLoader
		);
		
		// 4. تهيئة VectorSearchManager
		searchManager = new VectorSearchManager(embeddingManager, searchDao, backgroundExecutor);
	}
	
	private void setupListeners() {
		sendButton.setOnClickListener(v -> {
			String message = messageInput.getText().toString().trim();
			if (!message.isEmpty()) {
				sendMessage(message);
				messageInput.setText("");
			}
		});
	}
	
	// ------------------- منطق الدردشة والبحث -------------------
	
	private void sendMessage(String message) {
		// 1. عرض رسالة المستخدم
		addMessage("أنت", message, true);
		
		// 2. عرض مؤشر التحميل
		addMessage("مساعد البحث", "جاري البحث...", false);
		
		// 3. تشغيل البحث في الخلفية
		backgroundExecutor.execute(() -> {
			try {
				// إجراء البحث الدلالي
				final List<SearchResult> results = searchManager.semanticSearch(message);
				
				// بناء الإجابة
				String botReply = buildBotResponse(message, results);
				
				// 4. تحديث الواجهة: إزالة مؤشر التحميل وإضافة الإجابة
				runOnUiThread(() -> {
					// إزالة رسالة "جاري البحث..." (هي الرسالة الأخيرة دائمًا)
					if (!chatMessages.isEmpty() && chatMessages.get(chatMessages.size() - 1).getSender().equals("مساعد البحث")) {
						chatMessages.remove(chatMessages.size() - 1);
						chatAdapter.notifyItemRemoved(chatMessages.size());
					}
					
					addMessage("مساعد البحث", botReply, false);
				});
				} catch (Exception e) {
				Log.e(TAG, "Search failed: " + e.getMessage(), e);
				runOnUiThread(() -> {
					Toast.makeText(this, "فشل عملية البحث.", Toast.LENGTH_SHORT).show();
					// إزالة مؤشر التحميل وإضافة رسالة خطأ
					if (!chatMessages.isEmpty() && chatMessages.get(chatMessages.size() - 1).getSender().equals("مساعد البحث")) {
						chatMessages.remove(chatMessages.size() - 1);
						chatAdapter.notifyItemRemoved(chatMessages.size());
					}
					addMessage("مساعد البحث", "❌ حدث خطأ أثناء البحث: " + e.getMessage(), false);
				});
			}
		});
	}
	
	private void addMessage(String sender, String text, boolean isUser) {
		chatMessages.add(new ChatMessage(sender, text, isUser));
		chatAdapter.notifyItemInserted(chatMessages.size() - 1);
		recyclerView.scrollToPosition(chatMessages.size() - 1);
	}
	
	/**
	* يقوم ببناء الإجابة النهائية اعتمادًا على السؤال ونتائج البحث.
	*/
	private String buildBotResponse(String query, List<SearchResult> results) {
		if (results.isEmpty()) {
			return "عفواً، لم أجد معلومات ذات صلة بسؤالك في قاعدة المعرفة المتاحة.";
		}
		
		StringBuilder response = new StringBuilder("إليك أبرز ما وجدته في الوثائق الخاصة بك:\n\n");
		
		// اقتباس جزء من النص الأكثر صلة
		for (int i = 0; i < results.size(); i++) {
			SearchResult result = results.get(i);
			
			// نأخذ جزءاً صغيراً من النص المعثر عليه (مثلاً أول 120 حرف)
			String fullText = result.getTextContent();
			String snippet = fullText.substring(0, Math.min(fullText.length(), 120)) + (fullText.length() > 120 ? "..." : "");
			
			response.append(String.format("📜 المصدر: %s (صلة: %.1f%%)\n",
			result.getSourceReference(),
			result.getSimilarity() * 100))
			.append(">> ")
			.append(snippet)
			.append("\n\n---\n");
			
			// نكتفي بأفضل 3 نتائج لعدم إرباك المستخدم
			if (i >= 2) break;
		}
		
		response.append("\nالرجاء طرح أسئلة أكثر تحديدًا لتحسين النتائج.");
		
		return response.toString();
	}
}