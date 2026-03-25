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

    // 🔥 TYPES
    public static final int TYPE_HOME = 0;
    public static final int TYPE_GRID = 1;

    private int type;

    // 🔥 CONSTRUCTOR
    public MangaAdapter(int type) {
        this.type = type;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();

        int layout;

        if (type == TYPE_HOME) {
            layout = R.layout.item_manga_home;
        } else {
            layout = R.layout.item_manga_grid;
        }

        View view = LayoutInflater.from(context).inflate(layout, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Manga manga = mangaList.get(position);

        Glide.with(context)
                .load(manga.getImageUrl())
                .into(holder.image);

        holder.title.setText(manga.getTitle());

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, DetailActivity.class);

            // 🔥 ONLY PASS ID
            intent.putExtra("id", manga.getId());

            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return mangaList.size();
    }

    public void setData(List<Manga> list) {
        this.mangaList = list;
        notifyDataSetChanged();
    }

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