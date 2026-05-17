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
import com.example.walletzen.model.BudgetItem;

import java.util.List;

public class BudgetAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_EXPENSE = 0;
    private static final int TYPE_INCOME = 1;

    public interface OnBudgetClickListener {
        void onEditClick(BudgetItem item);
    }

    private Context context;
    private List<BudgetItem> list;
    private OnBudgetClickListener listener;

    public BudgetAdapter(Context context, List<BudgetItem> list, OnBudgetClickListener listener) {
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
            View v = LayoutInflater.from(context).inflate(R.layout.item_budget_expense, parent, false);
            return new ExpenseViewHolder(v);
        } else {
            View v = LayoutInflater.from(context).inflate(R.layout.item_budget_income, parent, false);
            return new IncomeViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        BudgetItem item = list.get(position);
        
        String catName = item.getCategoryName() != null ? item.getCategoryName() : "Khác";
        int iconRes = R.drawable.ic_payments;
        int bgTint = Color.parseColor("#FFF7ED"); // default Orange-light
        int iconTint = Color.parseColor("#EA580C"); // default Orange-dark
        
        if (catName.toLowerCase().contains("ăn") || catName.toLowerCase().contains("thực phẩm")) {
            iconRes = R.drawable.ic_food;
            bgTint = Color.parseColor("#E0E7FF");
            iconTint = Color.parseColor("#378ADD");
        } else if (catName.toLowerCase().contains("lương") || catName.toLowerCase().contains("thu nhập")) {
            iconRes = R.drawable.ic_money;
            bgTint = Color.parseColor("#DCFCE7");
            iconTint = Color.parseColor("#059669");
        } else if (catName.toLowerCase().contains("mua sắm") || catName.toLowerCase().contains("quần áo")) {
            iconRes = R.drawable.ic_payments;
            bgTint = Color.parseColor("#FCE7F3");
            iconTint = Color.parseColor("#BE185D");
        }

        if (holder instanceof ExpenseViewHolder) {
            ExpenseViewHolder eh = (ExpenseViewHolder) holder;
            eh.tvCategoryName.setText(catName);
            
            // Set dynamic icon and color
            eh.ivIconBudget.setImageResource(iconRes);
            eh.ivIconBudget.setColorFilter(iconTint);
            ((View) eh.ivIconBudget.getParent()).setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(bgTint));
            
            double remaining = item.getLimitAmount() - item.getCurrentAmount();
            if (remaining < 0) remaining = 0;
            eh.tvRemainingAmount.setText("Còn lại " + formatMoney(remaining));
            
            eh.tvSpentAmount.setText(formatMoney(item.getCurrentAmount()));
            eh.tvLimitAmount.setText("Giới hạn: " + formatMoney(item.getLimitAmount()));
            
            int percent = item.getLimitAmount() > 0 ? 
                    (int) ((item.getCurrentAmount() / item.getLimitAmount()) * 100) : 100;
            if (percent > 100) percent = 100;
            eh.progressBudget.setProgress(percent);
            
            // Red bar if limit exceeded
            if (percent >= 100) {
                eh.tvRemainingAmount.setTextColor(Color.parseColor("#EF4444"));
                eh.tvRemainingAmount.setText("Vượt giới hạn!");
            } else {
                eh.tvRemainingAmount.setTextColor(Color.parseColor("#059669"));
            }

            eh.btnEditBudget.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditClick(item);
                }
            });

        } else if (holder instanceof IncomeViewHolder) {
            IncomeViewHolder ih = (IncomeViewHolder) holder;
            ih.tvCategoryName.setText(catName);
            ih.tvTotal.setText("+" + formatMoney(item.getCurrentAmount()));
            ih.tvLimitAmount.setText("Giới hạn: " + formatMoney(item.getLimitAmount()));
            
            // Hide unwanted views for Income category budgets
            ih.tvLimitAmount.setVisibility(View.GONE);
            ih.btnEditBudget.setVisibility(View.GONE);
            if (ih.layoutIncomeAmountRow != null) {
                ih.layoutIncomeAmountRow.setVisibility(View.GONE);
            }
            
            // Set dynamic icon and color
            ih.ivIconBudget.setImageResource(iconRes);
            ih.ivIconBudget.setColorFilter(iconTint);
            ((View) ih.ivIconBudget.getParent()).setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(bgTint));

            ih.btnEditBudget.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditClick(item);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    private String formatMoney(double amount) {
        return String.format("%,.0fđ", amount).replace(",", ".");
    }

    static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategoryName, tvRemainingAmount, tvSpentAmount, tvLimitAmount;
        ProgressBar progressBudget;
        ImageView ivIconBudget, btnEditBudget;

        public ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            tvRemainingAmount = itemView.findViewById(R.id.tvRemainingAmount);
            tvSpentAmount = itemView.findViewById(R.id.tvSpentAmount);
            tvLimitAmount = itemView.findViewById(R.id.tvLimitAmount);
            progressBudget = itemView.findViewById(R.id.progressBudget);
            ivIconBudget = itemView.findViewById(R.id.ivIconBudget);
            btnEditBudget = itemView.findViewById(R.id.btnEditBudget);
        }
    }

    static class IncomeViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategoryName, tvLimitAmount, tvTotal;
        ImageView ivIconBudget, btnEditBudget;
        View layoutIncomeAmountRow;

        public IncomeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            tvLimitAmount = itemView.findViewById(R.id.tvLimitAmount);
            tvTotal = itemView.findViewById(R.id.tvTotal);
            ivIconBudget = itemView.findViewById(R.id.ivIconBudget);
            btnEditBudget = itemView.findViewById(R.id.btnEditBudget);
            layoutIncomeAmountRow = itemView.findViewById(R.id.layoutIncomeAmountRow);
        }
    }
}
