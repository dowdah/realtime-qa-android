package com.dowdah.asknow.ui.adapter;

import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dowdah.asknow.R;

/**
 * 加载更多底部ViewHolder基类
 * 统一处理列表底部的加载状态显示和重试逻辑
 * 
 * 功能：
 * - 显示加载中状态
 * - 显示重试按钮
 * - 处理重试点击事件
 */
public class BaseLoadingFooterViewHolder extends RecyclerView.ViewHolder {
    
    private final ProgressBar progressBar;
    private final TextView tvLoadingText;
    private final TextView tvRetry;
    
    /**
     * 重试点击监听器接口
     */
    public interface OnRetryClickListener {
        void onRetryClick();
    }
    
    public BaseLoadingFooterViewHolder(@NonNull View itemView) {
        super(itemView);
        progressBar = itemView.findViewById(R.id.progressBar);
        tvLoadingText = itemView.findViewById(R.id.tvLoadingText);
        tvRetry = itemView.findViewById(R.id.tvRetry);
    }
    
    /**
     * 绑定加载状态
     * 
     * @param showRetry 是否显示重试按钮
     * @param retryListener 重试点击监听器
     */
    public void bind(boolean showRetry, @NonNull OnRetryClickListener retryListener) {
        if (showRetry) {
            // 显示重试状态
            progressBar.setVisibility(View.GONE);
            tvLoadingText.setVisibility(View.GONE);
            tvRetry.setVisibility(View.VISIBLE);
            tvRetry.setOnClickListener(v -> {
                if (retryListener != null) {
                    retryListener.onRetryClick();
                }
            });
        } else {
            // 显示加载中状态
            progressBar.setVisibility(View.VISIBLE);
            tvLoadingText.setVisibility(View.VISIBLE);
            tvRetry.setVisibility(View.GONE);
        }
    }
}

