package com.example.walletzen.ui.splash;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.animation.AnimationSet;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.walletzen.R;
import com.example.walletzen.data.SessionManager;
import com.example.walletzen.ui.auth.LoginActivity;
import com.example.walletzen.ui.home.HomeActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Animate logo
        TextView tvLogo = findViewById(R.id.tvLogoSplash);
        TextView tvTagline = findViewById(R.id.tvTagline);

        // Scale + Fade animation for logo
        ScaleAnimation scale = new ScaleAnimation(0.5f, 1f, 0.5f, 1f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        scale.setDuration(600);

        AlphaAnimation fade = new AlphaAnimation(0f, 1f);
        fade.setDuration(800);

        AnimationSet logoAnim = new AnimationSet(true);
        logoAnim.addAnimation(scale);
        logoAnim.addAnimation(fade);
        logoAnim.setFillAfter(true);

        if (tvLogo != null) tvLogo.startAnimation(logoAnim);

        // Tagline fade in with delay
        if (tvTagline != null) {
            AlphaAnimation taglineAnim = new AlphaAnimation(0f, 1f);
            taglineAnim.setDuration(600);
            taglineAnim.setStartOffset(500);
            taglineAnim.setFillAfter(true);
            tvTagline.startAnimation(taglineAnim);
        }

        // Navigate after 2 seconds
        new Handler().postDelayed(() -> {
            SessionManager session = new SessionManager(this);
            Intent intent;
            if (session.isLoggedIn()) {
                intent = new Intent(this, HomeActivity.class);
            } else {
                intent = new Intent(this, LoginActivity.class);
            }
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 2000);
    }
}