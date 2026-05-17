package com.example.walletzen.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.walletzen.R;
import com.example.walletzen.adapter.TransactionAdapter;
import com.example.walletzen.data.SessionManager;
import com.example.walletzen.model.Transaction;
import com.example.walletzen.network.RetrofitClient;
import com.example.walletzen.ui.profile.ProfileActivity;
import com.example.walletzen.ui.statistics.StatisticsActivity;
import com.example.walletzen.ui.budget.BudgetActivity;
import com.example.walletzen.ui.transaction.AddTransactionActivity;
import com.example.walletzen.ui.transaction.TransactionListActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity {

    private TextView tvUserName, tvBalance, tvIncome, tvExpense;
    private ImageView ivToggleBalance;
    private View tvEmptyState;
    private boolean isBalanceVisible = true;
    private double currentBalance = 0.0;
    private double currentIncome = 0.0;
    private double currentExpense = 0.0;
    private RecyclerView rvTransactions;
    private SwipeRefreshLayout swipeRefresh;
    private BottomNavigationView bottomNav;
    private FloatingActionButton fabAdd;
    private TransactionAdapter adapter;
    private List<Transaction> transactionList;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        session = new SessionManager(this);

        // Check session
        if (!session.isLoggedIn()) {
            startActivity(new Intent(this, com.example.walletzen.ui.auth.LoginActivity.class));
            finish();
            return;
        }

        initViews();
        setupBottomNav();
        setupFab();
        loadDashboard();
        loadRecentTransactions();
    }

    private void initViews() {
        tvUserName = findViewById(R.id.tvUserName);
        tvBalance = findViewById(R.id.tvBalanceMonth);
        tvIncome = findViewById(R.id.tvIncomeMonth);
        tvExpense = findViewById(R.id.tvExpenseMonth);
        ivToggleBalance = findViewById(R.id.ivToggleBalance);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        rvTransactions = findViewById(R.id.rvRecentTransactions);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        bottomNav = findViewById(R.id.bottomNavigation);
        fabAdd = findViewById(R.id.fabAdd);

        // Set username
        if (tvUserName != null) tvUserName.setText(session.getFullName());

        // Setup Toggle Balance
        ivToggleBalance.setOnClickListener(v -> {
            isBalanceVisible = !isBalanceVisible;
            updateBalanceVisibility();
        });

        // Setup RecyclerView
        transactionList = new ArrayList<>();
        adapter = new TransactionAdapter(this, transactionList, transaction -> {
            // Click item → detail
            Intent intent = new Intent(this,
                    com.example.walletzen.ui.detail.TransactionDetailActivity.class);
            intent.putExtra("transactionId", transaction.getTransactionId());
            intent.putExtra("note", transaction.getNote());
            intent.putExtra("amount", transaction.getAmount());
            intent.putExtra("date", transaction.getDate());
            intent.putExtra("type", transaction.getType());
            intent.putExtra("category", transaction.getCategoryName());
            intent.putExtra("icon", transaction.getCategoryIcon());
            if (transaction.getCategory() != null) {
                intent.putExtra("categoryId", transaction.getCategory().getCategoryId());
            }
            startActivity(intent);
        });
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        rvTransactions.setAdapter(adapter);
        rvTransactions.setNestedScrollingEnabled(false);

        // Swipe to refresh
        swipeRefresh.setColorSchemeResources(R.color.primary);
        swipeRefresh.setOnRefreshListener(() -> {
            loadDashboard();
            loadRecentTransactions();
        });

        // View all transactions
        TextView tvViewAll = findViewById(R.id.tvViewAll);
        if (tvViewAll != null) {
            tvViewAll.setOnClickListener(v -> {
                startActivity(new Intent(this, TransactionListActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }
    }

    private void setupBottomNav() {
        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_statistics) {
                startActivity(new Intent(this, StatisticsActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                return true;
            } else if (id == R.id.nav_budget) {
                startActivity(new Intent(this, BudgetActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                return true;
            }
            return false;
        });
    }

    private void setupFab() {
        fabAdd.setOnClickListener(v -> {
            startActivity(new Intent(this, AddTransactionActivity.class));
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.fade_out);
        });
    }

    private void loadDashboard() {
        Long userId = session.getUserId();

        // 1. Get cumulative balance
        RetrofitClient.getApiService().getBalance(userId).enqueue(new Callback<Map<String, Double>>() {
            @Override
            public void onResponse(Call<Map<String, Double>> call, Response<Map<String, Double>> response) {
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Double> data = response.body();
                    currentBalance = getFirstNonZero(data, "balance", "totalBalance", "total");
                    updateBalanceVisibility();
                }
            }
            @Override
            public void onFailure(Call<Map<String, Double>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                tvBalance.setText("-- ₫");
            }
        });

        // 2. Calculate monthly income & expense directly from transaction list (bypasses unreliable API)
        RetrofitClient.getApiService().getTransactions(userId).enqueue(new Callback<List<Transaction>>() {
            @Override
            public void onResponse(Call<List<Transaction>> call, Response<List<Transaction>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String currentMonth = new java.text.SimpleDateFormat("yyyy-MM",
                            java.util.Locale.getDefault()).format(new java.util.Date());
                    double income = 0, expense = 0;
                    for (Transaction t : response.body()) {
                        String date = t.getDate();
                        // Match transactions in current month (date format: yyyy-MM-dd)
                        if (date != null && date.startsWith(currentMonth)) {
                            double amt = t.getAmount() != null ? Math.abs(t.getAmount()) : 0;
                            if ("THU".equals(t.getType())) income  += amt;
                            else if ("CHI".equals(t.getType())) expense += amt;
                        }
                    }
                    currentIncome  = income;
                    currentExpense = expense;
                    updateBalanceVisibility();
                }
            }
            @Override
            public void onFailure(Call<List<Transaction>> call, Throwable t) {}
        });
    }


    private void updateBalanceVisibility() {
        if (isBalanceVisible) {
            tvBalance.setText(formatMoney(currentBalance));
            tvIncome.setText("+" + formatMoney(Math.abs(currentIncome)));
            tvExpense.setText("-" + formatMoney(Math.abs(currentExpense)));
            ivToggleBalance.setAlpha(1.0f);
        } else {
            tvBalance.setText("******");
            tvIncome.setText("******");
            tvExpense.setText("******");
            ivToggleBalance.setAlpha(0.5f);
        }
    }

    private void loadRecentTransactions() {
        Long userId = session.getUserId();
        RetrofitClient.getApiService().getRecentTransactions(userId).enqueue(new Callback<List<Transaction>>() {
            @Override
            public void onResponse(Call<List<Transaction>> call, Response<List<Transaction>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    transactionList.clear();
                    transactionList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                    tvEmptyState.setVisibility(transactionList.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onFailure(Call<List<Transaction>> call, Throwable t) {
                tvEmptyState.setVisibility(View.VISIBLE);
                Toast.makeText(HomeActivity.this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        bottomNav.setSelectedItemId(R.id.nav_home);
        loadDashboard();
        loadRecentTransactions();
    }

    private double getOrDefault(Map<String, Double> map, String key, double def) {
        Double val = map.get(key);
        return val != null ? val : def;
    }

    /** Try multiple key names; return first non-null (prefer non-zero) */
    private double getFirstNonZero(Map<String, Double> map, String... keys) {
        for (String k : keys) {
            Double v = map.get(k);
            if (v != null && v != 0) return v;
        }
        for (String k : keys) {
            Double v = map.get(k);
            if (v != null) return v;
        }
        return 0.0;
    }

    private String formatMoney(double amount) {
        return String.format("%,.0f ₫", amount).replace(",", ".");
    }
}