package com.example.walletzen.ui.statistics;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.walletzen.R;

import java.util.List;
import java.util.Locale;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder> {

    private final List<CalendarDay> days;
    private final OnDayClickListener listener;

    public interface OnDayClickListener {
        void onDayClick(CalendarDay day);
    }

    public CalendarAdapter(List<CalendarDay> days, OnDayClickListener listener) {
        this.days = days;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CalendarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_day, parent, false);
        return new CalendarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CalendarViewHolder holder, int position) {
        CalendarDay day = days.get(position);
        holder.tvDay.setText(String.valueOf(day.getDay()));

        if (day.isCurrentMonth()) {
            holder.tvDay.setTextColor(Color.parseColor("#1E293B")); // text_primary
        } else {
            holder.tvDay.setTextColor(Color.parseColor("#94A3B8")); // text_secondary
        }

        if (day.getIncome() > 0) {
            holder.tvIncome.setText("+" + formatShortMoney(day.getIncome()));
            holder.tvIncome.setVisibility(View.VISIBLE);
        } else {
            holder.tvIncome.setVisibility(View.GONE);
        }

        if (day.getExpense() > 0) {
            holder.tvExpense.setText("-" + formatShortMoney(day.getExpense()));
            holder.tvExpense.setVisibility(View.VISIBLE);
        } else {
            holder.tvExpense.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onDayClick(day);
        });
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    public static class CalendarViewHolder extends RecyclerView.ViewHolder {
        TextView tvDay, tvIncome, tvExpense;

        public CalendarViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDay = itemView.findViewById(R.id.tvDay);
            tvIncome = itemView.findViewById(R.id.tvIncome);
            tvExpense = itemView.findViewById(R.id.tvExpense);
        }
    }

    private String formatShortMoney(double amount) {
        if (amount == 0) return "";
        if (amount >= 1_000_000_000) {
            return String.format(Locale.US, "%.1fb", amount / 1_000_000_000.0).replace(".0", "").replace(".", ",");
        } else if (amount >= 1_000_000) {
            return String.format(Locale.US, "%.1fm", amount / 1_000_000.0).replace(".0", "").replace(".", ",");
        } else if (amount >= 1_000) {
            return String.format(Locale.US, "%.1fk", amount / 1_000.0).replace(".0", "").replace(".", ",");
        } else {
            return String.valueOf((long) amount);
        }
    }
}
