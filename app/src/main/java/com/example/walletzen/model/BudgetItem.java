package com.example.walletzen.model;

public class BudgetItem {
    private String categoryName;
    private String icon;
    private double currentAmount;
    private double limitAmount;
    private boolean isExpense;

    public BudgetItem(String categoryName, String icon, double currentAmount, double limitAmount, boolean isExpense) {
        this.categoryName = categoryName;
        this.icon = icon;
        this.currentAmount = currentAmount;
        this.limitAmount = limitAmount;
        this.isExpense = isExpense;
    }

    public String getCategoryName() { return categoryName; }
    public String getIcon() { return icon; }
    public double getCurrentAmount() { return currentAmount; }
    public double getLimitAmount() { return limitAmount; }
    public boolean isExpense() { return isExpense; }
}
