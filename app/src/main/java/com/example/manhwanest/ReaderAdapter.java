package com.example.manhwanest;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.github.chrisbanes.photoview.PhotoView;

import java.util.ArrayList;
import java.util.List;

public class ReaderAdapter extends RecyclerView.Adapter<ReaderAdapter.ViewHolder> {

    private List<String> images = new ArrayList<>();
    private final int PRELOAD_COUNT = 5;

    // 🔥 SOURCE TRACKING (IMPORTANT FIX)
    private String currentSource = "mangapill";

    public void setSource(String source) {
        this.currentSource = source;
    }

    public interface OnImageTapListener {
        void onImageTap();
    }

    private OnImageTapListener listener;

    public void setOnImageTapListener(OnImageTapListener listener) {
        this.listener = listener;
    }

    public void setImages(List<String> newImages) {
        images = newImages;
        notifyDataSetChanged();
    }

    // 🔥 FIXED HEADER LOGIC (NO URL DETECTION)
    // 🔥 FIXED HEADER LOGIC
    private GlideUrl getGlideUrl(String url) {
        LazyHeaders.Builder headers = new LazyHeaders.Builder()
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

        // 🔥 FIX: Use equalsIgnoreCase to prevent "MangaKatana" != "mangakatana" bugs
        if ("mangakatana".equalsIgnoreCase(currentSource)) {
            headers.addHeader("Referer", "https://mangakatana.com/");
        } else {
            headers.addHeader("Referer", "https://mangapill.com/");
        }

        return new GlideUrl(url, headers.build());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reader_page, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String imageUrl = images.get(position);
        GlideUrl glideUrl = getGlideUrl(imageUrl);

        // 🔥 PRELOAD NEXT IMAGES
        int total = images.size();
        for (int i = 1; i <= PRELOAD_COUNT; i++) {
            int nextPos = position + i;
            if (nextPos < total) {
                GlideUrl preloadUrl = getGlideUrl(images.get(nextPos));
                Glide.with(holder.itemView.getContext())
                        .load(preloadUrl)
                        .preload();
            }
        }

        // RESET VIEW (IMPORTANT FOR RECYCLING)
        holder.imageView.setImageDrawable(null);
        holder.loader.setVisibility(View.VISIBLE);
        holder.imageView.setVisibility(View.INVISIBLE);

        Glide.with(holder.itemView.getContext())
                .load(glideUrl)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                Target<Drawable> target, boolean isFirstResource) {
                        holder.loader.setVisibility(View.GONE);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model,
                                                   Target<Drawable> target, DataSource dataSource,
                                                   boolean isFirstResource) {
                        holder.loader.setVisibility(View.GONE);
                        holder.imageView.setVisibility(View.VISIBLE);
                        return false;
                    }
                })
                .into(holder.imageView);

        holder.imageView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onImageTap();
            }
        });
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        PhotoView imageView;
        ProgressBar loader;

        public ViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.pageImage);
            loader = itemView.findViewById(R.id.pageLoader);
        }
    }
}