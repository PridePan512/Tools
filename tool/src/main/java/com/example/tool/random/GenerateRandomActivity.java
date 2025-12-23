package com.example.tool.random;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.tool.R;

import com.example.tool.random.fragment.GenerateRandomNumberFragment;
import com.example.tool.random.fragment.GenerateRandomStringFragment;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class GenerateRandomActivity extends AppCompatActivity {

    private static final String[] TAB_TITLES = new String[]{
            "随机数", "随机字符串"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_random);

        ViewPager2 viewPager = findViewById(R.id.view_pager);
        TabLayout tabLayout = findViewById(R.id.tab_layout);

        // 设置ViewPager2适配器
        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                if (position == 0) {
                    return new GenerateRandomNumberFragment();

                } else {
                    return new GenerateRandomStringFragment();
                }
            }

            @Override
            public int getItemCount() {
                return 2;
            }
        });

        // 关联TabLayout和ViewPager2
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(TAB_TITLES[position])).attach();
    }
}