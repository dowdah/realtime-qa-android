package com.dowdah.asknow.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.dowdah.asknow.R;
import com.dowdah.asknow.constants.QuestionStatus;
import com.dowdah.asknow.data.local.dao.MessageDao;
import com.dowdah.asknow.data.local.entity.QuestionEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;

public class QuestionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    
    private static final int VIEW_TYPE_QUESTION = 0;
    private static final int VIEW_TYPE_LOADING = 1;
    
    private List<QuestionEntity> questions = new ArrayList<>();
    private final OnQuestionClickListener listener;
    private boolean showLoadingFooter = false;
    private boolean showRetryFooter = false;
    private BaseLoadingFooterViewHolder.OnRetryClickListener retryListener;
    private MessageDao messageDao;
    private long currentUserId;
    private ExecutorService executor;
    
    /**
     * 问题点击监听器接口
     */
    public interface OnQuestionClickListener {
        void onQuestionClick(@NonNull QuestionEntity question);
    }
    
    public QuestionAdapter(@NonNull OnQuestionClickListener listener) {
        this.listener = listener;
    }
    
    public QuestionAdapter(
        @NonNull OnQuestionClickListener listener,
        @Nullable MessageDao messageDao,
        long currentUserId,
        @Nullable ExecutorService executor
    ) {
        this.listener = listener;
        this.messageDao = messageDao;
        this.currentUserId = currentUserId;
        this.executor = executor;
    }
    
    /**
     * 设置重试点击监听器
     * 
     * @param retryListener 重试点击监听器
     */
    public void setRetryListener(@Nullable BaseLoadingFooterViewHolder.OnRetryClickListener retryListener) {
        this.retryListener = retryListener;
    }
    
    /**
     * 设置问题列表（使用DiffUtil优化）
     * 
     * @param newQuestions 新的问题列表
     */
    public void setQuestions(@Nullable List<QuestionEntity> newQuestions) {
        if (newQuestions == null) {
            newQuestions = new ArrayList<>();
        }
        
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new QuestionDiffCallback(this.questions, newQuestions));
        this.questions = new ArrayList<>(newQuestions);
        diffResult.dispatchUpdatesTo(this);
    }
    
    /**
     * DiffUtil回调，用于高效的列表更新
     */
    private static class QuestionDiffCallback extends DiffUtil.Callback {
        private final List<QuestionEntity> oldList;
        private final List<QuestionEntity> newList;
        
        public QuestionDiffCallback(List<QuestionEntity> oldList, List<QuestionEntity> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }
        
        @Override
        public int getOldListSize() {
            return oldList != null ? oldList.size() : 0;
        }
        
        @Override
        public int getNewListSize() {
            return newList != null ? newList.size() : 0;
        }
        
        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            QuestionEntity oldQuestion = oldList.get(oldItemPosition);
            QuestionEntity newQuestion = newList.get(newItemPosition);
            return oldQuestion.getId() == newQuestion.getId();
        }
        
        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            QuestionEntity oldQuestion = oldList.get(oldItemPosition);
            QuestionEntity newQuestion = newList.get(newItemPosition);
            
            // 比较所有相关字段
            return oldQuestion.getId() == newQuestion.getId() &&
                   oldQuestion.getUserId() == newQuestion.getUserId() &&
                   (oldQuestion.getTutorId() == null ? newQuestion.getTutorId() == null : 
                    oldQuestion.getTutorId().equals(newQuestion.getTutorId())) &&
                   oldQuestion.getContent().equals(newQuestion.getContent()) &&
                   (oldQuestion.getImagePaths() == null ? newQuestion.getImagePaths() == null : 
                    oldQuestion.getImagePaths().equals(newQuestion.getImagePaths())) &&
                   oldQuestion.getStatus().equals(newQuestion.getStatus()) &&
                   oldQuestion.getCreatedAt() == newQuestion.getCreatedAt() &&
                   oldQuestion.getUpdatedAt() == newQuestion.getUpdatedAt();
        }
    }
    
    /**
     * 刷新未读数量（当收到新消息时调用）
     */
    public void refreshUnreadCounts() {
        notifyDataSetChanged();
    }
    
    public void showLoadingFooter() {
        if (!showLoadingFooter) {
            showLoadingFooter = true;
            showRetryFooter = false;
            notifyItemInserted(questions.size());
        }
    }
    
    public void hideLoadingFooter() {
        if (showLoadingFooter) {
            showLoadingFooter = false;
            notifyItemRemoved(questions.size());
        }
    }
    
    public void showRetryFooter() {
        if (!showRetryFooter) {
            hideLoadingFooter();
            showRetryFooter = true;
            notifyItemInserted(questions.size());
        }
    }
    
    public void hideRetryFooter() {
        if (showRetryFooter) {
            showRetryFooter = false;
            notifyItemRemoved(questions.size());
        }
    }
    
    @Override
    public int getItemViewType(int position) {
        if (position == questions.size() && (showLoadingFooter || showRetryFooter)) {
            return VIEW_TYPE_LOADING;
        }
        return VIEW_TYPE_QUESTION;
    }
    
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_LOADING) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_loading_footer, parent, false);
            return new BaseLoadingFooterViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_question, parent, false);
            return new QuestionViewHolder(view);
        }
    }
    
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof QuestionViewHolder) {
            QuestionEntity question = questions.get(position);
            ((QuestionViewHolder) holder).bind(question, listener, messageDao, currentUserId, executor);
        } else if (holder instanceof BaseLoadingFooterViewHolder) {
            ((BaseLoadingFooterViewHolder) holder).bind(showRetryFooter, retryListener);
        }
    }
    
    @Override
    public int getItemCount() {
        return questions.size() + (showLoadingFooter || showRetryFooter ? 1 : 0);
    }
    
    static class QuestionViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvContent;
        private final TextView tvStatus;
        private final TextView tvDate;
        private final TextView tvUnreadBadge;
        
        public QuestionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvUnreadBadge = itemView.findViewById(R.id.tvUnreadBadge);
        }
        
        public void bind(QuestionEntity question, OnQuestionClickListener listener, MessageDao messageDao, long currentUserId, ExecutorService executor) {
            if (question == null) {
                return;
            }
            
            String content = question.getContent();
            tvContent.setText(content != null ? content : "");
            
            String status = question.getStatus();
            tvStatus.setText(getStatusText(status != null ? status : QuestionStatus.PENDING));
            
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            tvDate.setText(sdf.format(new Date(question.getCreatedAt())));
            
            // 显示未读消息数量
            if (messageDao != null && currentUserId > 0 && executor != null) {
                executor.execute(() -> {
                    int unreadCount = messageDao.getUnreadMessageCount(question.getId(), currentUserId);
                    com.dowdah.asknow.utils.ThreadUtils.executeOnMain(() -> {
                        if (unreadCount > 0) {
                            tvUnreadBadge.setText(String.valueOf(Math.min(unreadCount, com.dowdah.asknow.constants.AppConstants.MAX_UNREAD_BADGE_COUNT)));
                            tvUnreadBadge.setVisibility(View.VISIBLE);
                        } else {
                            tvUnreadBadge.setVisibility(View.GONE);
                        }
                    });
                });
            } else {
                tvUnreadBadge.setVisibility(View.GONE);
            }
            
            if (listener != null) {
                itemView.setOnClickListener(v -> listener.onQuestionClick(question));
            }
        }
        
        private String getStatusText(String status) {
            switch (status) {
                case QuestionStatus.PENDING:
                    return itemView.getContext().getString(R.string.status_pending);
                case QuestionStatus.IN_PROGRESS:
                    return itemView.getContext().getString(R.string.status_in_progress);
                case QuestionStatus.CLOSED:
                    return itemView.getContext().getString(R.string.status_closed);
                default:
                    return status;
            }
        }
    }
}

