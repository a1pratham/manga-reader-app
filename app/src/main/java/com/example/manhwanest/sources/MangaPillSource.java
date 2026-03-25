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

    @Override
    public void getChapters(String title, ChapterCallback callback) {

        new AsyncTask<Void, Void, List<Chapter>>() {

            @Override
            protected List<Chapter> doInBackground(Void... voids) {
                try {
                    // 🔍 STEP 1: SEARCH
                    String searchUrl = BASE_URL + "/search?q=" + URLEncoder.encode(title, "UTF-8");
                    Document searchDoc = Jsoup.connect(searchUrl).get();

                    // ✅ FIXED SELECTOR
                    Element firstResult = searchDoc.selectFirst("a[href^=/manga/]");

                    if (firstResult == null) return new ArrayList<>();

                    String mangaUrl = BASE_URL + firstResult.attr("href");

                    // 📖 STEP 2: OPEN MANGA PAGE
                    Document mangaDoc = Jsoup.connect(mangaUrl).get();

                    // ✅ FIXED CHAPTER SELECTOR (IMPORTANT)
                    Elements chapterElements = mangaDoc.select("a[href^=/chapters/]");

                    List<Chapter> chapterList = new ArrayList<>();
                    Map<String, Chapter> uniqueMap = new LinkedHashMap<>();

                    for (Element el : chapterElements) {

                        String href = el.attr("href");

                        // extra safety
                        if (!href.contains("/chapters/")) continue;

                        String url = BASE_URL + href;
                        String text = el.text().trim();

                        // 🧹 CLEAN NUMBER
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

                    // ❗ VERY IMPORTANT: MangaPill already gives latest → oldest
                    // We reverse to make it 1 → latest (required for your pagination)
                    List<Chapter> finalList = new ArrayList<>();
                    for (int i = chapterList.size() - 1; i >= 0; i--) {
                        finalList.add(chapterList.get(i));
                    }

                    return finalList;

                } catch (Exception e) {
                    e.printStackTrace();
                    return new ArrayList<>();
                }
            }

            @Override
            protected void onPostExecute(List<Chapter> chapters) {
                callback.onSuccess(chapters);
            }
        }.execute();
    }
}