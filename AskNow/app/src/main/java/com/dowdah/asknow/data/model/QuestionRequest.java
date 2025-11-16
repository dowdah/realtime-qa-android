package com.dowdah.asknow.data.model;

public class QuestionRequest {
    private String content;
    private String imagePath;

    public QuestionRequest() {
    }

    public QuestionRequest(String content, String imagePath) {
        this.content = content;
        this.imagePath = imagePath;
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
}

