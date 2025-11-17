package com.dowdah.asknow.data.model;

import java.util.List;

public class QuestionRequest {
    private String content;
    private List<String> imagePaths;

    public QuestionRequest() {
    }

    public QuestionRequest(String content, List<String> imagePaths) {
        this.content = content;
        this.imagePaths = imagePaths;
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
}

