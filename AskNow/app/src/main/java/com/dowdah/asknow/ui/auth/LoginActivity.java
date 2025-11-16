package com.dowdah.asknow.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.dowdah.asknow.databinding.ActivityLoginBinding;
import com.dowdah.asknow.ui.student.StudentMainActivity;
import com.dowdah.asknow.ui.tutor.TutorMainActivity;
import com.dowdah.asknow.utils.SharedPreferencesManager;

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
        
        viewModel.login(username, password);
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

