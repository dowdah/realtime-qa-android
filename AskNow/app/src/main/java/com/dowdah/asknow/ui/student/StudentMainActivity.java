package com.dowdah.asknow.ui.student;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.dowdah.asknow.R;
import com.dowdah.asknow.databinding.ActivityStudentMainBinding;
import com.dowdah.asknow.ui.auth.LoginActivity;
import com.dowdah.asknow.utils.SharedPreferencesManager;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class StudentMainActivity extends AppCompatActivity {
    
    private ActivityStudentMainBinding binding;
    private StudentViewModel viewModel;
    
    @Inject
    SharedPreferencesManager prefsManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStudentMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        viewModel = new ViewModelProvider(this).get(StudentViewModel.class);
        
        setupToolbar();
        setupFragments(savedInstanceState);
        observeViewModel();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // 延迟同步，避免阻塞主线程初始化
        binding.getRoot().postDelayed(() -> {
            viewModel.syncQuestionsFromServer();
        }, 300);
    }
    
    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.my_questions);
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_student, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            logout();
            return true;
        } else if (item.getItemId() == R.id.action_new_question) {
            startActivity(new Intent(this, PublishQuestionActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void setupFragments(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new QuestionListFragment())
                .commit();
        }
    }
    
    private void observeViewModel() {
        viewModel.isConnected().observe(this, connected -> {
            // Update UI based on connection status
            if (connected) {
                Toast.makeText(this, R.string.connected_to_server, Toast.LENGTH_SHORT).show();
            }
        });
        
        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });
    }
    
    private void logout() {
        prefsManager.clear();
        Intent intent = new Intent(this, LoginActivity.class);
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

