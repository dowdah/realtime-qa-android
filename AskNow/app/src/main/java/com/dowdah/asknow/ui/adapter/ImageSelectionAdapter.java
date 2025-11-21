package com.dowdah.asknow.ui.adapter;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.dowdah.asknow.R;
import com.dowdah.asknow.utils.ImageBindingHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * 图片选择适配器，用于显示本地选择的图片并支持删除操作
 * 
 * 用途：在发布问题时，显示用户从相册或相机选择的本地图片列表
 * 功能：
 * - 显示本地图片（Uri）
 * - 支持删除图片
 * - 支持点击图片预览
 */
public class ImageSelectionAdapter extends RecyclerView.Adapter<ImageSelectionAdapter.ViewHolder> {
    
    private List<Uri> imageUris = new ArrayList<>();
    private OnImageRemoveListener removeListener;
    private OnImageClickListener clickListener;
    
    /**
     * 图片移除监听器接口
     */
    public interface OnImageRemoveListener {
        void onImageRemove(int position);
    }
    
    /**
     * 图片点击监听器接口
     */
    public interface OnImageClickListener {
        void onImageClick(int position, @NonNull Uri imageUri);
    }
    
    public ImageSelectionAdapter() {
    }
    
    /**
     * 设置图片移除监听器
     * 
     * @param listener 图片移除监听器
     */
    public void setRemoveListener(@Nullable OnImageRemoveListener listener) {
        this.removeListener = listener;
    }
    
    /**
     * 设置图片点击监听器
     * 
     * @param listener 图片点击监听器
     */
    public void setClickListener(@Nullable OnImageClickListener listener) {
        this.clickListener = listener;
    }
    
    /**
     * 设置图片列表
     * 
     * @param images 图片Uri列表
     */
    public void setImages(@NonNull List<Uri> images) {
        this.imageUris = new ArrayList<>(images);
        notifyDataSetChanged();
    }
    
    /**
     * 获取图片列表
     * 
     * @return 图片Uri列表的副本
     */
    @NonNull
    public List<Uri> getImages() {
        return new ArrayList<>(imageUris);
    }
    
    /**
     * 添加图片
     * 
     * @param imageUri 图片Uri
     */
    public void addImage(@NonNull Uri imageUri) {
        imageUris.add(imageUri);
        notifyItemInserted(imageUris.size() - 1);
    }
    
    /**
     * 移除图片
     * 
     * @param position 图片位置
     */
    public void removeImage(int position) {
        if (position >= 0 && position < imageUris.size()) {
            imageUris.remove(position);
            notifyItemRemoved(position);
        }
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_image_preview, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Uri imageUri = imageUris.get(position);
        
        // 使用ImageBindingHelper加载图片
        ImageBindingHelper.loadLocalImage(holder.itemView.getContext(), imageUri, holder.imageView);
        
        // 删除按钮点击事件
        holder.btnRemove.setOnClickListener(v -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                if (removeListener != null) {
                    removeListener.onImageRemove(adapterPosition);
                }
                // 不在这里直接调用removeImage，而是通过listener回调让Activity统一管理数据源
                // Activity会调用updateImagePreview()来刷新整个列表
            }
        });
        
        // 图片点击事件
        holder.imageView.setOnClickListener(v -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION && clickListener != null) {
                clickListener.onImageClick(adapterPosition, imageUri);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return imageUris.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageView btnRemove;
        
        ViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.ivPreview);
            btnRemove = itemView.findViewById(R.id.btnRemove);
        }
    }
}

