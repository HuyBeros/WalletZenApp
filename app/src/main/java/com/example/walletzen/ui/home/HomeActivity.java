package com.example.walletzen.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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
import com.example.walletzen.ui.transaction.AddTransactionActivity;
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
    private View tvEmptyState;
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
        tvEmptyState = findViewById(R.id.tvEmptyState);
        rvTransactions = findViewById(R.id.rvRecentTransactions);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        bottomNav = findViewById(R.id.bottomNavigation);
        fabAdd = findViewById(R.id.fabAdd);

        // Set username
        tvUserName.setText("Xin chào, " + session.getFullName() + "!");

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
        RetrofitClient.getApiService().getBalance(userId).enqueue(new Callback<Map<String, Double>>() {
            @Override
            public void onResponse(Call<Map<String, Double>> call, Response<Map<String, Double>> response) {
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Double> data = response.body();
                    double balance = getOrDefault(data, "balance", 0.0);
                    double income = getOrDefault(data, "income", 0.0);
                    double expense = getOrDefault(data, "expense", 0.0);

                    tvBalance.setText(formatMoney(balance));
                    tvIncome.setText(formatMoney(income));
                    tvExpense.setText(formatMoney(expense));
                }
            }

            @Override
            public void onFailure(Call<Map<String, Double>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                tvBalance.setText("-- ₫");
            }
        });
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

    private String formatMoney(double amount) {
        return String.format("%,.0f ₫", Math.abs(amount)).replace(",", ".");
    }
}