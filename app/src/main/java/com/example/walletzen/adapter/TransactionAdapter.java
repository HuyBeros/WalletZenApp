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

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

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
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction t = list.get(position);

        String catName = t.getCategoryName();
        String note = (t.getNote() != null && !t.getNote().isEmpty()) ? t.getNote() : catName;
        String formattedDate = t.getFormattedDate();
        
        holder.tvTransactionCategory.setText(catName);
        holder.tvTransactionNote.setText(note + "  •  " + formattedDate);
        holder.tvTransactionIcon.setText(t.getCategoryIcon());

        // Amount and Color based on type
        if (t.isExpense()) {
            holder.tvTransactionAmount.setText("-" + t.getFormattedAmount());
            holder.tvTransactionAmount.setTextColor(Color.parseColor("#EF4444")); // Tailwind Red 500
            holder.cardTransactionIcon.setCardBackgroundColor(Color.parseColor("#FEE2E2")); // Tailwind Red 100
        } else {
            holder.tvTransactionAmount.setText("+" + t.getFormattedAmount());
            holder.tvTransactionAmount.setTextColor(Color.parseColor("#059669")); // Tailwind Emerald 600
            holder.cardTransactionIcon.setCardBackgroundColor(Color.parseColor("#D1FAE5")); // Tailwind Emerald 100
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(t);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        CardView cardTransactionIcon;
        TextView tvTransactionIcon;
        TextView tvTransactionCategory;
        TextView tvTransactionNote;
        TextView tvTransactionAmount;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            cardTransactionIcon = itemView.findViewById(R.id.cardTransactionIcon);
            tvTransactionIcon = itemView.findViewById(R.id.tvTransactionIcon);
            tvTransactionCategory = itemView.findViewById(R.id.tvTransactionCategory);
            tvTransactionNote = itemView.findViewById(R.id.tvTransactionNote);
            tvTransactionAmount = itemView.findViewById(R.id.tvTransactionAmount);
        }
    }
}