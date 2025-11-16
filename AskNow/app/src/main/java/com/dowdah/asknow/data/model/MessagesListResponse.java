package com.dowdah.asknow.data.model;

import java.util.List;

public class MessagesListResponse {
    private boolean success;
    private List<MessageData> messages;
    private Pagination pagination;
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public List<MessageData> getMessages() {
        return messages;
    }
    
    public void setMessages(List<MessageData> messages) {
        this.messages = messages;
    }
    
    public Pagination getPagination() {
        return pagination;
    }
    
    public void setPagination(Pagination pagination) {
        this.pagination = pagination;
    }
    
    public static class MessageData {
        private long id;
        private long questionId;
        private long senderId;
        private String content;
        private String messageType;
        private long createdAt;
        private boolean isRead;
        
        public long getId() {
            return id;
        }
        
        public void setId(long id) {
            this.id = id;
        }
        
        public long getQuestionId() {
            return questionId;
        }
        
        public void setQuestionId(long questionId) {
            this.questionId = questionId;
        }
        
        public long getSenderId() {
            return senderId;
        }
        
        public void setSenderId(long senderId) {
            this.senderId = senderId;
        }
        
        public String getContent() {
            return content;
        }
        
        public void setContent(String content) {
            this.content = content;
        }
        
        public String getMessageType() {
            return messageType;
        }
        
        public void setMessageType(String messageType) {
            this.messageType = messageType;
        }
        
        public long getCreatedAt() {
            return createdAt;
        }
        
        public void setCreatedAt(long createdAt) {
            this.createdAt = createdAt;
        }
        
        public boolean isRead() {
            return isRead;
        }
        
        public void setRead(boolean read) {
            isRead = read;
        }
    }
}

