package com.dowdah.asknow.data.model;

public class MessageResponse {
    private boolean success;
    private String message;
    private MessageData data;
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public MessageData getData() {
        return data;
    }
    
    public void setData(MessageData data) {
        this.data = data;
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

