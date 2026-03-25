package com.example.manhwanest.sources;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.*;

public class MangaDexSource implements Source {

    private static RequestQueue queue;

    // ✅ SIMPLE CACHE
    private static final Map<String, List<Chapter>> cache = new HashMap<>();

    public MangaDexSource(Context context) {
        if (queue == null) {
            queue = Volley.newRequestQueue(context.getApplicationContext());
        }
    }

    @Override
    public void getChapters(String title, ChapterCallback callback) {

        // ✅ CACHE HIT
        if (cache.containsKey(title)) {
            callback.onSuccess(cache.get(title));
            return;
        }

        searchManga(title, callback);
    }

    // 🔍 SEARCH MANGA (IMPROVED)
    private void searchManga(String title, ChapterCallback callback) {
        try {
            String encodedTitle = URLEncoder.encode(title, "UTF-8");

            String url = "https://api.mangadex.org/manga"
                    + "?limit=5"
                    + "&title=" + encodedTitle;

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.GET,
                    url,
                    null,
                    response -> {
                        try {
                            JSONArray data = response.getJSONArray("data");

                            if (data.length() == 0) {
                                callback.onError("No manga found");
                                return;
                            }

                            // ✅ TAKE BEST MATCH (first is usually correct enough)
                            String mangaId = data.getJSONObject(0).getString("id");

                            fetchChapters(mangaId, title, callback);

                        } catch (Exception e) {
                            callback.onError(e.getMessage());
                        }
                    },
                    error -> callback.onError(error.toString())
            );

            queue.add(request);

        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    // 🔥 FETCH ALL CHAPTERS
    private void fetchChapters(String mangaId, String title, ChapterCallback callback) {
        List<Chapter> all = new ArrayList<>();
        fetchPage(mangaId, 0, all, title, callback);
    }

    // 🔥 PAGINATION LOOP
    private void fetchPage(String mangaId, int offset,
                           List<Chapter> all,
                           String title,
                           ChapterCallback callback) {

        String url = "https://api.mangadex.org/chapter"
                + "?manga=" + mangaId
                + "&translatedLanguage[]=en"
                + "&order[chapter]=asc"
                + "&limit=100"
                + "&offset=" + offset;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        JSONArray data = response.getJSONArray("data");

                        if (data.length() == 0) {
                            // ✅ DONE → CLEAN
                            List<Chapter> cleaned = clean(all);

                            cache.put(title, cleaned);
                            callback.onSuccess(cleaned);
                            return;
                        }

                        for (int i = 0; i < data.length(); i++) {

                            JSONObject obj = data.getJSONObject(i);
                            JSONObject attr = obj.getJSONObject("attributes");

                            String number = attr.optString("chapter", null);
                            String id = obj.getString("id");

                            if (number == null || number.equals("") || number.equals("null"))
                                continue;

                            if (!number.matches("\\d+(\\.\\d+)?"))
                                continue;

                            all.add(new Chapter(id, number));
                        }

                        // 🔁 NEXT PAGE
                        fetchPage(mangaId, offset + 100, all, title, callback);

                    } catch (Exception e) {
                        callback.onError(e.getMessage());
                    }
                },
                error -> callback.onError(error.toString())
        );

        queue.add(request);
    }

    // ✅ CLEAN + FIX DUPLICATES
    private List<Chapter> clean(List<Chapter> list) {

        // ⭐ KEEP FIRST occurrence (NOT last)
        Map<String, Chapter> map = new LinkedHashMap<>();

        for (Chapter ch : list) {
            if (!map.containsKey(ch.getNumber())) {
                map.put(ch.getNumber(), ch);
            }
        }

        List<Chapter> result = new ArrayList<>(map.values());

        // ✅ SORT PROPERLY
        result.sort((a, b) -> {
            try {
                return Float.compare(
                        Float.parseFloat(a.getNumber()),
                        Float.parseFloat(b.getNumber())
                );
            } catch (Exception e) {
                return 0;
            }
        });

        return result;
    }
}