package com.example.manhwanest;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SeekBar;


import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.manhwanest.sources.Chapter;

import java.util.ArrayList;
import java.util.List;

public class ReaderActivity extends AppCompatActivity {
    SeekBar pageSlider;

    RecyclerView rvReader;
    ReaderAdapter adapter;
    WebView webView;

    ImageView backBtn;
    TextView prevBtn, nextBtn, chapterTitle, pageIndicator;
    View topBar, bottomBar;

    ArrayList<Chapter> chapterList;
    int currentIndex;

    LinearLayoutManager layoutManager;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reader);

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

        // Bind views
        rvReader = findViewById(R.id.readerRecyclerView);
        webView = findViewById(R.id.webView);
        pageIndicator = findViewById(R.id.pageIndicator);
        pageSlider = findViewById(R.id.pageSlider);

        backBtn = findViewById(R.id.backBtn);
        prevBtn = findViewById(R.id.prevChapterBtn);
        nextBtn = findViewById(R.id.nextChapterBtn);
        chapterTitle = findViewById(R.id.chapterTitle);
        topBar = findViewById(R.id.topBar);
        bottomBar = findViewById(R.id.bottomBar);

        // Layout
        layoutManager = new LinearLayoutManager(this);
        rvReader.setLayoutManager(layoutManager);

        adapter = new ReaderAdapter();
        rvReader.setAdapter(adapter);

        // 🔥 TAP LISTENER (FINAL FIX)
        adapter.setOnImageTapListener(() -> {
            if (topBar.getVisibility() == View.VISIBLE) {
                hideUI();
                hideSystemUI();
            } else {
                showUI();
            }
        });

        // DATA
        String chapterUrl = getIntent().getStringExtra("chapter_id");
        String chapterNumber = getIntent().getStringExtra("chapter_number");

        chapterList = (ArrayList<Chapter>)
                getIntent().getSerializableExtra("chapter_list");

        currentIndex = getIntent().getIntExtra("chapter_index", 0);

        if (chapterUrl == null) {
            Toast.makeText(this, "No chapter URL", Toast.LENGTH_SHORT).show();
            return;
        }

        if (chapterNumber != null) {
            chapterTitle.setText("Chapter " + chapterNumber);
        }

        backBtn.setOnClickListener(v -> finish());

        // 🔥 PAGE TRACKING
        rvReader.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {

                int currentPage = layoutManager.findFirstVisibleItemPosition() + 1;
                int totalPages = adapter.getItemCount();

                if (totalPages > 0) {
                    pageIndicator.setText(currentPage + " / " + totalPages);
                    pageSlider.setProgress(currentPage - 1);
                }
            }
        });

        // WEBVIEW
        webView.getSettings().setJavaScriptEnabled(true);

        webView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void processHTML(String html) {
                runOnUiThread(() -> extractImages(html));
            }
        }, "HTMLOUT");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                webView.loadUrl(
                        "javascript:window.HTMLOUT.processHTML(document.documentElement.outerHTML);");
            }
        });

        pageSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                if (fromUser) {
                    rvReader.scrollToPosition(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        webView.loadUrl(chapterUrl);
    }

    private void hideUI() {

        topBar.animate()
                .alpha(0f)
                .translationY(-topBar.getHeight())
                .setDuration(200)
                .withEndAction(() -> topBar.setVisibility(View.GONE));

        bottomBar.animate()
                .alpha(0f)
                .translationY(bottomBar.getHeight())
                .setDuration(200)
                .withEndAction(() -> bottomBar.setVisibility(View.GONE));

        pageSlider.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction(() -> pageSlider.setVisibility(View.GONE));
    }

    private void showUI() {

        topBar.setVisibility(View.VISIBLE);
        bottomBar.setVisibility(View.VISIBLE);
        pageSlider.setVisibility(View.VISIBLE);

        topBar.setAlpha(0f);
        bottomBar.setAlpha(0f);
        pageSlider.setAlpha(0f);

        topBar.setTranslationY(-topBar.getHeight());
        bottomBar.setTranslationY(bottomBar.getHeight());

        topBar.animate()
                .alpha(1f)
                .translationY(0)
                .setDuration(200);

        bottomBar.animate()
                .alpha(1f)
                .translationY(0)
                .setDuration(200);

        pageSlider.animate()
                .alpha(1f)
                .setDuration(200);
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

    private void loadChapter(Chapter chapter) {
        String url = chapter.getId();
        String number = chapter.getNumber();

        chapterTitle.setText("Chapter " + number);
        webView.loadUrl(url);
    }


    private void extractImages(String html) {

        List<String> images = new ArrayList<>();

        try {
            org.jsoup.nodes.Document doc = org.jsoup.Jsoup.parse(html);
            org.jsoup.select.Elements imgTags = doc.select("img");

            for (org.jsoup.nodes.Element img : imgTags) {

                String url = img.attr("data-src");

                if (url == null || url.isEmpty()) {
                    url = img.attr("src");
                }

                if (url != null &&
                        !url.isEmpty() &&
                        (url.contains("cdn") || url.contains("uploads"))) {

                    images.add(url);
                }
            }

            if (images.isEmpty()) {
                Toast.makeText(this, "No images found 😭", Toast.LENGTH_SHORT).show();
            } else {
                adapter.setImages(images);

                // set slider range
                pageSlider.setMax(images.size() - 1);
                pageSlider.setProgress(0);

                // initial page indicator
                pageIndicator.setText("1 / " + images.size());
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error parsing images", Toast.LENGTH_SHORT).show();
        }
    }
}