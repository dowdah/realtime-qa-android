package com.dowdah.asknow.data.local.entity;

import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "questions",
    indices = {
        @Index(value = "userId", name = "idx_questions_userId"),
        @Index(value = "status", name = "idx_questions_status"),
        @Index(value = "tutorId", name = "idx_questions_tutorId"),
        @Index(value = {"tutorId", "status"}, name = "idx_questions_tutorId_status"),
        @Index(value = "createdAt", name = "idx_questions_createdAt"),
        @Index(value = "updatedAt", name = "idx_questions_updatedAt")
    }
)
public class QuestionEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;
    
    private long userId;
    @Nullable
    private Long tutorId; // 接受问题的老师ID
    private String content;
    @Nullable
    private String imagePaths; // JSON 格式存储多图片路径
    private String status; // "pending", "in_progress", "closed"
    private long createdAt;
    private long updatedAt;

    public QuestionEntity() {
    }

    @Ignore
    public QuestionEntity(long userId, Long tutorId, String content, String imagePaths, String status, long createdAt, long updatedAt) {
        this.userId = userId;
        this.tutorId = tutorId;
        this.content = content;
        this.imagePaths = imagePaths;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public Long getTutorId() {
        return tutorId;
    }

    public void setTutorId(Long tutorId) {
        this.tutorId = tutorId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getImagePaths() {
        return imagePaths;
    }

    public void setImagePaths(String imagePaths) {
        this.imagePaths = imagePaths;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}

