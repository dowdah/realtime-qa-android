package com.dowdah.asknow.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "messages",
    foreignKeys = @ForeignKey(
        entity = QuestionEntity.class,
        parentColumns = "id",
        childColumns = "questionId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {@Index("questionId")}
)
public class MessageEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;
    
    private long questionId;
    private long senderId;
    private String content;
    private String messageType; // "text", "image"
    private long createdAt;
    private boolean isRead; // 消息是否已读
    private String sendStatus; // "pending", "sent", "failed"

    public MessageEntity() {
    }

    @Ignore
    public MessageEntity(long questionId, long senderId, String content, String messageType, long createdAt) {
        this.questionId = questionId;
        this.senderId = senderId;
        this.content = content;
        this.messageType = messageType;
        this.createdAt = createdAt;
        this.isRead = false; // 默认未读
        this.sendStatus = "sent"; // 默认已发送
    }

    // Getters and Setters
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

    public String getSendStatus() {
        return sendStatus;
    }

    public void setSendStatus(String sendStatus) {
        this.sendStatus = sendStatus;
    }
}

