package com.dowdah.asknow.data.model;

import com.google.gson.JsonObject;

public class WebSocketMessage {
    private String type; // "NEW_QUESTION", "NEW_ANSWER", "ACK", etc.
    private JsonObject data;
    private String timestamp;
    private String messageId;

    public WebSocketMessage() {
    }

    public WebSocketMessage(String type, JsonObject data, String timestamp, String messageId) {
        this.type = type;
        this.data = data;
        this.timestamp = timestamp;
        this.messageId = messageId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public JsonObject getData() {
        return data;
    }

    public void setData(JsonObject data) {
        this.data = data;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
}

