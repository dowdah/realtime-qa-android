package com.dowdah.asknow.data.model;

import java.util.List;

public class QuestionResponse {
    private boolean success;
    private String message;
    private QuestionData question;

    public QuestionResponse() {
    }

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

    public QuestionData getQuestion() {
        return question;
    }

    public void setQuestion(QuestionData question) {
        this.question = question;
    }

    public static class QuestionData {
        private long id;
        private long userId;
        private String content;
        private List<String> imagePaths;
        private String status;
        private long createdAt;

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

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public List<String> getImagePaths() {
            return imagePaths;
        }

        public void setImagePaths(List<String> imagePaths) {
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
    }
}

