package com.example.manhwanest;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.example.manhwanest.sources.Chapter;
import com.example.manhwanest.sources.MangaKatanaSource; // 🔥 Using the stable Katana source
import com.example.manhwanest.sources.MangaPillSource;
import com.example.manhwanest.sources.Source;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DetailActivity extends AppCompatActivity {

    ImageView bannerImage, coverImage;
    TextView titleText, descriptionText, statusText, ratingText, genresText, chaptersText;
    Button listEditorButton, infoTab, readTab, continueButton, sourceSelector, prevBtn, nextBtn;
    ProgressBar chapterLoader;
    View loaderOverlay;
    LinearLayout infoLayout, readLayout;
    GridLayout chaptersGrid;

    int mediaId = -1;
    String selectedSource = "MangaPill";
    int totalChapters = 0;
    String currentTitle = "";
    String currentStatus = "Add to List";

    int currentStart = 1;
    int rangeSize = 100;
    List<Chapter> cachedChapters = null;

    int savedProgress = 0;
    int savedScore = 0;
    String savedStatus = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        bindViews();
        setupClickListeners();

        mediaId = getIntent().getIntExtra("id", -1);

        if (mediaId != -1) {
            fetchAnimeDetails(mediaId);
        } else {
            Toast.makeText(this, "Invalid ID", Toast.LENGTH_SHORT).show();
        }

        switchTab(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mediaId != -1) {
            fetchTrackingData();
        }
    }

    private void bindViews() {
        bannerImage = findViewById(R.id.bannerImage);
        coverImage = findViewById(R.id.coverImage);
        titleText = findViewById(R.id.titleText);
        descriptionText = findViewById(R.id.descriptionText);
        statusText = findViewById(R.id.statusText);
        ratingText = findViewById(R.id.ratingText);
        genresText = findViewById(R.id.genresText);
        chaptersText = findViewById(R.id.chaptersText);
        chapterLoader = findViewById(R.id.chapterLoader);
        loaderOverlay = findViewById(R.id.loaderOverlay);
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
        sourceSelector.setText("Source: " + selectedSource + " ▼");
    }

    private void setupClickListeners() {
        sourceSelector.setOnClickListener(v -> showSourceDialog());
        infoTab.setOnClickListener(v -> switchTab(true));
        readTab.setOnClickListener(v -> switchTab(false));
        continueButton.setOnClickListener(v -> handleContinueReading());
        listEditorButton.setOnClickListener(v -> showListEditor());

        prevBtn.setOnClickListener(v -> {
            if (currentStart > 1) {
                currentStart = Math.max(1, currentStart - rangeSize);
                loadChaptersFromSource();
            } else {
                Toast.makeText(this, "Already at first page", Toast.LENGTH_SHORT).show();
            }
        });

        nextBtn.setOnClickListener(v -> {
            if (cachedChapters == null || currentStart + rangeSize <= cachedChapters.size()) {
                currentStart += rangeSize;
                loadChaptersFromSource();
            } else {
                Toast.makeText(this, "No more chapters", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void switchTab(boolean isInfoTab) {
        infoLayout.setVisibility(isInfoTab ? View.VISIBLE : View.GONE);
        readLayout.setVisibility(isInfoTab ? View.GONE : View.VISIBLE);

        infoTab.setBackgroundResource(isInfoTab ? R.drawable.pink_pill : R.drawable.pill_bg);
        infoTab.setTextColor(getResources().getColor(isInfoTab ? android.R.color.white : android.R.color.darker_gray));

        readTab.setBackgroundResource(isInfoTab ? R.drawable.pill_bg : R.drawable.pink_pill);
        readTab.setTextColor(getResources().getColor(isInfoTab ? android.R.color.darker_gray : android.R.color.white));

        if (!isInfoTab) {
            currentStart = 1;
            cachedChapters = null;
            loadChaptersFromSource();
        }
    }

    private void fetchAnimeDetails(int id) {
        String url = "https://graphql.anilist.co";
        String query = "{ Media(id: " + id + ", type: MANGA) { title { english userPreferred } description bannerImage coverImage { large } genres averageScore status chapters } }";

        JSONObject jsonBody = new JSONObject();
        try { jsonBody.put("query", query); } catch (Exception ignored) {}

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, jsonBody,
                response -> {
                    try {
                        parseMediaResponse(response.getJSONObject("data").getJSONObject("Media"));
                        fetchTrackingData();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                error -> Toast.makeText(this, "Failed to load details", Toast.LENGTH_SHORT).show()
        );

        Volley.newRequestQueue(this).add(request);
    }

    private void parseMediaResponse(JSONObject media) throws Exception {
        JSONObject titleObj = media.getJSONObject("title");
        currentTitle = titleObj.optString("english");
        if (TextUtils.isEmpty(currentTitle) || currentTitle.equals("null")) {
            currentTitle = titleObj.optString("userPreferred");
        }
        titleText.setText(currentTitle);

        Glide.with(this).load(media.getJSONObject("coverImage").optString("large")).into(coverImage);
        Glide.with(this).load(media.optString("bannerImage")).into(bannerImage);

        String description = media.optString("description");
        if (!TextUtils.isEmpty(description)) {
            descriptionText.setText(Html.fromHtml(description, Html.FROM_HTML_MODE_LEGACY));
        }

        JSONArray genresArray = media.getJSONArray("genres");
        List<String> genreList = new ArrayList<>();
        for (int i = 0; i < genresArray.length(); i++) genreList.add(genresArray.getString(i));
        genresText.setText("Genres: " + TextUtils.join(", ", genreList));

        ratingText.setText("⭐ " + (media.optInt("averageScore", 0) / 10f));
        statusText.setText("Status: " + media.optString("status", "Unknown"));

        totalChapters = media.optInt("chapters", 0);
        chaptersText.setText("Chapters: " + (totalChapters == 0 ? "?" : totalChapters));
    }

    private void fetchTrackingData() {
        AniListApi.fetchMediaListEntry(this, mediaId, new AniListApi.AniListCallback() {
            @Override
            public void onSuccess(String status, int progress, int score) {
                runOnUiThread(() -> updateTrackingState(status, progress, score));
            }
            @Override public void onEmpty() {}
            @Override public void onFailure(String error) {}
        });
    }

    private void updateTrackingState(String apiStatus, int progress, int score) {
        savedStatus = mapStatusToUi(apiStatus);
        savedProgress = progress;
        savedScore = score;
        currentStatus = savedStatus;
        updateTrackingUI();
    }

    private void updateTrackingUI() {
        listEditorButton.setText(savedStatus + " (" + savedProgress + "/" + (totalChapters == 0 ? "?" : totalChapters) + ")");
        continueButton.setText(savedProgress == 0 ? "Start Reading" : "Continue Chapter " + savedProgress);
    }

    private void loadChaptersFromSource() {
        if (TextUtils.isEmpty(currentTitle)) {
            Toast.makeText(this, "Title not loaded yet", Toast.LENGTH_SHORT).show();
            return;
        }

        updatePaginationUI();

        if (cachedChapters != null) {
            hideLoader();
            showFilteredChapters(cachedChapters);
            return;
        }

        showLoader();

        Source source = null;
        if (selectedSource.equals("MangaPill")) {
            source = new MangaPillSource();
        } else if (selectedSource.equals("MangaKatana")) {
            source = new MangaKatanaSource();
        }

        if (source == null) {
            Toast.makeText(this, "Source not implemented yet", Toast.LENGTH_SHORT).show();
            hideLoader();
            return;
        }

        source.getChapters(currentTitle, new Source.ChapterCallback() {
            @Override
            public void onSuccess(List<Chapter> chapters) {
                cachedChapters = chapters;
                hideLoader();
                showFilteredChapters(chapters);
            }

            @Override
            public void onError(String error) {
                hideLoader();
                Toast.makeText(DetailActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showFilteredChapters(List<Chapter> chapters) {
        chaptersGrid.removeAllViews();
        chaptersGrid.setColumnCount(4);

        int startIndex = currentStart - 1;
        int endIndex = Math.min(startIndex + rangeSize, chapters.size());

        if (startIndex >= chapters.size()) {
            Toast.makeText(this, "No more chapters", Toast.LENGTH_SHORT).show();
            return;
        }

        for (int i = startIndex; i < endIndex; i++) {
            final int index = i;
            Chapter chapter = chapters.get(i);

            TextView btn = new TextView(this);
            btn.setText(chapter.getNumber());
            btn.setBackgroundResource(R.drawable.pill_bg);
            btn.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            btn.setTextColor(getResources().getColor(android.R.color.white));
            btn.setPadding(0, 24, 0, 24);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(8, 8, 8, 8);
            btn.setLayoutParams(params);

            btn.setOnClickListener(v -> openReader(chapter, index));
            chaptersGrid.addView(btn);
        }
    }

    private void openReader(Chapter chapter, int index) {
        int chapterNumber = safeParseInt(chapter.getNumber(), index + 1);

        if (savedStatus.isEmpty()) savedStatus = "Reading";

        if (savedStatus.equals("Reading") && totalChapters > 0 && chapterNumber >= totalChapters) {
            savedProgress = totalChapters - 1;
        } else {
            savedProgress = chapterNumber;
        }

        updateTrackingUI();
        saveListProgressToApi();

        Intent intent = new Intent(this, ReaderActivity.class);
        intent.putExtra("media_id", mediaId);
        intent.putExtra("chapter_id", chapter.getId());
        intent.putExtra("chapter_number", chapter.getNumber());
        intent.putExtra("chapter_index", index);
        intent.putExtra("chapter_list", new ArrayList<>(cachedChapters));
        intent.putExtra("total_chapters", totalChapters);
        intent.putExtra("source_name", selectedSource); // 🔥 Passing source name
        startActivity(intent);
    }

    private void handleContinueReading() {
        if (cachedChapters == null || cachedChapters.isEmpty()) {
            Toast.makeText(this, "Load chapters first", Toast.LENGTH_SHORT).show();
            return;
        }

        int targetIndex = -1;
        for (int i = 0; i < cachedChapters.size(); i++) {
            int currentChapNum = safeParseInt(cachedChapters.get(i).getNumber(), i + 1);
            if (currentChapNum == savedProgress) {
                targetIndex = i;
                break;
            }
        }

        if (targetIndex == -1) {
            targetIndex = savedProgress > 0 ? savedProgress - 1 : 0;
        }

        int finalIndex = Math.min(Math.max(0, targetIndex), cachedChapters.size() - 1);
        openReader(cachedChapters.get(finalIndex), finalIndex);
    }

    private void showListEditor() {
        ListEditorBottomSheet sheet = new ListEditorBottomSheet();
        Bundle args = new Bundle();
        args.putString("status", savedStatus.isEmpty() ? currentStatus : savedStatus);
        args.putInt("progress", savedProgress);
        args.putInt("score", savedScore);
        sheet.setArguments(args);

        sheet.setOnSaveListener((status, progress, score) -> {
            savedStatus = status;
            savedScore = safeParseInt(score, 0);
            savedProgress = safeParseInt(progress, 0);

            if (totalChapters > 0 && savedProgress > totalChapters) {
                savedProgress = totalChapters;
                Toast.makeText(this, "Max chapters reached", Toast.LENGTH_SHORT).show();
            }

            if (status.equals("Reading") && totalChapters > 0 && savedProgress >= totalChapters) {
                savedProgress = totalChapters - 1;
            }

            updateTrackingUI();
            saveListProgressToApi();
        });

        sheet.show(getSupportFragmentManager(), "ListEditor");
    }

    private void saveListProgressToApi() {
        String token = getSharedPreferences("user", MODE_PRIVATE).getString("token", null);
        if (token == null || mediaId == -1) return;

        AniListApi.saveToList(DetailActivity.this, token, mediaId, savedProgress, mapStatusToApi(savedStatus), new AniListApi.ApiCallback() {
            @Override public void onSuccess(JSONObject response) {
                Toast.makeText(DetailActivity.this, "Saved ✅", Toast.LENGTH_SHORT).show();
            }
            @Override public void onError(String error) {
                Toast.makeText(DetailActivity.this, "Error ❌", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showSourceDialog() {
        String[] sources = {"MangaPill", "MangaKatana"}; // 🔥 Swapping experimental for stable Katana
        new AlertDialog.Builder(this)
                .setTitle("Select Source")
                .setItems(sources, (dialog, which) -> {
                    selectedSource = sources[which];
                    sourceSelector.setText("Source: " + selectedSource + " ▼");
                    cachedChapters = null;
                    currentStart = 1;
                    loadChaptersFromSource();
                }).show();
    }

    private void updatePaginationUI() {
        String currentRange = currentStart + " - " + (currentStart + rangeSize - 1);
        String nextRange = (currentStart + rangeSize) + " - " + (currentStart + 2 * rangeSize - 1);
        nextBtn.setText("Next ➡ (" + nextRange + ")");
        prevBtn.setText("⬅ Prev (" + currentRange + ")");
        prevBtn.setVisibility(currentStart <= 1 ? View.INVISIBLE : View.VISIBLE);
    }

    private void showLoader() {
        loaderOverlay.setVisibility(View.VISIBLE);
        chapterLoader.setVisibility(View.VISIBLE);
        chaptersGrid.removeAllViews();
    }

    private void hideLoader() {
        loaderOverlay.setVisibility(View.GONE);
        chapterLoader.setVisibility(View.GONE);
    }

    private int safeParseInt(String val, int defaultVal) {
        try {
            if (TextUtils.isEmpty(val)) return defaultVal;
            String cleaned = val.replaceAll("[^0-9.]", "");
            if (cleaned.isEmpty()) return defaultVal;
            return (int) Float.parseFloat(cleaned);
        } catch (Exception e) {
            return defaultVal;
        }
    }

    private String mapStatusToApi(String uiStatus) {
        switch (uiStatus) {
            case "Reading": return "CURRENT";
            case "Completed": return "COMPLETED";
            case "Dropped": return "DROPPED";
            case "Paused": return "PAUSED";
            case "Re-reading": return "REPEATING";
            default: return "PLANNING";
        }
    }

    private String mapStatusToUi(String apiStatus) {
        switch (apiStatus) {
            case "CURRENT": return "Reading";
            case "COMPLETED": return "Completed";
            case "DROPPED": return "Dropped";
            case "PAUSED": return "Paused";
            case "REPEATING": return "Re-reading";
            default: return "Planning";
        }
    }
}