package com.example.test_filesync;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class ViewPagerAdapter extends FragmentStateAdapter {

    private String userMode;

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
        this.userMode = MainActivity.MODE_PARENT; // 默认家长模式
    }

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity, String userMode) {
        super(fragmentActivity);
        this.userMode = userMode != null ? userMode : MainActivity.MODE_PARENT;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // 根据用户模式返回不同的 Fragment
        if (MainActivity.MODE_CHILD.equals(userMode)) {
            // 孩子模式
            switch (position) {
                case 0:
                    return new ChildHomeFragment();
                case 1:
                    return new RecordFragment();
                case 2:
                    return new ProfileFragment();
                default:
                    return new ChildHomeFragment();
            }
        } else {
            // 家长模式
            switch (position) {
                case 0:
                    return new HomeFragment();
                case 1:
                    return new RecordFragment();
                case 2:
                    return new ProfileFragment();
                default:
                    return new HomeFragment();
            }
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}

