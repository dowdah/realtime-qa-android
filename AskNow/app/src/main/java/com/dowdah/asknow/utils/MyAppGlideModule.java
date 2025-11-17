package com.dowdah.asknow.utils;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory;
import com.bumptech.glide.load.engine.cache.LruResourceCache;
import com.bumptech.glide.load.engine.cache.MemorySizeCalculator;
import com.bumptech.glide.request.RequestOptions;

/**
 * Glide 配置模块
 * 用于自定义 Glide 的全局配置，优化图片加载性能
 */
@GlideModule
public class MyAppGlideModule extends com.bumptech.glide.module.AppGlideModule {
    
    private static final String TAG = "MyAppGlideModule";
    private static final int DISK_CACHE_SIZE = 100 * 1024 * 1024; // 100 MB
    
    @Override
    public void applyOptions(@NonNull Context context, @NonNull GlideBuilder builder) {
        super.applyOptions(context, builder);
        
        // 设置日志级别为ERROR（生产环境）
        builder.setLogLevel(Log.ERROR);
        
        // 配置内存缓存
        MemorySizeCalculator calculator = new MemorySizeCalculator.Builder(context)
                .setMemoryCacheScreens(2) // 缓存2屏幕大小的图片
                .build();
        builder.setMemoryCache(new LruResourceCache(calculator.getMemoryCacheSize()));
        
        // 配置磁盘缓存
        builder.setDiskCache(new InternalCacheDiskCacheFactory(context, DISK_CACHE_SIZE));
        
        // 设置默认请求选项
        RequestOptions defaultOptions = new RequestOptions()
                .format(DecodeFormat.PREFER_RGB_565) // 使用 RGB_565 降低内存占用
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC) // 自动选择缓存策略
                .skipMemoryCache(false); // 启用内存缓存
        
        builder.setDefaultRequestOptions(defaultOptions);
        
        Log.d(TAG, "Glide module configured with optimized settings");
    }
    
    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
        super.registerComponents(context, glide, registry);
        // 可以在这里注册自定义组件
    }
    
    @Override
    public boolean isManifestParsingEnabled() {
        // 禁用清单解析以提高初始化速度
        return false;
    }
}

