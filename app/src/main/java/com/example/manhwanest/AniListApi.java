package com.example.manhwanest;

import android.content.Context;
import android.content.SharedPreferences;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class AniListApi {

    private static final String URL_GRAPHQL = "https://graphql.anilist.co";

    // ================= CORE NETWORK HELPER =================

    /**
     * A unified Volley request method. Handles query structuring, variables, and authentication tokens.
     */
    private static void executeGraphQL(Context context, String query, JSONObject variables, String token,
                                       Response.Listener<JSONObject> onSuccess, Response.ErrorListener onError) {
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("query", query);
            if (variables != null) jsonBody.put("variables", variables);

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, URL_GRAPHQL, jsonBody, onSuccess, onError) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    headers.put("Accept", "application/json");
                    if (token != null && !token.isEmpty()) {
                        headers.put("Authorization", "Bearer " + token);
                    }
                    return headers;
                }
            };

            Volley.newRequestQueue(context).add(request);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // ================= FETCH TRENDING / POPULAR =================

    public static void fetchTrending(Context context, ApiCallback callback) {
        fetchBySort(context, "TRENDING_DESC", callback);
    }

    public static void fetchPopular(Context context, ApiCallback callback) {
        fetchBySort(context, "POPULARITY_DESC", callback);
    }

    private static void fetchBySort(Context context, String sortType, ApiCallback callback) {
        String query = "{ Page(page: 1, perPage: 50) { media(type: MANGA, sort: " + sortType + ") { id title { english romaji userPreferred } coverImage { large } description } } }";

        executeGraphQL(context, query, null, null,
                callback::onSuccess,
                error -> callback.onError(error.toString())
        );
    }

    // ================= SEARCH =================

    public static void searchManga(Context context, String search, ApiCallback callback) {
        String query = "query ($search: String) { Page(page: 1, perPage: 20) { media(type: MANGA, search: $search, sort: POPULARITY_DESC) { id title { english romaji userPreferred } coverImage { large } description } } }";

        JSONObject variables = new JSONObject();
        try { variables.put("search", search); } catch (JSONException ignored) {}

        executeGraphQL(context, query, variables, null,
                callback::onSuccess,
                error -> callback.onError(error.toString())
        );
    }

    // ================= SAVE =================

    public static void saveToList(Context context, String token, int mediaId, int progress, String status, ApiCallback callback) {
        String query = "mutation ($mediaId: Int, $progress: Int, $status: MediaListStatus) { SaveMediaListEntry(mediaId: $mediaId, progress: $progress, status: $status) { id } }";

        JSONObject variables = new JSONObject();
        try {
            variables.put("mediaId", mediaId);
            variables.put("progress", progress);
            variables.put("status", status);
        } catch (JSONException ignored) {}

        executeGraphQL(context, query, variables, token,
                callback::onSuccess,
                error -> callback.onError(error.toString())
        );
    }

    // ================= FETCH TRACKING =================

    public static void fetchMediaListEntry(Context context, int mediaId, AniListCallback callback) {
        SharedPreferences prefs = context.getSharedPreferences("user", Context.MODE_PRIVATE);
        String token = prefs.getString("token", null);

        if (token == null) {
            callback.onFailure("No token found");
            return;
        }

        String query = "query ($mediaId: Int) { Media(id: $mediaId) { mediaListEntry { status progress score } } }";

        JSONObject variables = new JSONObject();
        try { variables.put("mediaId", mediaId); } catch (JSONException ignored) {}

        executeGraphQL(context, query, variables, token,
                response -> {
                    try {
                        JSONObject data = response.optJSONObject("data");
                        if (data != null && !data.isNull("Media")) {
                            JSONObject media = data.getJSONObject("Media");
                            if (!media.isNull("mediaListEntry")) {
                                JSONObject mediaList = media.getJSONObject("mediaListEntry");
                                callback.onSuccess(
                                        mediaList.optString("status", ""),
                                        mediaList.optInt("progress", 0),
                                        mediaList.optInt("score", 0)
                                );
                            } else {
                                callback.onEmpty(); // Not on user's list yet
                            }
                        } else {
                            callback.onEmpty();
                        }
                    } catch (Exception e) {
                        callback.onFailure(e.getMessage());
                    }
                },
                error -> callback.onFailure(error.toString())
        );
    }

    public static String getUserQuery() {
        return "{ Viewer { name avatar { large } } }";
    }

    // ================= FETCH CONTINUE READING =================

    public static void fetchContinueReading(Context context, ApiCallback callback) {
        SharedPreferences prefs = context.getSharedPreferences("user", Context.MODE_PRIVATE);
        String token = prefs.getString("token", null);
        int userId = prefs.getInt("user_id", -1);

        if (token == null) {
            callback.onError("Not logged in");
            return;
        }

        // If we don't know the User ID yet, fetch it first!
        if (userId == -1) {
            String viewerQuery = "{ Viewer { id } }";
            executeGraphQL(context, viewerQuery, null, token, response -> {
                try {
                    int fetchedUserId = response.getJSONObject("data").getJSONObject("Viewer").getInt("id");
                    // Save the user ID so we don't have to fetch it again
                    prefs.edit().putInt("user_id", fetchedUserId).apply();

                    // Now fetch their list
                    fetchUserMediaList(context, fetchedUserId, token, callback);
                } catch (Exception e) {
                    callback.onError("Failed to parse Viewer ID");
                }
            }, error -> callback.onError(error.toString()));
        } else {
            // We already know the User ID, go straight to fetching the list
            fetchUserMediaList(context, userId, token, callback);
        }
    }

    private static void fetchUserMediaList(Context context, int userId, String token, ApiCallback callback) {
        // Fetches manga currently being read, sorted by most recently updated!
        String query = "query ($userId: Int) { MediaListCollection(userId: $userId, type: MANGA, status: CURRENT, sort: UPDATED_TIME_DESC) { lists { entries { media { id title { english romaji userPreferred } coverImage { large } description } } } } }";

        JSONObject variables = new JSONObject();
        try { variables.put("userId", userId); } catch (Exception ignored) {}

        executeGraphQL(context, query, variables, token,
                callback::onSuccess,
                error -> callback.onError(error.toString())
        );
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
}