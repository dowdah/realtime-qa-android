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

import com.bumptech.glide.Glide;
import com.dowdah.asknow.R;
import com.dowdah.asknow.data.api.ApiService;
import com.dowdah.asknow.data.model.UploadResponse;
import com.dowdah.asknow.databinding.ActivityPublishQuestionBinding;
import com.dowdah.asknow.utils.SharedPreferencesManager;
import com.dowdah.asknow.utils.ValidationUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

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
    
    private Uri selectedImageUri;
    private String uploadedImagePath;
    
    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                selectedImageUri = result.getData().getData();
                displayImage();
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
        binding.btnAddImage.setOnClickListener(v -> checkPermissionAndPickImage());
        
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
        imagePickerLauncher.launch(intent);
    }
    
    private void displayImage() {
        if (selectedImageUri != null) {
            Glide.with(this)
                .load(selectedImageUri)
                .into(binding.ivPreview);
            binding.ivPreview.setVisibility(android.view.View.VISIBLE);
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
        
        if (selectedImageUri != null) {
            uploadImageThenSubmit(content);
        } else {
            submitQuestionToServer(content, null);
        }
    }
    
    private void uploadImageThenSubmit(String content) {
        File file = new File(getCacheDir(), "temp_image.jpg");
        
        // 使用try-with-resources自动关闭资源，防止内存泄漏
        try (InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
             FileOutputStream outputStream = new FileOutputStream(file)) {
            
            if (inputStream == null) {
                resetSubmitState();
                Toast.makeText(this, R.string.error_reading_image, Toast.LENGTH_SHORT).show();
                return;
            }
            
            byte[] buffer = new byte[4096]; // 增大缓冲区以提高性能
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            
            // 创建上传请求
            RequestBody requestBody = RequestBody.create(file, MediaType.parse("image/*"));
            MultipartBody.Part imagePart = MultipartBody.Part.createFormData("image", file.getName(), requestBody);
            
            String token = "Bearer " + prefsManager.getToken();
            apiService.uploadImage(token, imagePart).enqueue(new Callback<UploadResponse>() {
                @Override
                public void onResponse(Call<UploadResponse> call, Response<UploadResponse> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        uploadedImagePath = response.body().getImagePath();
                        submitQuestionToServer(content, uploadedImagePath);
                    } else {
                        resetSubmitState();
                        String errorMsg = response.body() != null && response.body().getMessage() != null ?
                            response.body().getMessage() : getString(R.string.failed_to_upload_image);
                        Toast.makeText(PublishQuestionActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                    
                    // 清理临时文件
                    if (file.exists()) {
                        file.delete();
                    }
                }
                
                @Override
                public void onFailure(Call<UploadResponse> call, Throwable t) {
                    resetSubmitState();
                    String errorMsg = t.getMessage() != null ? 
                        getString(R.string.upload_error, t.getMessage()) : 
                        getString(R.string.failed_to_upload_image);
                    Toast.makeText(PublishQuestionActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    
                    // 清理临时文件
                    if (file.exists()) {
                        file.delete();
                    }
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
    
    private void submitQuestionToServer(String content, String imagePath) {
        viewModel.createQuestion(content, imagePath);
        Toast.makeText(this, R.string.question_submitted, Toast.LENGTH_SHORT).show();
        finish();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}

