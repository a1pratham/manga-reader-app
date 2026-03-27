package com.example.manhwanest;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import android.net.Uri;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    RecyclerView rvContinue, rvRecent, rvPopular;
    Button tabManga, tabManhwa;

    MangaAdapter continueAdapter;
    MangaAdapter recentAdapter;
    MangaAdapter popularAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rvContinue = findViewById(R.id.rvContinue);
        rvRecent = findViewById(R.id.rvRecent);
        rvPopular = findViewById(R.id.rvPopular);

        continueAdapter = setupRecycler(rvContinue);
        recentAdapter = setupRecycler(rvRecent);
        popularAdapter = setupRecycler(rvPopular);

        findViewById(R.id.searchBar).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SearchActivity.class));
        });

        findViewById(R.id.profileIcon).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ProfileActivity.class));
        });

        loadHomeData();
    }

    // 🔥 HANDLE LOGIN RESPONSE
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
                        Toast.makeText(this, "Login Success 🔥", Toast.LENGTH_SHORT).show();

                        // 🔥 Refresh data immediately so the Continue Reading list appears!
                        loadHomeData();
                    }
                }
            }

            // 🔥 PREVENT MULTIPLE TRIGGERS
            setIntent(new Intent());
        }
    }

    // 🔥 SAVE TOKEN
    private void saveToken(String token) {
        getSharedPreferences("user", MODE_PRIVATE)
                .edit()
                .putString("token", token)
                .apply();
    }

    private MangaAdapter setupRecycler(RecyclerView recyclerView) {
        recyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );

        MangaAdapter adapter = new MangaAdapter(MangaAdapter.TYPE_HOME);
        recyclerView.setAdapter(adapter);

        return adapter;
    }

    private void loadHomeData() {

        // 🔥 TRENDING NOW
        AniListApi.fetchTrending(this, new AniListApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                recentAdapter.setData(parseResponse(response));
            }
            @Override public void onError(String error) { }
        });

        // 🔥 ALL TIME POPULAR
        AniListApi.fetchPopular(this, new AniListApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                popularAdapter.setData(parseResponse(response));
            }
            @Override public void onError(String error) { }
        });

        // 🟣 REAL CONTINUE READING DATA
        SharedPreferences prefs = getSharedPreferences("user", MODE_PRIVATE);
        String token = prefs.getString("token", null);

        if (token != null) {
            AniListApi.fetchContinueReading(this, new AniListApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    List<Manga> activeList = parseContinueReadingResponse(response);
                    continueAdapter.setData(activeList);
                }
                @Override
                public void onError(String error) {
                    // Fail silently or show a small error if needed
                }
            });
        } else {
            // Empty the list if not logged in
            continueAdapter.setData(new ArrayList<>());
        }
    }

    // 🔥 PARSER FOR TRENDING / POPULAR
    private List<Manga> parseResponse(JSONObject response) {
        List<Manga> list = new ArrayList<>();

        try {
            JSONArray mediaArray = response
                    .getJSONObject("data")
                    .getJSONObject("Page")
                    .getJSONArray("media");

            for (int i = 0; i < mediaArray.length(); i++) {
                JSONObject item = mediaArray.getJSONObject(i);

                int id = item.getInt("id");

                JSONObject titleObj = item.getJSONObject("title");
                String title = titleObj.optString("english");

                if (title == null || title.equals("null") || title.isEmpty()) {
                    title = titleObj.optString("userPreferred");
                }
                if (title == null || title.equals("null") || title.isEmpty()) {
                    title = titleObj.optString("romaji");
                }

                String image = item.getJSONObject("coverImage").getString("large");
                String desc = item.optString("description", "No description");

                list.add(new Manga(id, title, image, desc));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    // 🔥 NEW: PARSER EXCLUSIVELY FOR USER'S READING LIST
    private List<Manga> parseContinueReadingResponse(JSONObject response) {
        List<Manga> list = new ArrayList<>();

        try {
            JSONArray listsArray = response
                    .getJSONObject("data")
                    .getJSONObject("MediaListCollection")
                    .getJSONArray("lists");

            // If the user is reading at least one thing, listsArray will have length > 0
            if (listsArray.length() > 0) {
                JSONArray entries = listsArray.getJSONObject(0).getJSONArray("entries");

                for (int i = 0; i < entries.length(); i++) {
                    JSONObject media = entries.getJSONObject(i).getJSONObject("media");

                    int id = media.getInt("id");

                    JSONObject titleObj = media.getJSONObject("title");
                    String title = titleObj.optString("english");

                    if (title == null || title.equals("null") || title.isEmpty()) {
                        title = titleObj.optString("userPreferred");
                    }
                    if (title == null || title.equals("null") || title.isEmpty()) {
                        title = titleObj.optString("romaji");
                    }

                    String image = media.getJSONObject("coverImage").getString("large");
                    String desc = media.optString("description", "No description");

                    list.add(new Manga(id, title, image, desc));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }
}