package com.example.manhwanest;

import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity {

    EditText searchInput;
    RecyclerView recyclerView;
    MangaAdapter adapter;
    ProgressBar loading;
    TextView emptyText;

    Handler handler = new Handler();
    Runnable runnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        searchInput = findViewById(R.id.searchInput);
        recyclerView = findViewById(R.id.searchRecycler);
        loading = findViewById(R.id.loading);
        emptyText = findViewById(R.id.emptyText);

        adapter = new MangaAdapter(MangaAdapter.TYPE_GRID);

        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        recyclerView.setAdapter(adapter);

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if (runnable != null) {
                    handler.removeCallbacks(runnable);
                }

                runnable = () -> {
                    if (s.length() > 2) {
                        searchManga(s.toString());
                    }
                };

                handler.postDelayed(runnable, 400);
            }

            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void searchManga(String query) {

        loading.setVisibility(View.VISIBLE);
        emptyText.setVisibility(View.GONE);

        AniListApi.searchManga(this, query, new AniListApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {

                loading.setVisibility(View.GONE);

                List<Manga> result = parseResponse(response);

                if (result.isEmpty()) {
                    emptyText.setVisibility(View.VISIBLE);
                } else {
                    emptyText.setVisibility(View.GONE);
                }

                adapter.setData(result);
            }

            @Override
            public void onError(String error) {
                loading.setVisibility(View.GONE);
                emptyText.setVisibility(View.VISIBLE);
            }
        });
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

                String title = titleObj.optString("english");

                if (title.equals("null") || title.isEmpty()) {
                    title = titleObj.optString("userPreferred");
                }

                if (title.equals("null") || title.isEmpty()) {
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
}