package com.dowdah.asknow.data.local.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "pending_messages")
public class PendingMessageEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;
    
    private String messageType; // "QUESTION", "ANSWER", etc.
    private String payload; // JSON string
    private int retryCount;
    private long createdAt;
    private String messageId; // UUID for tracking

    public PendingMessageEntity() {
    }

    @Ignore
    public PendingMessageEntity(String messageType, String payload, int retryCount, long createdAt, String messageId) {
        this.messageType = messageType;
        this.payload = payload;
        this.retryCount = retryCount;
        this.createdAt = createdAt;
        this.messageId = messageId;
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
}

