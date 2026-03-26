package com.example.manhwanest;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class ProfileActivity extends AppCompatActivity {

    private ImageView avatar;
    private TextView username;
    private Button logoutBtn;

    private String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        avatar = findViewById(R.id.avatar);
        username = findViewById(R.id.username);
        logoutBtn = findViewById(R.id.logoutBtn);

        // ✅ SAME PREF NAME AS MAIN ACTIVITY
        SharedPreferences prefs = getSharedPreferences("user", MODE_PRIVATE);
        token = prefs.getString("token", null);

        // 🔥 if not logged in → go back
        if (token == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        fetchUserData();

        logoutBtn.setOnClickListener(v -> logout());
    }

    private void fetchUserData() {
        new Thread(() -> {
            try {
                Log.d("TOKEN", "Token: " + token);

                URL url = new URL("https://graphql.anilist.co");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject json = new JSONObject();
                json.put("query", "{ Viewer { name avatar { large } } }");

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes());
                os.close();

                Scanner scanner = new Scanner(conn.getInputStream());
                String response = scanner.useDelimiter("\\A").next();
                scanner.close();

                Log.d("API_RESPONSE", response);

                JSONObject data = new JSONObject(response)
                        .getJSONObject("data")
                        .getJSONObject("Viewer");

                String name = data.getString("name");
                String avatarUrl = data.getJSONObject("avatar").getString("large");

                runOnUiThread(() -> {
                    username.setText(name);
                    Glide.with(ProfileActivity.this)
                            .load(avatarUrl)
                            .into(avatar);
                });

            } catch (Exception e) {
                Log.e("PROFILE_ERROR", e.toString());
            }
        }).start();
    }

    private void logout() {
        SharedPreferences prefs = getSharedPreferences("user", MODE_PRIVATE);
        prefs.edit().clear().apply();

        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}