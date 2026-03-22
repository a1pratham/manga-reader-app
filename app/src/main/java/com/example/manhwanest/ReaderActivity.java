package com.example.manhwanest;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ReaderActivity extends AppCompatActivity {

    RecyclerView rvReader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reader);

        rvReader = findViewById(R.id.rvReader);

        rvReader.setLayoutManager(new LinearLayoutManager(this));
        rvReader.setAdapter(new ReaderAdapter());
    }
}