package com.example.walletzen.network;

import com.example.walletzen.model.Budget;
import com.example.walletzen.model.Category;
import com.example.walletzen.model.Transaction;
import com.example.walletzen.model.User;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // ============ AUTH ============

    // POST /api/users/register  Body: { username, password, fullName, email }
    @POST("api/users/register")
    Call<String> register(@Body User user);

    // POST /api/users/login  Body: { username, password }
    @POST("api/users/login")
    Call<User> login(@Body Map<String, String> credentials);

    // PUT /api/users/{userId}  Body: User
    @PUT("api/users/{userId}")
    Call<User> updateUser(@Path("userId") Long userId, @Body User user);

    // PUT /api/users/change-password  Body: { userId, oldPassword, newPassword }
    @PUT("api/users/change-password")
    Call<String> changePassword(@Body Map<String, String> payload);

    // POST /api/users/forgot-password  Body: { email }
    @POST("api/users/forgot-password")
    Call<String> forgotPassword(@Body Map<String, String> payload);

    // POST /api/users/google-login Body: { idToken }
    @POST("api/users/google-login")
    Call<User> loginWithGoogle(@Body Map<String, String> payload);

    // ============ TRANSACTIONS ============

    // GET /api/transactions?userId=1
    @GET("api/transactions")
    Call<List<Transaction>> getTransactions(@Query("userId") Long userId);

    // GET /api/transactions/recent?userId=1
    @GET("api/transactions/recent")
    Call<List<Transaction>> getRecentTransactions(@Query("userId") Long userId);

    // POST /api/transactions  Body: Transaction
    @POST("api/transactions")
    Call<TransactionResponse> createTransaction(@Body Transaction transaction);

    // PUT /api/transactions/{id}?userId=1
    @PUT("api/transactions/{id}")
    Call<TransactionResponse> updateTransaction(@Path("id") Long id,
                                                @Body Transaction transaction,
                                                @Query("userId") Long userId);

    // DELETE /api/transactions/{id}?userId=1
    @DELETE("api/transactions/{id}")
    Call<String> deleteTransaction(@Path("id") Long id, @Query("userId") Long userId);

    // GET /api/transactions/search?note=...&userId=1
    @GET("api/transactions/search")
    Call<List<Transaction>> searchTransactions(@Query("note") String note,
                                               @Query("userId") Long userId);

    // GET /api/transactions?userId=1&type=THU
    @GET("api/transactions")
    Call<List<Transaction>> getTransactionsByType(@Query("userId") Long userId,
                                                   @Query("type") String type);

    // ============ CATEGORIES ============

    // GET /api/categories?type=CHI  (type optional)
    @GET("api/categories")
    Call<List<Category>> getCategories();

    @GET("api/categories")
    Call<List<Category>> getCategoriesByType(@Query("type") String type);

    // ============ DASHBOARD ============

    // GET /api/dashboard/balance?userId=1
    @GET("api/dashboard/balance")
    Call<Map<String, Double>> getBalance(@Query("userId") Long userId);

    // GET /api/dashboard/balance-month?userId=1&month=2025-01
    @GET("api/dashboard/balance-month")
    Call<Map<String, Double>> getBalanceByMonth(@Query("userId") Long userId,
                                                @Query("month") String month);

    // GET /api/dashboard/monthly-summary?userId=1&months=6
    @GET("api/dashboard/monthly-summary")
    Call<Map<String, Object>> getMonthlySummary(@Query("userId") Long userId,
                                                @Query("months") int months);

    // GET /api/dashboard/spending-by-category?userId=1
    @GET("api/dashboard/spending-by-category")
    Call<Map<String, Double>> getSpendingByCategory(@Query("userId") Long userId);

    // GET /api/dashboard/trend?userId=1&months=6
    @GET("api/dashboard/trend")
    Call<List<Map<String, Object>>> getTrend(@Query("userId") Long userId,
                                             @Query("months") int months);

    // ============ BUDGETS ============
    @GET("api/budgets")
    Call<List<Budget>> getBudgets(@Query("userId") Long userId);
}