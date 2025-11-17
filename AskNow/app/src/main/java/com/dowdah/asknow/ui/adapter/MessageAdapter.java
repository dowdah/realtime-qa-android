package com.dowdah.asknow.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.dowdah.asknow.R;
import com.dowdah.asknow.data.local.entity.MessageEntity;
import com.dowdah.asknow.databinding.ItemMessageReceivedBinding;
import com.dowdah.asknow.databinding.ItemMessageSentBinding;
import com.dowdah.asknow.utils.ImageBindingHelper;

import java.util.ArrayList;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    
    private static final int TYPE_SENT = 1;
    private static final int TYPE_RECEIVED = 2;
    private static final int TYPE_LOADING = 3;
    
    private List<MessageEntity> messages = new ArrayList<>();
    private long currentUserId;
    private boolean showLoadingFooter = false;
    private boolean showRetryFooter = false;
    private BaseLoadingFooterViewHolder.OnRetryClickListener retryListener;
    private OnImageClickListener imageClickListener;
    
    /**
     * 图片点击监听器接口
     */
    public interface OnImageClickListener {
        void onImageClick(@NonNull String imagePath);
    }
    
    public MessageAdapter(long currentUserId) {
        this.currentUserId = currentUserId;
    }
    
    /**
     * 设置图片点击监听器
     * 
     * @param listener 图片点击监听器
     */
    public void setImageClickListener(@Nullable OnImageClickListener listener) {
        this.imageClickListener = listener;
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
     * 设置消息列表（使用DiffUtil优化）
     * 
     * @param newMessages 新的消息列表
     */
    public void setMessages(@Nullable List<MessageEntity> newMessages) {
        if (newMessages == null) {
            newMessages = new ArrayList<>();
        }
        
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new MessageDiffCallback(this.messages, newMessages));
        this.messages = new ArrayList<>(newMessages);
        diffResult.dispatchUpdatesTo(this);
    }
    
    /**
     * DiffUtil回调，用于高效的列表更新
     */
    private static class MessageDiffCallback extends DiffUtil.Callback {
        private final List<MessageEntity> oldList;
        private final List<MessageEntity> newList;
        
        public MessageDiffCallback(List<MessageEntity> oldList, List<MessageEntity> newList) {
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
            MessageEntity oldMessage = oldList.get(oldItemPosition);
            MessageEntity newMessage = newList.get(newItemPosition);
            return oldMessage.getId() == newMessage.getId();
        }
        
        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            MessageEntity oldMessage = oldList.get(oldItemPosition);
            MessageEntity newMessage = newList.get(newItemPosition);
            
            // 比较所有相关字段
            return oldMessage.getId() == newMessage.getId() &&
                   oldMessage.getQuestionId() == newMessage.getQuestionId() &&
                   oldMessage.getSenderId() == newMessage.getSenderId() &&
                   oldMessage.getContent().equals(newMessage.getContent()) &&
                   oldMessage.getMessageType().equals(newMessage.getMessageType()) &&
                   oldMessage.isRead() == newMessage.isRead() &&
                   (oldMessage.getSendStatus() == null ? newMessage.getSendStatus() == null : 
                    oldMessage.getSendStatus().equals(newMessage.getSendStatus()));
        }
    }
    
    public void showLoadingFooter() {
        if (!showLoadingFooter) {
            showLoadingFooter = true;
            showRetryFooter = false;
            notifyItemInserted(messages.size());
        }
    }
    
    public void hideLoadingFooter() {
        if (showLoadingFooter) {
            showLoadingFooter = false;
            notifyItemRemoved(messages.size());
        }
    }
    
    public void showRetryFooter() {
        if (!showRetryFooter) {
            hideLoadingFooter();
            showRetryFooter = true;
            notifyItemInserted(messages.size());
        }
    }
    
    public void hideRetryFooter() {
        if (showRetryFooter) {
            showRetryFooter = false;
            notifyItemRemoved(messages.size());
        }
    }
    
    @Override
    public int getItemViewType(int position) {
        if (position == messages.size() && (showLoadingFooter || showRetryFooter)) {
            return TYPE_LOADING;
        }
        MessageEntity message = messages.get(position);
        if (message.getSenderId() == currentUserId) {
            return TYPE_SENT;
        } else {
            return TYPE_RECEIVED;
        }
    }
    
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_SENT) {
            ItemMessageSentBinding binding = ItemMessageSentBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
            );
            return new SentMessageViewHolder(binding);
        } else if (viewType == TYPE_RECEIVED) {
            ItemMessageReceivedBinding binding = ItemMessageReceivedBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
            );
            return new ReceivedMessageViewHolder(binding);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_loading_footer, parent, false);
            return new BaseLoadingFooterViewHolder(view);
        }
    }
    
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof BaseLoadingFooterViewHolder) {
            ((BaseLoadingFooterViewHolder) holder).bind(showRetryFooter, retryListener);
        } else {
            MessageEntity message = messages.get(position);
            if (holder instanceof SentMessageViewHolder) {
                ((SentMessageViewHolder) holder).bind(message, imageClickListener);
            } else if (holder instanceof ReceivedMessageViewHolder) {
                ((ReceivedMessageViewHolder) holder).bind(message, imageClickListener);
            }
        }
    }
    
    @Override
    public int getItemCount() {
        return messages.size() + (showLoadingFooter || showRetryFooter ? 1 : 0);
    }
    
    /**
     * 绑定消息内容到视图（图片或文本）
     * 
     * @param message 消息实体
     * @param imageView 图片视图
     * @param textView 文本视图
     * @param imageClickListener 图片点击监听器
     */
    private static void bindMessageContent(
        @NonNull MessageEntity message,
        @NonNull ImageView imageView,
        @NonNull TextView textView,
        @Nullable OnImageClickListener imageClickListener
    ) {
        String messageType = message.getMessageType();
        String content = message.getContent();
        
        if ("image".equals(messageType)) {
            // 显示图片消息
            textView.setVisibility(View.GONE);
            imageView.setVisibility(View.VISIBLE);
            
            ImageBindingHelper.loadMessageImage(imageView.getContext(), content, imageView);
            
            // 添加图片点击事件
            imageView.setOnClickListener(v -> {
                if (imageClickListener != null) {
                    imageClickListener.onImageClick(content);
                }
            });
        } else {
            // 显示文本消息
            imageView.setVisibility(View.GONE);
            textView.setVisibility(View.VISIBLE);
            textView.setText(content != null ? content : "");
        }
    }
    
    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        private final ItemMessageSentBinding binding;
        
        SentMessageViewHolder(ItemMessageSentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
        
        /**
         * 绑定消息数据到视图
         * 
         * @param message 消息实体
         * @param imageClickListener 图片点击监听器
         */
        void bind(@Nullable MessageEntity message, @Nullable OnImageClickListener imageClickListener) {
            if (message == null) {
                return;
            }
            
            bindMessageContent(message, binding.ivMessageImage, binding.tvMessage, imageClickListener);
        }
    }
    
    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        private final ItemMessageReceivedBinding binding;
        
        ReceivedMessageViewHolder(ItemMessageReceivedBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
        
        /**
         * 绑定消息数据到视图
         * 
         * @param message 消息实体
         * @param imageClickListener 图片点击监听器
         */
        void bind(@Nullable MessageEntity message, @Nullable OnImageClickListener imageClickListener) {
            if (message == null) {
                return;
            }
            
            bindMessageContent(message, binding.ivMessageImage, binding.tvMessage, imageClickListener);
            
            // 显示未读标记
            View unreadIndicator = binding.getRoot().findViewById(R.id.unread_indicator);
            if (unreadIndicator != null) {
                if (message.isRead()) {
                    unreadIndicator.setVisibility(View.GONE);
                } else {
                    unreadIndicator.setVisibility(View.VISIBLE);
                }
            }
        }
    }
}

