package com.example.walletzen.model;

import com.google.gson.annotations.SerializedName;

public class Transaction {
    @SerializedName("transactionId")
    private Long transactionId;

    @SerializedName("amount")
    private Double amount;

    @SerializedName("date")
    private String date; // "yyyy-MM-dd"

    @SerializedName("note")
    private String note;

    @SerializedName("type")
    private String type; // "THU" or "CHI"

    @SerializedName("user")
    private User user;

    @SerializedName("category")
    private Category category;

    public Transaction() {}

    // Constructor for creating new transaction
    public Transaction(Double amount, String date, String note, String type, User user, Category category) {
        this.amount = amount;
        this.date = date;
        this.note = note;
        this.type = type;
        this.user = user;
        this.category = category;
    }

    public Long getTransactionId() { return transactionId; }
    public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public boolean isExpense() { return "CHI".equals(type); }
    public boolean isIncome() { return "THU".equals(type); }

    public String getFormattedAmount() {
        if (amount == null) return "0 ₫";
        long val = (long) Math.abs(amount);
        return String.format("%,d ₫", val).replace(",", ".");
    }

    public String getCategoryName() {
        if (category != null) return category.getCategoryName();
        return "Khác";
    }

    public String getCategoryIcon() {
        if (category != null) return category.getIcon();
        return "💰";
    }

    public String getFormattedDate() {
        if (date == null || date.isEmpty()) return "";
        try {
            if (date.contains("T")) {
                String[] parts = date.split("T");
                String datePart = parts[0]; // "yyyy-MM-dd"
                String timePart = parts[1]; // "HH:mm:ss"
                
                String[] dateSplit = datePart.split("-"); // ["yyyy", "MM", "dd"]
                String[] timeSplit = timePart.split(":"); // ["HH", "mm", "ss"]
                
                return timeSplit[0] + ":" + timeSplit[1] + " " + dateSplit[2] + "/" + dateSplit[1] + "/" + dateSplit[0];
            } else {
                String[] dateSplit = date.split("-");
                return dateSplit[2] + "/" + dateSplit[1] + "/" + dateSplit[0];
            }
        } catch (Exception e) {
            return date;
        }
    }
}