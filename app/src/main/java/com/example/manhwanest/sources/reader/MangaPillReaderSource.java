package com.example.manhwanest.sources.reader;

import android.os.AsyncTask;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

public class MangaPillReaderSource {

    public interface PageCallback {
        void onSuccess(List<String> imageUrls);
        void onError(String error);
    }

    public void getPages(String chapterUrl, PageCallback callback) {

        new AsyncTask<Void, Void, List<String>>() {

            @Override
            protected List<String> doInBackground(Void... voids) {
                try {

                    // 🔥 IMPORTANT: Use headers (THIS IS THE REAL FIX)
                    Connection connection = Jsoup.connect(chapterUrl)
                            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                            .header("Accept-Language", "en-US,en;q=0.9")
                            .header("Referer", "https://mangapill.com/")
                            .timeout(10000);

                    Document doc = connection.get();

                    List<String> imageUrls = new ArrayList<>();

                    // 🔥 REAL SELECTOR (CONFIRMED)
                    Elements images = doc.select("img[data-src]");

                    for (Element img : images) {

                        String src = img.absUrl("data-src");

                        if (src != null && !src.isEmpty()) {
                            imageUrls.add(src);
                        }
                    }

                    return imageUrls;

                } catch (Exception e) {
                    e.printStackTrace();
                    return new ArrayList<>();
                }
            }

            @Override
            protected void onPostExecute(List<String> result) {
                if (result == null || result.isEmpty()) {
                    callback.onError("No images found");
                } else {
                    callback.onSuccess(result);
                }
            }
        }.execute();
    }
}