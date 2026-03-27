package com.example.manhwanest;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
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

    private EditText searchInput;
    private RecyclerView recyclerView;
    private MangaAdapter adapter;
    private ProgressBar loading;
    private TextView emptyText;

    private Handler handler = new Handler();
    private Runnable runnable;

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

        // 1. AUTO-SEARCH AS THE USER TYPES (Debounced)
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (runnable != null) {
                    handler.removeCallbacks(runnable);
                }

                runnable = () -> {
                    if (s.length() > 2) {
                        searchManga(s.toString().trim());
                    }
                };

                handler.postDelayed(runnable, 400);
            }

            @Override public void afterTextChanged(Editable s) {}
        });

        // 2. LISTEN FOR THE "SEARCH" MAGNIFYING GLASS BUTTON ON KEYBOARD
        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            boolean isEnterKeyPressed = event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER;

            if (actionId == EditorInfo.IME_ACTION_SEARCH || isEnterKeyPressed) {
                String query = searchInput.getText().toString().trim();

                if (!query.isEmpty()) {
                    // Cancel any pending auto-searches so we don't trigger the API twice
                    if (runnable != null) {
                        handler.removeCallbacks(runnable);
                    }

                    // Force the search
                    searchManga(query);

                    // Hide the keyboard so the user can see the grid results cleanly
                    hideKeyboard(v);
                }
                return true; // Tells Android we successfully handled the button press
            }
            return false;
        });
    }

    private void searchManga(String query) {
        loading.setVisibility(View.VISIBLE);
        emptyText.setVisibility(View.GONE);

        AniListApi.searchManga(this, query, new AniListApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                // Ensure UI updates run on the main thread
                runOnUiThread(() -> {
                    loading.setVisibility(View.GONE);
                    List<Manga> result = parseResponse(response);

                    if (result.isEmpty()) {
                        emptyText.setVisibility(View.VISIBLE);
                        emptyText.setText("No results found for '" + query + "'");
                    } else {
                        emptyText.setVisibility(View.GONE);
                    }

                    adapter.setData(result);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    loading.setVisibility(View.GONE);
                    emptyText.setVisibility(View.VISIBLE);
                    emptyText.setText("Error loading results. Please try again.");
                });
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

    // Helper method to cleanly close the keyboard
    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}