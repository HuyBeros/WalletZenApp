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

    private Context context;
    private List<BudgetItem> list;

    public BudgetAdapter(Context context, List<BudgetItem> list) {
        this.context = context;
        this.list = list;
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
        
        // Define color and icon mapping
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
            bgTint = Color.parseColor("#69F0AE");
            iconTint = Color.parseColor("#064E3B");
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
            eh.tvLimitAmount.setText(formatMoneyShort(item.getLimitAmount()));
            
            int percent = item.getLimitAmount() > 0 ? 
                    (int) ((item.getCurrentAmount() / item.getLimitAmount()) * 100) : 100;
            if (percent > 100) percent = 100;
            eh.tvPercent.setText(percent + "%");
            eh.progressBudget.setProgress(percent);
            
            if (percent > 90) {
                eh.tvStatus.setText("VƯỢT MỨC");
                eh.tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FEE2E2")));
                eh.tvStatus.setTextColor(Color.parseColor("#991B1B"));
            } else if (percent > 75) {
                eh.tvStatus.setText("CẢNH BÁO");
                eh.tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FEF3C7")));
                eh.tvStatus.setTextColor(Color.parseColor("#92400E"));
            } else {
                eh.tvStatus.setText("AN TOÀN");
                eh.tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#DCFCE7")));
                eh.tvStatus.setTextColor(Color.parseColor("#166534"));
            }

        } else if (holder instanceof IncomeViewHolder) {
            IncomeViewHolder ih = (IncomeViewHolder) holder;
            ih.tvCategoryName.setText(catName);
            ih.tvTotal.setText("+" + formatMoney(item.getCurrentAmount()));
            
            // Set dynamic icon and color
            ih.ivIconBudget.setImageResource(iconRes);
            ih.ivIconBudget.setColorFilter(iconTint);
            ((View) ih.ivIconBudget.getParent()).setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(bgTint));
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    private String formatMoney(double amount) {
        return String.format("%,.0fđ", amount).replace(",", ".");
    }
    
    private String formatMoneyShort(double amount) {
        if (amount >= 1000000) {
            return String.format("%,.0ftr", amount / 1000000).replace(",", ".");
        } else if (amount >= 1000) {
            return String.format("%,.0fk", amount / 1000).replace(",", ".");
        }
        return formatMoney(amount);
    }

    static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategoryName, tvRemainingAmount, tvStatus, tvSpentAmount, tvLimitAmount, tvPercent;
        ProgressBar progressBudget;
        ImageView ivIconBudget;

        public ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            tvRemainingAmount = itemView.findViewById(R.id.tvRemainingAmount);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvSpentAmount = itemView.findViewById(R.id.tvSpentAmount);
            tvLimitAmount = itemView.findViewById(R.id.tvLimitAmount);
            tvPercent = itemView.findViewById(R.id.tvPercent);
            progressBudget = itemView.findViewById(R.id.progressBudget);
            ivIconBudget = itemView.findViewById(R.id.ivIconBudget);
        }
    }

    static class IncomeViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategoryName, tvTotal;
        ImageView ivIconBudget;

        public IncomeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            tvTotal = itemView.findViewById(R.id.tvTotal);
            ivIconBudget = itemView.findViewById(R.id.ivIconBudget);
        }
    }
}
