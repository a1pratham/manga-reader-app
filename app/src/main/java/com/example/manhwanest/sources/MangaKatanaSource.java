package com.example.manhwanest.sources;

import android.os.AsyncTask;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MangaKatanaSource implements Source {

    private static final String BASE_URL = "https://mangakatana.com";

    @Override
    public void getChapters(String title, ChapterCallback callback) {
        new AsyncTask<Void, Void, List<Chapter>>() {
            String errorMsg = "Failed to load chapters.";

            @Override
            protected List<Chapter> doInBackground(Void... voids) {
                try {
                    // 1. Search for the Manga
                    String searchUrl = BASE_URL + "/?search=" + URLEncoder.encode(title, "UTF-8") + "&search_by=book_name";
                    Document searchDoc = Jsoup.connect(searchUrl).userAgent("Mozilla/5.0").get();

                    // MangaKatana search results are in h3.title
                    Element firstResult = searchDoc.selectFirst(".item .title a");
                    if (firstResult == null) {
                        errorMsg = "Manga not found on MangaKatana.";
                        return null;
                    }

                    String mangaUrl = firstResult.attr("abs:href");

                    // 2. Load Manga Page
                    Document mangaDoc = Jsoup.connect(mangaUrl).userAgent("Mozilla/5.0").get();
                    Elements chapterElements = mangaDoc.select(".chapters .chapter a");

                    Map<String, Chapter> uniqueChapters = new LinkedHashMap<>();
                    Pattern chapterPattern = Pattern.compile("(?i)chapter\\s*([0-9.]+)");

                    for (Element el : chapterElements) {
                        String url = el.attr("abs:href");
                        String text = el.text();

                        String number = "0";
                        Matcher matcher = chapterPattern.matcher(text);
                        if (matcher.find()) {
                            number = matcher.group(1);
                        } else {
                            number = text.replaceAll("[^0-9.]", "");
                        }

                        if (!number.isEmpty() && !uniqueChapters.containsKey(number)) {
                            uniqueChapters.put(number, new Chapter(url, number));
                        }
                    }

                    List<Chapter> chapterList = new ArrayList<>(uniqueChapters.values());

                    // Reverse list so it goes Ch 1 -> Ch Max
                    Collections.reverse(chapterList);
                    return chapterList;

                } catch (Exception e) {
                    e.printStackTrace();
                    errorMsg = "Error: " + e.getMessage();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(List<Chapter> chapters) {
                if (chapters != null && !chapters.isEmpty()) {
                    callback.onSuccess(chapters);
                } else {
                    callback.onError(errorMsg);
                }
            }
        }.execute();
    }

    // Inside MangaKatanaSource.java - Replace the getImages method
    @Override
    public void getImages(String chapterUrl, ImageCallback callback) {
        new AsyncTask<Void, Void, List<String>>() {
            String errorMsg = "No images found.";

            @Override
            protected List<String> doInBackground(Void... voids) {
                try {
                    // 1. Fetch the page with a very specific User-Agent to avoid being flagged as a bot
                    Document doc = Jsoup.connect(chapterUrl)
                            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                            .timeout(20000)
                            .get();

                    List<String> images = new ArrayList<>();
                    String html = doc.html();

                    // 🕵️‍♂️ THE NEW "GREEDY" REGEX
                    // This looks for any array [...] that contains strings ending in image extensions.
                    // It doesn't care about the variable name (thzq, ytaw, etc.)
                    Pattern pattern = Pattern.compile("\\[\\s*['\"]([^'\"]+\\.(?:jpg|png|webp|jpeg)[^'\"]*)['\"](?:\\s*,\\s*['\"]([^'\"]+)['\"])*\\s*\\]", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
                    Matcher matcher = pattern.matcher(html);

                    if (matcher.find()) {
                        String fullMatch = matcher.group(0);
                        // Strip the brackets [ ]
                        String cleaned = fullMatch.substring(1, fullMatch.length() - 1);
                        // Split by comma
                        String[] urls = cleaned.split(",");
                        for (String u : urls) {
                            // Strip quotes and whitespace
                            String cleanUrl = u.replace("'", "").replace("\"", "").trim();
                            if (!cleanUrl.isEmpty()) {
                                // Sometimes they use relative URLs; ensure they are absolute
                                if (cleanUrl.startsWith("//")) cleanUrl = "https:" + cleanUrl;
                                images.add(cleanUrl);
                            }
                        }
                    }

                    // 🖼️ FALLBACK: If Regex fails, try the HTML tags directly
                    if (images.isEmpty()) {
                        // Look for common image containers on MangaKatana
                        Elements imgElements = doc.select("div#imgs img, .wrap_img img, img[data-src]");
                        for (Element img : imgElements) {
                            String src = img.attr("data-src");
                            if (src.isEmpty()) src = img.attr("src");

                            // Filter out logos, icons, and ads
                            if (!src.isEmpty() && !src.contains("logo") && !src.contains("icon") && !src.contains("banner")) {
                                if (src.startsWith("//")) src = "https:" + src;
                                images.add(src);
                            }
                        }
                    }

                    return images;
                } catch (Exception e) {
                    e.printStackTrace();
                    errorMsg = "Error: " + e.getMessage();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(List<String> images) {
                if (images != null && !images.isEmpty()) {
                    callback.onSuccess(images);
                } else {
                    callback.onError("MangaKatana Error: " + errorMsg);
                }
            }
        }.execute();
    }
}