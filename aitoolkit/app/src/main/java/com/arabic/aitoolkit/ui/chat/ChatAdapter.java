package com.arabic.aitoolkit.ui.chat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.arabic.aitoolkit.R;
// (ستحتاج إلى إنشاء هذا الكلاس لاحقًا لتمثيل الرسالة)
// import com.arabic.aitoolkit.core.chat.ChatMessage; 

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // أنواع الرسائل
    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_SYSTEM = 2;

    private final List<ChatMessage> messages;
    private final Context context;
    // واجهة للمسؤولة عن التعامل مع النقر على مصدر الصورة
    private final OnSourceClickListener sourceClickListener; 

    // واجهة للتعامل مع النقر على مصدر الصورة
    public interface OnSourceClickListener {
        void onSourceClick(String imagePath);
    }

    // بناء المُحوّل (Adapter Constructor)
    public ChatAdapter(Context context, List<ChatMessage> messages, OnSourceClickListener listener) {
        this.context = context;
        this.messages = messages;
        this.sourceClickListener = listener;
    }

    // 1. تحديد نوع طريقة العرض لكل عنصر
    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messages.get(position);
        if (message.isUserMessage()) {
            return VIEW_TYPE_USER;
        } else {
            return VIEW_TYPE_SYSTEM;
        }
    }

    // 2. إنشاء طريقة العرض المناسبة
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);

        if (viewType == VIEW_TYPE_USER) {
            View view = inflater.inflate(R.layout.item_message_user, parent, false);
            return new UserMessageViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_message_system, parent, false);
            return new SystemMessageViewHolder(view);
        }
    }

    // 3. ربط البيانات بطريقة العرض
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);

        if (holder.getItemViewType() == VIEW_TYPE_USER) {
            ((UserMessageViewHolder) holder).bind(message);
        } else {
            ((SystemMessageViewHolder) holder).bind(message, sourceClickListener);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    // --------------- فئات حاملي طرق العرض (ViewHolders) ---------------

    // لرسائل المستخدم
    public static class UserMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;

        public UserMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
        }

        public void bind(ChatMessage message) {
            messageTextView.setText(message.getContent());
        }
    }

    // لرسائل النظام (الإجابة مع الإسناد)
    public static class SystemMessageViewHolder extends RecyclerView.ViewHolder {
        TextView answerTextView;
        TextView sourceAttributionTextView;
        ImageButton sourceImageButton;

        public SystemMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            answerTextView = itemView.findViewById(R.id.answerTextView);
            sourceAttributionTextView = itemView.findViewById(R.id.sourceAttributionTextView);
            sourceImageButton = itemView.findViewById(R.id.sourceImageButton);
        }

        public void bind(final ChatMessage message, final OnSourceClickListener listener) {
            answerTextView.setText(message.getContent());

            // معالجة إسناد المصدر
            if (message.hasSource()) {
                sourceAttributionTextView.setText(message.getSourceReference());
                sourceAttributionTextView.setVisibility(View.VISIBLE);
                sourceImageButton.setVisibility(View.VISIBLE); // نظهر زر الصورة

                // ربط حدث النقر لفتح الصورة
                sourceImageButton.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onSourceClick(message.getSourceImagePath());
                    }
                });
                
                // إضافة إمكانية النقر على النص نفسه للإسناد (لتحسين UX)
                 sourceAttributionTextView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onSourceClick(message.getSourceImagePath());
                    }
                });


            } else {
                sourceAttributionTextView.setVisibility(View.GONE);
                sourceImageButton.setVisibility(View.GONE);
                sourceImageButton.setOnClickListener(null);
                sourceAttributionTextView.setOnClickListener(null);
            }
        }
    }
}
