package com.dowdah.asknow.ui.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.dowdah.asknow.ui.tutor.QuestionListByStatusFragment;

public class TutorQuestionsPagerAdapter extends FragmentStateAdapter {
    
    private static final int NUM_TABS = 3;
    
    public TutorQuestionsPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }
    
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return QuestionListByStatusFragment.newInstance("pending");
            case 1:
                return QuestionListByStatusFragment.newInstance("in_progress");
            case 2:
                return QuestionListByStatusFragment.newInstance("closed");
            default:
                return QuestionListByStatusFragment.newInstance("pending");
        }
    }
    
    @Override
    public int getItemCount() {
        return NUM_TABS;
    }
}

