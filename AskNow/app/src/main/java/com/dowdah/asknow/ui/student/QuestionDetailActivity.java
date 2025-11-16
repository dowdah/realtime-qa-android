package com.dowdah.asknow.ui.student;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.dowdah.asknow.R;
import com.dowdah.asknow.data.local.dao.MessageDao;
import com.dowdah.asknow.data.local.dao.QuestionDao;
import com.dowdah.asknow.data.local.entity.QuestionEntity;
import com.dowdah.asknow.databinding.ActivityQuestionDetailBinding;
import com.dowdah.asknow.ui.adapter.MessageAdapter;
import com.dowdah.asknow.ui.chat.ChatViewModel;
import com.dowdah.asknow.utils.SharedPreferencesManager;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class QuestionDetailActivity extends AppCompatActivity {
    
    private ActivityQuestionDetailBinding binding;
    private ChatViewModel chatViewModel;
    private MessageAdapter messageAdapter;
    private long questionId;
    private long currentUserId;
    
    @Inject
    QuestionDao questionDao;
    
    @Inject
    MessageDao messageDao;
    
    @Inject
    SharedPreferencesManager prefsManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQuestionDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        questionId = getIntent().getLongExtra("question_id", -1);
        if (questionId == -1) {
            Toast.makeText(this, R.string.invalid_question, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        currentUserId = prefsManager.getUserId();
        chatViewModel = new ViewModelProvider(this).get(ChatViewModel.class);
        
        setupToolbar();
        setupRecyclerView();
        loadQuestionDetails();
        observeMessages();
        observeViewModel();
        setupInputArea();
    }
    
    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.question_detail);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }
    
    private void setupRecyclerView() {
        messageAdapter = new MessageAdapter(currentUserId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.recyclerViewMessages.setLayoutManager(layoutManager);
        binding.recyclerViewMessages.setAdapter(messageAdapter);
    }
    
    private void loadQuestionDetails() {
        // 使用 LiveData 来观察问题状态的变化
        questionDao.getQuestionByIdLive(questionId).observe(this, question -> {
            if (question != null) {
                binding.tvContent.setText(question.getContent());
                binding.tvStatus.setText(getStatusText(question.getStatus()));
                
                if (question.getImagePath() != null && !question.getImagePath().isEmpty()) {
                    String imageUrl = "http://10.0.2.2:8000" + question.getImagePath();
                    Glide.with(this)
                        .load(imageUrl)
                        .into(binding.ivQuestion);
                    binding.ivQuestion.setVisibility(View.VISIBLE);
                } else {
                    binding.ivQuestion.setVisibility(View.GONE);
                }
                
                // 根据问题状态启用/禁用输入
                boolean isActive = !"closed".equals(question.getStatus()) && 
                                 !"pending".equals(question.getStatus());
                binding.etMessage.setEnabled(isActive);
                binding.btnSend.setEnabled(isActive);
            }
        });
    }
    
    private String getStatusText(String status) {
        switch (status) {
            case "pending":
                return getString(R.string.status_pending);
            case "in_progress":
                return getString(R.string.status_in_progress);
            case "closed":
                return getString(R.string.status_closed);
            default:
                return status;
        }
    }
    
    private void observeMessages() {
        messageDao.getMessagesByQuestionId(questionId).observe(this, messages -> {
            if (messages != null) {
                messageAdapter.setMessages(messages);
                // 滚动到最新消息
                if (messages.size() > 0) {
                    binding.recyclerViewMessages.scrollToPosition(messages.size() - 1);
                }
            }
        });
    }
    
    private void observeViewModel() {
        chatViewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });
        
        chatViewModel.getMessageSent().observe(this, sent -> {
            if (sent != null && sent) {
                binding.etMessage.setText("");
            }
        });
    }
    
    private void setupInputArea() {
        binding.btnSend.setOnClickListener(v -> sendMessage());
    }
    
    private void sendMessage() {
        String content = binding.etMessage.getText().toString().trim();
        
        if (content.isEmpty()) {
            Toast.makeText(this, R.string.please_enter_message, Toast.LENGTH_SHORT).show();
            return;
        }
        
        chatViewModel.sendMessage(questionId, content);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // 检查是否有未读消息，只在有未读消息时才标记
        checkAndMarkMessagesAsRead();
    }
    
    /**
     * 检查并标记消息为已读
     * 只有在有未读消息时才调用 API，避免不必要的网络请求
     */
    private void checkAndMarkMessagesAsRead() {
        new Thread(() -> {
            int unreadCount = messageDao.getUnreadMessageCount(questionId, currentUserId);
            if (unreadCount > 0) {
                runOnUiThread(() -> {
                    chatViewModel.markMessagesAsRead(questionId);
                });
            }
        }).start();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
