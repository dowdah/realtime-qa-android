package com.dowdah.asknow.data.model;

import java.util.List;

public class QuestionsListResponse {
    private boolean success;
    private List<QuestionData> questions;
    private Pagination pagination;
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public List<QuestionData> getQuestions() {
        return questions;
    }
    
    public void setQuestions(List<QuestionData> questions) {
        this.questions = questions;
    }
    
    public Pagination getPagination() {
        return pagination;
    }
    
    public void setPagination(Pagination pagination) {
        this.pagination = pagination;
    }
    
    public static class QuestionData {
        private long id;
        private long userId;
        private Long tutorId;
        private String content;
        private String imagePath;
        private String status;
        private long createdAt;
        private long updatedAt;
        
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
        
        public String getImagePath() {
            return imagePath;
        }
        
        public void setImagePath(String imagePath) {
            this.imagePath = imagePath;
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
}

