package com.example.manhwanest.sources;

import android.os.AsyncTask;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MangaDexSource implements Source {

    private static final String BASE_API = "https://api.mangadex.org";

    private String fetchJson(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "ManhwaNest-App/1.0");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new Exception("HTTP Code: " + responseCode);
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        return sb.toString();
    }

    @Override
    public void getChapters(String title, ChapterCallback callback) {
        new AsyncTask<Void, Void, List<Chapter>>() {
            String errorMessage = "Failed to fetch from MangaDex.";

            @Override
            protected List<Chapter> doInBackground(Void... voids) {
                try {
                    // STEP 1: Search
                    String searchUrl = BASE_API + "/manga?title=" + URLEncoder.encode(title, "UTF-8") + "&limit=1";
                    String searchResponse = fetchJson(searchUrl);
                    JSONObject searchJson = new JSONObject(searchResponse);
                    JSONArray searchData = searchJson.getJSONArray("data");

                    if (searchData.length() == 0) {
                        errorMessage = "Manga not found on MangaDex.";
                        return null;
                    }

                    String mangaId = searchData.getJSONObject(0).getString("id");

                    // STEP 2: Fetch Chapters
                    String feedUrl = BASE_API + "/manga/" + mangaId + "/feed?translatedLanguage[]=en&order[chapter]=asc&limit=500";
                    String feedResponse = fetchJson(feedUrl);
                    JSONObject feedJson = new JSONObject(feedResponse);
                    JSONArray feedData = feedJson.getJSONArray("data");

                    Map<String, Chapter> uniqueChapters = new LinkedHashMap<>();

                    for (int i = 0; i < feedData.length(); i++) {
                        JSONObject chapterObj = feedData.getJSONObject(i);
                        String chapterId = chapterObj.getString("id");

                        JSONObject attributes = chapterObj.getJSONObject("attributes");
                        String chapterNum = attributes.optString("chapter", "");

                        // Skip External URLs to prevent crashes
                        if (attributes.has("externalUrl") && !attributes.isNull("externalUrl")) continue;

                        // WE REMOVED THE "ZERO PAGES" FILTER HERE! 🚀

                        if (TextUtils.isEmpty(chapterNum) || chapterNum.equals("null")) continue;

                        if (!uniqueChapters.containsKey(chapterNum)) {
                            uniqueChapters.put(chapterNum, new Chapter(chapterId, chapterNum));
                        }
                    }

                    if (uniqueChapters.isEmpty()) {
                        errorMessage = "No readable English chapters found. (Might be licensed/external only)";
                        return null;
                    }

                    return new ArrayList<>(uniqueChapters.values());

                } catch (Exception e) {
                    e.printStackTrace();
                    errorMessage = "Error: " + e.getMessage();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(List<Chapter> chapters) {
                if (chapters != null) {
                    callback.onSuccess(chapters);
                } else {
                    callback.onError(errorMessage);
                }
            }
        }.execute();
    }

    @Override
    public void getImages(String chapterUrl, ImageCallback callback) {
        // Empty for now!
    }
}