package com.dowdah.asknow.ui.tutor;

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

import com.dowdah.asknow.data.local.dao.MessageDao;
import com.dowdah.asknow.databinding.FragmentQuestionListByStatusBinding;
import com.dowdah.asknow.ui.adapter.QuestionAdapter;
import com.dowdah.asknow.utils.SharedPreferencesManager;
import com.dowdah.asknow.utils.WebSocketManager;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class QuestionListByStatusFragment extends Fragment {
    
    private static final String ARG_STATUS = "status";
    
    private FragmentQuestionListByStatusBinding binding;
    private TutorViewModel viewModel;
    private QuestionAdapter adapter;
    private String status;
    
    @Inject
    MessageDao messageDao;
    
    @Inject
    SharedPreferencesManager prefsManager;
    
    @Inject
    WebSocketManager webSocketManager;
    
    public static QuestionListByStatusFragment newInstance(String status) {
        QuestionListByStatusFragment fragment = new QuestionListByStatusFragment();
        Bundle args = new Bundle();
        args.putString(ARG_STATUS, status);
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            status = getArguments().getString(ARG_STATUS);
        }
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentQuestionListByStatusBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        viewModel = new ViewModelProvider(requireActivity()).get(TutorViewModel.class);
        
        setupRecyclerView();
        observeViewModel();
    }
    
    private void setupRecyclerView() {
        long currentUserId = prefsManager.getUserId();
        adapter = new QuestionAdapter(question -> {
            Intent intent = new Intent(requireContext(), AnswerActivity.class);
            intent.putExtra("question_id", question.getId());
            startActivity(intent);
        }, messageDao, currentUserId);
        
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        binding.recyclerView.setLayoutManager(layoutManager);
        binding.recyclerView.setAdapter(adapter);
        
        // 设置重试监听
        adapter.setRetryListener(() -> {
            viewModel.loadMoreQuestions();
        });
        
        // 设置下拉刷新
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            viewModel.syncQuestionsFromServer();
        });
        
        // 设置滚动监听，加载更多
        binding.recyclerView.addOnScrollListener(new androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull androidx.recyclerview.widget.RecyclerView recyclerView, int dx, int dy) {
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
        if ("pending".equals(status)) {
            viewModel.getPendingQuestions().observe(getViewLifecycleOwner(), questions -> {
                if (questions != null) {
                    adapter.setQuestions(questions);
                    binding.emptyView.setVisibility(questions.isEmpty() ? View.VISIBLE : View.GONE);
                }
            });
        } else if ("in_progress".equals(status)) {
            viewModel.getInProgressQuestions().observe(getViewLifecycleOwner(), questions -> {
                if (questions != null) {
                    adapter.setQuestions(questions);
                    binding.emptyView.setVisibility(questions.isEmpty() ? View.VISIBLE : View.GONE);
                }
            });
        } else if ("closed".equals(status)) {
            viewModel.getClosedQuestions().observe(getViewLifecycleOwner(), questions -> {
                if (questions != null) {
                    adapter.setQuestions(questions);
                    binding.emptyView.setVisibility(questions.isEmpty() ? View.VISIBLE : View.GONE);
                }
            });
        }
        
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

