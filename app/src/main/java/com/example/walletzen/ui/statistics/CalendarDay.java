package com.example.walletzen.ui.statistics;

public class CalendarDay {
    private int day;
    private int month;
    private int year;
    private boolean isCurrentMonth;
    private double income;
    private double expense;
    private int transactionCount;

    public CalendarDay(int day, int month, int year, boolean isCurrentMonth) {
        this.day = day;
        this.month = month;
        this.year = year;
        this.isCurrentMonth = isCurrentMonth;
        this.income = 0;
        this.expense = 0;
        this.transactionCount = 0;
    }

    public int getDay() { return day; }
    public int getMonth() { return month; }
    public int getYear() { return year; }
    public boolean isCurrentMonth() { return isCurrentMonth; }
    public double getIncome() { return income; }
    public void setIncome(double income) { this.income = income; }
    public double getExpense() { return expense; }
    public void setExpense(double expense) { this.expense = expense; }
    public int getTransactionCount() { return transactionCount; }
    public void setTransactionCount(int transactionCount) { this.transactionCount = transactionCount; }
}
