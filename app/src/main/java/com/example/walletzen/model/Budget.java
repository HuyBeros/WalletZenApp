package com.example.walletzen.model;

import com.google.gson.annotations.SerializedName;

public class Budget {
    @SerializedName("budgetId")
    private Long budgetId;

    @SerializedName("month")
    private String month;

    @SerializedName("totalLimit")
    private Double totalLimit;

    @SerializedName("categoryLimit")
    private Double categoryLimit;

    @SerializedName("category")
    private Category category;

    public Long getBudgetId() {
        return budgetId;
    }

    public void setBudgetId(Long budgetId) {
        this.budgetId = budgetId;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public Double getTotalLimit() {
        return totalLimit;
    }

    public void setTotalLimit(Double totalLimit) {
        this.totalLimit = totalLimit;
    }

    public Double getCategoryLimit() {
        return categoryLimit;
    }

    public void setCategoryLimit(Double categoryLimit) {
        this.categoryLimit = categoryLimit;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }
}
