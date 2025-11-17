package com.dowdah.asknow.di;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

/**
 * ExecutorModule - 提供统一的线程池管理
 * 
 * 提供的线程池类型：
 * - io: 用于IO密集型操作（网络请求、文件读写、数据库操作）
 * - computation: 用于CPU密集型操作（数据处理、图片处理）
 * - single: 用于需要顺序执行的操作
 * - scheduled: 用于需要延迟或定期执行的操作
 */
@Module
@InstallIn(SingletonComponent.class)
public class ExecutorModule {
    
    /**
     * IO线程池：用于网络请求、文件读写、数据库操作
     * 使用固定大小的线程池，避免创建过多线程
     */
    @Provides
    @Singleton
    @Named("io")
    public ExecutorService provideIOExecutor() {
        // 使用固定线程池，线程数为CPU核心数 * 2 + 1
        int threadCount = Runtime.getRuntime().availableProcessors() * 2 + 1;
        return Executors.newFixedThreadPool(threadCount);
    }
    
    /**
     * 计算线程池：用于CPU密集型操作
     * 线程数量等于CPU核心数
     */
    @Provides
    @Singleton
    @Named("computation")
    public ExecutorService provideComputationExecutor() {
        int threadCount = Runtime.getRuntime().availableProcessors();
        return Executors.newFixedThreadPool(threadCount);
    }
    
    /**
     * 单线程池：用于需要顺序执行的操作
     * 保证任务按顺序执行，避免并发问题
     */
    @Provides
    @Singleton
    @Named("single")
    public ExecutorService provideSingleThreadExecutor() {
        return Executors.newSingleThreadExecutor();
    }
    
    /**
     * 定时任务线程池：用于需要延迟或定期执行的操作
     * 例如：心跳检测、自动同步等
     */
    @Provides
    @Singleton
    @Named("scheduled")
    public ScheduledExecutorService provideScheduledExecutor() {
        // 使用单线程的定时任务线程池
        return Executors.newSingleThreadScheduledExecutor();
    }
}

