package com.dowdah.asknow.ui.tutor;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.dowdah.asknow.R;
import com.dowdah.asknow.databinding.ActivityTutorMainBinding;
import com.dowdah.asknow.ui.adapter.TutorQuestionsPagerAdapter;
import com.dowdah.asknow.ui.auth.LoginActivity;
import com.dowdah.asknow.utils.SharedPreferencesManager;
import com.google.android.material.tabs.TabLayoutMediator;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class TutorMainActivity extends AppCompatActivity {
    
    private ActivityTutorMainBinding binding;
    private TutorViewModel viewModel;
    
    @Inject
    SharedPreferencesManager prefsManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTutorMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        viewModel = new ViewModelProvider(this).get(TutorViewModel.class);
        
        setupToolbar();
        setupViewPager();
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
    }
    
    private void setupViewPager() {
        TutorQuestionsPagerAdapter adapter = new TutorQuestionsPagerAdapter(this);
        binding.viewPager.setAdapter(adapter);
        
        String[] tabTitles = {
            getString(R.string.tab_pending),
            getString(R.string.tab_in_progress),
            getString(R.string.tab_completed)
        };
        
        new TabLayoutMediator(binding.tabLayout, binding.viewPager,
            (tab, position) -> tab.setText(tabTitles[position])
        ).attach();
    }
    
    private void observeViewModel() {
        viewModel.isConnected().observe(this, connected -> {
            if (connected) {
                // Toast.makeText(this, "Connected to server", Toast.LENGTH_SHORT).show();
            }
        });
        
        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
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
