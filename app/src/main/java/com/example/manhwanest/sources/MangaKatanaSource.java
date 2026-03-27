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

                    // 🕵️‍♂️ BULLETPROOF REGEX
                    // Pattern.DOTALL allows the regex to match arrays that span multiple lines
                    Pattern pattern = Pattern.compile("var\\s+[a-zA-Z0-9_]+\\s*=\\s*\\[(.*?)\\];", Pattern.DOTALL);
                    Matcher matcher = pattern.matcher(html);

                    List<String> largestImageArray = new ArrayList<>();

                    while (matcher.find()) {
                        String arrayContent = matcher.group(1);

                        // Verify if this array actually contains image links
                        if (arrayContent.matches(".*\\.(jpg|png|webp|jpeg).*")) {
                            List<String> tempImages = new ArrayList<>();

                            // Better extraction: Grab everything inside single or double quotes
                            Pattern urlPattern = Pattern.compile("['\"]([^'\"]+)['\"]");
                            Matcher urlMatcher = urlPattern.matcher(arrayContent);

                            while (urlMatcher.find()) {
                                String cleanUrl = urlMatcher.group(1).trim();

                                // Ensure it's an actual image URL and not a random string
                                if (cleanUrl.matches(".*\\.(jpg|png|webp|jpeg).*")) {
                                    if (cleanUrl.startsWith("//")) {
                                        cleanUrl = "https:" + cleanUrl;
                                    }
                                    tempImages.add(cleanUrl);
                                }
                            }

                            // The actual chapter pages will be the largest array.
                            // This prevents the app from getting stuck on a 1-image thumbnail array.
                            if (tempImages.size() > largestImageArray.size()) {
                                largestImageArray = tempImages;
                            }
                        }
                    }

                    // Add the winning array to our final images list
                    if (!largestImageArray.isEmpty()) {
                        images.addAll(largestImageArray);
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