package com.example.walletzen.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.walletzen.R;
import com.example.walletzen.model.Transaction;

import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

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

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transaction t = list.get(position);

        holder.tvIcon.setText(t.getCategoryIcon());
        holder.tvCategory.setText(t.getCategoryName());
        holder.tvNote.setText(t.getNote() != null && !t.getNote().isEmpty()
                ? t.getNote() : t.getCategoryName());
        holder.tvDate.setText(t.getDate() != null ? t.getDate() : "");

        // Amount với màu
        boolean isExpense = t.isExpense();
        String amountText = (isExpense ? "-" : "+") + t.getFormattedAmount();
        holder.tvAmount.setText(amountText);
        holder.tvAmount.setTextColor(isExpense
                ? context.getResources().getColor(R.color.expense_red, null)
                : context.getResources().getColor(R.color.income_green, null));

        // Icon background color
        int bgColor;
        String category = t.getCategoryName().toLowerCase();
        if (category.contains("ăn") || category.contains("food")) {
            bgColor = context.getResources().getColor(R.color.cat_food, null);
        } else if (category.contains("giao") || category.contains("transport")) {
            bgColor = context.getResources().getColor(R.color.cat_transport, null);
        } else if (category.contains("mua") || category.contains("shopping")) {
            bgColor = context.getResources().getColor(R.color.cat_shopping, null);
        } else if (category.contains("sức") || category.contains("health")) {
            bgColor = context.getResources().getColor(R.color.cat_health, null);
        } else if (category.contains("giải") || category.contains("entertainment")) {
            bgColor = context.getResources().getColor(R.color.cat_entertainment, null);
        } else if (t.isIncome()) {
            bgColor = context.getResources().getColor(R.color.cat_salary, null);
        } else {
            bgColor = context.getResources().getColor(R.color.cat_other, null);
        }
        holder.cardIcon.setCardBackgroundColor(bgColor);

        // Click
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(t);
        });
    }

    @Override
    public int getItemCount() { return list.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvIcon, tvCategory, tvNote, tvDate, tvAmount;
        CardView cardIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIcon = itemView.findViewById(R.id.tvTransactionIcon);
            tvCategory = itemView.findViewById(R.id.tvTransactionCategory);
            tvNote = itemView.findViewById(R.id.tvTransactionNote);
            tvDate = itemView.findViewById(R.id.tvTransactionDate);
            tvAmount = itemView.findViewById(R.id.tvTransactionAmount);
            cardIcon = itemView.findViewById(R.id.cardTransactionIcon);
        }
    }
}