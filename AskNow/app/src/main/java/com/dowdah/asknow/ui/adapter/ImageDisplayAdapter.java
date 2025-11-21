package com.dowdah.asknow.ui.adapter;

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
 * 图片显示适配器，用于只读显示图片（不支持删除）
 */
public class ImageDisplayAdapter extends RecyclerView.Adapter<ImageDisplayAdapter.ViewHolder> {
    
    private List<String> imagePaths = new ArrayList<>();
    private OnImageClickListener clickListener;
    
    /**
     * 图片点击监听器接口
     */
    public interface OnImageClickListener {
        void onImageClick(int position, @NonNull String imagePath);
    }
    
    public ImageDisplayAdapter() {
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
     * @param images 图片路径列表
     */
    public void setImages(@NonNull List<String> images) {
        this.imagePaths = new ArrayList<>(images);
        notifyDataSetChanged();
    }
    
    /**
     * 获取图片列表
     * 
     * @return 图片路径列表的副本
     */
    @NonNull
    public List<String> getImages() {
        return new ArrayList<>(imagePaths);
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_image_display, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String imagePath = imagePaths.get(position);
        
        // 使用ImageBindingHelper加载缩略图
        ImageBindingHelper.loadServerImageThumbnail(holder.itemView.getContext(), imagePath, holder.imageView);
        
        // 图片点击事件
        holder.imageView.setOnClickListener(v -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION && clickListener != null) {
                clickListener.onImageClick(adapterPosition, imagePath);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return imagePaths.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        
        ViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.ivImage);
        }
    }
}


