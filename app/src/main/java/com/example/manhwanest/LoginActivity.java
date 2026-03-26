package com.example.manhwanest;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    Button loginBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        loginBtn = findViewById(R.id.loginBtn);

        loginBtn.setOnClickListener(v -> {

            String clientId = "37872";

            String url = "https://anilist.co/api/v2/oauth/authorize" +
                    "?client_id=" + clientId +
                    "&response_type=token";

            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        });

        String token = getSharedPreferences("user", MODE_PRIVATE)
                .getString("token", null);

        if (token != null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        Uri uri = getIntent().getData();

        if (uri != null && uri.toString().contains("access_token")) {

            String fragment = uri.getFragment();

            if (fragment != null) {

                String[] parts = fragment.split("&");

                for (String part : parts) {
                    if (part.startsWith("access_token=")) {

                        String token = part.replace("access_token=", "");

                        saveToken(token);

                        // 🔥 GO TO MAIN
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    }
                }
            }

            setIntent(new Intent());
        }
    }

    private void saveToken(String token) {
        getSharedPreferences("user", MODE_PRIVATE)
                .edit()
                .putString("token", token)
                .apply();
    }
}