    package com.example.manhwanest;

    import android.content.Context;

    import com.android.volley.Request;
    import com.android.volley.RequestQueue;
    import com.android.volley.toolbox.JsonObjectRequest;
    import com.android.volley.toolbox.Volley;

    import org.json.JSONObject;

    public class AniListApi {

        private static final String URL = "https://graphql.anilist.co";

        public static void fetchTrending(Context context, ApiCallback callback) {
            fetch(context, "TRENDING_DESC", callback);
        }

        public static void fetchPopular(Context context, ApiCallback callback) {
            fetch(context, "POPULARITY_DESC", callback);
        }

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
                        URL,
                        jsonBody,
                        response -> callback.onSuccess(response),
                        error -> callback.onError(error.toString())
                );

                queue.add(request);

            } catch (Exception e) {
                callback.onError(e.toString());
            }
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
                        URL,
                        jsonBody,
                        response -> callback.onSuccess(response),
                        error -> callback.onError(error.toString())
                );

                queue.add(request);

            } catch (Exception e) {
                callback.onError(e.toString());
            }
        }

        public interface ApiCallback {
            void onSuccess(JSONObject response);
            void onError(String error);
        }
    }