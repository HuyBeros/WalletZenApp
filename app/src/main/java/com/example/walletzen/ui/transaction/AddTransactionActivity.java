package com.example.walletzen.ui.transaction;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.example.walletzen.R;
import com.example.walletzen.data.SessionManager;
import com.example.walletzen.model.Category;
import com.example.walletzen.model.Transaction;
import com.example.walletzen.model.User;
import com.example.walletzen.network.RetrofitClient;
import com.example.walletzen.network.TransactionResponse;
import com.example.walletzen.ui.home.HomeActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddTransactionActivity extends AppCompatActivity {

    private TextInputEditText edtAmount, edtNote, edtDate;
    private ChipGroup chipGroupType;
    private Chip chipExpense, chipIncome;
    private AutoCompleteTextView spinnerCategory;
    private int selectedCategoryIndex = -1;
    private MaterialButton btnSave;
    private ImageView btnBack;
    private ProgressBar progressBar;

    private List<Category> categoryList = new ArrayList<>();
    private String selectedType = "CHI"; // default
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_transaction);

        session = new SessionManager(this);

        initViews();
        loadCategories("CHI");
        setupDatePicker();
    }

    private void initViews() {
        edtAmount = findViewById(R.id.edtAmount);
        edtNote = findViewById(R.id.edtNote);
        edtDate = findViewById(R.id.edtDate);
        chipGroupType = findViewById(R.id.chipGroupType);
        chipExpense = findViewById(R.id.chipExpense);
        chipIncome = findViewById(R.id.chipIncome);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerCategory.setOnItemClickListener((parent, view, position, id) -> {
            selectedCategoryIndex = position;
        });
        btnSave = findViewById(R.id.btnSave);
        btnBack = findViewById(R.id.btnBack);
        progressBar = findViewById(R.id.progressBar);

        // Default date = today
        Calendar cal = Calendar.getInstance();
        String today = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
        edtDate.setText(today);

        // Chip type change
        chipGroupType.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chipExpense)) {
                selectedType = "CHI";
                loadCategories("CHI");
            } else if (checkedIds.contains(R.id.chipIncome)) {
                selectedType = "THU";
                loadCategories("THU");
            }
        });

        btnBack.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveTransaction());
    }

    private void setupDatePicker() {
        edtDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, day) -> {
                String date = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day);
                edtDate.setText(date);
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });
    }

    private void loadCategories(String type) {
        RetrofitClient.getApiService().getCategoriesByType(type).enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    categoryList.clear();
                    categoryList.addAll(response.body());
                    List<String> names = new ArrayList<>();
                    for (Category c : categoryList) {
                        names.add(c.getIcon() + "  " + c.getCategoryName());
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(AddTransactionActivity.this,
                            android.R.layout.simple_dropdown_item_1line, names);
                    spinnerCategory.setAdapter(adapter);
                    selectedCategoryIndex = -1;
                    spinnerCategory.setText("", false);
                    if (names.isEmpty()) {
                        Toast.makeText(AddTransactionActivity.this, "Chưa có danh mục cho loại này!", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {
                Toast.makeText(AddTransactionActivity.this, "Lỗi tải danh mục", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveTransaction() {
        String amountStr = edtAmount.getText().toString().trim();
        String note = edtNote.getText().toString().trim();
        String date = edtDate.getText().toString().trim();

        if (TextUtils.isEmpty(amountStr)) {
            edtAmount.setError("Nhập số tiền");
            return;
        }
        if (categoryList.isEmpty()) {
            Toast.makeText(this, "Chưa chọn danh mục", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try { amount = Double.parseDouble(amountStr); }
        catch (NumberFormatException e) {
            edtAmount.setError("Số tiền không hợp lệ");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);

        if (selectedCategoryIndex < 0 || selectedCategoryIndex >= categoryList.size()) {
            Toast.makeText(this, "Vui lòng chọn danh mục", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            btnSave.setEnabled(true);
            return;
        }
        Category selectedCat = categoryList.get(selectedCategoryIndex);

        User user = new User();
        user.setUserId(session.getUserId());

        Transaction transaction = new Transaction(amount, date, note, selectedType, user, selectedCat);

        RetrofitClient.getApiService().createTransaction(transaction).enqueue(new Callback<TransactionResponse>() {
            @Override
            public void onResponse(Call<TransactionResponse> call, Response<TransactionResponse> response) {
                progressBar.setVisibility(View.GONE);
                btnSave.setEnabled(true);
                if (response.isSuccessful()) {
                    String msg = "Đã lưu giao dịch!";
                    if (response.body() != null && response.body().getBudgetMessage() != null
                            && !response.body().getBudgetMessage().isEmpty()) {
                        msg += "\n⚠️ " + response.body().getBudgetMessage();
                    }
                    Toast.makeText(AddTransactionActivity.this, msg, Toast.LENGTH_LONG).show();
                    startActivity(new Intent(AddTransactionActivity.this, HomeActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                    finish();
                } else {
                    Toast.makeText(AddTransactionActivity.this, "Lưu thất bại!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<TransactionResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnSave.setEnabled(true);
                Toast.makeText(AddTransactionActivity.this,
                        "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}