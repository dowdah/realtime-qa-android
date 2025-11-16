package com.dowdah.asknow.data.model;

public class MessageRequest {
    private long questionId;
    private String content;
    private String messageType;
    
    public MessageRequest(long questionId, String content, String messageType) {
        this.questionId = questionId;
        this.content = content;
        this.messageType = messageType;
    }
    
    public long getQuestionId() {
        return questionId;
    }
    
    public void setQuestionId(long questionId) {
        this.questionId = questionId;
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
}

