package com.example.manhwanest;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.manhwanest.sources.Chapter;

import java.util.ArrayList;
import java.util.List;

public class ReaderActivity extends AppCompatActivity {

    RecyclerView rvReader;
    ReaderAdapter adapter;
    WebView webView;

    // 🔥 UI
    ImageView backBtn;
    TextView prevBtn, nextBtn, chapterTitle;
    View topBar, bottomBar;

    // 🔥 CHAPTER DATA
    ArrayList<Chapter> chapterList;
    int currentIndex;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reader);

        // 🔥 Bind views
        rvReader = findViewById(R.id.readerRecyclerView);
        webView = findViewById(R.id.webView);

        backBtn = findViewById(R.id.backBtn);
        prevBtn = findViewById(R.id.prevChapterBtn);
        nextBtn = findViewById(R.id.nextChapterBtn);
        chapterTitle = findViewById(R.id.chapterTitle);
        topBar = findViewById(R.id.topBar);
        bottomBar = findViewById(R.id.bottomBar);

        rvReader.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ReaderAdapter();
        rvReader.setAdapter(adapter);

        // 🔥 GET DATA
        String chapterUrl = getIntent().getStringExtra("chapter_id");
        String chapterNumber = getIntent().getStringExtra("chapter_number");

        chapterList = (ArrayList<Chapter>)
                getIntent().getSerializableExtra("chapter_list");

        currentIndex = getIntent().getIntExtra("chapter_index", 0);

        if (chapterUrl == null) {
            Toast.makeText(this, "No chapter URL", Toast.LENGTH_SHORT).show();
            return;
        }

        // 🔥 TITLE
        if (chapterNumber != null) {
            chapterTitle.setText("Chapter " + chapterNumber);
        }

        // 🔥 BACK
        backBtn.setOnClickListener(v -> finish());

        // 🔥 SHOW / HIDE UI
        rvReader.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (topBar.getVisibility() == View.VISIBLE) {
                    topBar.setVisibility(View.GONE);
                    bottomBar.setVisibility(View.GONE);
                } else {
                    topBar.setVisibility(View.VISIBLE);
                    bottomBar.setVisibility(View.VISIBLE);
                }
            }
            return false;
        });

        // 🔥 PREV BUTTON
        prevBtn.setOnClickListener(v -> {
            if (chapterList != null && currentIndex > 0) {
                currentIndex--;
                loadChapter(chapterList.get(currentIndex));
            } else {
                Toast.makeText(this, "No previous chapter", Toast.LENGTH_SHORT).show();
            }
        });

        // 🔥 NEXT BUTTON
        nextBtn.setOnClickListener(v -> {
            if (chapterList != null && currentIndex < chapterList.size() - 1) {
                currentIndex++;
                loadChapter(chapterList.get(currentIndex));
            } else {
                Toast.makeText(this, "No next chapter", Toast.LENGTH_SHORT).show();
            }
        });

        // 🔥 WEBVIEW SETUP
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

        // 🔥 LOAD FIRST CHAPTER
        webView.loadUrl(chapterUrl);
    }

    // 🔥 LOAD NEW CHAPTER
    private void loadChapter(Chapter chapter) {

        String url = chapter.getId();
        String number = chapter.getNumber();

        chapterTitle.setText("Chapter " + number);

        webView.loadUrl(url);
    }

    // 🔥 IMAGE EXTRACTION
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
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error parsing images", Toast.LENGTH_SHORT).show();
        }
    }
}