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

    private TransactionAdapter adapter;
    private List<Transaction> allTransactions = new ArrayList<>();
    private List<Transaction> filteredTransactions = new ArrayList<>();
    private SessionManager session;

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

        btnBack.setOnClickListener(v -> finish());

        adapter = new TransactionAdapter(this, filteredTransactions, t -> {
            Intent intent = new Intent(this, TransactionDetailActivity.class);
            intent.putExtra("transactionId", t.getTransactionId());
            intent.putExtra("note", t.getNote());
            intent.putExtra("amount", t.getAmount());
            intent.putExtra("date", t.getDate());
            intent.putExtra("type", t.getType());
            intent.putExtra("category", t.getCategoryName());
            intent.putExtra("icon", t.getCategoryIcon());
            startActivity(intent);
        });

        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        rvTransactions.setAdapter(adapter);

        if (edtSearch != null) {
            edtSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterTransactions(s.toString());
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
                            filterTransactions(edtSearch != null ? edtSearch.getText().toString() : "");
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

    private void filterTransactions(String query) {
        filteredTransactions.clear();
        if (query.isEmpty()) {
            filteredTransactions.addAll(allTransactions);
        } else {
            String q = query.toLowerCase().trim();
            for (Transaction t : allTransactions) {
                boolean matchNote     = t.getNote() != null && t.getNote().toLowerCase().contains(q);
                boolean matchCategory = t.getCategoryName() != null && t.getCategoryName().toLowerCase().contains(q);
                if (matchNote || matchCategory) filteredTransactions.add(t);
            }
        }
        adapter.notifyDataSetChanged();
        if (tvEmpty != null) {
            tvEmpty.setVisibility(filteredTransactions.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void showEmpty(String message) {
        if (tvEmpty != null) {
            tvEmpty.setText(message);
            tvEmpty.setVisibility(View.VISIBLE);
        }
    }
}
