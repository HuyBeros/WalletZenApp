package com.example.walletzen.model;

import com.google.gson.annotations.SerializedName;

public class Category {
    @SerializedName("categoryId")
    private Long categoryId;

    @SerializedName("categoryName")
    private String categoryName;

    @SerializedName("type")
    private String type; // "THU" or "CHI"

    @SerializedName("userId")
    private Long userId;

    public Category() {}

    public Category(Long categoryId, String categoryName, String type) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.type = type;
    }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public boolean isExpense() { return "CHI".equals(type); }
    public boolean isIncome() { return "THU".equals(type); }

    /** Map category name to emoji icon */
    public String getIcon() {
        if (categoryName == null) return "💰";
        switch (categoryName.toLowerCase()) {
            case "ăn uống": case "food": return "🍴";
            case "giao thông": case "transport": return "🚗";
            case "mua sắm": case "shopping": return "🛍";
            case "sức khỏe": case "health": return "❤️";
            case "giải trí": case "entertainment": return "🎮";
            case "tiền lương": case "salary": return "💼";
            case "học phí": case "giáo dục": return "🎓";
            case "nhà ở": case "housing": return "🏠";
            case "du lịch": return "✈️";
            case "thú cưng": return "🐶";
            case "điện nước": return "💡";
            case "quà tặng": return "🎁";
            case "tiền thưởng": return "🎉";
            default: return "💰";
        }
    }
}
