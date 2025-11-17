package com.dowdah.asknow.ui.image;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.dowdah.asknow.BuildConfig;
import com.dowdah.asknow.R;
import com.github.chrisbanes.photoview.PhotoView;

import java.util.ArrayList;
import java.util.List;

/**
 * 图片预览 ViewPager 适配器
 * 使用 PhotoView 提供流畅的缩放和拖动体验
 */
public class ImagePreviewAdapter extends RecyclerView.Adapter<ImagePreviewAdapter.ViewHolder> {
    
    private List<String> imagePaths = new ArrayList<>();
    
    public void setImages(List<String> images) {
        this.imagePaths = new ArrayList<>(images);
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_image_preview_page, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String imagePath = imagePaths.get(position);
        
        // 构建完整的图片 URL
        String baseUrl = BuildConfig.BASE_URL.replaceAll("/$", "");
        String imageUrl = baseUrl + imagePath;
        
        holder.progressBar.setVisibility(View.VISIBLE);
        
        // 配置 PhotoView 的缩放参数
        holder.imageView.setMaximumScale(4.0f);  // 最大缩放4倍
        holder.imageView.setMediumScale(2.0f);   // 中等缩放2倍（双击目标）
        holder.imageView.setMinimumScale(1.0f);  // 最小缩放1倍（自适应大小）
        
        // 使用 Glide 加载高清大图
        Glide.with(holder.itemView.getContext())
                .load(imageUrl)
                .transition(DrawableTransitionOptions.withCrossFade())
                .error(R.drawable.ic_image_error)
                .into(holder.imageView);
        
        // 监听图片加载完成后隐藏进度条
        holder.imageView.post(() -> holder.progressBar.setVisibility(View.GONE));
    }
    
    @Override
    public int getItemCount() {
        return imagePaths.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        PhotoView imageView;
        ProgressBar progressBar;
        
        ViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.ivPreview);
            progressBar = itemView.findViewById(R.id.progressBar);
        }
    }
}

