package com.example.walletzen.ui.budget;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.walletzen.R;
import com.example.walletzen.data.SessionManager;
import com.example.walletzen.model.Budget;
import com.example.walletzen.model.Category;
import com.example.walletzen.model.User;
import com.example.walletzen.network.RetrofitClient;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddBudgetActivity extends AppCompatActivity {

    private EditText edtBudgetLimit;
    private TextView chipExpense, chipIncome;
    private AutoCompleteTextView edtCategoryName;
    private MaterialButton btnSaveBudget;
    private ImageView btnBack;
    private View layoutLimitContainer;

    private SessionManager session;
    private List<Category> allCategories = new ArrayList<>();
    private List<Category> filteredCategories = new ArrayList<>();
    private boolean isExpenseSelected = true; // true = CHI, false = THU

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_budget);

        session = new SessionManager(this);
        initViews();
        setupToggles();
        loadCategories();

        btnBack.setOnClickListener(v -> finish());
        btnSaveBudget.setOnClickListener(v -> saveBudget());
    }

    private void initViews() {
        edtBudgetLimit = findViewById(R.id.edtBudgetLimit);
        chipExpense = findViewById(R.id.chipExpense);
        chipIncome = findViewById(R.id.chipIncome);
        edtCategoryName = findViewById(R.id.edtCategoryName);
        btnSaveBudget = findViewById(R.id.btnSaveBudget);
        btnBack = findViewById(R.id.btnBack);
        layoutLimitContainer = findViewById(R.id.layoutLimitContainer);
    }

    private void setupToggles() {
        chipExpense.setOnClickListener(v -> {
            if (!isExpenseSelected) {
                isExpenseSelected = true;
                updateToggleUI();
            }
        });

        chipIncome.setOnClickListener(v -> {
            if (isExpenseSelected) {
                isExpenseSelected = false;
                updateToggleUI();
            }
        });
    }

    private void updateToggleUI() {
        if (isExpenseSelected) {
            chipExpense.setTextColor(Color.WHITE);
            chipExpense.setBackgroundResource(R.drawable.bg_toggle_selected);
            chipExpense.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#1E40AF")));

            chipIncome.setTextColor(Color.parseColor("#64748B"));
            chipIncome.setBackgroundColor(Color.TRANSPARENT);
            layoutLimitContainer.setVisibility(View.VISIBLE);
        } else {
            chipIncome.setTextColor(Color.WHITE);
            chipIncome.setBackgroundResource(R.drawable.bg_toggle_selected);
            chipIncome.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#1E40AF")));

            chipExpense.setTextColor(Color.parseColor("#64748B"));
            chipExpense.setBackgroundColor(Color.TRANSPARENT);
            layoutLimitContainer.setVisibility(View.GONE);
        }

        filterCategoriesByType();
    }

    private void loadCategories() {
        RetrofitClient.getApiService().getCategories(session.getUserId()).enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allCategories = response.body();
                    filterCategoriesByType();
                } else {
                    Toast.makeText(AddBudgetActivity.this, "Không thể tải danh mục từ máy chủ", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {
                Toast.makeText(AddBudgetActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterCategoriesByType() {
        filteredCategories.clear();
        String targetType = isExpenseSelected ? "CHI" : "THU";
        for (Category c : allCategories) {
            if (targetType.equals(c.getType())) {
                filteredCategories.add(c);
            }
        }
        setupCategoryAutoComplete();
    }

    private void setupCategoryAutoComplete() {
        if (filteredCategories.isEmpty()) {
            edtCategoryName.setAdapter(null);
            return;
        }

        List<String> names = new ArrayList<>();
        for (Category c : filteredCategories) {
            names.add(c.getCategoryName());
        }

        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                names
        );
        edtCategoryName.setAdapter(adapter);

        // Optional: show dropdown on click
        edtCategoryName.setOnClickListener(v -> edtCategoryName.showDropDown());
    }

    private void saveBudget() {
        double limit = 0.0;
        if (isExpenseSelected) {
            String limitStr = edtBudgetLimit.getText().toString().trim();
            if (TextUtils.isEmpty(limitStr)) {
                edtBudgetLimit.setError("Vui lòng nhập hạn mức");
                return;
            }
            try {
                limit = Double.parseDouble(limitStr);
            } catch (NumberFormatException e) {
                edtBudgetLimit.setError("Hạn mức không hợp lệ");
                return;
            }
        }

        String catName = edtCategoryName.getText().toString().trim();
        if (TextUtils.isEmpty(catName)) {
            edtCategoryName.setError("Vui lòng nhập tên danh mục");
            return;
        }

        // Check for matching category case-insensitively
        Category match = null;
        for (Category c : filteredCategories) {
            if (c.getCategoryName().equalsIgnoreCase(catName)) {
                match = c;
                break;
            }
        }

        if (match != null) {
            btnSaveBudget.setEnabled(false);
            saveBudgetWithCategory(match, limit);
        } else {
            createCategoryAndSaveBudget(catName, limit);
        }
    }

    private void createCategoryAndSaveBudget(String name, double limit) {
        btnSaveBudget.setEnabled(false);
        String targetType = isExpenseSelected ? "CHI" : "THU";
        Category newCat = new Category(null, name, targetType);
        User user = new User();
        user.setUserId(session.getUserId());
        newCat.setUser(user);

        RetrofitClient.getApiService().createCategory(newCat).enqueue(new Callback<Category>() {
            @Override
            public void onResponse(Call<Category> call, Response<Category> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Category created = response.body();
                    saveBudgetWithCategory(created, limit);
                } else {
                    btnSaveBudget.setEnabled(true);
                    Toast.makeText(AddBudgetActivity.this, "Tạo danh mục tự động thất bại!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Category> call, Throwable t) {
                btnSaveBudget.setEnabled(true);
                Toast.makeText(AddBudgetActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveBudgetWithCategory(Category cat, double limit) {
        Budget budget = new Budget();
        String currentMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());
        budget.setMonth(currentMonth);
        budget.setCategoryLimit(limit);
        budget.setCategory(cat);

        User user = new User();
        user.setUserId(session.getUserId());
        budget.setUser(user);

        RetrofitClient.getApiService().setBudgetLimit(budget).enqueue(new Callback<Budget>() {
            @Override
            public void onResponse(Call<Budget> call, Response<Budget> response) {
                btnSaveBudget.setEnabled(true);
                if (response.isSuccessful()) {
                    Toast.makeText(AddBudgetActivity.this, "Đã lưu hạn mức ngân sách!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(AddBudgetActivity.this, "Lưu hạn mức thất bại!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Budget> call, Throwable t) {
                btnSaveBudget.setEnabled(true);
                Toast.makeText(AddBudgetActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
