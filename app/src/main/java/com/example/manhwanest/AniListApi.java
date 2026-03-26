package com.example.manhwanest;

import android.content.Context;
import android.content.SharedPreferences;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class AniListApi {

    private static final String URL_GRAPHQL = "https://graphql.anilist.co";

    // ================= FETCH TRENDING / POPULAR =================

    public static void fetchTrending(Context context, ApiCallback callback) {
        fetch(context, "TRENDING_DESC", callback);
    }

    public static void fetchPopular(Context context, ApiCallback callback) {
        fetch(context, "POPULARITY_DESC", callback);
    }

    private static void fetch(Context context, String sortType, ApiCallback callback) {

        RequestQueue queue = Volley.newRequestQueue(context);

        String query = "{ Page(page: 1, perPage: 50) { media(type: MANGA, sort: "
                + sortType +
                ") { id title { english romaji userPreferred } coverImage { large } description } } }";

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("query", query);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    URL_GRAPHQL,
                    jsonBody,
                    callback::onSuccess,
                    error -> callback.onError(error.toString())
            );

            queue.add(request);

        } catch (Exception e) {
            callback.onError(e.toString());
        }
    }

    // ================= SEARCH =================

    public static void searchManga(Context context, String search, ApiCallback callback) {

        RequestQueue queue = Volley.newRequestQueue(context);

        String query = "query ($search: String) { Page(page: 1, perPage: 20) { media(type: MANGA, search: $search, sort: POPULARITY_DESC) { id title { english romaji userPreferred } coverImage { large } description } } }";

        try {
            JSONObject variables = new JSONObject();
            variables.put("search", search);

            JSONObject jsonBody = new JSONObject();
            jsonBody.put("query", query);
            jsonBody.put("variables", variables);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    URL_GRAPHQL,
                    jsonBody,
                    callback::onSuccess,
                    error -> callback.onError(error.toString())
            );

            queue.add(request);

        } catch (Exception e) {
            callback.onError(e.toString());
        }
    }

    // ================= SAVE =================

    public static void saveToList(String token, int mediaId, int progress, String status, ApiCallback callback) {

        new Thread(() -> {
            try {
                URL url = new URL(URL_GRAPHQL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject json = new JSONObject();
                json.put("query", saveToListQuery(mediaId, progress, status));

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes());
                os.close();

                Scanner scanner = new Scanner(conn.getInputStream());
                String response = scanner.useDelimiter("\\A").next();
                scanner.close();

                callback.onSuccess(new JSONObject(response));

            } catch (Exception e) {
                callback.onError(e.toString());
            }
        }).start();
    }

    // ================= FETCH TRACKING (IMPORTANT) =================

    public static void fetchMediaListEntry(Context context, int mediaId, AniListCallback callback) {
        new Thread(() -> {
            try {
                SharedPreferences prefs = context.getSharedPreferences("user", Context.MODE_PRIVATE);
                String token = prefs.getString("token", null);

                if (token == null) {
                    callback.onFailure("No token");
                    return;
                }

                URL url = new URL(URL_GRAPHQL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setDoOutput(true);

                String query = "query ($mediaId: Int) { " +
                        "MediaList(mediaId: $mediaId) { " +
                        "status progress score " +
                        "} " +
                        "}";

                JSONObject variables = new JSONObject();
                variables.put("mediaId", mediaId);

                JSONObject json = new JSONObject();
                json.put("query", query);
                json.put("variables", variables);

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes());
                os.flush();
                os.close();

                InputStream is = (conn.getResponseCode() == 200)
                        ? conn.getInputStream()
                        : conn.getErrorStream();

                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder result = new StringBuilder();

                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                reader.close();

                JSONObject response = new JSONObject(result.toString());

                JSONObject data = response.optJSONObject("data");

                if (data != null && !data.isNull("MediaList")) {
                    JSONObject mediaList = data.getJSONObject("MediaList");

                    String status = mediaList.optString("status", "");
                    int progress = mediaList.optInt("progress", 0);
                    int score = mediaList.optInt("score", 0);

                    callback.onSuccess(status, progress, score);
                } else {
                    callback.onEmpty();
                }

            } catch (Exception e) {
                callback.onFailure(e.getMessage());
            }
        }).start();
    }

    // ================= INTERFACES =================

    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onError(String error);
    }

    public interface AniListCallback {
        void onSuccess(String status, int progress, int score);
        void onEmpty();
        void onFailure(String error);
    }

    // ================= QUERIES =================

    public static String getUserQuery() {
        return "{ Viewer { name avatar { large } } }";
    }

    public static String saveToListQuery(int mediaId, int progress, String status) {
        return "mutation { " +
                "SaveMediaListEntry(mediaId: " + mediaId +
                ", status: " + status +
                ", progress: " + progress + ") { id } }";
    }
}