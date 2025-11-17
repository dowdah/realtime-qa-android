package com.dowdah.asknow.ui.adapter;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.dowdah.asknow.R;

import java.util.ArrayList;
import java.util.List;

/**
 * 图片预览适配器，用于显示和删除图片
 */
public class ImagePreviewAdapter extends RecyclerView.Adapter<ImagePreviewAdapter.ViewHolder> {
    
    private List<Uri> imageUris = new ArrayList<>();
    private OnImageRemoveListener removeListener;
    private OnImageClickListener clickListener;
    
    public interface OnImageRemoveListener {
        void onImageRemove(int position);
    }
    
    public interface OnImageClickListener {
        void onImageClick(int position, Uri imageUri);
    }
    
    public ImagePreviewAdapter() {
    }
    
    public void setRemoveListener(OnImageRemoveListener listener) {
        this.removeListener = listener;
    }
    
    public void setClickListener(OnImageClickListener listener) {
        this.clickListener = listener;
    }
    
    public void setImages(List<Uri> images) {
        this.imageUris = new ArrayList<>(images);
        notifyDataSetChanged();
    }
    
    public List<Uri> getImages() {
        return new ArrayList<>(imageUris);
    }
    
    public void addImage(Uri imageUri) {
        imageUris.add(imageUri);
        notifyItemInserted(imageUris.size() - 1);
    }
    
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
        
        // 使用 Glide 加载图片
        Glide.with(holder.itemView.getContext())
                .load(imageUri)
                .centerCrop()
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_error)
                .into(holder.imageView);
        
        // 删除按钮点击事件
        holder.btnRemove.setOnClickListener(v -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                if (removeListener != null) {
                    removeListener.onImageRemove(adapterPosition);
                }
                removeImage(adapterPosition);
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


