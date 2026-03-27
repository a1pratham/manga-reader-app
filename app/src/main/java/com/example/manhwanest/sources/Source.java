package com.example.manhwanest.sources;

import java.util.List;

public interface Source {

    interface ChapterCallback {
        void onSuccess(List<Chapter> chapters);
        void onError(String error);
    }

    // 🔥 NEW: Callback for fetching images
    interface ImageCallback {
        void onSuccess(List<String> images);
        void onError(String error);
    }

    void getChapters(String title, ChapterCallback callback);

    // 🔥 NEW: Method signature for fetching images
    void getImages(String chapterUrl, ImageCallback callback);
}