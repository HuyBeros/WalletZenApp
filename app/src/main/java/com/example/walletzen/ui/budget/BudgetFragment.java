package com.example.walletzen.ui.budget;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.walletzen.R;
import com.example.walletzen.adapter.BudgetAdapter;
import com.example.walletzen.data.SessionManager;
import com.example.walletzen.model.Budget;
import com.example.walletzen.model.BudgetItem;
import com.example.walletzen.model.Category;
import com.example.walletzen.model.Transaction;
import com.example.walletzen.model.User;
import com.example.walletzen.network.RetrofitClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

            @Override
            public void onDeleteClick(BudgetItem item) {
                confirmDeleteCategory(item);
            }

            @Override
            public void onEditNameClick(BudgetItem item) {
                showEditNameDialog(item);
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
                                                Set<String> seenCategories = new HashSet<>();
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
                                                Set<String> seenCategories = new HashSet<>();
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

    // ====== EDIT NAME DIALOG ======

    private void showEditNameDialog(BudgetItem item) {
        if (!isAdded() || item.getCategoryId() == null) {
            Toast.makeText(requireContext(), "Không thể sửa danh mục này", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Sửa tên danh mục");

        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        int p = (int) (20 * getResources().getDisplayMetrics().density);
        container.setPadding(p, p, p, p);

        EditText edtName = new EditText(requireContext());
        edtName.setHint("Tên danh mục");
        edtName.setSingleLine(true);
        edtName.setText(item.getCategoryName());
        container.addView(edtName);

        // If expense, also show budget limit field
        final EditText[] edtLimitHolder = {null};
        if (item.isExpense()) {
            TextView tvLimitLabel = new TextView(requireContext());
            tvLimitLabel.setText("Hạn mức chi tiêu tháng này (đ):");
            tvLimitLabel.setTextSize(13f);
            tvLimitLabel.setTextColor(0xFF64748B);
            tvLimitLabel.setPadding(0, (int) (14 * getResources().getDisplayMetrics().density), 0,
                    (int) (4 * getResources().getDisplayMetrics().density));
            container.addView(tvLimitLabel);

            TextView tvSpentInfo = new TextView(requireContext());
            tvSpentInfo.setText("Đã chi: " + formatMoney(item.getCurrentAmount()));
            tvSpentInfo.setTextSize(12f);
            tvSpentInfo.setTextColor(0xFF94A3B8);
            tvSpentInfo.setPadding(0, 0, 0, (int) (6 * getResources().getDisplayMetrics().density));
            container.addView(tvSpentInfo);

            EditText edtLimit = new EditText(requireContext());
            edtLimit.setHint("Hạn mức (để trống = không đặt)");
            edtLimit.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            if (item.getLimitAmount() > 0) {
                edtLimit.setText(String.valueOf((long) item.getLimitAmount()));
            }
            container.addView(edtLimit);
            edtLimitHolder[0] = edtLimit;
        }

        builder.setView(container);
        builder.setNegativeButton("Hủy", null);
        builder.setPositiveButton("Lưu", null);

        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = edtName.getText().toString().trim();
            if (TextUtils.isEmpty(name)) {
                edtName.setError("Không được để trống");
                return;
            }
            dialog.dismiss();

            // Update category name
            String type = item.isExpense() ? "CHI" : "THU";
            Category cat = new Category(item.getCategoryId(), name, type);
            User u = new User();
            u.setUserId(session.getUserId());
            cat.setUser(u);

            progressLoading.setVisibility(View.VISIBLE);
            RetrofitClient.getApiService().updateCategory(item.getCategoryId(), session.getUserId(), cat)
                    .enqueue(new Callback<Category>() {
                        @Override
                        public void onResponse(Call<Category> call, Response<Category> r) {
                            if (!isAdded()) return;
                            if (r.isSuccessful()) {
                                Toast.makeText(requireContext(), "Đã cập nhật tên danh mục!", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(requireContext(), "Cập nhật tên thất bại!", Toast.LENGTH_SHORT).show();
                            }
                            // Also save budget limit if expense
                            if (edtLimitHolder[0] != null) {
                                String limitStr = edtLimitHolder[0].getText().toString().trim();
                                if (!TextUtils.isEmpty(limitStr)) {
                                    try {
                                        double limit = Double.parseDouble(limitStr);
                                        saveBudgetLimit(item, limit);
                                        return; // saveBudgetLimit will reload data
                                    } catch (NumberFormatException ignored) {}
                                }
                            }
                            progressLoading.setVisibility(View.GONE);
                            loadBudgetData();
                        }

                        @Override
                        public void onFailure(Call<Category> call, Throwable t) {
                            if (!isAdded()) return;
                            progressLoading.setVisibility(View.GONE);
                            Toast.makeText(requireContext(), "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }

    // ====== DELETE CONFIRMATION ======

    private void confirmDeleteCategory(BudgetItem item) {
        if (!isAdded() || item.getCategoryId() == null) {
            Toast.makeText(requireContext(), "Không thể xóa danh mục này", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("⚠️ Xóa danh mục")
                .setMessage("Bạn có chắc chắn muốn xóa danh mục \"" + item.getCategoryName() + "\"?\n\n"
                        + "Chú ý: TẤT CẢ giao dịch và ngân sách liên quan đến danh mục này cũng sẽ bị xóa vĩnh viễn!")
                .setPositiveButton("Xóa", (d, w) -> callDeleteCategory(item))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void callDeleteCategory(BudgetItem item) {
        if (!isAdded()) return;
        progressLoading.setVisibility(View.VISIBLE);
        rvBudget.setVisibility(View.GONE);

        RetrofitClient.getApiService().deleteCategory(item.getCategoryId(), session.getUserId())
                .enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(Call<String> call, Response<String> r) {
                        if (!isAdded()) return;
                        progressLoading.setVisibility(View.GONE);
                        if (r.isSuccessful()) {
                            Toast.makeText(requireContext(),
                                    "Đã xóa danh mục \"" + item.getCategoryName() + "\" thành công!",
                                    Toast.LENGTH_SHORT).show();
                            loadBudgetData();
                        } else {
                            rvBudget.setVisibility(View.VISIBLE);
                            String errorMsg = "Xóa thất bại!";
                            try {
                                if (r.errorBody() != null) {
                                    errorMsg = r.errorBody().string();
                                }
                            } catch (Exception ignored) {}
                            Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<String> call, Throwable t) {
                        if (!isAdded()) return;
                        progressLoading.setVisibility(View.GONE);
                        rvBudget.setVisibility(View.VISIBLE);
                        Toast.makeText(requireContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ====== EDIT BUDGET LIMIT DIALOG ======

    private void showEditLimitDialog(BudgetItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
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
        
        EditText edtLimit = new EditText(requireContext());
        edtLimit.setHint("Nhập hạn mức chi tiêu mới (đ)");
        edtLimit.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        if (item.getLimitAmount() > 0) {
            edtLimit.setText(String.valueOf((long) item.getLimitAmount()));
        }
        container.addView(edtLimit);
        
        builder.setView(container);
        builder.setPositiveButton("LƯU", null);
        builder.setNegativeButton("HỦY", (dialog, which) -> dialog.dismiss());
        
        AlertDialog dialog = builder.create();
        dialog.show();
        
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String limitStr = edtLimit.getText().toString().trim();
            if (TextUtils.isEmpty(limitStr)) {
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

    private String formatMoney(double v) {
        return String.format("%,.0fđ", v).replace(",", ".");
    }
}
