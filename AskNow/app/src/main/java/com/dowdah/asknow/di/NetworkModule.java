package com.dowdah.asknow.di;

import android.content.Context;

import com.dowdah.asknow.BuildConfig;
import com.dowdah.asknow.data.api.ApiService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@Module
@InstallIn(SingletonComponent.class)
public class NetworkModule {
    
    // 从 BuildConfig 读取后端地址，根据构建类型自动切换
    private static final String BASE_URL = BuildConfig.BASE_URL;
    
    @Provides
    @Singleton
    public Gson provideGson() {
        return new GsonBuilder()
            .setLenient()
            .create();
    }
    
    @Provides
    @Singleton
    public HttpLoggingInterceptor provideLoggingInterceptor() {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        return interceptor;
    }
    
    @Provides
    @Singleton
    public OkHttpClient provideOkHttpClient(HttpLoggingInterceptor loggingInterceptor) {
        return new OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            // 连接超时
            .connectTimeout(15, TimeUnit.SECONDS)
            // 读取超时
            .readTimeout(30, TimeUnit.SECONDS)
            // 写入超时  
            .writeTimeout(30, TimeUnit.SECONDS)
            // 调用超时（整个请求的总超时）
            .callTimeout(60, TimeUnit.SECONDS)
            // 连接失败重试
            .retryOnConnectionFailure(true)
            // 连接池配置：最多5个空闲连接，保持5分钟
            .connectionPool(new okhttp3.ConnectionPool(5, 5, TimeUnit.MINUTES))
            // Ping间隔，保持WebSocket连接活跃
            .pingInterval(30, TimeUnit.SECONDS)
            .build();
    }
    
    @Provides
    @Singleton
    public Retrofit provideRetrofit(OkHttpClient okHttpClient, Gson gson) {
        return new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build();
    }
    
    @Provides
    @Singleton
    public ApiService provideApiService(Retrofit retrofit) {
        return retrofit.create(ApiService.class);
    }
    
    @Provides
    @Singleton
    public String provideWebSocketUrl() {
        // Convert HTTP URL to WebSocket URL
        return BASE_URL.replace("http://", "ws://").replace("https://", "wss://") + "ws/";
    }
}

