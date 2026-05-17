package com.example.walletzen.ui.statistics;

import android.os.Bundle;
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
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        session = new SessionManager(this);

        barChart = findViewById(R.id.barChart);
        pieChart = findViewById(R.id.pieChart);
        tvIncomeMonth = findViewById(R.id.tvIncomeMonth);
        tvExpenseMonth = findViewById(R.id.tvExpenseMonth);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

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
        incomeSet.setColor(0xFF00BCD4);
        BarDataSet expenseSet = new BarDataSet(expenseEntries, "Chi tiêu");
        expenseSet.setColor(0xFFFF4081);

        BarData barData = new BarData(incomeSet, expenseSet);
        barData.setBarWidth(0.35f);
        barData.groupBars(0f, 0.2f, 0.05f);

        barChart.setData(barData);
        barChart.getDescription().setEnabled(false);
        barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        barChart.getXAxis().setGranularity(1f);
        barChart.getAxisLeft().setAxisMinimum(0f);
        barChart.getAxisRight().setEnabled(false);
        barChart.getLegend().setEnabled(true);
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