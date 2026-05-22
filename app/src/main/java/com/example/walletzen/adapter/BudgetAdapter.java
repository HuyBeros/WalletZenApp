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
        void onDeleteClick(BudgetItem item);
        void onEditNameClick(BudgetItem item);
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
            
            eh.tvSpentAmount.setText("Đã chi: " + formatMoney(item.getCurrentAmount()));

            if (item.getLimitAmount() > 0) {
                eh.tvLimitAmount.setText("Hạn mức: " + formatMoney(item.getLimitAmount()));
                
                int percent = (int) ((item.getCurrentAmount() / item.getLimitAmount()) * 100);
                if (percent > 100) percent = 100;
                eh.progressBudget.setProgress(percent);
                eh.progressBudget.setVisibility(View.VISIBLE);
                
                // Status badge
                eh.tvBudgetStatus.setVisibility(View.VISIBLE);
                if (percent >= 100) {
                    eh.tvBudgetStatus.setText("🔴 Vượt giới hạn!");
                    eh.tvBudgetStatus.setTextColor(Color.parseColor("#EF4444"));
                    eh.tvBudgetStatus.setBackgroundResource(R.drawable.bg_badge_red);
                } else if (percent >= 80) {
                    eh.tvBudgetStatus.setText("⚠️ Sắp chạm (" + percent + "%)");
                    eh.tvBudgetStatus.setTextColor(Color.parseColor("#D97706"));
                    eh.tvBudgetStatus.setBackgroundResource(R.drawable.bg_badge_yellow);
                } else {
                    eh.tvBudgetStatus.setText("✅ Ổn (" + percent + "%)");
                    eh.tvBudgetStatus.setTextColor(Color.parseColor("#059669"));
                    eh.tvBudgetStatus.setBackgroundResource(R.drawable.bg_badge_green);
                }
            } else {
                eh.tvLimitAmount.setText("Hạn mức: Chưa đặt");
                eh.progressBudget.setProgress(0);
                eh.progressBudget.setVisibility(View.GONE);
                eh.tvBudgetStatus.setVisibility(View.GONE);
            }

            eh.btnEditBudget.setOnClickListener(v -> {
                if (listener != null) listener.onEditNameClick(item);
            });
            eh.btnDeleteBudget.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteClick(item);
            });

        } else if (holder instanceof IncomeViewHolder) {
            IncomeViewHolder ih = (IncomeViewHolder) holder;
            ih.tvCategoryName.setText(catName);
            ih.tvTotal.setText("+" + formatMoney(item.getCurrentAmount()));
            ih.tvLimitAmount.setText("Giới hạn: " + formatMoney(item.getLimitAmount()));
            
            // Hide unwanted views for Income category budgets
            ih.tvLimitAmount.setVisibility(View.GONE);
            if (ih.layoutIncomeAmountRow != null) {
                ih.layoutIncomeAmountRow.setVisibility(View.GONE);
            }
            
            // Set dynamic icon and color
            ih.ivIconBudget.setImageResource(iconRes);
            ih.ivIconBudget.setColorFilter(iconTint);
            ((View) ih.ivIconBudget.getParent()).setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(bgTint));

            // Show edit and delete buttons for income too
            ih.btnEditBudget.setVisibility(View.VISIBLE);
            ih.btnDeleteBudget.setVisibility(View.VISIBLE);

            ih.btnEditBudget.setOnClickListener(v -> {
                if (listener != null) listener.onEditNameClick(item);
            });
            ih.btnDeleteBudget.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteClick(item);
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
        TextView tvCategoryName, tvSpentAmount, tvLimitAmount, tvBudgetStatus;
        ProgressBar progressBudget;
        ImageView ivIconBudget, btnEditBudget, btnDeleteBudget;

        public ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            tvSpentAmount = itemView.findViewById(R.id.tvSpentAmount);
            tvLimitAmount = itemView.findViewById(R.id.tvLimitAmount);
            tvBudgetStatus = itemView.findViewById(R.id.tvBudgetStatus);
            progressBudget = itemView.findViewById(R.id.progressBudget);
            ivIconBudget = itemView.findViewById(R.id.ivIconBudget);
            btnEditBudget = itemView.findViewById(R.id.btnEditBudget);
            btnDeleteBudget = itemView.findViewById(R.id.btnDeleteBudget);
        }
    }

    static class IncomeViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategoryName, tvLimitAmount, tvTotal;
        ImageView ivIconBudget, btnEditBudget, btnDeleteBudget;
        View layoutIncomeAmountRow;

        public IncomeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            tvLimitAmount = itemView.findViewById(R.id.tvLimitAmount);
            tvTotal = itemView.findViewById(R.id.tvTotal);
            ivIconBudget = itemView.findViewById(R.id.ivIconBudget);
            btnEditBudget = itemView.findViewById(R.id.btnEditBudget);
            btnDeleteBudget = itemView.findViewById(R.id.btnDeleteBudget);
            layoutIncomeAmountRow = itemView.findViewById(R.id.layoutIncomeAmountRow);
        }
    }
}
