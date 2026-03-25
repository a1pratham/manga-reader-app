package com.example.manhwanest.sources;

import java.util.List;

public interface Source {

    interface ChapterCallback {
        void onSuccess(List<Chapter> chapters);
        void onError(String error);
    }

    void getChapters(String title, ChapterCallback callback);
}