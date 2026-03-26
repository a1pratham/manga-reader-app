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
import java.util.ArrayList;

import com.example.manhwanest.sources.Source;
import com.example.manhwanest.sources.Chapter;
import com.example.manhwanest.sources.MangaPillSource;

import java.util.List;

public class DetailActivity extends AppCompatActivity {

    ImageView bannerImage, coverImage;
    TextView titleText, descriptionText, statusText, ratingText, genresText, chaptersText;

    Button listEditorButton;
    Button infoTab, readTab, continueButton;

    Button sourceSelector;
    String selectedSource = "Select Source";

    LinearLayout infoLayout, readLayout;
    GridLayout chaptersGrid;

    String currentTitle = "";
    String currentStatus = "Add to List";

    int currentStart = 1;
    int rangeSize = 100;
    List<Chapter> cachedChapters = null;

    Button prevBtn, nextBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        // UI Bind
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

        prevBtn = findViewById(R.id.prevBtn);
        nextBtn = findViewById(R.id.nextBtn);

        listEditorButton.setText(currentStatus);
        infoLayout.setVisibility(View.VISIBLE);
        readLayout.setVisibility(View.GONE);

        int animeId = getIntent().getIntExtra("id", -1);

        if (animeId != -1) {
            fetchAnimeDetails(animeId);
        } else {
            Toast.makeText(this, "Invalid ID", Toast.LENGTH_SHORT).show();
        }

        // Source selector
        sourceSelector.setText("Source: " + selectedSource + " ▼");
        sourceSelector.setOnClickListener(v -> showSourceDialog());

        // Tabs
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

            currentStart = 1; //    🔥 RESET RANGE
            cachedChapters = null;
            loadChaptersFromSource();
        });

        continueButton.setOnClickListener(v -> {
            startActivity(new Intent(DetailActivity.this, ReaderActivity.class));
        });

        // List Editor
        listEditorButton.setOnClickListener(v -> {
            ListEditorBottomSheet sheet = new ListEditorBottomSheet();

            sheet.setOnSaveListener((status, progress, score) -> {
                currentStatus = status;
                listEditorButton.setText(status);
            });

            sheet.show(getSupportFragmentManager(), "ListEditor");
        });

        prevBtn.setOnClickListener(v -> {
            if (currentStart > 1) {
                currentStart = Math.max(1, currentStart - rangeSize);
                loadChaptersFromSource();
            } else {
                Toast.makeText(this, "Already at first page", Toast.LENGTH_SHORT).show();
            }
        });

        nextBtn.setOnClickListener(v -> {
            if (cachedChapters != null) {

                // 🔥 check if next page exists
                if (currentStart + rangeSize <= cachedChapters.size()) {
                    currentStart += rangeSize;
                    loadChaptersFromSource();
                } else {
                    Toast.makeText(this, "No more chapters", Toast.LENGTH_SHORT).show();
                }

            } else {
                // fallback (shouldn't happen normally)
                currentStart += rangeSize;
                loadChaptersFromSource();
            }
        });
    }

    // 🔥 FETCH ANILIST DATA
    private void fetchAnimeDetails(int id) {

        String url = "https://graphql.anilist.co";

        String query = "{ Media(id: " + id + ", type: MANGA) { " +
                "title { english userPreferred } " +
                "description bannerImage coverImage { large } " +
                "genres averageScore status chapters } }";

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

                        JSONObject titleObj = media.getJSONObject("title");
                        String title = titleObj.optString("english");

                        if (title == null || title.equals("null") || title.isEmpty()) {
                            title = titleObj.optString("userPreferred");
                        }

                        currentTitle = title;
                        titleText.setText(title);

                        String banner = media.optString("bannerImage");
                        String cover = media.getJSONObject("coverImage").optString("large");

                        Glide.with(this).load(cover).into(coverImage);
                        Glide.with(this).load(banner).into(bannerImage);

                        String description = media.optString("description");
                        if (description != null) {
                            descriptionText.setText(
                                    Html.fromHtml(description, Html.FROM_HTML_MODE_LEGACY)
                            );
                        }

                        JSONArray genresArray = media.getJSONArray("genres");
                        StringBuilder genres = new StringBuilder();

                        for (int i = 0; i < genresArray.length(); i++) {
                            genres.append(genresArray.getString(i));
                            if (i != genresArray.length() - 1) {
                                genres.append(", ");
                            }
                        }

                        genresText.setText("Genres: " + genres);

                        int score = media.optInt("averageScore", 0);
                        ratingText.setText("⭐ " + (score / 10f));

                        statusText.setText("Status: " + media.optString("status", "Unknown"));

                        int chapters = media.optInt("chapters", 0);
                        chaptersText.setText("Chapters: " + (chapters == 0 ? "?" : chapters));

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                error -> Toast.makeText(this, "Failed to load details", Toast.LENGTH_SHORT).show()
        );

        queue.add(request);
    }


    private void loadChaptersFromSource() {

        if (currentTitle == null || currentTitle.isEmpty()) {
            Toast.makeText(this, "Title not loaded yet", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ RANGE TEXT
        String currentRange = currentStart + " - " + (currentStart + rangeSize - 1);
        String nextRange = (currentStart + rangeSize) + " - " + (currentStart + 2 * rangeSize - 1);

        nextBtn.setText("Next ➡ (" + nextRange + ")");
        prevBtn.setText("⬅ Prev (" + currentRange + ")");

        // ✅ PREV BUTTON VISIBILITY
        prevBtn.setVisibility(currentStart <= 1 ? View.INVISIBLE : View.VISIBLE);

        // 🔥 IF ALREADY LOADED → JUST FILTER
        if (cachedChapters != null) {
            showFilteredChapters(cachedChapters);
            return;
        }

        chaptersGrid.removeAllViews();


        Source source;

        // 🔥 SOURCE SWITCH
        switch (selectedSource) {

            case "MangaPill":
                source = new MangaPillSource();
                break;

            default:
                Toast.makeText(this, "Source not implemented yet", Toast.LENGTH_SHORT).show();
                return;
        }

        source.getChapters(currentTitle, new Source.ChapterCallback() {
            @Override
            public void onSuccess(List<Chapter> chapters) {
                cachedChapters = chapters; // 🔥 SAVE
                showFilteredChapters(chapters);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(DetailActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showFilteredChapters(List<Chapter> chapters) {

        chaptersGrid.removeAllViews();
        chaptersGrid.setColumnCount(4); // 🔥 THIS FIXES YOUR GRID

        int startIndex = currentStart - 1;
        int endIndex = Math.min(startIndex + rangeSize, chapters.size());

        if (startIndex >= chapters.size()) {
            Toast.makeText(this, "No more chapters", Toast.LENGTH_SHORT).show();
            return;
        }

        for (int i = startIndex; i < endIndex; i++) {

            final int index = i;
            final List<Chapter> finalChapters = chapters;

            Chapter chapter = chapters.get(i);

            TextView btn = new TextView(this);
            btn.setText(chapter.getNumber());
            btn.setBackgroundResource(R.drawable.pill_bg);

            btn.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            btn.setTextColor(getResources().getColor(android.R.color.white));
            btn.setPadding(0, 24, 0, 24);

            // 🔥 THIS WAS MISSING (MAIN FIX)
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(8, 8, 8, 8);

            btn.setLayoutParams(params);

            // 🔥 CLICK LOGIC (your existing)
            btn.setOnClickListener(v -> {
                Intent intent = new Intent(this, ReaderActivity.class);

                intent.putExtra("chapter_id", chapter.getId());
                intent.putExtra("chapter_number", chapter.getNumber());
                intent.putExtra("chapter_index", index);
                intent.putExtra("chapter_list", new ArrayList<>(finalChapters));

                startActivity(intent);
            });

            chaptersGrid.addView(btn);
        }

        Toast.makeText(this,
                "Showing " + (startIndex + 1) + " - " + endIndex,
                Toast.LENGTH_SHORT).show();
    }
    private void showSourceDialog() {

        String[] sources = {
                "MangaPill",
                "MGecko"
        };

        new AlertDialog.Builder(this)
                .setTitle("Select Source")
                .setItems(sources, (dialog, which) -> {

                    selectedSource = sources[which];

                    sourceSelector.setText("Source: " + selectedSource + " ▼");

                    Toast.makeText(this,
                            "Selected: " + selectedSource,
                            Toast.LENGTH_SHORT).show();

// 🔥 VERY IMPORTANT FIX
                    cachedChapters = null;   // clear old chapters
                    currentStart = 1;        // reset pagination

// 🔥 Reload chapters
                    loadChaptersFromSource();
                })
                .show();
    }
}