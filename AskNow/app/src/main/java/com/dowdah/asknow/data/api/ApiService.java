package com.dowdah.asknow.data.api;

import com.dowdah.asknow.data.model.LoginRequest;
import com.dowdah.asknow.data.model.LoginResponse;
import com.dowdah.asknow.data.model.MessageRequest;
import com.dowdah.asknow.data.model.MessageResponse;
import com.dowdah.asknow.data.model.MessagesListResponse;
import com.dowdah.asknow.data.model.QuestionRequest;
import com.dowdah.asknow.data.model.QuestionResponse;
import com.dowdah.asknow.data.model.QuestionsListResponse;
import com.dowdah.asknow.data.model.RegisterRequest;
import com.dowdah.asknow.data.model.RegisterResponse;
import com.dowdah.asknow.data.model.UploadResponse;
import com.google.gson.JsonObject;

import java.util.List;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;

public interface ApiService {
    
    @POST("api/register")
    Call<RegisterResponse> register(@Body RegisterRequest request);
    
    @POST("api/login")
    Call<LoginResponse> login(@Body LoginRequest request);
    
    @GET("api/questions")
    Call<QuestionsListResponse> getQuestions(
        @Header("Authorization") String token,
        @Query("status") String status,
        @Query("page") int page,
        @Query("page_size") int pageSize
    );
    
    @POST("api/questions")
    Call<QuestionResponse> createQuestion(
        @Header("Authorization") String token,
        @Body QuestionRequest request
    );
    
    @Multipart
    @POST("api/upload")
    Call<UploadResponse> uploadImage(
        @Header("Authorization") String token,
        @Part MultipartBody.Part image
    );
    
    @POST("api/messages")
    Call<MessageResponse> sendMessage(
        @Header("Authorization") String token,
        @Body MessageRequest request
    );
    
    @GET("api/messages")
    Call<MessagesListResponse> getMessages(
        @Header("Authorization") String token,
        @Query("questionId") long questionId,
        @Query("page") int page,
        @Query("page_size") int pageSize
    );
    
    @POST("api/messages/mark-read")
    Call<JsonObject> markMessagesAsRead(
        @Header("Authorization") String token,
        @Body JsonObject request
    );
    
    @POST("api/questions/accept")
    Call<JsonObject> acceptQuestion(
        @Header("Authorization") String token,
        @Body JsonObject request
    );
    
    @POST("api/questions/close")
    Call<JsonObject> closeQuestion(
        @Header("Authorization") String token,
        @Body JsonObject request
    );
}

