package com.example.manhwanest;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

public class DetailActivity extends AppCompatActivity {

    ImageView bannerImage, coverImage;
    TextView titleText, descriptionText, statusText, ratingText, genresText, chaptersText;

    Button listEditorButton;
    Button infoTab, readTab, continueButton;

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

        // 🔥 Default state
        listEditorButton.setText(currentStatus);
        infoLayout.setVisibility(View.VISIBLE);
        readLayout.setVisibility(View.GONE);

        // 🔥 Get intent data
        String imageUrl = getIntent().getStringExtra("image");
        String title = getIntent().getStringExtra("title");
        String description = getIntent().getStringExtra("desc");

        Glide.with(this).load(imageUrl).into(coverImage);
        Glide.with(this).load(imageUrl).into(bannerImage);

        titleText.setText(title);

        if (description != null) {
            descriptionText.setText(Html.fromHtml(description, Html.FROM_HTML_MODE_LEGACY));
        } else {
            descriptionText.setText("No description available.");
        }

        statusText.setText("Status: Unknown");
        ratingText.setText("⭐ --");
        genresText.setText("Genres: --");
        chaptersText.setText("Chapters: --");

        // 🔥 TAB SWITCHING

        infoTab.setOnClickListener(v -> {
            infoLayout.setVisibility(View.VISIBLE);
            readLayout.setVisibility(View.GONE);

            infoTab.setBackgroundResource(R.drawable.pink_pill);
            readTab.setBackgroundResource(R.drawable.pill_bg);

            // ✅ FIX TEXT COLORS
            infoTab.setTextColor(getResources().getColor(android.R.color.white));
            readTab.setTextColor(getResources().getColor(android.R.color.darker_gray));
        });

        readTab.setOnClickListener(v -> {
            infoLayout.setVisibility(View.GONE);
            readLayout.setVisibility(View.VISIBLE);

            readTab.setBackgroundResource(R.drawable.pink_pill);
            infoTab.setBackgroundResource(R.drawable.pill_bg);

            // ✅ FIX TEXT COLORS
            readTab.setTextColor(getResources().getColor(android.R.color.white));
            infoTab.setTextColor(getResources().getColor(android.R.color.darker_gray));
        });

        // 🔥 CONTINUE BUTTON

        continueButton.setOnClickListener(v -> {
            startActivity(new Intent(DetailActivity.this, ReaderActivity.class));
        });

        // 🔥 CHAPTER GRID (DEMO)

        int lastReadChapter = 5; // 🔥 demo (later from storage)

        for (int i = 1; i <= 20; i++) {

            TextView chapterBtn = new TextView(this);
            chapterBtn.setText(String.valueOf(i));
            chapterBtn.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            chapterBtn.setTextColor(getResources().getColor(android.R.color.white));
            chapterBtn.setPadding(0, 24, 0, 24);

            // 🔥 Highlight last read
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

            int finalI = i;
            chapterBtn.setOnClickListener(v -> {
                // later pass chapter number
                startActivity(new Intent(DetailActivity.this, ReaderActivity.class));
            });

            chaptersGrid.addView(chapterBtn);
        }

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
}