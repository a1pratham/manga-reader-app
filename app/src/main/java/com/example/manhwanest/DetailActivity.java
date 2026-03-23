package com.example.manhwanest;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

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

        String imageUrl = getIntent().getStringExtra("image");
        String titleText = getIntent().getStringExtra("title");
        String description = getIntent().getStringExtra("desc");

        Glide.with(this)
                .load(imageUrl)
                .into(image);

        title.setText(titleText);
        desc.setText(description);

        findViewById(R.id.readButton).setOnClickListener(v -> {
            startActivity(new Intent(DetailActivity.this, ReaderActivity.class));
        });
    }
}