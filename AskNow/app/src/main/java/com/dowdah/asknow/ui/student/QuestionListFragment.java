package com.dowdah.asknow.ui.student;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.dowdah.asknow.ui.question.BaseQuestionListFragment;
import com.dowdah.asknow.ui.question.BaseQuestionListViewModel;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * QuestionListFragment - 学生端问题列表 Fragment
 * 
 * 继承自 BaseQuestionListFragment，复用了：
 * - RecyclerView 设置和优化
 * - 下拉刷新和滚动加载
 * - 未读消息数量更新
 * - 空数据提示
 * 
 * 学生端特有：
 * - 显示自己提出的所有问题
 * - 点击跳转到 QuestionDetailActivity
 */
@AndroidEntryPoint
public class QuestionListFragment extends BaseQuestionListFragment {
    
    private StudentViewModel viewModel;
    
    @Override
    protected BaseQuestionListViewModel getViewModel() {
        if (viewModel == null) {
            viewModel = new ViewModelProvider(requireActivity()).get(StudentViewModel.class);
        }
        return viewModel;
    }
    
    @Override
    protected Class<? extends AppCompatActivity> getDetailActivityClass() {
        return QuestionDetailActivity.class;
    }
}

