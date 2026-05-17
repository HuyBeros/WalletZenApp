package com.example.walletzen.ui.transaction;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.app.DatePickerDialog;

import java.util.Calendar;
import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.walletzen.R;
import com.example.walletzen.adapter.TransactionAdapter;
import com.example.walletzen.data.SessionManager;
import com.example.walletzen.model.Transaction;
import com.example.walletzen.network.RetrofitClient;
import com.example.walletzen.ui.detail.TransactionDetailActivity;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TransactionListActivity extends AppCompatActivity {

    private RecyclerView rvTransactions;
    private EditText edtSearch;
    private ImageView btnBack;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private Spinner spinnerType;
    private TextView tvFromDate, tvToDate;
    private com.google.android.material.button.MaterialButton btnClearFilter;

    private TransactionAdapter adapter;
    private List<Transaction> allTransactions = new ArrayList<>();
    private List<Transaction> filteredTransactions = new ArrayList<>();
    private SessionManager session;

    private String selectedType = "Tất cả";
    private String selectedFromDate = null; // Format: yyyy-MM-dd
    private String selectedToDate = null; // Format: yyyy-MM-dd

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_list);

        session = new SessionManager(this);

        rvTransactions = findViewById(R.id.rvTransactions);
        edtSearch      = findViewById(R.id.edtSearch);
        btnBack        = findViewById(R.id.btnBack);
        progressBar    = findViewById(R.id.progressBar);
        tvEmpty        = findViewById(R.id.tvEmpty);
        spinnerType    = findViewById(R.id.spinnerType);
        tvFromDate     = findViewById(R.id.tvFromDate);
        tvToDate       = findViewById(R.id.tvToDate);
        btnClearFilter = findViewById(R.id.btnClearFilter);

        btnBack.setOnClickListener(v -> finish());

        // Set up spinner
        String[] types = {"Tất cả", "Thu nhập", "Chi tiêu"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, types);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        if (spinnerType != null) {
            spinnerType.setAdapter(spinnerAdapter);
            spinnerType.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    selectedType = types[position];
                    applyFilters();
                }
                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });
        }

        // Set up date pickers
        if (tvFromDate != null) {
            tvFromDate.setOnClickListener(v -> showDatePickerDialog(true));
        }
        if (tvToDate != null) {
            tvToDate.setOnClickListener(v -> showDatePickerDialog(false));
        }

        // Set up clear button
        if (btnClearFilter != null) {
            btnClearFilter.setOnClickListener(v -> {
                if (edtSearch != null) edtSearch.setText("");
                if (spinnerType != null) spinnerType.setSelection(0);
                selectedType = "Tất cả";

                selectedFromDate = null;
                if (tvFromDate != null) {
                    tvFromDate.setText("nn/mm/yyyy");
                    tvFromDate.setTextColor(getResources().getColor(R.color.text_secondary, getTheme()));
                }

                selectedToDate = null;
                if (tvToDate != null) {
                    tvToDate.setText("nn/mm/yyyy");
                    tvToDate.setTextColor(getResources().getColor(R.color.text_secondary, getTheme()));
                }

                applyFilters();
            });
        }

        adapter = new TransactionAdapter(this, filteredTransactions, t -> {
            Intent intent = new Intent(this, TransactionDetailActivity.class);
            intent.putExtra("transactionId", t.getTransactionId());
            intent.putExtra("note", t.getNote());
            intent.putExtra("amount", t.getAmount());
            intent.putExtra("date", t.getDate());
            intent.putExtra("type", t.getType());
            intent.putExtra("category", t.getCategoryName());
            intent.putExtra("icon", t.getCategoryIcon());
            if (t.getCategory() != null) {
                intent.putExtra("categoryId", t.getCategory().getCategoryId());
            }
            startActivity(intent);
        });

        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        rvTransactions.setAdapter(adapter);

        if (edtSearch != null) {
            edtSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    applyFilters();
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        loadTransactions();
    }

    private void loadTransactions() {
        progressBar.setVisibility(View.VISIBLE);
        RetrofitClient.getApiService()
                .getTransactions(session.getUserId())
                .enqueue(new Callback<List<Transaction>>() {
                    @Override
                    public void onResponse(Call<List<Transaction>> call, Response<List<Transaction>> response) {
                        progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null) {
                            allTransactions.clear();
                            allTransactions.addAll(response.body());
                            // Sort by transactionId descending (newest first)
                            java.util.Collections.sort(allTransactions, (t1, t2) -> {
                                if (t1.getTransactionId() != null && t2.getTransactionId() != null) {
                                    return t2.getTransactionId().compareTo(t1.getTransactionId());
                                }
                                return 0;
                            });
                            applyFilters();
                        } else {
                            showEmpty("Không tải được dữ liệu");
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Transaction>> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(TransactionListActivity.this,
                                "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        showEmpty("Lỗi kết nối");
                    }
                });
    }

    private void applyFilters() {
        filteredTransactions.clear();
        String query = edtSearch != null ? edtSearch.getText().toString().toLowerCase().trim() : "";

        for (Transaction t : allTransactions) {
            // 1. Search filter
            boolean matchSearch = true;
            if (!query.isEmpty()) {
                boolean matchNote = t.getNote() != null && t.getNote().toLowerCase().contains(query);
                boolean matchCategory = t.getCategoryName() != null && t.getCategoryName().toLowerCase().contains(query);
                matchSearch = matchNote || matchCategory;
            }

            // 2. Type filter
            boolean matchType = true;
            if ("Thu nhập".equals(selectedType)) {
                matchType = "THU".equalsIgnoreCase(t.getType());
            } else if ("Chi tiêu".equals(selectedType)) {
                matchType = "CHI".equalsIgnoreCase(t.getType());
            }

            // Extract date only (yyyy-MM-dd)
            String tDateOnly = null;
            if (t.getDate() != null && t.getDate().length() >= 10) {
                tDateOnly = t.getDate().substring(0, 10);
            }

            // 3. From date filter
            boolean matchFromDate = true;
            if (selectedFromDate != null && tDateOnly != null) {
                matchFromDate = tDateOnly.compareTo(selectedFromDate) >= 0;
            }

            // 4. To date filter
            boolean matchToDate = true;
            if (selectedToDate != null && tDateOnly != null) {
                matchToDate = tDateOnly.compareTo(selectedToDate) <= 0;
            }

            if (matchSearch && matchType && matchFromDate && matchToDate) {
                filteredTransactions.add(t);
            }
        }

        adapter.notifyDataSetChanged();
        if (tvEmpty != null) {
            tvEmpty.setVisibility(filteredTransactions.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void showDatePickerDialog(boolean isFromDate) {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog = new DatePickerDialog(this, (view, y, m, d) -> {
            String dateStr = String.format(Locale.getDefault(), "%02d/%02d/%04d", d, m + 1, y);
            String dbDateStr = String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d);
            if (isFromDate) {
                selectedFromDate = dbDateStr;
                if (tvFromDate != null) {
                    tvFromDate.setText(dateStr);
                    tvFromDate.setTextColor(getResources().getColor(R.color.text_primary, getTheme()));
                }
            } else {
                selectedToDate = dbDateStr;
                if (tvToDate != null) {
                    tvToDate.setText(dateStr);
                    tvToDate.setTextColor(getResources().getColor(R.color.text_primary, getTheme()));
                }
            }
            applyFilters();
        }, year, month, day);
        dialog.show();
    }

    private void showEmpty(String message) {
        if (tvEmpty != null) {
            tvEmpty.setText(message);
            tvEmpty.setVisibility(View.VISIBLE);
        }
    }
}
