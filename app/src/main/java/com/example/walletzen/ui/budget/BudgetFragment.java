package com.example.walletzen.ui.budget;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.walletzen.R;
import com.example.walletzen.adapter.BudgetAdapter;
import com.example.walletzen.data.SessionManager;
import com.example.walletzen.model.Budget;
import com.example.walletzen.model.BudgetItem;
import com.example.walletzen.model.Transaction;
import com.example.walletzen.network.RetrofitClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BudgetFragment extends Fragment {

    private static final String ARG_IS_EXPENSE = "is_expense";
    private boolean isExpense;

    private RecyclerView rvBudget;
    private ProgressBar progressLoading;
    private TextView tvEmpty;
    private BudgetAdapter adapter;
    private List<BudgetItem> list;
    private SessionManager session;

    public static BudgetFragment newInstance(boolean isExpense) {
        BudgetFragment fragment = new BudgetFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_IS_EXPENSE, isExpense);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            isExpense = getArguments().getBoolean(ARG_IS_EXPENSE);
        }
        session = new SessionManager(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Build layout programmatically: FrameLayout with RecyclerView + loading + empty state
        android.widget.FrameLayout root = new android.widget.FrameLayout(requireContext());
        root.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        rvBudget = new RecyclerView(requireContext());
        rvBudget.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        rvBudget.setPadding(0, 8, 0, 100);
        rvBudget.setClipToPadding(false);

        progressLoading = new ProgressBar(requireContext());
        android.widget.FrameLayout.LayoutParams pbParams = new android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pbParams.gravity = android.view.Gravity.CENTER;
        progressLoading.setLayoutParams(pbParams);

        tvEmpty = new TextView(requireContext());
        android.widget.FrameLayout.LayoutParams tvParams = new android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tvParams.gravity = android.view.Gravity.CENTER;
        tvEmpty.setLayoutParams(tvParams);
        tvEmpty.setText(isExpense ? "Chưa có dữ liệu chi tiêu tháng này" : "Chưa có dữ liệu thu nhập tháng này");
        tvEmpty.setTextColor(0xFF94A3B8);
        tvEmpty.setTextSize(15f);
        tvEmpty.setGravity(android.view.Gravity.CENTER);
        tvEmpty.setPadding(48, 80, 48, 0);
        tvEmpty.setVisibility(View.GONE);

        root.addView(rvBudget);
        root.addView(progressLoading);
        root.addView(tvEmpty);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        list = new ArrayList<>();
        adapter = new BudgetAdapter(requireContext(), list, new BudgetAdapter.OnBudgetClickListener() {
            @Override
            public void onEditClick(BudgetItem item) {
                showEditLimitDialog(item);
            }
        });
        rvBudget.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvBudget.setAdapter(adapter);
        loadBudgetData();
    }

    private void loadBudgetData() {
        progressLoading.setVisibility(View.VISIBLE);
        rvBudget.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);

        if (isExpense) {
            loadExpenseData();
        } else {
            loadIncomeData();
        }
    }

    /** Tab CHI TIÊU — group real spending by category for current month */
    private void loadExpenseData() {
        RetrofitClient.getApiService().getBudgets(session.getUserId())
                .enqueue(new Callback<List<com.example.walletzen.model.Budget>>() {
                    @Override
                    public void onResponse(Call<List<com.example.walletzen.model.Budget>> call,
                                           Response<List<com.example.walletzen.model.Budget>> responseBudgets) {
                        if (!isAdded()) return;
                        if (responseBudgets.isSuccessful() && responseBudgets.body() != null) {
                            List<com.example.walletzen.model.Budget> serverBudgets = responseBudgets.body();
                            
                            RetrofitClient.getApiService().getTransactions(session.getUserId())
                                    .enqueue(new Callback<List<Transaction>>() {
                                        @Override
                                        public void onResponse(Call<List<Transaction>> call2,
                                                               Response<List<Transaction>> responseTx) {
                                            if (!isAdded()) return;
                                            progressLoading.setVisibility(View.GONE);
                                            if (responseTx.isSuccessful() && responseTx.body() != null) {
                                                String currentMonth = new java.text.SimpleDateFormat("yyyy-MM",
                                                        java.util.Locale.getDefault()).format(new java.util.Date());
                                                
                                                Map<String, Double> spentMap = new HashMap<>();
                                                Map<String, Long> categoryIdMap = new HashMap<>();
                                                for (Transaction t : responseTx.body()) {
                                                    String date = t.getDate();
                                                    if ("CHI".equals(t.getType()) && date != null && date.startsWith(currentMonth)) {
                                                        String catName = t.getCategoryName();
                                                        if (catName == null || catName.isEmpty()) catName = "Khác";
                                                        double amt = t.getAmount() != null ? Math.abs(t.getAmount()) : 0;
                                                        spentMap.put(catName, spentMap.getOrDefault(catName, 0.0) + amt);
                                                        if (t.getCategory() != null) {
                                                            categoryIdMap.put(catName, t.getCategory().getCategoryId());
                                                        }
                                                    }
                                                }
                                                
                                                list.clear();
                                                java.util.Set<String> seenCategories = new java.util.HashSet<>();
                                                // 1. Map actual budgets set on backend exactly as returned by API
                                                for (com.example.walletzen.model.Budget b : serverBudgets) {
                                                    if (b.getMonth() != null && b.getMonth().equals(currentMonth)) {
                                                        if (b.getCategory() != null && "CHI".equals(b.getCategory().getType())) {
                                                            String catName = b.getCategory().getCategoryName();
                                                            if (seenCategories.contains(catName)) continue; // Deduplicate
                                                            seenCategories.add(catName);
                                                            
                                                            double spent = spentMap.getOrDefault(catName, 0.0);
                                                            double limit = b.getCategoryLimit() != null ? b.getCategoryLimit() : 0.0;
                                                            list.add(new BudgetItem(catName, "💸", spent, limit, true, b.getBudgetId(), b.getCategory().getCategoryId()));
                                                            
                                                            spentMap.remove(catName); // Processed
                                                        }
                                                    }
                                                }
                                                
                                                // 2. Map remaining transactions that don't have a budget set (Limit = 0)
                                                for (Map.Entry<String, Double> entry : spentMap.entrySet()) {
                                                    String catName = entry.getKey();
                                                    double spent = entry.getValue();
                                                    if (spent > 0) {
                                                        Long catId = categoryIdMap.get(catName);
                                                        list.add(new BudgetItem(catName, "💸", spent, 0.0, true, null, catId));
                                                    }
                                                }
                                                
                                                if (list.isEmpty()) {
                                                    tvEmpty.setVisibility(View.VISIBLE);
                                                } else {
                                                    rvBudget.setVisibility(View.VISIBLE);
                                                    adapter.notifyDataSetChanged();
                                                }
                                            } else {
                                                tvEmpty.setVisibility(View.VISIBLE);
                                            }
                                        }

                                        @Override
                                        public void onFailure(Call<List<Transaction>> call2, Throwable t) {
                                            if (!isAdded()) return;
                                            progressLoading.setVisibility(View.GONE);
                                            tvEmpty.setVisibility(View.VISIBLE);
                                        }
                                    });
                        } else {
                            progressLoading.setVisibility(View.GONE);
                            tvEmpty.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<com.example.walletzen.model.Budget>> call, Throwable t) {
                        if (!isAdded()) return;
                        progressLoading.setVisibility(View.GONE);
                        tvEmpty.setVisibility(View.VISIBLE);
                    }
                });
    }

    /** Tab THU NHẬP — group real income by category for current month */
    private void loadIncomeData() {
        RetrofitClient.getApiService().getBudgets(session.getUserId())
                .enqueue(new Callback<List<com.example.walletzen.model.Budget>>() {
                    @Override
                    public void onResponse(Call<List<com.example.walletzen.model.Budget>> call,
                                           Response<List<com.example.walletzen.model.Budget>> responseBudgets) {
                        if (!isAdded()) return;
                        if (responseBudgets.isSuccessful() && responseBudgets.body() != null) {
                            List<com.example.walletzen.model.Budget> serverBudgets = responseBudgets.body();
                            
                            RetrofitClient.getApiService().getTransactions(session.getUserId())
                                    .enqueue(new Callback<List<Transaction>>() {
                                        @Override
                                        public void onResponse(Call<List<Transaction>> call2,
                                                               Response<List<Transaction>> responseTx) {
                                            if (!isAdded()) return;
                                            progressLoading.setVisibility(View.GONE);
                                            if (responseTx.isSuccessful() && responseTx.body() != null) {
                                                String currentMonth = new java.text.SimpleDateFormat("yyyy-MM",
                                                        java.util.Locale.getDefault()).format(new java.util.Date());
                                                
                                                Map<String, Double> spentMap = new HashMap<>();
                                                Map<String, Long> categoryIdMap = new HashMap<>();
                                                for (Transaction t : responseTx.body()) {
                                                    String date = t.getDate();
                                                    if ("THU".equals(t.getType()) && date != null && date.startsWith(currentMonth)) {
                                                        String catName = t.getCategoryName();
                                                        if (catName == null || catName.isEmpty()) catName = "Khác";
                                                        double amt = t.getAmount() != null ? Math.abs(t.getAmount()) : 0;
                                                        spentMap.put(catName, spentMap.getOrDefault(catName, 0.0) + amt);
                                                        if (t.getCategory() != null) {
                                                            categoryIdMap.put(catName, t.getCategory().getCategoryId());
                                                        }
                                                    }
                                                }
                                                
                                                list.clear();
                                                java.util.Set<String> seenCategories = new java.util.HashSet<>();
                                                // 1. Map actual budgets set on backend exactly as returned by API
                                                for (com.example.walletzen.model.Budget b : serverBudgets) {
                                                    if (b.getMonth() != null && b.getMonth().equals(currentMonth)) {
                                                        if (b.getCategory() != null && "THU".equals(b.getCategory().getType())) {
                                                            String catName = b.getCategory().getCategoryName();
                                                            if (seenCategories.contains(catName)) continue; // Deduplicate
                                                            seenCategories.add(catName);
                                                            
                                                            double spent = spentMap.getOrDefault(catName, 0.0);
                                                            list.add(new BudgetItem(catName, "💰", spent, 0, false, b.getBudgetId(), b.getCategory().getCategoryId()));
                                                            
                                                            spentMap.remove(catName); // Processed
                                                        }
                                                    }
                                                }
                                                
                                                // 2. Map remaining transactions that don't have a budget set
                                                for (Map.Entry<String, Double> entry : spentMap.entrySet()) {
                                                    String catName = entry.getKey();
                                                    double spent = entry.getValue();
                                                    if (spent > 0) {
                                                        Long catId = categoryIdMap.get(catName);
                                                        list.add(new BudgetItem(catName, "💰", spent, 0, false, null, catId));
                                                    }
                                                }
                                                
                                                if (list.isEmpty()) {
                                                    tvEmpty.setVisibility(View.VISIBLE);
                                                } else {
                                                    rvBudget.setVisibility(View.VISIBLE);
                                                    adapter.notifyDataSetChanged();
                                                }
                                            } else {
                                                tvEmpty.setVisibility(View.VISIBLE);
                                            }
                                        }

                                        @Override
                                        public void onFailure(Call<List<Transaction>> call2, Throwable t) {
                                            if (!isAdded()) return;
                                            progressLoading.setVisibility(View.GONE);
                                            tvEmpty.setVisibility(View.VISIBLE);
                                        }
                                    });
                        } else {
                            progressLoading.setVisibility(View.GONE);
                            tvEmpty.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<com.example.walletzen.model.Budget>> call, Throwable t) {
                        if (!isAdded()) return;
                        progressLoading.setVisibility(View.GONE);
                        tvEmpty.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void showEditLimitDialog(BudgetItem item) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Hạn mức ngân sách");
        
        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        int paddingPx = (int) (20 * getResources().getDisplayMetrics().density);
        container.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
        
        TextView tvCategoryLabel = new TextView(requireContext());
        tvCategoryLabel.setText("Danh mục: " + item.getCategoryName());
        tvCategoryLabel.setTextSize(15f);
        tvCategoryLabel.setPadding(0, 0, 0, (int) (10 * getResources().getDisplayMetrics().density));
        container.addView(tvCategoryLabel);
        
        android.widget.EditText edtLimit = new android.widget.EditText(requireContext());
        edtLimit.setHint("Nhập hạn mức chi tiêu mới (đ)");
        edtLimit.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        if (item.getLimitAmount() > 0) {
            edtLimit.setText(String.valueOf((long) item.getLimitAmount()));
        }
        container.addView(edtLimit);
        
        builder.setView(container);
        builder.setPositiveButton("LƯU", null);
        builder.setNegativeButton("HỦY", (dialog, which) -> dialog.dismiss());
        
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();
        
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String limitStr = edtLimit.getText().toString().trim();
            if (android.text.TextUtils.isEmpty(limitStr)) {
                edtLimit.setError("Hạn mức không được để trống");
                return;
            }
            double limit;
            try {
                limit = Double.parseDouble(limitStr);
            } catch (NumberFormatException e) {
                edtLimit.setError("Hạn mức không hợp lệ");
                return;
            }
            
            dialog.dismiss();
            saveBudgetLimit(item, limit);
        });
    }

    private void saveBudgetLimit(BudgetItem item, double newLimit) {
        progressLoading.setVisibility(View.VISIBLE);
        
        com.example.walletzen.model.Budget b = new com.example.walletzen.model.Budget();
        b.setBudgetId(item.getBudgetId());
        
        String currentMonth = new java.text.SimpleDateFormat("yyyy-MM",
                java.util.Locale.getDefault()).format(new java.util.Date());
        b.setMonth(currentMonth);
        b.setCategoryLimit(newLimit);
        
        com.example.walletzen.model.Category cat = new com.example.walletzen.model.Category();
        cat.setCategoryId(item.getCategoryId());
        b.setCategory(cat);
        
        com.example.walletzen.model.User user = new com.example.walletzen.model.User();
        user.setUserId(session.getUserId());
        b.setUser(user);
        
        RetrofitClient.getApiService().setBudgetLimit(b).enqueue(new Callback<com.example.walletzen.model.Budget>() {
            @Override
            public void onResponse(Call<com.example.walletzen.model.Budget> call, Response<com.example.walletzen.model.Budget> response) {
                if (!isAdded()) return;
                if (response.isSuccessful()) {
                    Toast.makeText(requireContext(), "Đã lưu hạn mức ngân sách!", Toast.LENGTH_SHORT).show();
                    loadBudgetData();
                } else {
                    progressLoading.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Lưu hạn mức thất bại!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<com.example.walletzen.model.Budget> call, Throwable t) {
                if (!isAdded()) return;
                progressLoading.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
