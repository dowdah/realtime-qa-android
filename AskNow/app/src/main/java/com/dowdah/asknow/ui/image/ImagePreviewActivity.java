package com.dowdah.asknow.ui.image;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.dowdah.asknow.R;
import com.dowdah.asknow.databinding.ActivityImagePreviewBinding;

import java.util.ArrayList;

/**
 * 图片预览Activity
 * 支持多图片浏览、缩放、滑动
 */
public class ImagePreviewActivity extends AppCompatActivity {
    
    private ActivityImagePreviewBinding binding;
    private ImagePreviewAdapter adapter;
    private ArrayList<String> imagePaths;
    private int currentPosition;
    private boolean isToolbarVisible = true;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 设置窗口以支持edge-to-edge布局
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        
        binding = ActivityImagePreviewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // 获取传入的图片路径列表和当前位置
        Intent intent = getIntent();
        imagePaths = intent.getStringArrayListExtra("image_paths");
        currentPosition = intent.getIntExtra("position", 0);
        
        if (imagePaths == null || imagePaths.isEmpty()) {
            finish();
            return;
        }
        
        setupWindowInsets();
        setupToolbar();
        setupViewPager();
        updateIndicator();
    }
    
    /**
     * 设置窗口insets以适应系统栏
     */
    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.appBarLayout, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            // 为AppBarLayout设置顶部padding以避免与状态栏重叠
            v.setPadding(
                    v.getPaddingLeft(),
                    insets.top,
                    v.getPaddingRight(),
                    v.getPaddingBottom()
            );
            return windowInsets;
        });
    }
    
    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.image_preview);
        }
        
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        
        // 点击图片切换工具栏显示/隐藏
        binding.viewPager.setOnClickListener(v -> toggleToolbar());
    }
    
    private void setupViewPager() {
        adapter = new ImagePreviewAdapter();
        adapter.setImages(imagePaths);
        
        binding.viewPager.setAdapter(adapter);
        binding.viewPager.setCurrentItem(currentPosition, false);
        
        // 监听页面切换
        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                currentPosition = position;
                updateIndicator();
            }
        });
    }
    
    /**
     * 更新图片指示器
     */
    private void updateIndicator() {
        if (imagePaths.size() > 1) {
            String indicator = (currentPosition + 1) + " / " + imagePaths.size();
            binding.tvImageIndicator.setText(indicator);
            binding.tvImageIndicator.setVisibility(View.VISIBLE);
        } else {
            binding.tvImageIndicator.setVisibility(View.GONE);
        }
    }
    
    /**
     * 切换工具栏显示/隐藏
     */
    private void toggleToolbar() {
        if (isToolbarVisible) {
            hideSystemUI();
            binding.appBarLayout.animate()
                    .translationY(-binding.appBarLayout.getHeight())
                    .setDuration(200)
                    .start();
            binding.tvImageIndicator.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .start();
        } else {
            showSystemUI();
            binding.appBarLayout.animate()
                    .translationY(0)
                    .setDuration(200)
                    .start();
            binding.tvImageIndicator.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .start();
        }
        isToolbarVisible = !isToolbarVisible;
    }
    
    /**
     * 隐藏系统UI
     * 使用 WindowInsetsController API（Android 11+）替代已弃用的 setSystemUiVisibility
     */
    private void hideSystemUI() {
        // 使用 WindowInsetsController API（Android 11+）隐藏系统栏
        WindowInsetsControllerCompat windowInsetsController = 
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (windowInsetsController != null) {
            // 隐藏状态栏和导航栏
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
            // 设置沉浸式模式：滑动边缘时系统栏临时显示后自动隐藏
            windowInsetsController.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }
    }
    
    /**
     * 显示系统UI
     * 使用 WindowInsetsController API（Android 11+）替代已弃用的 setSystemUiVisibility
     */
    private void showSystemUI() {
        // 使用 WindowInsetsController API（Android 11+）显示系统栏
        WindowInsetsControllerCompat windowInsetsController = 
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (windowInsetsController != null) {
            // 显示状态栏和导航栏
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars());
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}

