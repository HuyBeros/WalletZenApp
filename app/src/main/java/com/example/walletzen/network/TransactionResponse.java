package com.example.walletzen.network;

import com.example.walletzen.model.Transaction;
import com.google.gson.annotations.SerializedName;

public class TransactionResponse {
    @SerializedName("transaction")
    private Transaction transaction;

    @SerializedName("budgetMessage")
    private String budgetMessage;

    public Transaction getTransaction() { return transaction; }
    public String getBudgetMessage() { return budgetMessage; }
}
