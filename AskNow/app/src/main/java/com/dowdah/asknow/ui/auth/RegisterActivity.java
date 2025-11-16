package com.dowdah.asknow.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.dowdah.asknow.R;
import com.dowdah.asknow.databinding.ActivityRegisterBinding;
import com.dowdah.asknow.ui.student.StudentMainActivity;
import com.dowdah.asknow.ui.tutor.TutorMainActivity;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class RegisterActivity extends AppCompatActivity {
    
    private ActivityRegisterBinding binding;
    private AuthViewModel viewModel;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        
        setupViews();
        observeViewModel();
    }
    
    private void setupViews() {
        binding.btnRegister.setOnClickListener(v -> performRegister());
        
        binding.tvGoToLogin.setOnClickListener(v -> {
            finish();
        });
    }
    
    private void observeViewModel() {
        viewModel.getLoading().observe(this, loading -> {
            binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            binding.btnRegister.setEnabled(!loading);
        });
        
        viewModel.getRegisterResult().observe(this, result -> {
            if (result.isSuccess()) {
                Toast.makeText(this, result.getMessage(), Toast.LENGTH_SHORT).show();
                navigateToMain(result.getRole());
            } else {
                Toast.makeText(this, result.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
    
    private void performRegister() {
        String username = binding.etUsername.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        String confirmPassword = binding.etConfirmPassword.getText().toString().trim();
        
        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, R.string.passwords_do_not_match, Toast.LENGTH_SHORT).show();
            return;
        }
        
        String role = binding.radioStudent.isChecked() ? "student" : "tutor";
        
        viewModel.register(username, password, role);
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

