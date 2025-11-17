package com.dowdah.asknow.ui.adapter;

import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.dowdah.asknow.BuildConfig;
import com.dowdah.asknow.R;

import java.util.ArrayList;
import java.util.List;

/**
 * 图片显示适配器，用于只读显示图片（不支持删除）
 */
public class ImageDisplayAdapter extends RecyclerView.Adapter<ImageDisplayAdapter.ViewHolder> {
    
    private static final String TAG = "ImageDisplayAdapter";
    
    private List<String> imagePaths = new ArrayList<>();
    private OnImageClickListener clickListener;
    
    public interface OnImageClickListener {
        void onImageClick(int position, String imagePath);
    }
    
    public ImageDisplayAdapter() {
    }
    
    public void setClickListener(OnImageClickListener listener) {
        this.clickListener = listener;
    }
    
    public void setImages(List<String> images) {
        this.imagePaths = new ArrayList<>(images);
        notifyDataSetChanged();
    }
    
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
        
        // 构建完整的图片 URL
        String baseUrl = BuildConfig.BASE_URL.replaceAll("/$", "");
        String imageUrl = baseUrl + imagePath;
        
        // 计算图片尺寸（120dp转换为px）
        int imageSizePx = (int) (120 * holder.itemView.getContext().getResources().getDisplayMetrics().density);
        
        // 使用优化的 Glide 加载配置
        Glide.with(holder.itemView.getContext())
                .load(imageUrl)
                .override(imageSizePx, imageSizePx)  // 限制图片尺寸，避免加载原图
                .centerCrop()
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_error)
                .into(holder.imageView);
        
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


