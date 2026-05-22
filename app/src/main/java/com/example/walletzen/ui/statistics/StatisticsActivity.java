package com.example.walletzen.ui.statistics;

import android.os.Bundle;
import android.content.Intent;
import android.app.AlertDialog;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.walletzen.R;
import com.example.walletzen.data.SessionManager;
import com.example.walletzen.network.RetrofitClient;
import com.example.walletzen.ui.home.HomeActivity;
import com.example.walletzen.ui.budget.BudgetActivity;
import com.example.walletzen.ui.profile.ProfileActivity;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.*;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StatisticsActivity extends AppCompatActivity {

    private BarChart barChart;
    private PieChart pieChart;
    private TextView tvIncomeMonth, tvExpenseMonth;
    
    // Calendar Custom
    private RecyclerView rvCalendar;
    private TextView tvCalendarMonth;
    private ImageView btnPrevMonth, btnNextMonth;
    private Calendar currentCalendar;
    private List<CalendarDay> calendarDays;
    private CalendarAdapter calendarAdapter;

    private BottomNavigationView bottomNav;
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
        bottomNav = findViewById(R.id.bottomNavigation);
        
        rvCalendar = findViewById(R.id.rvCalendar);
        tvCalendarMonth = findViewById(R.id.tvCalendarMonth);
        btnPrevMonth = findViewById(R.id.btnPrevMonth);
        btnNextMonth = findViewById(R.id.btnNextMonth);

        setupBottomNav();

        // Calendar init
        currentCalendar = Calendar.getInstance();
        calendarDays = new ArrayList<>();
        rvCalendar.setLayoutManager(new GridLayoutManager(this, 7));
        calendarAdapter = new CalendarAdapter(calendarDays, this::showDayDialog);
        rvCalendar.setAdapter(calendarAdapter);

        btnPrevMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, -1);
            updateCalendarUI();
            
            // Cập nhật thống kê khi chuyển tháng trên lịch
            updateMonthStatsUI();
        });

        btnNextMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, 1);
            updateCalendarUI();
            
            // Cập nhật thống kê khi chuyển tháng trên lịch
            updateMonthStatsUI();
        });

        loadMonthStats();
        loadTrend();
        updateCalendarUI();
    }

    private void setupBottomNav() {
        bottomNav.setSelectedItemId(R.id.nav_statistics);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_statistics) {
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_budget) {
                startActivity(new Intent(this, BudgetActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        bottomNav.setSelectedItemId(R.id.nav_statistics);
    }

    private void updateCalendarUI() {
        SimpleDateFormat sdf = new SimpleDateFormat("'Tháng' MM, yyyy", new Locale("vi", "VN"));
        tvCalendarMonth.setText(sdf.format(currentCalendar.getTime()));
        
        buildCalendarDays();
        populateCalendarData();
    }

    private void buildCalendarDays() {
        calendarDays.clear();
        
        Calendar cal = (Calendar) currentCalendar.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        
        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1; // 0=Sun, 1=Mon...
        
        cal.add(Calendar.DAY_OF_MONTH, -firstDayOfWeek); // Rewind to start of grid
        
        int currentMonth = currentCalendar.get(Calendar.MONTH);
        
        for (int i = 0; i < 42; i++) {
            boolean isCurrentMonth = (cal.get(Calendar.MONTH) == currentMonth);
            CalendarDay day = new CalendarDay(
                cal.get(Calendar.DAY_OF_MONTH),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.YEAR),
                isCurrentMonth
            );
            calendarDays.add(day);
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        
        calendarAdapter.notifyDataSetChanged();
    }
    
    private void populateCalendarData() {
        if (allTransactions.isEmpty() || calendarDays.isEmpty()) return;
        
        for (CalendarDay day : calendarDays) {
            day.setIncome(0);
            day.setExpense(0);
            day.setTransactionCount(0);
        }
        
        for (com.example.walletzen.model.Transaction t : allTransactions) {
            String tDate = t.getDate(); // yyyy-MM-dd
            if (tDate == null || tDate.length() < 10) continue;
            
            try {
                int y = Integer.parseInt(tDate.substring(0, 4));
                int m = Integer.parseInt(tDate.substring(5, 7)) - 1;
                int d = Integer.parseInt(tDate.substring(8, 10));
                
                for (CalendarDay day : calendarDays) {
                    if (day.getYear() == y && day.getMonth() == m && day.getDay() == d) {
                        double amt = t.getAmount() != null ? Math.abs(t.getAmount()) : 0;
                        if ("THU".equals(t.getType())) {
                            day.setIncome(day.getIncome() + amt);
                        } else if ("CHI".equals(t.getType())) {
                            day.setExpense(day.getExpense() + amt);
                        }
                        day.setTransactionCount(day.getTransactionCount() + 1);
                        break;
                    }
                }
            } catch (Exception ignored) {}
        }
        
        calendarAdapter.notifyDataSetChanged();
    }
    
    private void showDayDialog(CalendarDay day) {
        String displayDate = String.format(Locale.getDefault(), "%02d/%02d/%04d", day.getDay(), day.getMonth() + 1, day.getYear());
        new AlertDialog.Builder(StatisticsActivity.this)
                .setTitle("Giao dịch ngày " + displayDate)
                .setMessage(String.format(Locale.getDefault(),
                        "Tổng số giao dịch: %d\n\n🟢 Thu nhập: +%s\n🔴 Chi tiêu: -%s",
                        day.getTransactionCount(), formatMoney(day.getIncome()), formatMoney(day.getExpense())))
                .setPositiveButton("Đóng", null)
                .show();
    }

    private void updateMonthStatsUI() {
        String currentMonthStr = new java.text.SimpleDateFormat("yyyy-MM",
                java.util.Locale.getDefault()).format(currentCalendar.getTime());
        
        double income = 0, expense = 0;
        Map<String, Double> expenseByCategory = new HashMap<>();

        for (com.example.walletzen.model.Transaction t : allTransactions) {
            if (t == null) continue;
            String date = t.getDate();
            if (date != null && date.startsWith(currentMonthStr)) {
                double amt = t.getAmount() != null ? Math.abs(t.getAmount()) : 0;
                if ("THU".equals(t.getType())) {
                    income += amt;
                } else if ("CHI".equals(t.getType())) {
                    expense += amt;
                    String catName = t.getCategoryName();
                    double current = expenseByCategory.containsKey(catName) ? expenseByCategory.get(catName) : 0.0;
                    expenseByCategory.put(catName, current + amt);
                }
            }
        }
        tvIncomeMonth.setText("+" + formatMoney(income));
        tvExpenseMonth.setText("-" + formatMoney(expense));
        
        setupPieChart(expenseByCategory);
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
                            
                            populateCalendarData(); // Update grid when data arrives
                            updateMonthStatsUI(); // Update UI
                        }
                    }
                    @Override
                    public void onFailure(Call<List<com.example.walletzen.model.Transaction>> call, Throwable t) {}
                });
    }

    // Removed loadSpendingByCategory() network call as it calculates locally now

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
        if (data == null || data.isEmpty()) {
            pieChart.clear();
            return;
        }

        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Double> e : data.entrySet()) {
            if (e.getValue() != null && e.getValue() > 0) {
                entries.add(new PieEntry(e.getValue().floatValue(), e.getKey()));
            }
        }
        
        if (entries.isEmpty()) {
            pieChart.clear();
            return;
        }

        PieDataSet dataSet = new PieDataSet(entries, ""); // Blank label for legend so it doesn't duplicate
        dataSet.setColors(new int[]{
                0xFFFF4081, 0xFF7C4DFF, 0xFF00BCD4, 0xFFFF9800, 0xFF4CAF50,
                0xFF9C27B0, 0xFF2196F3, 0xFFFF5722
        });
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(android.graphics.Color.WHITE);
        dataSet.setSliceSpace(3f);
        
        // Use Percent Formatter
        pieChart.setUsePercentValues(true);
        dataSet.setValueFormatter(new PercentFormatter(pieChart));

        PieData pieData = new PieData(dataSet);
        pieChart.setData(pieData);
        pieChart.getDescription().setEnabled(false);
        pieChart.setHoleRadius(55f);
        pieChart.setTransparentCircleRadius(60f);
        pieChart.setHoleColor(getResources().getColor(R.color.background, null));
        pieChart.setCenterText("Chi tiêu");
        pieChart.setCenterTextSize(16f);
        
        // Custom Legend
        Legend legend = pieChart.getLegend();
        legend.setEnabled(true);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setWordWrapEnabled(true);
        legend.setDrawInside(false);
        legend.setTextColor(android.graphics.Color.parseColor("#4B5563")); // text_secondary
        legend.setTextSize(12f);
        legend.setXEntrySpace(15f);
        legend.setYEntrySpace(5f);

        pieChart.animate();
        pieChart.invalidate();
    }

    private void setupBarChart(List<Map<String, Object>> trend) {
        if (trend == null || trend.isEmpty()) {
            barChart.clear();
            return;
        }

        List<BarEntry> incomeEntries = new ArrayList<>();
        List<BarEntry> expenseEntries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        for (int i = 0; i < trend.size(); i++) {
            Map<String, Object> m = trend.get(i);
            String month = m.containsKey("month") && m.get("month") != null ? String.valueOf(m.get("month")) : "";
            double income = getDouble(m, "income");
            double expense = getDouble(m, "expense");
            incomeEntries.add(new BarEntry(i, (float) income));
            expenseEntries.add(new BarEntry(i, (float) expense));
            labels.add(month != null && month.length() >= 7 ? month.substring(5) : month);
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