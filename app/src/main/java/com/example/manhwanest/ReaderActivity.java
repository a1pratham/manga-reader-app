package com.example.manhwanest;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.manhwanest.sources.Chapter;
import com.example.manhwanest.sources.MangaKatanaSource; // 🔥 Import updated
import com.example.manhwanest.sources.MangaPillSource;
import com.example.manhwanest.sources.Source;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ReaderActivity extends AppCompatActivity {

    private RecyclerView rvReader;
    private SeekBar pageSlider;
    private ImageView backBtn;
    private TextView prevBtn, nextBtn, chapterTitle, pageIndicator;
    private View topBar, bottomBar;

    private ReaderAdapter adapter;
    private LinearLayoutManager layoutManager;

    private ArrayList<Chapter> chapterList;
    private int currentIndex;
    private int mediaId = -1;
    private int totalChapters = 0;
    private String currentChapterId;
    private String sourceName;

    private int lastSavedPage = -1;
    private boolean chapterMarked = false;
    private boolean isUiVisible = true;

    private Source mangaSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reader);

        bindViews();
        loadIntentData();
        setupRecyclerView();
        setupListeners();

        // 🔥 Updated routing logic
        if ("MangaKatana".equals(sourceName)) {
            mangaSource = new MangaKatanaSource();
        } else {
            mangaSource = new MangaPillSource();
        }

        if (currentChapterId != null) {
            fetchImagesDirectly(currentChapterId);
        }

        // 🔥 MODERN BACK BUTTON HANDLER
        // This fixes the warning you saw in Android Studio
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                onChapterFinished();
                finish();
            }
        });
    }

    private void bindViews() {
        rvReader = findViewById(R.id.readerRecyclerView);
        pageIndicator = findViewById(R.id.pageIndicator);
        pageSlider = findViewById(R.id.pageSlider);
        backBtn = findViewById(R.id.backBtn);
        prevBtn = findViewById(R.id.prevChapterBtn);
        nextBtn = findViewById(R.id.nextChapterBtn);
        chapterTitle = findViewById(R.id.chapterTitle);
        topBar = findViewById(R.id.topBar);
        bottomBar = findViewById(R.id.bottomBar);
    }

    private void loadIntentData() {
        currentChapterId = getIntent().getStringExtra("chapter_id");
        String chapterNumber = getIntent().getStringExtra("chapter_number");
        sourceName = getIntent().getStringExtra("source_name");

        chapterList = (ArrayList<Chapter>) getIntent().getSerializableExtra("chapter_list");
        currentIndex = getIntent().getIntExtra("chapter_index", 0);
        mediaId = getIntent().getIntExtra("media_id", -1);
        totalChapters = getIntent().getIntExtra("total_chapters", 0);

        if (currentChapterId == null) {
            Toast.makeText(this, "No chapter URL", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (chapterNumber != null) {
            chapterTitle.setText("Chapter " + chapterNumber);
        }
    }

    private void setupRecyclerView() {
        layoutManager = new LinearLayoutManager(this);
        rvReader.setLayoutManager(layoutManager);
        adapter = new ReaderAdapter();

        // 🔥 FIX: Pass the source name to the adapter so it uses the correct headers
        if (sourceName != null) {
            adapter.setSource(sourceName);
        }

        rvReader.setAdapter(adapter);
    }

    private void setupListeners() {
        backBtn.setOnClickListener(v -> {
            onChapterFinished();
            finish();
        });
        prevBtn.setOnClickListener(v -> changeChapter(false));
        nextBtn.setOnClickListener(v -> changeChapter(true));

        adapter.setOnImageTapListener(this::toggleUI);

        rvReader.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                handleScrollTracking();
            }
        });

        pageSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) rvReader.scrollToPosition(progress);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void changeChapter(boolean isNext) {
        if (chapterList == null) return;

        if (isNext) {
            if (currentIndex < chapterList.size() - 1) {
                if (!chapterMarked) onChapterFinished();
                currentIndex++;
                loadNewChapterState();
            } else {
                Toast.makeText(this, "Last chapter", Toast.LENGTH_SHORT).show();
            }
        } else {
            if (currentIndex > 0) {
                currentIndex--;
                loadNewChapterState();
            } else {
                Toast.makeText(this, "First chapter", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadNewChapterState() {
        chapterMarked = false;
        Chapter chapter = chapterList.get(currentIndex);
        currentChapterId = chapter.getId();

        chapterTitle.setText("Chapter " + chapter.getNumber());
        adapter.setImages(new ArrayList<>());

        pageIndicator.setText("0 / 0");
        pageSlider.setProgress(0);
        rvReader.scrollToPosition(0);

        fetchImagesDirectly(currentChapterId);
    }

    private void fetchImagesDirectly(String url) {
        mangaSource.getImages(url, new Source.ImageCallback() {
            @Override
            public void onSuccess(List<String> images) {
                runOnUiThread(() -> {
                    adapter.setImages(images);
                    int savedPage = getSharedPreferences("reader_progress", MODE_PRIVATE).getInt(currentChapterId, 0);

                    pageSlider.setMax(images.size() - 1);
                    pageSlider.setProgress(savedPage);
                    pageIndicator.setText((savedPage + 1) + " / " + images.size());

                    rvReader.post(() -> {
                        layoutManager.scrollToPositionWithOffset(savedPage, 0);
                    });
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(ReaderActivity.this, error, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void handleScrollTracking() {
        int currentPage = layoutManager.findFirstVisibleItemPosition() + 1;
        int totalPages = adapter.getItemCount();

        if (totalPages > 0) {
            pageIndicator.setText(currentPage + " / " + totalPages);
            pageSlider.setProgress(currentPage - 1);

            int pageToSave = currentPage - 1;
            if (pageToSave != lastSavedPage) {
                lastSavedPage = pageToSave;
                saveReadingProgress(pageToSave);
            }
        }

        int lastVisible = layoutManager.findLastVisibleItemPosition();
        if (lastVisible == totalPages - 1 && totalPages > 0) {
            View lastView = layoutManager.findViewByPosition(lastVisible);
            if (lastView != null && lastView.getBottom() <= rvReader.getHeight()) {
                onChapterFinished();
            }
        }
    }

    private void onChapterFinished() {
        if (chapterMarked || isFinishing() || mediaId == -1) return;

        String token = getSharedPreferences("user", MODE_PRIVATE).getString("token", null);
        if (token == null || chapterList == null || currentIndex >= chapterList.size()) return;

        chapterMarked = true;
        Chapter currentChapter = chapterList.get(currentIndex);

        int safeProgress;
        try {
            // Clean chapter number for AniList
            String cleanNum = currentChapter.getNumber().replaceAll("[^0-9.]", "");
            safeProgress = (int) Float.parseFloat(cleanNum);
        } catch (Exception e) {
            safeProgress = currentIndex + 1;
        }

        if (totalChapters > 0 && safeProgress >= totalChapters) {
            safeProgress = totalChapters - 1;
        }

        AniListApi.saveToList(ReaderActivity.this, token, mediaId, safeProgress, "CURRENT", new AniListApi.ApiCallback() {
            @Override public void onSuccess(JSONObject response) {}
            @Override public void onError(String error) {}
        });
    }

    private void saveReadingProgress(int page) {
        if (currentChapterId == null) return;
        getSharedPreferences("reader_progress", MODE_PRIVATE).edit().putInt(currentChapterId, page).apply();
    }

    private void toggleUI() {
        if (isUiVisible) {
            animateView(topBar, false, -topBar.getHeight());
            animateView(bottomBar, false, bottomBar.getHeight());
            animateView(pageSlider, false, 0);
            hideSystemUI();
        } else {
            animateView(topBar, true, 0);
            animateView(bottomBar, true, 0);
            animateView(pageSlider, true, 0);
        }
        isUiVisible = !isUiVisible;
    }

    private void animateView(View view, boolean show, float translationY) {
        if (show) {
            view.setVisibility(View.VISIBLE);
            view.animate().alpha(1f).translationY(translationY).setDuration(200).withEndAction(null);
        } else {
            view.animate().alpha(0f).translationY(translationY).setDuration(200)
                    .withEndAction(() -> view.setVisibility(View.GONE));
        }
    }

    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
    }

    @Override
    protected void onPause() {
        super.onPause();
        onChapterFinished();
    }
}