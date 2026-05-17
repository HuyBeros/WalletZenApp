package com.example.walletzen.ui.statistics;

import android.os.Bundle;
import android.widget.CalendarView;
import android.app.AlertDialog;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.walletzen.R;
import com.example.walletzen.data.SessionManager;
import com.example.walletzen.network.RetrofitClient;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.text.SimpleDateFormat;
import java.util.*;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StatisticsActivity extends AppCompatActivity {

    private BarChart barChart;
    private PieChart pieChart;
    private TextView tvIncomeMonth, tvExpenseMonth;
    private CalendarView calendarView;
    private SessionManager session;
    private List<com.example.walletzen.model.Transaction> allTransactions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        session = new SessionManager(this);

        barChart = findViewById(R.id.barChart);
        pieChart = findViewById(R.id.pieChart);
        tvIncomeMonth = findViewById(R.id.tvIncomeMonth);
        tvExpenseMonth = findViewById(R.id.tvExpenseMonth);
        calendarView = findViewById(R.id.calendarView);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        if (calendarView != null) {
            calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
                String selectedDateStr = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                double dayIncome = 0;
                double dayExpense = 0;
                int count = 0;
                for (com.example.walletzen.model.Transaction t : allTransactions) {
                    String tDate = t.getDate();
                    if (tDate != null && tDate.startsWith(selectedDateStr)) {
                        double amt = t.getAmount() != null ? Math.abs(t.getAmount()) : 0;
                        if ("THU".equals(t.getType())) {
                            dayIncome += amt;
                        } else if ("CHI".equals(t.getType())) {
                            dayExpense += amt;
                        }
                        count++;
                    }
                }
                
                String displayDate = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year);
                new AlertDialog.Builder(StatisticsActivity.this)
                        .setTitle("Giao dịch ngày " + displayDate)
                        .setMessage(String.format(Locale.getDefault(),
                                "Tổng số giao dịch: %d\n\n🟢 Thu nhập: +%s\n🔴 Chi tiêu: -%s",
                                count, formatMoney(dayIncome), formatMoney(dayExpense)))
                        .setPositiveButton("Đóng", null)
                        .show();
            });
        }

        loadMonthStats();
        loadSpendingByCategory();
        loadTrend();
    }

    private void loadMonthStats() {
        RetrofitClient.getApiService()
                .getTransactions(session.getUserId())
                .enqueue(new Callback<List<com.example.walletzen.model.Transaction>>() {
                    @Override
                    public void onResponse(Call<List<com.example.walletzen.model.Transaction>> call,
                                           Response<List<com.example.walletzen.model.Transaction>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            allTransactions.clear();
                            allTransactions.addAll(response.body());
                            String currentMonth = new java.text.SimpleDateFormat("yyyy-MM",
                                    java.util.Locale.getDefault()).format(new java.util.Date());
                            double income = 0, expense = 0;
                            for (com.example.walletzen.model.Transaction t : response.body()) {
                                String date = t.getDate();
                                if (date != null && date.startsWith(currentMonth)) {
                                    double amt = t.getAmount() != null ? Math.abs(t.getAmount()) : 0;
                                    if ("THU".equals(t.getType()))      income  += amt;
                                    else if ("CHI".equals(t.getType())) expense += amt;
                                }
                            }
                            double balance = income - expense;
                            tvIncomeMonth.setText("+" + formatMoney(income));
                            tvExpenseMonth.setText("-" + formatMoney(expense));
                        }
                    }
                    @Override
                    public void onFailure(Call<List<com.example.walletzen.model.Transaction>> call, Throwable t) {}
                });
    }


    private void loadSpendingByCategory() {
        RetrofitClient.getApiService()
                .getSpendingByCategory(session.getUserId())
                .enqueue(new Callback<Map<String, Double>>() {
                    @Override
                    public void onResponse(Call<Map<String, Double>> call,
                                           Response<Map<String, Double>> response) {
                        if (response.isSuccessful() && response.body() != null
                                && !response.body().isEmpty()) {
                            setupPieChart(response.body());
                        }
                    }
                    @Override
                    public void onFailure(Call<Map<String, Double>> call, Throwable t) {}
                });
    }

    private void loadTrend() {
        RetrofitClient.getApiService()
                .getTrend(session.getUserId(), 6)
                .enqueue(new Callback<List<Map<String, Object>>>() {
                    @Override
                    public void onResponse(Call<List<Map<String, Object>>> call,
                                           Response<List<Map<String, Object>>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            setupBarChart(response.body());
                        }
                    }
                    @Override
                    public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {}
                });
    }

    private void setupPieChart(Map<String, Double> data) {
        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Double> e : data.entrySet()) {
            if (e.getValue() > 0) {
                entries.add(new PieEntry(e.getValue().floatValue(), e.getKey()));
            }
        }
        PieDataSet dataSet = new PieDataSet(entries, "Chi tiêu theo danh mục");
        dataSet.setColors(new int[]{
                0xFFFF4081, 0xFF7C4DFF, 0xFF00BCD4, 0xFFFF9800, 0xFF4CAF50,
                0xFF9C27B0, 0xFF2196F3, 0xFFFF5722
        });
        dataSet.setValueTextSize(11f);
        dataSet.setSliceSpace(3f);

        PieData pieData = new PieData(dataSet);
        pieChart.setData(pieData);
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setHoleRadius(55f);
        pieChart.setTransparentCircleRadius(60f);
        pieChart.setHoleColor(getResources().getColor(R.color.background, null));
        pieChart.setCenterText("Chi tiêu");
        pieChart.setCenterTextSize(16f);
        pieChart.animate();
        pieChart.invalidate();
    }

    private void setupBarChart(List<Map<String, Object>> trend) {
        List<BarEntry> incomeEntries = new ArrayList<>();
        List<BarEntry> expenseEntries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        for (int i = 0; i < trend.size(); i++) {
            Map<String, Object> m = trend.get(i);
            String month = String.valueOf(m.getOrDefault("month", ""));
            double income = getDouble(m, "income");
            double expense = getDouble(m, "expense");
            incomeEntries.add(new BarEntry(i, (float) income));
            expenseEntries.add(new BarEntry(i, (float) expense));
            labels.add(month.length() >= 7 ? month.substring(5) : month);
        }

        BarDataSet incomeSet = new BarDataSet(incomeEntries, "Thu nhập");
        incomeSet.setColor(0xFF06B6D4); // Premium Modern Cyan/Teal
        incomeSet.setDrawValues(false); // Clean modern look

        BarDataSet expenseSet = new BarDataSet(expenseEntries, "Chi tiêu");
        expenseSet.setColor(0xFFF43F5E); // Premium Modern Rose/Red
        expenseSet.setDrawValues(false); // Clean modern look

        float groupSpace = 0.3f;
        float barSpace = 0.05f;
        float barWidth = 0.3f; // Math: (0.3 + 0.05)*2 + 0.3 = 1.0f

        BarData barData = new BarData(incomeSet, expenseSet);
        barData.setBarWidth(barWidth);

        barChart.setData(barData);
        barChart.groupBars(0f, groupSpace, barSpace);

        // Chart styling & customization
        barChart.getDescription().setEnabled(false);
        barChart.setDoubleTapToZoomEnabled(false);
        barChart.setPinchZoom(false);
        barChart.setScaleEnabled(false);

        // XAxis styling
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setCenterAxisLabels(true); // Center the labels under the groups
        xAxis.setAxisMinimum(0f);
        xAxis.setAxisMaximum(trend.size());
        xAxis.setDrawGridLines(false); // No vertical grid lines
        xAxis.setDrawAxisLine(true);
        xAxis.setAxisLineColor(0x3F000000);
        xAxis.setTextColor(0xFF4B5563);
        xAxis.setTextSize(10f);

        // YAxis Left styling
        barChart.getAxisLeft().setAxisMinimum(0f);
        barChart.getAxisLeft().setDrawGridLines(true);
        barChart.getAxisLeft().setGridColor(0x1F000000); // 12% opacity dark grid lines
        barChart.getAxisLeft().setTextColor(0xFF4B5563);
        barChart.getAxisLeft().setTextSize(10f);

        // Disable YAxis Right
        barChart.getAxisRight().setEnabled(false);

        // Legend styling
        barChart.getLegend().setEnabled(true);
        barChart.getLegend().setTextColor(0xFF4B5563);
        barChart.getLegend().setTextSize(11f);

        barChart.animateY(800);
        barChart.invalidate();
    }

    private double getOrDefault(Map<String, Double> m, String k, double d) {
        Double v = m.get(k); return v != null ? v : d;
    }

    /** Try multiple key names, return first non-zero value found */
    private double getFirstNonZero(Map<String, Double> m, String... keys) {
        for (String k : keys) {
            Double v = m.get(k);
            if (v != null && v != 0) return v;
        }
        // Return first non-null even if 0
        for (String k : keys) {
            Double v = m.get(k);
            if (v != null) return v;
        }
        return 0.0;
    }

    private double getDouble(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null) return 0;
        if (v instanceof Double) return (Double) v;
        if (v instanceof Number) return ((Number) v).doubleValue();
        try { return Double.parseDouble(v.toString()); } catch (Exception e) { return 0; }
    }

    private String formatMoney(double amount) {
        return String.format("%,.0f ₫", Math.abs(amount)).replace(",", ".");
    }
}