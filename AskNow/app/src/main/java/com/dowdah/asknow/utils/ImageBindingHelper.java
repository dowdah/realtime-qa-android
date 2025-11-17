package com.dowdah.asknow.utils;

import android.content.Context;
import android.net.Uri;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.dowdah.asknow.BuildConfig;
import com.dowdah.asknow.R;
import com.dowdah.asknow.constants.AppConstants;

/**
 * 图片加载辅助工具类
 * 统一管理应用中的图片加载逻辑，使用Glide实现
 * 
 * 功能：
 * - 加载网络图片（从服务器URL）
 * - 加载本地图片（从Uri）
 * - 统一的占位符和错误处理
 * - 优化的缩略图加载
 */
public final class ImageBindingHelper {
    
    /**
     * 私有构造函数，防止实例化
     */
    private ImageBindingHelper() {
        throw new AssertionError("Cannot instantiate utility class");
    }
    
    /**
     * 加载服务器图片到ImageView（标准尺寸）
     * 
     * @param context 上下文
     * @param imagePath 图片路径（服务器相对路径）
     * @param imageView 目标ImageView
     */
    public static void loadServerImage(
        @NonNull Context context,
        @Nullable String imagePath,
        @NonNull ImageView imageView
    ) {
        if (imagePath == null || imagePath.isEmpty()) {
            imageView.setImageResource(R.drawable.ic_image_placeholder);
            return;
        }
        
        String baseUrl = BuildConfig.BASE_URL.replaceAll("/$", "");
        String imageUrl = baseUrl + imagePath;
        
        Glide.with(context)
            .load(imageUrl)
            .centerCrop()
            .placeholder(R.drawable.ic_image_placeholder)
            .error(R.drawable.ic_image_error)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(imageView);
    }
    
    /**
     * 加载服务器图片缩略图到ImageView（优化尺寸）
     * 
     * @param context 上下文
     * @param imagePath 图片路径（服务器相对路径）
     * @param imageView 目标ImageView
     */
    public static void loadServerImageThumbnail(
        @NonNull Context context,
        @Nullable String imagePath,
        @NonNull ImageView imageView
    ) {
        if (imagePath == null || imagePath.isEmpty()) {
            imageView.setImageResource(R.drawable.ic_image_placeholder);
            return;
        }
        
        String baseUrl = BuildConfig.BASE_URL.replaceAll("/$", "");
        String imageUrl = baseUrl + imagePath;
        
        // 计算缩略图尺寸（dp转px）
        int thumbnailSizePx = (int) (AppConstants.IMAGE_THUMBNAIL_SIZE_DP * 
            context.getResources().getDisplayMetrics().density);
        
        Glide.with(context)
            .load(imageUrl)
            .override(thumbnailSizePx, thumbnailSizePx)
            .centerCrop()
            .placeholder(R.drawable.ic_image_placeholder)
            .error(R.drawable.ic_image_error)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(imageView);
    }
    
    /**
     * 加载本地图片到ImageView（从Uri）
     * 
     * @param context 上下文
     * @param imageUri 图片Uri
     * @param imageView 目标ImageView
     */
    public static void loadLocalImage(
        @NonNull Context context,
        @Nullable Uri imageUri,
        @NonNull ImageView imageView
    ) {
        if (imageUri == null) {
            imageView.setImageResource(R.drawable.ic_image_placeholder);
            return;
        }
        
        Glide.with(context)
            .load(imageUri)
            .centerCrop()
            .placeholder(R.drawable.ic_image_placeholder)
            .error(R.drawable.ic_image_error)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .into(imageView);
    }
    
    /**
     * 加载消息中的图片到ImageView
     * 专门用于聊天消息中的图片显示
     * 
     * @param context 上下文
     * @param imagePath 图片路径（服务器相对路径）
     * @param imageView 目标ImageView
     */
    public static void loadMessageImage(
        @NonNull Context context,
        @Nullable String imagePath,
        @NonNull ImageView imageView
    ) {
        if (imagePath == null || imagePath.isEmpty()) {
            imageView.setImageResource(R.drawable.ic_image_placeholder);
            return;
        }
        
        String baseUrl = BuildConfig.BASE_URL.replaceAll("/$", "");
        String imageUrl = baseUrl + imagePath;
        
        Glide.with(context)
            .load(imageUrl)
            .centerCrop()
            .placeholder(R.drawable.ic_image_placeholder)
            .error(R.drawable.ic_image_error)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(imageView);
    }
}

