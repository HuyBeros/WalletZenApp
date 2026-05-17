package com.example.walletzen.ui.detail;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.walletzen.R;
import com.example.walletzen.data.SessionManager;
import com.example.walletzen.network.RetrofitClient;
import com.example.walletzen.ui.home.HomeActivity;
import com.google.android.material.button.MaterialButton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TransactionDetailActivity extends AppCompatActivity {

    private TextView tvIcon, tvCategory, tvAmount, tvDate, tvNote;
    private MaterialButton btnDelete, btnEdit;
    private ImageView btnBack;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_detail);

        session = new SessionManager(this);

        tvIcon = findViewById(R.id.tvDetailIcon);
        tvCategory = findViewById(R.id.tvDetailCategory);
        tvAmount = findViewById(R.id.tvDetailAmount);
        tvDate = findViewById(R.id.tvDetailDate);
        tvNote = findViewById(R.id.tvDetailNote);
        // tvType removed
        btnDelete = findViewById(R.id.btnDelete);
        btnEdit = findViewById(R.id.btnEdit);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        // Load data from Intent
        Intent intent = getIntent();
        long transactionId = intent.getLongExtra("transactionId", -1);
        String note = intent.getStringExtra("note");
        double amount = intent.getDoubleExtra("amount", 0);
        String date = intent.getStringExtra("date");
        String type = intent.getStringExtra("type");
        String category = intent.getStringExtra("category");
        String icon = intent.getStringExtra("icon");

        tvIcon.setText(icon != null ? icon : "💰");
        tvCategory.setText(category != null ? category : "Khác");
        tvNote.setText(note != null && !note.isEmpty() ? note : "Không có ghi chú");
        String formattedDate = date;
        if (date != null && date.contains("T")) {
            try {
                String[] parts = date.split("T");
                String[] dateSplit = parts[0].split("-");
                String[] timeSplit = parts[1].split(":");
                formattedDate = timeSplit[0] + ":" + timeSplit[1] + " " + dateSplit[2] + "/" + dateSplit[1] + "/" + dateSplit[0];
            } catch (Exception e) {}
        } else if (date != null && date.contains("-")) {
            try {
                String[] dateSplit = date.split("-");
                formattedDate = dateSplit[2] + "/" + dateSplit[1] + "/" + dateSplit[0];
            } catch (Exception e) {}
        }
        tvDate.setText(formattedDate != null ? formattedDate : "");
        // tvType removed

        // Format amount
        String formattedAmount = String.format("%,.0f ₫", Math.abs(amount)).replace(",", ".");
        tvAmount.setText(("CHI".equals(type) ? "-" : "+") + formattedAmount);
        tvAmount.setTextColor(getResources().getColor(
                "CHI".equals(type) ? R.color.expense_red : R.color.income_green, null));

        // Delete button
        if (transactionId != -1) {
            btnDelete.setOnClickListener(v -> confirmDelete(transactionId));
        }

        // Edit button
        if (transactionId != -1) {
            btnEdit.setOnClickListener(v -> {
                Intent editIntent = new Intent(this, com.example.walletzen.ui.transaction.AddTransactionActivity.class);
                editIntent.putExtra("transactionId", transactionId);
                editIntent.putExtra("note", note);
                editIntent.putExtra("amount", amount);
                editIntent.putExtra("date", date);
                editIntent.putExtra("type", type);
                editIntent.putExtra("category", category);
                editIntent.putExtra("icon", icon);
                editIntent.putExtra("categoryId", intent.getLongExtra("categoryId", -1));
                startActivity(editIntent);
            });
        }
    }

    private void confirmDelete(long transactionId) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa giao dịch")
                .setMessage("Bạn có chắc muốn xóa giao dịch này?")
                .setPositiveButton("Xóa", (d, w) -> deleteTransaction(transactionId))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deleteTransaction(long transactionId) {
        RetrofitClient.getApiService()
                .deleteTransaction(transactionId, session.getUserId())
                .enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(TransactionDetailActivity.this,
                                    "Đã xóa giao dịch", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(TransactionDetailActivity.this,
                                    HomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                            finish();
                        } else {
                            Toast.makeText(TransactionDetailActivity.this,
                                    "Xóa thất bại!", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(Call<String> call, Throwable t) {
                        Toast.makeText(TransactionDetailActivity.this,
                                "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}