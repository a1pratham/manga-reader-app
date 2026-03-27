package com.example.manhwanest;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.inputmethod.InputMethodManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.splashscreen.SplashScreen;

public class LoginActivity extends AppCompatActivity {

    private AppCompatButton loginBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 1. Install Splash Screen BEFORE super.onCreate()
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);

        // 2. CHECK LOGIN STATE IMMEDIATELY
        // If they have a token, skip this screen and go to Main
        if (isLoggedIn()) {
            startMainActivity();
            return;
        }

        setContentView(R.layout.activity_login);

        loginBtn = findViewById(R.id.loginBtn);

        // 3. OAUTH REDIRECT LOGIC
        loginBtn.setOnClickListener(v -> {
            String clientId = "37872";
            String url = "https://anilist.co/api/v2/oauth/authorize" +
                    "?client_id=" + clientId +
                    "&response_type=token";

            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(browserIntent);
        });

        // Check if we arrived here via an OAuth redirect
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        Uri uri = intent.getData();

        // The AniList token arrives in the URL fragment (#access_token=...)
        if (uri != null && "myapp".equals(uri.getScheme())) {
            String fragment = uri.getFragment();

            if (fragment != null && fragment.contains("access_token=")) {
                String[] parts = fragment.split("&");
                for (String part : parts) {
                    if (part.startsWith("access_token=")) {
                        String token = part.replace("access_token=", "");
                        saveToken(token);
                        startMainActivity();
                    }
                }
            }
        }
    }

    private boolean isLoggedIn() {
        return getSharedPreferences("user", MODE_PRIVATE)
                .getString("token", null) != null;
    }

    private void saveToken(String token) {
        getSharedPreferences("user", MODE_PRIVATE)
                .edit()
                .putString("token", token)
                .apply();
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish(); // Important: Remove LoginActivity from the backstack
    }
}