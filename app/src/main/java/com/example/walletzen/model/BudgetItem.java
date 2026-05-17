package com.example.walletzen.model;

public class BudgetItem {
    private String categoryName;
    private String icon;
    private double currentAmount;
    private double limitAmount;
    private boolean isExpense;
    private Long budgetId;
    private Long categoryId;

    public BudgetItem(String categoryName, String icon, double currentAmount, double limitAmount, boolean isExpense, Long budgetId, Long categoryId) {
        this.categoryName = categoryName;
        this.icon = icon;
        this.currentAmount = currentAmount;
        this.limitAmount = limitAmount;
        this.isExpense = isExpense;
        this.budgetId = budgetId;
        this.categoryId = categoryId;
    }

    public String getCategoryName() { return categoryName; }
    public String getIcon() { return icon; }
    public double getCurrentAmount() { return currentAmount; }
    public double getLimitAmount() { return limitAmount; }
    public boolean isExpense() { return isExpense; }
    public Long getBudgetId() { return budgetId; }
    public Long getCategoryId() { return categoryId; }
}
