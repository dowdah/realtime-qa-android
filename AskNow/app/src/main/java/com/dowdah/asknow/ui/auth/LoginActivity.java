package com.dowdah.asknow.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.dowdah.asknow.R;
import com.dowdah.asknow.databinding.ActivityLoginBinding;
import com.dowdah.asknow.ui.student.StudentMainActivity;
import com.dowdah.asknow.ui.tutor.TutorMainActivity;
import com.dowdah.asknow.utils.SharedPreferencesManager;
import com.dowdah.asknow.utils.ValidationUtils;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class LoginActivity extends AppCompatActivity {
    
    private ActivityLoginBinding binding;
    private AuthViewModel viewModel;
    
    @Inject
    SharedPreferencesManager prefsManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 检查是否已经登录
        if (prefsManager.isLoggedIn()) {
            String role = prefsManager.getRole();
            navigateToMain(role);
            return;
        }
        
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        
        setupViews();
        observeViewModel();
    }
    
    private void setupViews() {
        binding.btnLogin.setOnClickListener(v -> performLogin());
        
        binding.tvGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
    }
    
    private void observeViewModel() {
        viewModel.getLoading().observe(this, loading -> {
            binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            binding.btnLogin.setEnabled(!loading);
        });
        
        viewModel.getLoginResult().observe(this, result -> {
            if (result.isSuccess()) {
                Toast.makeText(this, result.getMessage(), Toast.LENGTH_SHORT).show();
                navigateToMain(result.getRole());
            } else {
                Toast.makeText(this, result.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
    
    private void performLogin() {
        String username = binding.etUsername.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        
        // 清除之前的错误
        binding.tilUsername.setError(null);
        binding.tilPassword.setError(null);
        
        // 验证用户名
        if (!ValidationUtils.isNotNullOrEmpty(username)) {
            binding.tilUsername.setError(getString(R.string.error_username_empty));
            binding.etUsername.requestFocus();
            return;
        }
        
        if (!ValidationUtils.isValidUsername(username)) {
            binding.tilUsername.setError(getString(R.string.error_username_invalid));
            binding.etUsername.requestFocus();
            return;
        }
        
        // 验证密码
        if (!ValidationUtils.isNotNullOrEmpty(password)) {
            binding.tilPassword.setError(getString(R.string.error_password_empty));
            binding.etPassword.requestFocus();
            return;
        }
        
        if (!ValidationUtils.isValidPassword(password)) {
            binding.tilPassword.setError(getString(R.string.error_password_too_short));
            binding.etPassword.requestFocus();
            return;
        }
        
        // 隐藏键盘
        hideKeyboard();
        
        // 发起登录请求
        viewModel.login(username, password);
    }
    
    /**
     * 隐藏软键盘
     */
    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }
    
    private void navigateToMain(String role) {
        Intent intent;
        if ("tutor".equalsIgnoreCase(role)) {
            intent = new Intent(this, TutorMainActivity.class);
        } else {
            intent = new Intent(this, StudentMainActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}

