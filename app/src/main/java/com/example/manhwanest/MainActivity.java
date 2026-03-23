package com.example.manhwanest;

import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;

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

        loadHomeData();
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

            @Override
            public void onError(String error) { }
        });

        // 🔥 ALL TIME POPULAR
        AniListApi.fetchPopular(this, new AniListApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                popularAdapter.setData(parseResponse(response));
            }

            @Override
            public void onError(String error) { }
        });

        // 🟣 CONTINUE READING (dummy)
        continueAdapter.setData(getDummyContinue());
    }

    private List<Manga> parseResponse(JSONObject response) {
        List<Manga> list = new ArrayList<>();

        try {
            JSONArray mediaArray = response
                    .getJSONObject("data")
                    .getJSONObject("Page")
                    .getJSONArray("media");

            for (int i = 0; i < mediaArray.length(); i++) {
                JSONObject item = mediaArray.getJSONObject(i);

                JSONObject titleObj = item.getJSONObject("title");

// 🔥 FORCE ENGLISH FIRST
                String title = titleObj.optString("english");

// fallback → userPreferred
                if (title == null || title.equals("null") || title.isEmpty()) {
                    title = titleObj.optString("userPreferred");
                }

// final fallback → romaji
                if (title == null || title.equals("null") || title.isEmpty()) {
                    title = titleObj.optString("romaji");
                }

                String image = item
                        .getJSONObject("coverImage")
                        .getString("large");

                String desc = item.optString("description", "No description");

                list.add(new Manga(title, image, desc));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    private List<Manga> getDummyContinue() {
        List<Manga> list = new ArrayList<>();

        list.add(new Manga(
                "Solo Leveling",
                "https://s4.anilist.co/file/anilistcdn/media/manga/cover/large/bx113138.jpg",
                "Continue reading..."
        ));

        list.add(new Manga(
                "One Piece",
                "https://s4.anilist.co/file/anilistcdn/media/manga/cover/large/bx30013.jpg",
                "Continue reading..."
        ));

        return list;
    }
}