package com.dowdah.asknow.ui.student;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dowdah.asknow.data.local.dao.MessageDao;
import com.dowdah.asknow.databinding.FragmentQuestionListBinding;
import com.dowdah.asknow.ui.adapter.QuestionAdapter;
import com.dowdah.asknow.utils.SharedPreferencesManager;
import com.dowdah.asknow.utils.WebSocketManager;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class QuestionListFragment extends Fragment {
    
    private FragmentQuestionListBinding binding;
    private StudentViewModel viewModel;
    private QuestionAdapter adapter;
    
    @Inject
    MessageDao messageDao;
    
    @Inject
    SharedPreferencesManager prefsManager;
    
    @Inject
    WebSocketManager webSocketManager;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentQuestionListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        viewModel = new ViewModelProvider(requireActivity()).get(StudentViewModel.class);
        
        setupRecyclerView();
        observeViewModel();
    }
    
    private void setupRecyclerView() {
        long currentUserId = prefsManager.getUserId();
        adapter = new QuestionAdapter(question -> {
            Intent intent = new Intent(requireContext(), QuestionDetailActivity.class);
            intent.putExtra("question_id", question.getId());
            startActivity(intent);
        }, messageDao, currentUserId);
        
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        binding.recyclerView.setLayoutManager(layoutManager);
        binding.recyclerView.setAdapter(adapter);
        
        // 性能优化：RecyclerView大小固定，避免重新计算
        binding.recyclerView.setHasFixedSize(true);
        
        // 性能优化：减少item变化动画，提高流畅度
        if (binding.recyclerView.getItemAnimator() != null) {
            binding.recyclerView.getItemAnimator().setChangeDuration(0);
        }
        
        // 设置重试监听
        adapter.setRetryListener(() -> {
            viewModel.loadMoreQuestions();
        });
        
        // 设置下拉刷新
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            viewModel.syncQuestionsFromServer();
        });
        
        // 设置滚动监听，加载更多
        binding.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                
                // 向下滚动且接近底部时触发加载更多
                if (dy > 0) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
                    
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 2
                        && firstVisibleItemPosition >= 0) {
                        viewModel.loadMoreQuestions();
                    }
                }
            }
        });
    }
    
    private void observeViewModel() {
        viewModel.getMyQuestions().observe(getViewLifecycleOwner(), questions -> {
            if (questions != null && !questions.isEmpty()) {
                adapter.setQuestions(questions);
                binding.recyclerView.setVisibility(View.VISIBLE);
                binding.tvNoData.setVisibility(View.GONE);
            } else {
                binding.recyclerView.setVisibility(View.GONE);
                binding.tvNoData.setVisibility(View.VISIBLE);
            }
        });
        
        // 监听同步状态
        viewModel.getIsSyncing().observe(getViewLifecycleOwner(), isSyncing -> {
            binding.swipeRefreshLayout.setRefreshing(isSyncing != null && isSyncing);
            
            // 同步完成后，刷新未读数量
            if (isSyncing != null && !isSyncing) {
                adapter.refreshUnreadCounts();
            }
        });
        
        // 监听加载更多状态
        viewModel.getIsLoadingMore().observe(getViewLifecycleOwner(), isLoadingMore -> {
            if (isLoadingMore != null && isLoadingMore) {
                adapter.showLoadingFooter();
            } else {
                adapter.hideLoadingFooter();
            }
        });
        
        // 监听是否还有更多数据
        viewModel.getHasMoreData().observe(getViewLifecycleOwner(), hasMoreData -> {
            if (hasMoreData != null && !hasMoreData) {
                adapter.hideLoadingFooter();
            }
        });
        
        // 监听新消息到达，刷新未读数量
        webSocketManager.getNewMessageReceived().observe(getViewLifecycleOwner(), questionId -> {
            if (questionId != null) {
                // 收到新消息，刷新适配器以更新未读数量
                adapter.refreshUnreadCounts();
            }
        });
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

