package com.example.manhwanest;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONObject;

public class DetailActivity extends AppCompatActivity {

    ImageView bannerImage, coverImage;
    TextView titleText, descriptionText, statusText, ratingText, genresText, chaptersText;

    Button listEditorButton;
    Button infoTab, readTab, continueButton;

    // ✅ Source selector
    Button sourceSelector;
    String selectedSource = "MangaDex";

    LinearLayout infoLayout, readLayout;
    GridLayout chaptersGrid;

    String currentStatus = "Add to List";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        // 🔥 Bind UI
        bannerImage = findViewById(R.id.bannerImage);
        coverImage = findViewById(R.id.coverImage);
        titleText = findViewById(R.id.titleText);
        descriptionText = findViewById(R.id.descriptionText);
        statusText = findViewById(R.id.statusText);
        ratingText = findViewById(R.id.ratingText);
        genresText = findViewById(R.id.genresText);
        chaptersText = findViewById(R.id.chaptersText);

        listEditorButton = findViewById(R.id.listEditorButton);

        infoTab = findViewById(R.id.infoTab);
        readTab = findViewById(R.id.readTab);

        infoLayout = findViewById(R.id.infoLayout);
        readLayout = findViewById(R.id.readLayout);

        continueButton = findViewById(R.id.continueButton);
        chaptersGrid = findViewById(R.id.chaptersGrid);

        sourceSelector = findViewById(R.id.sourceSelector);

        // 🔥 Default UI
        listEditorButton.setText(currentStatus);
        infoLayout.setVisibility(View.VISIBLE);
        readLayout.setVisibility(View.GONE);

        // 🔥 Get ID
        int animeId = getIntent().getIntExtra("id", -1);

// 🔍 DEBUG LOG
        android.util.Log.d("DEBUG_ID", "Received ID: " + animeId);

        if (animeId != -1) {
            fetchAnimeDetails(animeId);
        } else {
            Toast.makeText(this, "Invalid ID", Toast.LENGTH_SHORT).show();
        }

        // 🔥 Source selector
        sourceSelector.setText("Source: " + selectedSource + " ▼");
        sourceSelector.setOnClickListener(v -> showSourceDialog());

        // 🔥 TAB SWITCHING

        infoTab.setOnClickListener(v -> {
            infoLayout.setVisibility(View.VISIBLE);
            readLayout.setVisibility(View.GONE);

            infoTab.setBackgroundResource(R.drawable.pink_pill);
            readTab.setBackgroundResource(R.drawable.pill_bg);

            infoTab.setTextColor(getResources().getColor(android.R.color.white));
            readTab.setTextColor(getResources().getColor(android.R.color.darker_gray));
        });

        readTab.setOnClickListener(v -> {
            infoLayout.setVisibility(View.GONE);
            readLayout.setVisibility(View.VISIBLE);

            readTab.setBackgroundResource(R.drawable.pink_pill);
            infoTab.setBackgroundResource(R.drawable.pill_bg);

            readTab.setTextColor(getResources().getColor(android.R.color.white));
            infoTab.setTextColor(getResources().getColor(android.R.color.darker_gray));
        });

        // 🔥 Continue button
        continueButton.setOnClickListener(v -> {
            startActivity(new Intent(DetailActivity.this, ReaderActivity.class));
        });

        // 🔥 DEMO CHAPTER GRID (will replace later)
        loadDummyChapters();

        // 🔥 LIST EDITOR
        listEditorButton.setOnClickListener(v -> {

            ListEditorBottomSheet sheet = new ListEditorBottomSheet();

            sheet.setOnSaveListener((status, progress, score) -> {
                currentStatus = status;
                listEditorButton.setText(status);
            });

            sheet.show(getSupportFragmentManager(), "ListEditor");
        });
    }

    // 🔥 ANILIST API
    private void fetchAnimeDetails(int id) {

        String url = "https://graphql.anilist.co";

        String query = "{ Media(id: " + id + ", type: MANGA) { " +
                "id " +
                "title { english userPreferred } " +
                "description " +
                "bannerImage " +
                "coverImage { large } " +
                "genres " +
                "averageScore " +
                "status " +
                "chapters " +
                "} }";

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("query", query);
        } catch (Exception e) {
            e.printStackTrace();
        }

        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                jsonBody,
                response -> {
                    try {
                        JSONObject media = response.getJSONObject("data").getJSONObject("Media");

                        // ✅ TITLE (ENGLISH FIRST)
                        JSONObject titleObj = media.getJSONObject("title");
                        String title = titleObj.optString("english");

                        if (title == null || title.equals("null") || title.isEmpty()) {
                            title = titleObj.optString("userPreferred");
                        }

                        titleText.setText(title);

                        // ✅ IMAGES
                        String banner = media.optString("bannerImage");
                        String cover = media.getJSONObject("coverImage").optString("large");

                        Glide.with(this).load(cover).into(coverImage);
                        Glide.with(this).load(banner).into(bannerImage);

                        // ✅ DESCRIPTION CLEAN
                        String description = media.optString("description");

                        if (description != null) {
                            descriptionText.setText(
                                    Html.fromHtml(description, Html.FROM_HTML_MODE_LEGACY)
                            );
                        } else {
                            descriptionText.setText("No description available.");
                        }

                        // ✅ GENRES
                        JSONArray genresArray = media.getJSONArray("genres");
                        StringBuilder genres = new StringBuilder();

                        for (int i = 0; i < genresArray.length(); i++) {
                            genres.append(genresArray.getString(i));
                            if (i != genresArray.length() - 1) {
                                genres.append(", ");
                            }
                        }

                        genresText.setText("Genres: " + genres.toString());

                        // ✅ SCORE
                        int score = media.optInt("averageScore", 0);
                        ratingText.setText("⭐ " + score);

                        // ✅ STATUS
                        String status = media.optString("status", "Unknown");
                        statusText.setText("Status: " + status);

                        // ✅ CHAPTERS
                        int chapters = media.optInt("chapters", 0);
                        chaptersText.setText("Chapters: " + (chapters == 0 ? "?" : chapters));

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    error.printStackTrace();
                    Toast.makeText(this, "Failed to load details", Toast.LENGTH_SHORT).show();
                }
        );

        queue.add(request);
    }

    // 🔥 SOURCE DIALOG
    private void showSourceDialog() {
        String[] sources = {"MangaDex", "MangaBuddy", "MangaPill", "MGecko"};

        new AlertDialog.Builder(this)
                .setTitle("Select Source")
                .setItems(sources, (dialog, which) -> {
                    selectedSource = sources[which];
                    sourceSelector.setText("Source: " + selectedSource + " ▼");

                    Toast.makeText(this, "Selected: " + selectedSource, Toast.LENGTH_SHORT).show();

                    // 🔥 Later: reload chapters based on source
                })
                .show();
    }

    // 🔥 TEMP CHAPTER GRID
    private void loadDummyChapters() {

        int lastReadChapter = 5;

        for (int i = 1; i <= 20; i++) {

            TextView chapterBtn = new TextView(this);
            chapterBtn.setText(String.valueOf(i));
            chapterBtn.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            chapterBtn.setTextColor(getResources().getColor(android.R.color.white));
            chapterBtn.setPadding(0, 24, 0, 24);

            if (lastReadChapter > 0) {
                continueButton.setText("Continue Reading (Ch " + lastReadChapter + ")");
            } else {
                continueButton.setText("Start Reading");
            }

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(8, 8, 8, 8);

            chapterBtn.setLayoutParams(params);

            chapterBtn.setOnClickListener(v -> {
                startActivity(new Intent(DetailActivity.this, ReaderActivity.class));
            });

            chaptersGrid.addView(chapterBtn);
        }
    }
}