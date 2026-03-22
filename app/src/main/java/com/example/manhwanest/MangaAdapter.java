package com.example.manhwanest;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.content.Intent;

import androidx.recyclerview.widget.RecyclerView;

public class MangaAdapter extends RecyclerView.Adapter<MangaAdapter.ViewHolder> {

    int[] images = {
            R.drawable.manga1,
            R.drawable.manga2,
            R.drawable.manga3
    };

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_manga, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.image.setImageResource(images[position % images.length]);
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), DetailActivity.class);
            intent.putExtra("image", images[position % images.length]);
            intent.putExtra("title", "Solo Leveling");
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return 10;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;

        public ViewHolder(View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.mangaImage);
        }
    }
}