package com.example.manhwanest;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;

public class ReaderAdapter extends RecyclerView.Adapter<ReaderAdapter.ViewHolder> {

    private List<String> images = new ArrayList<>();

    public void setImages(List<String> newImages) {
        images = newImages;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reader_page, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        String imageUrl = images.get(position);

        GlideUrl glideUrl = new GlideUrl(
                imageUrl,
                new LazyHeaders.Builder()
                        .addHeader("Referer", "https://mangapill.com/")
                        .addHeader("User-Agent", "Mozilla/5.0")
                        .build()
        );

        Glide.with(holder.itemView.getContext())
                .load(glideUrl)
                .into(holder.imageView);
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView imageView;

        public ViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.pageImage);
        }
    }
}