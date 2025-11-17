package com.dowdah.asknow.ui.student;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.bumptech.glide.Glide;
import com.dowdah.asknow.R;
import com.dowdah.asknow.constants.ApiConstants;
import com.dowdah.asknow.data.api.ApiService;
import com.dowdah.asknow.data.model.UploadResponse;
import com.dowdah.asknow.databinding.ActivityPublishQuestionBinding;
import com.dowdah.asknow.ui.adapter.ImagePreviewAdapter;
import com.dowdah.asknow.utils.SharedPreferencesManager;
import com.dowdah.asknow.utils.ValidationUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class PublishQuestionActivity extends AppCompatActivity {
    
    private ActivityPublishQuestionBinding binding;
    private StudentViewModel viewModel;
    
    @Inject
    ApiService apiService;
    
    @Inject
    SharedPreferencesManager prefsManager;
    
    private static final int MAX_IMAGES = 9;
    private List<Uri> selectedImageUris = new ArrayList<>();
    private List<String> uploadedImagePaths = new ArrayList<>();
    private ImagePreviewAdapter imagePreviewAdapter;
    
    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                Intent data = result.getData();
                
                // 处理多图片选择
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    for (int i = 0; i < count && selectedImageUris.size() < MAX_IMAGES; i++) {
                        Uri imageUri = data.getClipData().getItemAt(i).getUri();
                        selectedImageUris.add(imageUri);
                    }
                } else if (data.getData() != null) {
                    // 单张图片
                    if (selectedImageUris.size() < MAX_IMAGES) {
                        selectedImageUris.add(data.getData());
                    }
                }
                
                updateImagePreview();
                
                if (selectedImageUris.size() >= MAX_IMAGES) {
                    Toast.makeText(this, getString(R.string.max_images_reached, MAX_IMAGES), 
                        Toast.LENGTH_SHORT).show();
                }
            }
        }
    );
    
    private final ActivityResultLauncher<String> permissionLauncher = registerForActivityResult(
        new ActivityResultContracts.RequestPermission(),
        isGranted -> {
            if (isGranted) {
                openImagePicker();
            } else {
                Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show();
            }
        }
    );
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPublishQuestionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        viewModel = new ViewModelProvider(this).get(StudentViewModel.class);
        
        setupToolbar();
        setupViews();
        observeViewModel();
    }
    
    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.ask_question);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }
    
    private void setupViews() {
        // 设置图片预览适配器
        imagePreviewAdapter = new ImagePreviewAdapter();
        binding.rvImagePreview.setLayoutManager(new GridLayoutManager(this, 3));
        binding.rvImagePreview.setAdapter(imagePreviewAdapter);
        
        imagePreviewAdapter.setRemoveListener(position -> {
            // 移除选中的图片
            selectedImageUris.remove(position);
            updateImagePreview();
        });
        
        binding.btnAddImage.setOnClickListener(v -> {
            if (selectedImageUris.size() >= MAX_IMAGES) {
                Toast.makeText(this, getString(R.string.max_images_reached, MAX_IMAGES), 
                    Toast.LENGTH_SHORT).show();
            } else {
                checkPermissionAndPickImage();
            }
        });
        
        binding.btnSubmit.setOnClickListener(v -> submitQuestion());
    }
    
    private void observeViewModel() {
        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });
    }
    
    private void checkPermissionAndPickImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
    }
    
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        // 支持多选
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        imagePickerLauncher.launch(intent);
    }
    
    private void updateImagePreview() {
        if (selectedImageUris.isEmpty()) {
            binding.rvImagePreview.setVisibility(android.view.View.GONE);
        } else {
            binding.rvImagePreview.setVisibility(android.view.View.VISIBLE);
            imagePreviewAdapter.setImages(selectedImageUris);
        }
    }
    
    private void submitQuestion() {
        String content = binding.etContent.getText().toString().trim();
        
        // 清除之前的错误
        binding.tilContent.setError(null);
        
        // 验证内容
        if (!ValidationUtils.isNotNullOrEmpty(content)) {
            binding.tilContent.setError(getString(R.string.please_enter_question_content));
            binding.etContent.requestFocus();
            return;
        }
        
        // 验证内容长度（至少10个字符，最多1000个字符）
        if (content.length() < 10) {
            binding.tilContent.setError(getString(R.string.error_question_too_short));
            binding.etContent.requestFocus();
            return;
        }
        
        if (content.length() > 1000) {
            binding.tilContent.setError(getString(R.string.error_question_too_long));
            binding.etContent.requestFocus();
            return;
        }
        
        // 禁用提交按钮，显示进度条
        binding.btnSubmit.setEnabled(false);
        binding.progressBar.setVisibility(android.view.View.VISIBLE);
        
        if (!selectedImageUris.isEmpty()) {
            uploadImagesThenSubmit(content);
        } else {
            submitQuestionToServer(content, null);
        }
    }
    
    private void uploadImagesThenSubmit(String content) {
        uploadedImagePaths.clear();
        uploadNextImage(content, 0);
    }
    
    private void uploadNextImage(String content, int index) {
        if (index >= selectedImageUris.size()) {
            // 所有图片上传完成，提交问题
            submitQuestionToServer(content, uploadedImagePaths);
            return;
        }
        
        Uri imageUri = selectedImageUris.get(index);
        File file = new File(getCacheDir(), "temp_image_" + index + ".jpg");
        
        try (InputStream inputStream = getContentResolver().openInputStream(imageUri);
             FileOutputStream outputStream = new FileOutputStream(file)) {
            
            if (inputStream == null) {
                resetSubmitState();
                Toast.makeText(this, R.string.error_reading_image, Toast.LENGTH_SHORT).show();
                return;
            }
            
            byte[] buffer = new byte[4096];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            
            // 创建上传请求
            RequestBody requestBody = RequestBody.create(file, MediaType.parse("image/*"));
            MultipartBody.Part imagePart = MultipartBody.Part.createFormData(ApiConstants.FORM_FIELD_IMAGE, file.getName(), requestBody);
            
            String token = "Bearer " + prefsManager.getToken();
            apiService.uploadImage(token, imagePart).enqueue(new Callback<UploadResponse>() {
                @Override
                public void onResponse(Call<UploadResponse> call, Response<UploadResponse> response) {
                    // 清理临时文件
                    if (file.exists()) {
                        file.delete();
                    }
                    
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        uploadedImagePaths.add(response.body().getImagePath());
                        // 上传下一张图片
                        uploadNextImage(content, index + 1);
                    } else {
                        resetSubmitState();
                        String errorMsg = response.body() != null && response.body().getMessage() != null ?
                            response.body().getMessage() : getString(R.string.failed_to_upload_image);
                        Toast.makeText(PublishQuestionActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                }
                
                @Override
                public void onFailure(Call<UploadResponse> call, Throwable t) {
                    // 清理临时文件
                    if (file.exists()) {
                        file.delete();
                    }
                    
                    resetSubmitState();
                    String errorMsg = t.getMessage() != null ? 
                        getString(R.string.upload_error, t.getMessage()) : 
                        getString(R.string.failed_to_upload_image);
                    Toast.makeText(PublishQuestionActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            });
            
        } catch (Exception e) {
            resetSubmitState();
            String errorMsg = e.getMessage() != null ? 
                getString(R.string.error_message, e.getMessage()) : 
                getString(R.string.error_reading_image);
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
            
            // 清理临时文件
            if (file.exists()) {
                file.delete();
            }
        }
    }
    
    /**
     * 重置提交状态（启用按钮，隐藏进度条）
     */
    private void resetSubmitState() {
        if (binding != null) {
            binding.btnSubmit.setEnabled(true);
            binding.progressBar.setVisibility(android.view.View.GONE);
        }
    }
    
    private void submitQuestionToServer(String content, List<String> imagePaths) {
        viewModel.createQuestion(content, imagePaths);
        Toast.makeText(this, R.string.question_submitted, Toast.LENGTH_SHORT).show();
        finish();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}

