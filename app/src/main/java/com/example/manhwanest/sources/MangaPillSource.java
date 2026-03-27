package com.example.manhwanest.sources;

import android.os.AsyncTask;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MangaPillSource implements Source {

    private static final String BASE_URL = "https://mangapill.com";

    // 📖 FETCH CHAPTERS (Restored and Improved)
    @Override
    public void getChapters(String title, ChapterCallback callback) {

        new AsyncTask<Void, Void, List<Chapter>>() {

            @Override
            protected List<Chapter> doInBackground(Void... voids) {
                try {
                    // 🔍 STEP 1: SEARCH
                    String searchUrl = BASE_URL + "/search?q=" + URLEncoder.encode(title, "UTF-8");

                    // Added userAgent to prevent the website from blocking the app
                    Document searchDoc = Jsoup.connect(searchUrl)
                            .userAgent("Mozilla/5.0")
                            .get();

                    Element firstResult = searchDoc.selectFirst("a[href^=/manga/]");

                    if (firstResult == null) return new ArrayList<>();

                    String mangaUrl = BASE_URL + firstResult.attr("href");

                    // 📖 STEP 2: OPEN MANGA PAGE
                    Document mangaDoc = Jsoup.connect(mangaUrl)
                            .userAgent("Mozilla/5.0")
                            .get();

                    Elements chapterElements = mangaDoc.select("a[href^=/chapters/]");

                    List<Chapter> chapterList = new ArrayList<>();
                    Map<String, Chapter> uniqueMap = new LinkedHashMap<>();

                    for (Element el : chapterElements) {
                        String href = el.attr("href");

                        if (!href.contains("/chapters/")) continue;

                        String url = BASE_URL + href;
                        String text = el.text().trim();

                        String number = text
                                .replace("Chapter", "")
                                .replace("Ch.", "")
                                .replace("Vol.", "")
                                .trim();

                        if (!uniqueMap.containsKey(url)) {
                            uniqueMap.put(url, new Chapter(url, number));
                        }
                    }

                    chapterList.addAll(uniqueMap.values());

                    // Reverse list so Chapter 1 is at the start
                    List<Chapter> finalList = new ArrayList<>();
                    for (int i = chapterList.size() - 1; i >= 0; i--) {
                        finalList.add(chapterList.get(i));
                    }

                    return finalList;

                } catch (Exception e) {
                    e.printStackTrace();
                    return null; // Return null so we can show an error
                }
            }

            @Override
            protected void onPostExecute(List<Chapter> chapters) {
                if (chapters != null) {
                    callback.onSuccess(chapters);
                } else {
                    callback.onError("Failed to load chapters. Check your internet connection.");
                }
            }
        }.execute();
    }

    // 🔥 NEW: FETCH IMAGES DIRECTLY
    @Override
    public void getImages(String chapterUrl, ImageCallback callback) {
        new AsyncTask<Void, Void, List<String>>() {

            @Override
            protected List<String> doInBackground(Void... voids) {
                try {
                    Document doc = Jsoup.connect(chapterUrl)
                            .userAgent("Mozilla/5.0")
                            .get();

                    Elements imgTags = doc.select("img");
                    List<String> images = new ArrayList<>();

                    for (Element img : imgTags) {
                        String url = img.attr("data-src");
                        if (url.isEmpty()) url = img.attr("src");

                        if (!url.isEmpty() && (url.contains("cdn") || url.contains("uploads"))) {
                            images.add(url);
                        }
                    }
                    return images;

                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(List<String> images) {
                if (images != null && !images.isEmpty()) {
                    callback.onSuccess(images);
                } else {
                    callback.onError("No images found or network error 😭");
                }
            }
        }.execute();
    }
}