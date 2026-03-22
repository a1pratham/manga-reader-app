package com.example.manhwanest;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

public class DetailActivity extends AppCompatActivity {

    ImageView image;
    TextView title, desc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        image = findViewById(R.id.detailImage);
        title = findViewById(R.id.detailTitle);
        desc = findViewById(R.id.detailDesc);

        // Get data from intent
        int img = getIntent().getIntExtra("image", 0);
        String txt = getIntent().getStringExtra("title");

        image.setImageResource(img);
        title.setText(txt);

        desc.setText("This is a demo description for " + txt + ". You can replace this with real data later.");

        findViewById(R.id.readButton).setOnClickListener(v -> {
            startActivity(new Intent(DetailActivity.this, ReaderActivity.class));
        });
    }


}