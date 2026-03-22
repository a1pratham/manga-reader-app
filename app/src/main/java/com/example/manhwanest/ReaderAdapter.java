package com.example.manhwanest;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

public class ReaderAdapter extends RecyclerView.Adapter<ReaderAdapter.ViewHolder> {

    int[] pages = {
            R.drawable.manga1,
            R.drawable.manga2,
            R.drawable.manga3,
            R.drawable.manga1,
            R.drawable.manga2
    };

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_page, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.image.setImageResource(pages[position]);
    }

    @Override
    public int getItemCount() {
        return pages.length;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;

        public ViewHolder(View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.pageImage);
        }
    }
}