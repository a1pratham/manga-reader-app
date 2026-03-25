package com.example.manhwanest;

public class Manga {

    int id; // ✅ IMPORTANT

    String title;
    String imageUrl;
    String description;

    public Manga(int id, String title, String imageUrl, String description) {
        this.id = id;
        this.title = title;
        this.imageUrl = imageUrl;
        this.description = description;
    }

    // ✅ GETTERS

    public int getId() { return id; }

    public String getTitle() { return title; }

    public String getImageUrl() { return imageUrl; }

    public String getDescription() { return description; }
}