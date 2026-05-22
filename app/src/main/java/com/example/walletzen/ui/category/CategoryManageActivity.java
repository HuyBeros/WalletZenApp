package com.example.walletzen.ui.category;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.walletzen.R;
import com.example.walletzen.adapter.CategoryManageAdapter;
import com.example.walletzen.data.SessionManager;
import com.example.walletzen.model.Budget;
import com.example.walletzen.model.Category;
import com.example.walletzen.model.Transaction;
import com.example.walletzen.model.User;
import com.example.walletzen.network.RetrofitClient;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.tabs.TabLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CategoryManageActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private RecyclerView rvCategories;
    private ProgressBar progressLoading;
    private TextView tvEmpty;

    private CategoryManageAdapter adapter;
    private List<Category> categoryList = new ArrayList<>();
    private String currentTab = "CHI"; // CHI or THU
    private SessionManager session;
    private String currentMonth;

    // Aggregated budget data
    private Map<Long, Double> spentMap = new HashMap<>();
    private Map<Long, Double> limitMap = new HashMap<>();
    private Map<Long, Long> budgetIdMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_manage);

        session = new SessionManager(this);
        currentMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());

        initViews();
        setupTabs();
        loadData();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnAddCategory).setOnClickListener(v -> showAddEditDialog(null));
    }

    private void initViews() {
        tabLayout = findViewById(R.id.tabLayoutCat);
        rvCategories = findViewById(R.id.rvCategories);
        progressLoading = findViewById(R.id.progressLoading);
        tvEmpty = findViewById(R.id.tvEmpty);

        adapter = new CategoryManageAdapter(this, categoryList, new CategoryManageAdapter.Listener() {
            @Override
            public void onEdit(Category category) { showAddEditDialog(category); }
            @Override
            public void onDelete(Category category) { confirmDelete(category); }
        });

        rvCategories.setLayoutManager(new LinearLayoutManager(this));
        rvCategories.setAdapter(adapter);
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("CHI TIÊU"));
        tabLayout.addTab(tabLayout.newTab().setText("THU NHẬP"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition() == 0 ? "CHI" : "THU";
                loadData();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void loadData() {
        showLoading(true);
        spentMap.clear();
        limitMap.clear();
        budgetIdMap.clear();

        // Load categories by type, then load budget+transaction data if CHI
        RetrofitClient.getApiService().getCategoriesByType(session.getUserId(), currentTab)
                .enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    categoryList.clear();
                    categoryList.addAll(response.body());

                    if ("CHI".equals(currentTab)) {
                        loadBudgetAndSpending();
                    } else {
                        adapter.clearBudgetData();
                        showLoading(false);
                        refreshList();
                    }
                } else {
                    showLoading(false);
                    showEmpty(true);
                }
            }
            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {
                showLoading(false);
                Toast.makeText(CategoryManageActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                showEmpty(true);
            }
        });
    }

    /** Load both budget limits and actual spending for the current month */
    private void loadBudgetAndSpending() {
        RetrofitClient.getApiService().getBudgets(session.getUserId())
                .enqueue(new Callback<List<Budget>>() {
            @Override
            public void onResponse(Call<List<Budget>> call, Response<List<Budget>> responseBudget) {
                if (responseBudget.isSuccessful() && responseBudget.body() != null) {
                    for (Budget b : responseBudget.body()) {
                        if (b.getMonth() != null && b.getMonth().equals(currentMonth)
                                && b.getCategory() != null && "CHI".equals(b.getCategory().getType())) {
                            Long catId = b.getCategory().getCategoryId();
                            if (catId != null) {
                                limitMap.put(catId, b.getCategoryLimit() != null ? b.getCategoryLimit() : 0.0);
                                budgetIdMap.put(catId, b.getBudgetId());
                            }
                        }
                    }
                }

                // Now load transactions for spending
                RetrofitClient.getApiService().getTransactions(session.getUserId())
                        .enqueue(new Callback<List<Transaction>>() {
                    @Override
                    public void onResponse(Call<List<Transaction>> call2, Response<List<Transaction>> responseTx) {
                        if (responseTx.isSuccessful() && responseTx.body() != null) {
                            for (Transaction t : responseTx.body()) {
                                String d = t.getDate();
                                if ("CHI".equals(t.getType()) && d != null && d.startsWith(currentMonth)
                                        && t.getCategory() != null && t.getCategory().getCategoryId() != null) {
                                    Long catId = t.getCategory().getCategoryId();
                                    double amt = t.getAmount() != null ? Math.abs(t.getAmount()) : 0;
                                    spentMap.put(catId, spentMap.getOrDefault(catId, 0.0) + amt);
                                }
                            }
                        }
                         adapter.setSpentData(spentMap, limitMap, budgetIdMap);
                         showLoading(false);
                         refreshList();
                     }
                     @Override
                     public void onFailure(Call<List<Transaction>> call2, Throwable t) {
                         adapter.setSpentData(spentMap, limitMap, budgetIdMap);
                         showLoading(false);
                         refreshList();
                     }
                });
            }
            @Override
            public void onFailure(Call<List<Budget>> call, Throwable t) {
                showLoading(false);
                refreshList();
            }
        });
    }



    // ====== DIALOGS ======

    private void showAddEditDialog(Category existing) {
        boolean isEdit = existing != null;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int p = dp(20);
        container.setPadding(p, p, p, p);

        EditText edtName = new EditText(this);
        edtName.setHint("Tên danh mục (VD: Ăn uống, Lương...)");
        edtName.setSingleLine(true);

        if (isEdit) {
            // EDIT MODE: sửa tên + hạn mức (nếu là CHI)
            builder.setTitle("Sửa danh mục");
            edtName.setText(existing.getCategoryName());
            container.addView(edtName);

            boolean isExpense = "CHI".equals(existing.getType());
            final EditText[] edtLimitHolder = {null};

            if (isExpense && existing.getCategoryId() != null) {
                double currentLimit = limitMap.getOrDefault(existing.getCategoryId(), 0.0);
                double currentSpent = spentMap.getOrDefault(existing.getCategoryId(), 0.0);

                TextView tvLimitLabel = new TextView(this);
                tvLimitLabel.setText("Hạn mức chi tiêu tháng này (đ):");
                tvLimitLabel.setTextSize(13f);
                tvLimitLabel.setTextColor(0xFF64748B);
                tvLimitLabel.setPadding(0, dp(14), 0, dp(4));
                container.addView(tvLimitLabel);

                TextView tvSpentInfo = new TextView(this);
                tvSpentInfo.setText("Đã chi: " + formatMoney(currentSpent));
                tvSpentInfo.setTextSize(12f);
                tvSpentInfo.setTextColor(0xFF94A3B8);
                tvSpentInfo.setPadding(0, 0, 0, dp(6));
                container.addView(tvSpentInfo);

                EditText edtLimit = new EditText(this);
                edtLimit.setHint("Hạn mức (để trống = không đặt)");
                edtLimit.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                if (currentLimit > 0) edtLimit.setText(String.valueOf((long) currentLimit));
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
                if (TextUtils.isEmpty(name)) { edtName.setError("Không được để trống"); return; }
                dialog.dismiss();

                // Cập nhật tên (giữ nguyên type)
                Category cat = new Category(existing.getCategoryId(), name, existing.getType());
                cat.setUserId(session.getUserId());
                callUpdateCategory(existing.getCategoryId(), cat);

                // Nếu là CHI, lưu hạn mức luôn
                if (edtLimitHolder[0] != null) {
                    String limitStr = edtLimitHolder[0].getText().toString().trim();
                    if (!TextUtils.isEmpty(limitStr)) {
                        try {
                            double limit = Double.parseDouble(limitStr);
                            saveBudgetLimit(cat, limit);
                        } catch (NumberFormatException ignored) {}
                    } else {
                        // Nếu để trống + đang có hạn mức -> xóa hạn mức
                        Long budgetId = budgetIdMap.get(existing.getCategoryId());
                        if (budgetId != null) deleteBudgetLimit(budgetId);
                    }
                }
            });

        } else {
            // ADD MODE: chọn cả tên, loại và hạn mức (nếu là CHI)
            builder.setTitle("Thêm danh mục mới");
            container.addView(edtName);

            TextView tvTypeLabel = new TextView(this);
            tvTypeLabel.setText("Loại:");
            tvTypeLabel.setTextSize(14f);
            tvTypeLabel.setPadding(0, dp(14), 0, 0);
            container.addView(tvTypeLabel);

            RadioGroup rgType = new RadioGroup(this);
            rgType.setOrientation(RadioGroup.HORIZONTAL);
            RadioButton rbChi = new RadioButton(this);
            rbChi.setText("Chi tiêu");
            rbChi.setId(View.generateViewId());
            RadioButton rbThu = new RadioButton(this);
            rbThu.setText("Thu nhập");
            rbThu.setId(View.generateViewId());
            rgType.addView(rbChi);
            rgType.addView(rbThu);
            container.addView(rgType);

            // Thêm ô nhập hạn mức (chỉ cho CHI TIÊU)
            TextView tvLimitLabel = new TextView(this);
            tvLimitLabel.setText("Hạn mức chi tiêu tháng này (đ):");
            tvLimitLabel.setTextSize(13f);
            tvLimitLabel.setTextColor(0xFF64748B);
            tvLimitLabel.setPadding(0, dp(14), 0, dp(4));
            container.addView(tvLimitLabel);

            EditText edtLimit = new EditText(this);
            edtLimit.setHint("Hạn mức (để trống = không đặt)");
            edtLimit.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            container.addView(edtLimit);

            // Lắng nghe sự kiện đổi loại để hiện/ẩn ô hạn mức
            rgType.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId == rbChi.getId()) {
                    tvLimitLabel.setVisibility(View.VISIBLE);
                    edtLimit.setVisibility(View.VISIBLE);
                } else {
                    tvLimitLabel.setVisibility(View.GONE);
                    edtLimit.setVisibility(View.GONE);
                    edtLimit.setText(""); // Xóa text hạn mức nếu chọn thu nhập
                }
            });

            // Set checked mặc định
            if ("THU".equals(currentTab)) {
                rbThu.setChecked(true);
                tvLimitLabel.setVisibility(View.GONE);
                edtLimit.setVisibility(View.GONE);
            } else {
                rbChi.setChecked(true);
                tvLimitLabel.setVisibility(View.VISIBLE);
                edtLimit.setVisibility(View.VISIBLE);
            }

            builder.setView(container);
            builder.setNegativeButton("Hủy", null);
            builder.setPositiveButton("Thêm", null);

            AlertDialog dialog = builder.create();
            dialog.show();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String name = edtName.getText().toString().trim();
                if (TextUtils.isEmpty(name)) { edtName.setError("Không được để trống"); return; }
                String type = rbThu.isChecked() ? "THU" : "CHI";
                
                Double limit = null;
                if ("CHI".equals(type)) {
                    String limitStr = edtLimit.getText().toString().trim();
                    if (!TextUtils.isEmpty(limitStr)) {
                        try {
                            limit = Double.parseDouble(limitStr);
                        } catch (NumberFormatException ignored) {}
                    }
                }
                
                dialog.dismiss();
                Category newCategory = new Category(null, name, type);
                newCategory.setUserId(session.getUserId());
                callCreateCategory(newCategory, limit);
            });
        }
    }

    private void confirmDelete(Category cat) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa danh mục")
                .setMessage("Xóa danh mục \"" + cat.getCategoryName() + "\"?\nCác giao dịch liên quan vẫn được giữ lại.")
                .setPositiveButton("Xóa", (d, w) -> callDeleteCategory(cat.getCategoryId()))
                .setNegativeButton("Hủy", null)
                .show();
    }

    // ====== API CALLS ======

    private void callCreateCategory(Category cat, Double limit) {
        showLoading(true);
        // Log request body for debugging
        android.util.Log.d("CategoryDebug", "Creating category: name=" + cat.getCategoryName()
                + ", type=" + cat.getType() + ", userId=" + cat.getUserId());
        RetrofitClient.getApiService().createCategory(cat).enqueue(new Callback<Category>() {
            @Override
            public void onResponse(Call<Category> call, Response<Category> r) {
                if (r.isSuccessful() && r.body() != null) {
                    Category newCat = r.body();
                    Toast.makeText(CategoryManageActivity.this, "Đã thêm danh mục!", Toast.LENGTH_SHORT).show();
                    
                    if ("CHI".equals(newCat.getType()) && limit != null && limit > 0) {
                        // Gọi lưu hạn mức luôn, sau khi lưu xong sẽ tắt loading và load data mới
                        saveBudgetLimit(newCat, limit);
                    } else {
                        showLoading(false);
                        currentTab = newCat.getType();
                        int tabIdx = "CHI".equals(currentTab) ? 0 : 1;
                        tabLayout.selectTab(tabLayout.getTabAt(tabIdx));
                        loadData();
                    }
                } else {
                    showLoading(false);
                    // Log error body to understand what server expects
                    try {
                        String errorBody = r.errorBody() != null ? r.errorBody().string() : "null";
                        android.util.Log.e("CategoryDebug", "Create failed HTTP " + r.code() + ": " + errorBody);
                        Toast.makeText(CategoryManageActivity.this, "Thêm thất bại! (HTTP " + r.code() + ")", Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(CategoryManageActivity.this, "Thêm thất bại!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            @Override
            public void onFailure(Call<Category> call, Throwable t) {
                showLoading(false);
                android.util.Log.e("CategoryDebug", "Create network error: " + t.getMessage(), t);
                Toast.makeText(CategoryManageActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void callUpdateCategory(Long id, Category cat) {
        showLoading(true);
        RetrofitClient.getApiService().updateCategory(id, cat).enqueue(new Callback<Category>() {
            @Override
            public void onResponse(Call<Category> call, Response<Category> r) {
                showLoading(false);
                if (r.isSuccessful()) {
                    Toast.makeText(CategoryManageActivity.this, "Đã cập nhật danh mục!", Toast.LENGTH_SHORT).show();
                    loadData();
                } else {
                    Toast.makeText(CategoryManageActivity.this, "Cập nhật thất bại!", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<Category> call, Throwable t) {
                showLoading(false);
                Toast.makeText(CategoryManageActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void callDeleteCategory(Long id) {
        showLoading(true);
        RetrofitClient.getApiService().deleteCategory(id).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> r) {
                showLoading(false);
                if (r.isSuccessful()) {
                    Toast.makeText(CategoryManageActivity.this, "Đã xóa danh mục!", Toast.LENGTH_SHORT).show();
                    loadData();
                } else {
                    Toast.makeText(CategoryManageActivity.this, "Xóa thất bại (danh mục đang được dùng?)", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                showLoading(false);
                Toast.makeText(CategoryManageActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveBudgetLimit(Category cat, double limit) {
        showLoading(true);
        Budget b = new Budget();
        b.setMonth(currentMonth);
        b.setCategoryLimit(limit);
        b.setCategory(cat);
        User u = new User();
        u.setUserId(session.getUserId());
        b.setUser(u);

        RetrofitClient.getApiService().setBudgetLimit(b).enqueue(new Callback<Budget>() {
            @Override
            public void onResponse(Call<Budget> call, Response<Budget> r) {
                showLoading(false);
                if (r.isSuccessful()) {
                    Toast.makeText(CategoryManageActivity.this, "Đã lưu hạn mức!", Toast.LENGTH_SHORT).show();
                    loadData();
                } else {
                    Toast.makeText(CategoryManageActivity.this, "Lưu thất bại!", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<Budget> call, Throwable t) {
                showLoading(false);
                Toast.makeText(CategoryManageActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteBudgetLimit(Long budgetId) {
        showLoading(true);
        RetrofitClient.getApiService().deleteBudget(budgetId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> r) {
                showLoading(false);
                Toast.makeText(CategoryManageActivity.this, "Đã xóa hạn mức!", Toast.LENGTH_SHORT).show();
                loadData();
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                showLoading(false);
                Toast.makeText(CategoryManageActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ====== UI helpers ======

    private void refreshList() {
        if (categoryList.isEmpty()) {
            showEmpty(true);
        } else {
            showEmpty(false);
            rvCategories.setVisibility(View.VISIBLE);
            adapter.notifyDataSetChanged();
        }
    }

    private void showLoading(boolean show) {
        progressLoading.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            rvCategories.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.GONE);
        }
    }

    private void showEmpty(boolean show) {
        tvEmpty.setVisibility(show ? View.VISIBLE : View.GONE);
        rvCategories.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private String formatMoney(double v) {
        return String.format("%,.0fđ", v).replace(",", ".");
    }

    private int dp(int dpValue) {
        return (int) (dpValue * getResources().getDisplayMetrics().density);
    }
}
