package com.example.manhwanest.sources;

import android.os.AsyncTask;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class MangaBuddySource implements Source {

    private static final String BASE_URL = "https://mangabuddy.com";

    @Override
    public void getChapters(String title, ChapterCallback callback) {

        new AsyncTask<Void, Void, List<Chapter>>() {

            String error = null;

            @Override
            protected List<Chapter> doInBackground(Void... voids) {

                List<Chapter> chapterList = new ArrayList<>();
                HashSet<String> seen = new HashSet<>();

                try {
                    // 🔍 SEARCH MANGA
                    String searchUrl = BASE_URL + "/search?q=" + URLEncoder.encode(title, "UTF-8");

                    Document searchDoc = Jsoup.connect(searchUrl)
                            .userAgent("Mozilla/5.0")
                            .get();

                    Element firstResult = searchDoc.selectFirst(".book-item a");

                    if (firstResult == null) {
                        error = "No results found";
                        return null;
                    }

                    String mangaUrl = BASE_URL + firstResult.attr("href");

                    // 🔥 FETCH ALL PAGES
                    for (int page = 1; page <= 20; page++) {

                        String pageUrl = mangaUrl + "?page=" + page;

                        Document doc = Jsoup.connect(pageUrl)
                                .userAgent("Mozilla/5.0")
                                .get();

                        Elements chapters = doc.select(".chapter-list a");

                        // ❗ STOP when no more chapters
                        if (chapters.isEmpty()) break;

                        for (Element ch : chapters) {

                            String url = BASE_URL + ch.attr("href");
                            String name = ch.text().trim();

                            String number = "";

                            try {
                                java.util.regex.Matcher matcher = java.util.regex.Pattern
                                        .compile("(\\d+\\.?\\d*)")
                                        .matcher(name);

                                if (matcher.find()) {
                                    number = matcher.group(1);
                                }

                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            // ❌ skip invalid
                            if (number.isEmpty()) continue;

                            // ❌ remove duplicates
                            if (seen.contains(number)) continue;

                            seen.add(number);

                            chapterList.add(new Chapter(url, number));
                        }
                    }

                    // ✅ SORT PROPERLY (ascending)
                    Collections.sort(chapterList, (c1, c2) -> {
                        try {
                            double n1 = Double.parseDouble(c1.getNumber());
                            double n2 = Double.parseDouble(c2.getNumber());
                            return Double.compare(n1, n2);
                        } catch (Exception e) {
                            return 0;
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    error = e.getMessage();
                }

                return chapterList;
            }

            @Override
            protected void onPostExecute(List<Chapter> chapters) {
                if (chapters != null && !chapters.isEmpty()) {
                    callback.onSuccess(chapters);
                } else {
                    callback.onError(error != null ? error : "Failed to load chapters");
                }
            }
        }.execute();
    }
}