package com.dowdah.asknow.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.dowdah.asknow.BuildConfig;
import com.dowdah.asknow.R;
import com.dowdah.asknow.data.local.entity.MessageEntity;
import com.dowdah.asknow.databinding.ItemMessageReceivedBinding;
import com.dowdah.asknow.databinding.ItemMessageSentBinding;

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
    private OnRetryClickListener retryListener;
    private OnImageClickListener imageClickListener;
    
    public interface OnRetryClickListener {
        void onRetryClick();
    }
    
    public interface OnImageClickListener {
        void onImageClick(String imagePath);
    }
    
    public MessageAdapter(long currentUserId) {
        this.currentUserId = currentUserId;
    }
    
    public void setImageClickListener(OnImageClickListener listener) {
        this.imageClickListener = listener;
    }
    
    public void setRetryListener(OnRetryClickListener retryListener) {
        this.retryListener = retryListener;
    }
    
    public void setMessages(List<MessageEntity> newMessages) {
        if (newMessages == null) {
            newMessages = new ArrayList<>();
        }
        
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new MessageDiffCallback(this.messages, newMessages));
        this.messages = new ArrayList<>(newMessages);
        diffResult.dispatchUpdatesTo(this);
    }
    
    /**
     * DiffUtil Callback for efficient list updates
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
            return new LoadingFooterViewHolder(view);
        }
    }
    
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof LoadingFooterViewHolder) {
            ((LoadingFooterViewHolder) holder).bind(showRetryFooter, retryListener);
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
    
    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        private final ItemMessageSentBinding binding;
        
        SentMessageViewHolder(ItemMessageSentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
        
        void bind(MessageEntity message, OnImageClickListener imageClickListener) {
            if (message == null) {
                return;
            }
            
            String messageType = message.getMessageType();
            String content = message.getContent();
            
            if ("image".equals(messageType)) {
                // 显示图片消息
                binding.tvMessage.setVisibility(View.GONE);
                binding.ivMessageImage.setVisibility(View.VISIBLE);
                
                String imageUrl = BuildConfig.BASE_URL.replaceAll("/$", "") + content;
                Glide.with(binding.getRoot().getContext())
                    .load(imageUrl)
                    .centerCrop()
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_error)
                    .into(binding.ivMessageImage);
                
                // 添加图片点击事件
                binding.ivMessageImage.setOnClickListener(v -> {
                    if (imageClickListener != null) {
                        imageClickListener.onImageClick(content);
                    }
                });
            } else {
                // 显示文本消息
                binding.ivMessageImage.setVisibility(View.GONE);
                binding.tvMessage.setVisibility(View.VISIBLE);
                binding.tvMessage.setText(content != null ? content : "");
            }
        }
    }
    
    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        private final ItemMessageReceivedBinding binding;
        
        ReceivedMessageViewHolder(ItemMessageReceivedBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
        
        void bind(MessageEntity message, OnImageClickListener imageClickListener) {
            if (message == null) {
                return;
            }
            
            String messageType = message.getMessageType();
            String content = message.getContent();
            
            if ("image".equals(messageType)) {
                // 显示图片消息
                binding.tvMessage.setVisibility(View.GONE);
                binding.ivMessageImage.setVisibility(View.VISIBLE);
                
                String imageUrl = BuildConfig.BASE_URL.replaceAll("/$", "") + content;
                Glide.with(binding.getRoot().getContext())
                    .load(imageUrl)
                    .centerCrop()
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_error)
                    .into(binding.ivMessageImage);
                
                // 添加图片点击事件
                binding.ivMessageImage.setOnClickListener(v -> {
                    if (imageClickListener != null) {
                        imageClickListener.onImageClick(content);
                    }
                });
            } else {
                // 显示文本消息
                binding.ivMessageImage.setVisibility(View.GONE);
                binding.tvMessage.setVisibility(View.VISIBLE);
                binding.tvMessage.setText(content != null ? content : "");
            }
            
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
    
    static class LoadingFooterViewHolder extends RecyclerView.ViewHolder {
        private final ProgressBar progressBar;
        private final TextView tvLoadingText;
        private final TextView tvRetry;
        
        public LoadingFooterViewHolder(@NonNull View itemView) {
            super(itemView);
            progressBar = itemView.findViewById(R.id.progressBar);
            tvLoadingText = itemView.findViewById(R.id.tvLoadingText);
            tvRetry = itemView.findViewById(R.id.tvRetry);
        }
        
        public void bind(boolean showRetry, OnRetryClickListener retryListener) {
            if (showRetry) {
                progressBar.setVisibility(View.GONE);
                tvLoadingText.setVisibility(View.GONE);
                tvRetry.setVisibility(View.VISIBLE);
                tvRetry.setOnClickListener(v -> {
                    if (retryListener != null) {
                        retryListener.onRetryClick();
                    }
                });
            } else {
                progressBar.setVisibility(View.VISIBLE);
                tvLoadingText.setVisibility(View.VISIBLE);
                tvRetry.setVisibility(View.GONE);
            }
        }
    }
}

