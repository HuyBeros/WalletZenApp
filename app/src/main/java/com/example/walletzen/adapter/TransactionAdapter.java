package com.example.walletzen.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.walletzen.R;
import com.example.walletzen.model.Transaction;

import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_EXPENSE = 0;
    private static final int TYPE_INCOME = 1;

    public interface OnItemClickListener {
        void onItemClick(Transaction transaction);
    }

    private final Context context;
    private final List<Transaction> list;
    private final OnItemClickListener listener;

    public TransactionAdapter(Context context, List<Transaction> list, OnItemClickListener listener) {
        this.context = context;
        this.list = list;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return list.get(position).isExpense() ? TYPE_EXPENSE : TYPE_INCOME;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_EXPENSE) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_budget_expense, parent, false);
            return new ExpenseViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_budget_income, parent, false);
            return new IncomeViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Transaction t = list.get(position);

        String catName = t.getCategoryName() != null ? t.getCategoryName() : "Khác";
        String note = t.getNote() != null && !t.getNote().isEmpty() ? t.getNote() : catName;
        String date = t.getDate() != null ? t.getDate() : "";

        // Determine icon and colors
        int iconRes = R.drawable.ic_payments;
        int bgTint = Color.parseColor("#FFF7ED");
        int iconTint = Color.parseColor("#EA580C");

        if (catName.toLowerCase().contains("ăn") || catName.toLowerCase().contains("thực phẩm") || catName.toLowerCase().contains("food")) {
            iconRes = R.drawable.ic_food;
            bgTint = Color.parseColor("#E0E7FF");
            iconTint = Color.parseColor("#378ADD");
        } else if (catName.toLowerCase().contains("lương") || catName.toLowerCase().contains("thu nhập") || catName.toLowerCase().contains("salary")) {
            iconRes = R.drawable.ic_money;
            bgTint = Color.parseColor("#69F0AE");
            iconTint = Color.parseColor("#064E3B");
        } else if (catName.toLowerCase().contains("mua sắm") || catName.toLowerCase().contains("quần áo") || catName.toLowerCase().contains("shopping")) {
            iconRes = R.drawable.ic_payments;
            bgTint = Color.parseColor("#FCE7F3");
            iconTint = Color.parseColor("#BE185D");
        } else if (catName.toLowerCase().contains("giao") || catName.toLowerCase().contains("transport")) {
            iconRes = R.drawable.ic_payments;
            bgTint = Color.parseColor("#E0F2FE");
            iconTint = Color.parseColor("#0284C7");
        }

        if (holder instanceof ExpenseViewHolder) {
            ExpenseViewHolder eh = (ExpenseViewHolder) holder;
            eh.tvCategoryName.setText(catName);
            eh.tvRemainingAmount.setText(note + "  •  " + date);
            eh.tvSpentAmount.setText("-" + t.getFormattedAmount());
            eh.tvSpentAmount.setTextColor(context.getResources().getColor(R.color.expense_red, null));

            // Set dynamic icon and color
            eh.ivIconBudget.setImageResource(iconRes);
            eh.ivIconBudget.setColorFilter(iconTint);
            ((View) eh.ivIconBudget.getParent()).setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(bgTint));

            // Hide budget-specific elements for transactions
            eh.tvStatus.setVisibility(View.GONE);
            eh.btnOptions.setVisibility(View.GONE);
            eh.tvLimitAmount.setVisibility(View.GONE);
            eh.tvSlash.setVisibility(View.GONE);
            eh.tvPercent.setVisibility(View.GONE);
            eh.progressBudget.setVisibility(View.GONE);

            eh.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(t);
            });
        } else if (holder instanceof IncomeViewHolder) {
            IncomeViewHolder ih = (IncomeViewHolder) holder;
            ih.tvCategoryName.setText(catName);
            ih.tvNote.setText(note + "  •  " + date);
            ih.tvTotal.setText("+" + t.getFormattedAmount());
            ih.tvTotal.setTextColor(context.getResources().getColor(R.color.income_green, null));

            // Set dynamic icon and color
            ih.ivIconBudget.setImageResource(iconRes);
            ih.ivIconBudget.setColorFilter(iconTint);
            ((View) ih.ivIconBudget.getParent()).setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(bgTint));

            // Hide options button
            ih.btnOptions.setVisibility(View.GONE);

            ih.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(t);
            });
        }
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategoryName, tvRemainingAmount, tvStatus, tvSpentAmount, tvSlash, tvLimitAmount, tvPercent;
        ProgressBar progressBudget;
        ImageView ivIconBudget, btnOptions;

        public ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            tvRemainingAmount = itemView.findViewById(R.id.tvRemainingAmount);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvSpentAmount = itemView.findViewById(R.id.tvSpentAmount);
            tvSlash = itemView.findViewById(R.id.tvSlash);
            tvLimitAmount = itemView.findViewById(R.id.tvLimitAmount);
            tvPercent = itemView.findViewById(R.id.tvPercent);
            progressBudget = itemView.findViewById(R.id.progressBudget);
            ivIconBudget = itemView.findViewById(R.id.ivIconBudget);
            btnOptions = itemView.findViewById(R.id.btnOptions);
        }
    }

    static class IncomeViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategoryName, tvNote, tvTotal;
        ImageView ivIconBudget, btnOptions;

        public IncomeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            tvNote = itemView.findViewById(R.id.tvNote);
            tvTotal = itemView.findViewById(R.id.tvTotal);
            ivIconBudget = itemView.findViewById(R.id.ivIconBudget);
            btnOptions = itemView.findViewById(R.id.btnOptions);
        }
    }
}