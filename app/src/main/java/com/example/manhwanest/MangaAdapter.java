package com.example.manhwanest;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class MangaAdapter extends RecyclerView.Adapter<MangaAdapter.ViewHolder> {

    private List<Manga> mangaList = new ArrayList<>();
    private Context context;

    // 🔥 CREATE VIEW
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();

        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_manga, parent, false);

        return new ViewHolder(view);
    }

    // 🔥 BIND DATA
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Manga manga = mangaList.get(position);

        Glide.with(context)
                .load(manga.getImageUrl())
                .into(holder.image);

        holder.title.setText(manga.getTitle()); // 🔥 THIS LINE WAS MISSING

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, DetailActivity.class);
            intent.putExtra("title", manga.getTitle());
            intent.putExtra("image", manga.getImageUrl());
            intent.putExtra("desc", manga.getDescription());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return mangaList.size();
    }

    // 🔥 UPDATE DATA FROM API
    public void setData(List<Manga> list) {
        this.mangaList = list;
        notifyDataSetChanged();
    }

    // 🔥 VIEW HOLDER
    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView title;

        public ViewHolder(View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.mangaImage);
            title = itemView.findViewById(R.id.mangaTitle);
        }
    }
}