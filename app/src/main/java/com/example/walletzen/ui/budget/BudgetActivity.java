package com.example.walletzen.ui.budget;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.walletzen.R;
import com.example.walletzen.ui.home.HomeActivity;
import com.example.walletzen.ui.profile.ProfileActivity;
import com.example.walletzen.ui.statistics.StatisticsActivity;
import com.example.walletzen.ui.transaction.AddTransactionActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class BudgetActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private TabLayout tabLayoutBudget;
    private ViewPager2 viewPagerBudget;
    private ExtendedFloatingActionButton fabAddBudget;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget);

        bottomNav = findViewById(R.id.bottomNavigation);
        tabLayoutBudget = findViewById(R.id.tabLayoutBudget);
        viewPagerBudget = findViewById(R.id.viewPagerBudget);
        fabAddBudget = findViewById(R.id.fabAddBudget);

        setupBottomNav();
        setupViewPager();

        // Show FAB to add new budgets
        fabAddBudget.setVisibility(com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton.VISIBLE);
        fabAddBudget.setOnClickListener(v -> {
            Intent intent = new Intent(BudgetActivity.this, AddBudgetActivity.class);
            startActivityForResult(intent, 100);
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            setupViewPager();
        }
    }

    private void setupViewPager() {
        BudgetPagerAdapter adapter = new BudgetPagerAdapter(this);
        viewPagerBudget.setAdapter(adapter);

        new TabLayoutMediator(tabLayoutBudget, viewPagerBudget, (tab, position) -> {
            if (position == 0) {
                tab.setText("CHI TIÊU");
            } else {
                tab.setText("THU NHẬP");
            }
        }).attach();
    }

    private void setupBottomNav() {
        bottomNav.setSelectedItemId(R.id.nav_budget);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_budget) {
                return true;
            } else if (id == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                return true;
            } else if (id == R.id.nav_statistics) {
                startActivity(new Intent(this, StatisticsActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        bottomNav.setSelectedItemId(R.id.nav_budget);
    }

    private class BudgetPagerAdapter extends FragmentStateAdapter {
        public BudgetPagerAdapter(@NonNull AppCompatActivity activity) {
            super(activity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return BudgetFragment.newInstance(position == 0);
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }
}
