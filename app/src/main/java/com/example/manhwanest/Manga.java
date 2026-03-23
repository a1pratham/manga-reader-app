package com.example.manhwanest;

public class Manga {

    String title;
    String imageUrl;
    String description;

    public Manga(String title, String imageUrl, String description) {
        this.title = title;
        this.imageUrl = imageUrl;
        this.description = description;
    }

    public String getTitle() { return title; }
    public String getImageUrl() { return imageUrl; }
    public String getDescription() { return description; }
}