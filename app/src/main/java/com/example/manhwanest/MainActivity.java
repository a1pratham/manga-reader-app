package com.example.manhwanest;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class MainActivity extends AppCompatActivity {

    RecyclerView rvContinue, rvRecent, rvPopular;
    Button tabManga, tabManhwa;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // RecyclerViews
        rvContinue = findViewById(R.id.rvContinue);
        rvRecent = findViewById(R.id.rvRecent);
        rvPopular = findViewById(R.id.rvPopular);

        // Tabs
        tabManga = findViewById(R.id.tabManga);
        tabManhwa = findViewById(R.id.tabManhwa);

        // Setup recyclers
        setupRecycler(rvContinue);
        setupRecycler(rvRecent);
        setupRecycler(rvPopular);

        // Tab Clicks
        tabManga.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tabManga.setBackgroundResource(R.drawable.tab_selected);
                tabManhwa.setBackgroundResource(R.drawable.tab_unselected);

                Toast.makeText(MainActivity.this, "Manga Selected", Toast.LENGTH_SHORT).show();
            }
        });

        tabManhwa.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tabManhwa.setBackgroundResource(R.drawable.tab_selected);
                tabManga.setBackgroundResource(R.drawable.tab_unselected);

                Toast.makeText(MainActivity.this, "Manhwa Selected", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupRecycler(RecyclerView recyclerView) {
        recyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );

        recyclerView.setAdapter(new MangaAdapter());
    }
}