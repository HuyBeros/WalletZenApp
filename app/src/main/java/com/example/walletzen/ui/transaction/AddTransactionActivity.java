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
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddTransactionActivity extends AppCompatActivity {

    private TextInputEditText edtAmount, edtNote;
    private TextView chipExpense, chipIncome;
    private AutoCompleteTextView spinnerCategory;
    private int selectedCategoryIndex = -1;
    private MaterialButton btnSave;
    private ImageView btnBack;
    private ProgressBar progressBar;
    private TextView tvAddNewCategory;

    private List<Category> categoryList = new ArrayList<>();
    private String selectedType = "CHI"; // default
    private SessionManager session;
    private long editTransactionId = -1;
    private long initialCategoryId = -1;
    private TextView tvTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_transaction);

        session = new SessionManager(this);

        initViews();
        
        Intent intent = getIntent();
        if (intent.hasExtra("transactionId")) {
            editTransactionId = intent.getLongExtra("transactionId", -1);
            double amount = intent.getDoubleExtra("amount", 0);
            String note = intent.getStringExtra("note");
            selectedType = intent.getStringExtra("type");
            initialCategoryId = intent.getLongExtra("categoryId", -1);

            if (tvTitle != null) tvTitle.setText("Chỉnh sửa giao dịch");
            btnSave.setText("LƯU THAY ĐỔI");
            
            if (amount != 0) {
                edtAmount.setText(String.valueOf((long) Math.abs(amount)));
            }
            edtNote.setText(note);

            if ("THU".equals(selectedType)) {
                setToggleSelected(chipIncome, chipExpense);
            } else {
                setToggleSelected(chipExpense, chipIncome);
            }
        }
        
        loadCategories(selectedType);
    }

    private void initViews() {
        tvTitle     = findViewById(R.id.tvTitle);
        edtAmount   = findViewById(R.id.edtAmount);
        edtNote     = findViewById(R.id.edtNote);
        // edtDate removed
        chipExpense = findViewById(R.id.chipExpense);
        chipIncome  = findViewById(R.id.chipIncome);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        btnSave     = findViewById(R.id.btnSave);
        btnBack     = (ImageView) findViewById(R.id.btnBack);
        progressBar = findViewById(R.id.progressBar);
        tvAddNewCategory = findViewById(R.id.tvAddNewCategory);

        if (tvAddNewCategory != null) {
            tvAddNewCategory.setOnClickListener(v -> showAddCategoryDialog());
        }

        spinnerCategory.setOnItemClickListener((parent, view, position, id) ->
                selectedCategoryIndex = position);

        // Default date = today
        // Date is now handled automatically when saving

        // Initial visual state — Chi tiêu selected
        setToggleSelected(chipExpense, chipIncome);

        // Toggle click listeners
        chipExpense.setOnClickListener(v -> {
            selectedType = "CHI";
            setToggleSelected(chipExpense, chipIncome);
            loadCategories("CHI");
        });
        chipIncome.setOnClickListener(v -> {
            selectedType = "THU";
            setToggleSelected(chipIncome, chipExpense);
            loadCategories("THU");
        });

        btnBack.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveTransaction());
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
                    if (editTransactionId != -1 && initialCategoryId != -1) {
                        for (int i = 0; i < categoryList.size(); i++) {
                            Category c = categoryList.get(i);
                            if (c.getCategoryId() == initialCategoryId) {
                                selectedCategoryIndex = i;
                                spinnerCategory.setText(c.getIcon() + "  " + c.getCategoryName(), false);
                                break;
                            }
                        }
                    } else {
                        selectedCategoryIndex = -1;
                        spinnerCategory.setText("", false);
                    }
                    if (names.isEmpty()) {
                        Toast.makeText(AddTransactionActivity.this,
                                "Chưa có danh mục cho loại này!", Toast.LENGTH_SHORT).show();
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
        String note      = edtNote.getText().toString().trim();
        Calendar cal = Calendar.getInstance();
        String date = String.format(Locale.getDefault(), "%04d-%02d-%02dT%02d:%02d:%02d",
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH),
                cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND));

        if (TextUtils.isEmpty(amountStr)) {
            edtAmount.setError("Nhập số tiền");
            return;
        }
        if (categoryList.isEmpty()) {
            Toast.makeText(this, "Chưa chọn danh mục", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            edtAmount.setError("Số tiền không hợp lệ");
            return;
        }

        if (selectedCategoryIndex < 0 || selectedCategoryIndex >= categoryList.size()) {
            Toast.makeText(this, "Vui lòng chọn danh mục", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);

        Category selectedCat = categoryList.get(selectedCategoryIndex);

        User user = new User();
        user.setUserId(session.getUserId());

        Transaction transaction = new Transaction(amount, date, note, selectedType, user, selectedCat);

        if (editTransactionId != -1) {
            RetrofitClient.getApiService().updateTransaction(editTransactionId, transaction, session.getUserId()).enqueue(new Callback<TransactionResponse>() {
                @Override
                public void onResponse(Call<TransactionResponse> call, Response<TransactionResponse> response) {
                    progressBar.setVisibility(View.GONE);
                    btnSave.setEnabled(true);
                    if (response.isSuccessful()) {
                        Toast.makeText(AddTransactionActivity.this, "Đã cập nhật giao dịch!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(AddTransactionActivity.this, HomeActivity.class)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                        finish();
                    } else {
                        Toast.makeText(AddTransactionActivity.this, "Cập nhật thất bại!", Toast.LENGTH_SHORT).show();
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
        } else {
            RetrofitClient.getApiService().createTransaction(transaction).enqueue(new Callback<TransactionResponse>() {
                @Override
                public void onResponse(Call<TransactionResponse> call, Response<TransactionResponse> response) {
                    progressBar.setVisibility(View.GONE);
                    btnSave.setEnabled(true);
                    if (response.isSuccessful()) {
                        Toast.makeText(AddTransactionActivity.this, "Đã lưu giao dịch!", Toast.LENGTH_SHORT).show();
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

    private void showAddCategoryDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Thêm danh mục mới");

        // Set up container layout
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int paddingPx = (int) (20 * getResources().getDisplayMetrics().density);
        container.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);

        // Name input
        EditText edtCatName = new EditText(this);
        edtCatName.setHint("Tên danh mục (ví dụ: Giải trí, Thú cưng...)");
        edtCatName.setSingleLine(true);
        container.addView(edtCatName);

        // Type selection layout
        LinearLayout toggleLayout = new LinearLayout(this);
        toggleLayout.setOrientation(LinearLayout.HORIZONTAL);
        toggleLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
        toggleLayout.setPadding(0, (int) (12 * getResources().getDisplayMetrics().density), 0, 0);

        TextView tvTypeLabel = new TextView(this);
        tvTypeLabel.setText("Loại: ");
        tvTypeLabel.setTextSize(14f);
        toggleLayout.addView(tvTypeLabel);

        RadioGroup rgType = new RadioGroup(this);
        rgType.setOrientation(RadioGroup.HORIZONTAL);

        RadioButton rbExpense = new RadioButton(this);
        rbExpense.setText("Chi tiêu");
        rbExpense.setId(View.generateViewId());
        rgType.addView(rbExpense);

        RadioButton rbIncome = new RadioButton(this);
        rbIncome.setText("Thu nhập");
        rbIncome.setId(View.generateViewId());
        rgType.addView(rbIncome);

        // Pre-select active transaction type
        if ("THU".equals(selectedType)) {
            rbIncome.setChecked(true);
        } else {
            rbExpense.setChecked(true);
        }

        toggleLayout.addView(rgType);
        container.addView(toggleLayout);

        builder.setView(container);

        builder.setPositiveButton("THÊM", null);
        builder.setNegativeButton("HỦY", (dialog, which) -> dialog.dismiss());

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();

        // Custom validation on positive button click
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String catName = edtCatName.getText().toString().trim();
            if (TextUtils.isEmpty(catName)) {
                edtCatName.setError("Tên danh mục không được để trống");
                return;
            }

            String catType = rbIncome.isChecked() ? "THU" : "CHI";

            dialog.dismiss();
            createNewCategory(catName, catType);
        });
    }

    private void createNewCategory(String name, String type) {
        progressBar.setVisibility(View.VISIBLE);
        Category newCat = new Category(null, name, type);

        RetrofitClient.getApiService().createCategory(newCat).enqueue(new Callback<Category>() {
            @Override
            public void onResponse(Call<Category> call, Response<Category> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    Category created = response.body();
                    Toast.makeText(AddTransactionActivity.this, "Đã thêm danh mục: " + created.getCategoryName(), Toast.LENGTH_SHORT).show();

                    // switch selectedType if created type is different
                    if (!selectedType.equals(created.getType())) {
                        selectedType = created.getType();
                        if ("THU".equals(selectedType)) {
                            setToggleSelected(chipIncome, chipExpense);
                        } else {
                            setToggleSelected(chipExpense, chipIncome);
                        }
                    }

                    // Reload category list and pre-select the newly created one
                    loadAndSelectCategory(created.getType(), created.getCategoryId());
                } else {
                    Toast.makeText(AddTransactionActivity.this, "Không thể thêm danh mục", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Category> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AddTransactionActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadAndSelectCategory(String type, Long preSelectId) {
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

                    // Search and pre-select
                    for (int i = 0; i < categoryList.size(); i++) {
                        Category c = categoryList.get(i);
                        if (c.getCategoryId() != null && c.getCategoryId().equals(preSelectId)) {
                            selectedCategoryIndex = i;
                            spinnerCategory.setText(c.getIcon() + "  " + c.getCategoryName(), false);
                            break;
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {
                Toast.makeText(AddTransactionActivity.this, "Lỗi tải danh mục", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Switches the pill segment toggle visual state.
     * @param selected   Active tab — white background, blue text
     * @param unselected Inactive tab — transparent, faded white text
     */
    private void setToggleSelected(TextView selected, TextView unselected) {
        selected.setBackgroundResource(R.drawable.bg_toggle_selected);
        selected.setTextColor(getResources().getColor(R.color.primary_light, getTheme()));
        unselected.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        unselected.setTextColor((int) 0xCCE8F4FFL);
    }
}