package com.dowdah.asknow.data.api;

import android.util.Log;

import com.dowdah.asknow.data.model.WebSocketMessage;
import com.google.gson.Gson;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class WebSocketClient {
    private static final String TAG = "WebSocketClient";
    private static final int[] BACKOFF_DELAYS = {1000, 2000, 4000, 8000, 16000, 30000}; // milliseconds
    private static final int MAX_RETRY_COUNT = 10; // 最多重连10次
    
    private WebSocket webSocket;
    private final OkHttpClient client;
    private final String url;
    private final WebSocketCallback callback;
    private int retryCount = 0;
    private boolean isManuallyDisconnected = false;
    
    public interface WebSocketCallback {
        void onConnected();
        void onMessage(WebSocketMessage message);
        void onDisconnected();
        void onError(Throwable error);
    }
    
    public WebSocketClient(OkHttpClient client, String url, WebSocketCallback callback) {
        this.client = client;
        this.url = url;
        this.callback = callback;
    }
    
    public void connect() {
        if (webSocket != null) {
            Log.d(TAG, "WebSocket already connected");
            return;
        }
        
        Log.d(TAG, "Connecting to WebSocket: " + url);
        isManuallyDisconnected = false;
        
        Request request = new Request.Builder()
                .url(url)
                .build();
        
        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                Log.d(TAG, "WebSocket connected");
                retryCount = 0;
                callback.onConnected();
            }
            
            @Override
            public void onMessage(WebSocket webSocket, String text) {
                Log.d(TAG, "Received message: " + text);
                try {
                    Gson gson = new Gson();
                    WebSocketMessage message = gson.fromJson(text, WebSocketMessage.class);
                    callback.onMessage(message);
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing message", e);
                }
            }
            
            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                Log.d(TAG, "WebSocket closing: " + reason);
                webSocket.close(1000, null);
            }
            
            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                Log.d(TAG, "WebSocket closed: " + reason);
                WebSocketClient.this.webSocket = null;
                callback.onDisconnected();
                
                if (!isManuallyDisconnected) {
                    reconnect();
                }
            }
            
            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                Log.e(TAG, "WebSocket error", t);
                WebSocketClient.this.webSocket = null;
                callback.onError(t);
                callback.onDisconnected();
                
                if (!isManuallyDisconnected) {
                    reconnect();
                }
            }
        });
    }
    
    public void disconnect() {
        isManuallyDisconnected = true;
        if (webSocket != null) {
            webSocket.close(1000, "Manual disconnect");
            webSocket = null;
        }
    }
    
    public void sendMessage(WebSocketMessage message) {
        if (webSocket != null) {
            Gson gson = new Gson();
            String json = gson.toJson(message);
            webSocket.send(json);
            Log.d(TAG, "Sent message: " + json);
        } else {
            Log.w(TAG, "Cannot send message: WebSocket not connected");
        }
    }
    
    public boolean isConnected() {
        return webSocket != null;
    }
    
    private void reconnect() {
        // 检查是否超过最大重连次数
        if (retryCount >= MAX_RETRY_COUNT) {
            Log.e(TAG, "Max retry count reached. Giving up reconnection.");
            callback.onError(new Exception("Max retry count reached"));
            return;
        }
        
        int delay = BACKOFF_DELAYS[Math.min(retryCount, BACKOFF_DELAYS.length - 1)];
        Log.d(TAG, "Reconnecting in " + delay + "ms (attempt " + (retryCount + 1) + "/" + MAX_RETRY_COUNT + ")");
        
        retryCount++;
        
        // 使用新线程处理延迟重连，避免阻塞 WebSocket 回调线程
        new Thread(() -> {
            try {
                Thread.sleep(delay);
                if (!isManuallyDisconnected) {
                    connect();
                }
            } catch (InterruptedException e) {
                Log.w(TAG, "Reconnect delay interrupted", e);
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    /**
     * 重置重连计数器（当手动重连时使用）
     */
    public void resetRetryCount() {
        retryCount = 0;
    }
}

