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

    // 🔥 HELPER METHOD: Improved with Error Code Tracking
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
            @Override
            protected List<Chapter> doInBackground(Void... voids) {
                try {
                    String searchUrl = BASE_API + "/manga?title=" + URLEncoder.encode(title, "UTF-8") + "&limit=1";
                    String searchResponse = fetchJson(searchUrl);
                    JSONObject searchJson = new JSONObject(searchResponse);
                    JSONArray searchData = searchJson.getJSONArray("data");

                    if (searchData.length() == 0) return new ArrayList<>();

                    String mangaId = searchData.getJSONObject(0).getString("id");

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

                        // 🔥 THE FIX: Skip chapters hosted externally or with 0 pages
                        if (attributes.has("externalUrl") && !attributes.isNull("externalUrl")) continue;
                        if (attributes.has("pages") && attributes.optInt("pages", 0) == 0) continue;

                        if (TextUtils.isEmpty(chapterNum) || chapterNum.equals("null")) continue;

                        if (!uniqueChapters.containsKey(chapterNum)) {
                            uniqueChapters.put(chapterNum, new Chapter(chapterId, chapterNum));
                        }
                    }

                    return new ArrayList<>(uniqueChapters.values());

                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(List<Chapter> chapters) {
                if (chapters != null) {
                    callback.onSuccess(chapters);
                } else {
                    callback.onError("Failed to fetch chapters from MangaDex.");
                }
            }
        }.execute();
    }

    @Override
    public void getImages(String chapterId, ImageCallback callback) {
        new AsyncTask<Void, Void, List<String>>() {
            String errorMessage = "Failed to load MangaDex images.";

            @Override
            protected List<String> doInBackground(Void... voids) {
                try {
                    String serverUrl = BASE_API + "/at-home/server/" + chapterId;
                    String response = fetchJson(serverUrl);

                    JSONObject json = new JSONObject(response);
                    String baseUrl = json.getString("baseUrl");

                    JSONObject chapterData = json.getJSONObject("chapter");
                    String hash = chapterData.getString("hash");
                    JSONArray dataArray = chapterData.getJSONArray("data");

                    List<String> imageUrls = new ArrayList<>();

                    for (int i = 0; i < dataArray.length(); i++) {
                        String filename = dataArray.getString(i);
                        String fullUrl = baseUrl + "/data/" + hash + "/" + filename;
                        imageUrls.add(fullUrl);
                    }

                    return imageUrls;

                } catch (Exception e) {
                    e.printStackTrace();
                    errorMessage = "Error: " + e.getMessage(); // Capture exact error
                    return null;
                }
            }

            @Override
            protected void onPostExecute(List<String> images) {
                if (images != null && !images.isEmpty()) {
                    callback.onSuccess(images);
                } else {
                    callback.onError(errorMessage);
                }
            }
        }.execute();
    }
}