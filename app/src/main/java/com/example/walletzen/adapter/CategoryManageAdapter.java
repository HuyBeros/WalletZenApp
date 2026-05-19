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
import com.example.walletzen.model.Category;

import java.util.List;
import java.util.Map;

public class CategoryManageAdapter extends RecyclerView.Adapter<CategoryManageAdapter.ViewHolder> {

    public interface Listener {
        void onEdit(Category category);
        void onDelete(Category category);
    }

    private final Context context;
    private final List<Category> list;
    private final Listener listener;
    // Map categoryId -> spent this month (only for CHI)
    private Map<Long, Double> spentMap;
    // Map categoryId -> budget limit set this month
    private Map<Long, Double> limitMap;
    // Map categoryId -> budgetId
    private Map<Long, Long> budgetIdMap;
    private boolean showBudgetInfo = false;

    public CategoryManageAdapter(Context context, List<Category> list, Listener listener) {
        this.context = context;
        this.list = list;
        this.listener = listener;
    }

    public void setSpentData(Map<Long, Double> spentMap, Map<Long, Double> limitMap,
                             Map<Long, Long> budgetIdMap) {
        this.spentMap = spentMap;
        this.limitMap = limitMap;
        this.budgetIdMap = budgetIdMap;
        this.showBudgetInfo = true;
    }

    public void clearBudgetData() {
        this.spentMap = null;
        this.limitMap = null;
        this.budgetIdMap = null;
        this.showBudgetInfo = false;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_category_manage, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Category cat = list.get(position);
        String name = cat.getCategoryName() != null ? cat.getCategoryName() : "Khác";

        h.tvCatName.setText(name);
        h.tvCatIcon.setText(cat.getIcon());

        boolean isExpense = "CHI".equals(cat.getType());

        // Type badge
        if (isExpense) {
            h.tvCatType.setText("CHI TIÊU");
            h.tvCatType.setTextColor(Color.parseColor("#EF4444"));
            h.tvCatType.setBackgroundResource(R.drawable.bg_badge_red);
        } else {
            h.tvCatType.setText("THU NHẬP");
            h.tvCatType.setTextColor(Color.parseColor("#378ADD"));
            h.tvCatType.setBackgroundResource(R.drawable.bg_badge_blue);
        }

        // Budget info (only for CHI)
        if (showBudgetInfo && isExpense && cat.getCategoryId() != null) {
            h.layoutBudgetInfo.setVisibility(View.VISIBLE);

            double spent = spentMap != null ? spentMap.getOrDefault(cat.getCategoryId(), 0.0) : 0;
            double limit = limitMap != null ? limitMap.getOrDefault(cat.getCategoryId(), 0.0) : 0;
            Long budgetId = budgetIdMap != null ? budgetIdMap.get(cat.getCategoryId()) : null;

            h.tvSpent.setText("Đã chi: " + formatMoney(spent));
            if (limit > 0) {
                h.tvLimit.setText("Hạn mức: " + formatMoney(limit));

                int percent = (int) ((spent / limit) * 100);
                if (percent > 100) percent = 100;
                h.progressBudget.setProgress(percent);
                h.progressBudget.setVisibility(View.VISIBLE);

                // Status badge
                h.tvBudgetStatus.setVisibility(View.VISIBLE);
                if (percent >= 100) {
                    // VƯỢT GIỚI HẠN 🔴
                    h.tvBudgetStatus.setText("🔴 Vượt giới hạn!");
                    h.tvBudgetStatus.setTextColor(Color.parseColor("#EF4444"));
                    h.tvBudgetStatus.setBackgroundResource(R.drawable.bg_badge_red);
                } else if (percent >= 80) {
                    // CẢNH BÁO SẮP CHẠM ⚠️
                    h.tvBudgetStatus.setText("⚠️ Sắp chạm (" + percent + "%)");
                    h.tvBudgetStatus.setTextColor(Color.parseColor("#D97706"));
                    h.tvBudgetStatus.setBackgroundResource(R.drawable.bg_badge_yellow);
                } else {
                    // ỔN ✅
                    h.tvBudgetStatus.setText("✅ Ổn (" + percent + "%)");
                    h.tvBudgetStatus.setTextColor(Color.parseColor("#059669"));
                    h.tvBudgetStatus.setBackgroundResource(R.drawable.bg_badge_green);
                }
            } else {
                h.tvLimit.setText("Hạn mức: Chưa đặt");
                h.progressBudget.setProgress(0);
                h.progressBudget.setVisibility(View.GONE);
                h.tvBudgetStatus.setVisibility(View.GONE);
            }

        } else {
            h.layoutBudgetInfo.setVisibility(View.GONE);
            h.tvBudgetStatus.setVisibility(View.GONE);
        }

        h.btnEditCategory.setOnClickListener(v -> { if (listener != null) listener.onEdit(cat); });
        h.btnDeleteCategory.setOnClickListener(v -> { if (listener != null) listener.onDelete(cat); });
    }

    @Override
    public int getItemCount() { return list.size(); }

    private String formatMoney(double amount) {
        return String.format("%,.0fđ", amount).replace(",", ".");
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCatIcon, tvCatName, tvCatType, tvBudgetStatus;
        TextView tvSpent, tvLimit;
        ProgressBar progressBudget;
        ImageView btnEditCategory, btnDeleteCategory;
        View layoutBudgetInfo;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCatIcon = itemView.findViewById(R.id.tvCatIcon);
            tvCatName = itemView.findViewById(R.id.tvCatName);
            tvCatType = itemView.findViewById(R.id.tvCatType);
            tvBudgetStatus = itemView.findViewById(R.id.tvBudgetStatus);
            tvSpent = itemView.findViewById(R.id.tvSpent);
            tvLimit = itemView.findViewById(R.id.tvLimit);
            progressBudget = itemView.findViewById(R.id.progressBudget);
            btnEditCategory = itemView.findViewById(R.id.btnEditCategory);
            btnDeleteCategory = itemView.findViewById(R.id.btnDeleteCategory);
            layoutBudgetInfo = itemView.findViewById(R.id.layoutBudgetInfo);
        }
    }
}
