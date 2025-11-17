package com.dowdah.asknow.ui.tutor;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.dowdah.asknow.ui.question.BaseQuestionListFragment;
import com.dowdah.asknow.ui.question.BaseQuestionListViewModel;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * PendingQuestionsFragment - 教师端待处理问题列表 Fragment
 * 
 * 继承自 BaseQuestionListFragment，复用了：
 * - RecyclerView 设置和优化
 * - 下拉刷新和滚动加载
 * - 未读消息数量更新
 * - 空数据提示
 * 
 * 教师端特有：
 * - 显示待处理的问题（status = "pending"）
 * - 点击跳转到 AnswerActivity
 */
@AndroidEntryPoint
public class PendingQuestionsFragment extends BaseQuestionListFragment {
    
    private TutorViewModel viewModel;
    
    @Override
    protected BaseQuestionListViewModel getViewModel() {
        if (viewModel == null) {
            viewModel = new ViewModelProvider(requireActivity()).get(TutorViewModel.class);
        }
        return viewModel;
    }
    
    @Override
    protected Class<? extends AppCompatActivity> getDetailActivityClass() {
        return AnswerActivity.class;
    }
}

