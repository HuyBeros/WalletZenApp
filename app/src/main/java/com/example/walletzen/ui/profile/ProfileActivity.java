package com.example.walletzen.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.walletzen.R;
import com.example.walletzen.data.SessionManager;
import com.example.walletzen.model.User;
import com.example.walletzen.network.RetrofitClient;
import com.example.walletzen.ui.auth.LoginActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvDisplayName, tvUsername, tvEmail;
    private TextInputEditText edtFullName, edtEmail;
    private TextInputEditText edtOldPassword, edtNewPassword;
    private com.google.android.material.button.MaterialButton btnUpdateInfo, btnChangePassword;
    private BottomNavigationView bottomNav;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        session = new SessionManager(this);

        initViews();
        setupBottomNav();
        loadUserInfo();
    }

    private void initViews() {
        tvDisplayName = findViewById(R.id.tvDisplayName);
        tvUsername = findViewById(R.id.tvUsername);
        tvEmail = findViewById(R.id.tvEmailDisplay);
        edtFullName = findViewById(R.id.edtFullName);
        edtEmail = findViewById(R.id.edtEmail);
        edtOldPassword = findViewById(R.id.edtOldPassword);
        edtNewPassword = findViewById(R.id.edtNewPassword);
        btnUpdateInfo = findViewById(R.id.btnUpdateInfo);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        bottomNav = findViewById(R.id.bottomNavigation);

        // Logout button
        findViewById(R.id.btnLogout).setOnClickListener(v -> confirmLogout());

        // Update info
        btnUpdateInfo.setOnClickListener(v -> updateProfile());

        // Change password
        btnChangePassword.setOnClickListener(v -> changePassword());
    }

    private void loadUserInfo() {
        String fullName = session.getFullName();
        String username = session.getUsername();
        String email = session.getEmail();

        tvDisplayName.setText(fullName);
        tvUsername.setText("@" + username);
        if (tvEmail != null) tvEmail.setText(email);
        if (edtFullName != null) edtFullName.setText(fullName);
        if (edtEmail != null) edtEmail.setText(email);
    }

    private void updateProfile() {
        String fullName = edtFullName.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();

        User userDetails = new User();
        userDetails.setFullName(fullName);
        userDetails.setEmail(email);

        RetrofitClient.getApiService()
                .updateUser(session.getUserId(), userDetails)
                .enqueue(new Callback<User>() {
                    @Override
                    public void onResponse(Call<User> call, Response<User> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            User u = response.body();
                            session.saveSession(u.getUserId(), u.getUsername(),
                                    u.getFullName(), u.getEmail());
                            loadUserInfo();
                            Toast.makeText(ProfileActivity.this,
                                    "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(ProfileActivity.this,
                                    "Cập nhật thất bại!", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(Call<User> call, Throwable t) {
                        Toast.makeText(ProfileActivity.this,
                                "Lỗi: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void changePassword() {
        String oldPw = edtOldPassword.getText().toString().trim();
        String newPw = edtNewPassword.getText().toString().trim();

        if (oldPw.isEmpty() || newPw.isEmpty()) {
            Toast.makeText(this, "Nhập mật khẩu cũ và mới", Toast.LENGTH_SHORT).show();
            return;
        }
        if (newPw.length() < 6) {
            Toast.makeText(this, "Mật khẩu mới ít nhất 6 ký tự", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, String> payload = new HashMap<>();
        payload.put("userId", String.valueOf(session.getUserId()));
        payload.put("oldPassword", oldPw);
        payload.put("newPassword", newPw);

        RetrofitClient.getApiService().changePassword(payload).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ProfileActivity.this,
                            "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show();
                    edtOldPassword.setText("");
                    edtNewPassword.setText("");
                } else {
                    Toast.makeText(ProfileActivity.this,
                            "Mật khẩu cũ không đúng!", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Toast.makeText(ProfileActivity.this,
                        "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc muốn đăng xuất?")
                .setPositiveButton("Đăng xuất", (d, w) -> {
                    session.logout();
                    startActivity(new Intent(this, LoginActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
                    finish();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void setupBottomNav() {
        bottomNav.setSelectedItemId(R.id.nav_profile);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, com.example.walletzen.ui.home.HomeActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
                return true;
            } else if (id == R.id.nav_statistics) {
                startActivity(new Intent(this, com.example.walletzen.ui.statistics.StatisticsActivity.class));
                return true;
            } else if (id == R.id.nav_profile) {
                return true;
            }
            return false;
        });
    }
}