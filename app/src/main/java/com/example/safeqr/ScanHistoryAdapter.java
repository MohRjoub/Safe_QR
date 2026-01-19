package com.example.safeqr;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;

import java.util.List;

public class ScanHistoryAdapter
        extends RecyclerView.Adapter<ScanHistoryAdapter.ViewHolder> {

    private final OnItemClickListener listener;
    private List<ScanHistoryItem> list;
    private final OnMenuActionListener menuListener;
    public ScanHistoryAdapter(List<ScanHistoryItem> list,  OnItemClickListener listener, OnMenuActionListener menuListener) {
        this.list = list;
        this.listener = listener;
        this.menuListener = menuListener;
    }

    public void removeAt(int position) {
        list.remove(position);
        notifyItemRemoved(position);
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ScanHistoryItem item = list.get(position);
        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
        holder.url.setText(item.url);
        holder.time.setText(item.time);
        holder.statusChip.setText(item.status);
        String status = item.status == null ? "" : item.status.trim().toLowerCase();

        if (status.equals("safe")) {
            holder.status.setImageResource(R.drawable.safe);

            holder.statusChip.setChipBackgroundColor(
                    ColorStateList.valueOf(Color.parseColor("#E8F5E9"))
            );
            holder.statusChip.setTextColor(Color.parseColor("#2E7D32"));

        } else if (status.equals("suspicious")) {
            holder.status.setImageResource(R.drawable.risk);

            holder.statusChip.setChipBackgroundColor(
                    ColorStateList.valueOf(Color.parseColor("#FDECEA"))
            );
            holder.statusChip.setTextColor(Color.parseColor("#C62828"));

        } else {
            holder.status.setImageResource(R.drawable.unknown);

            holder.statusChip.setChipBackgroundColor(
                    ColorStateList.valueOf(Color.parseColor("#FFF8E1"))
            );
            holder.statusChip.setTextColor(Color.parseColor("#B26A00"));
        }

        holder.menuBtn.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(v.getContext(), v);
            popup.getMenuInflater().inflate(R.menu.history_item_menu, popup.getMenu());

            popup.setOnMenuItemClickListener(menuItem -> {
                int id = menuItem.getItemId();
                int pos = holder.getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return false;

                if (id == R.id.action_delete) {
                    menuListener.onDelete(item, pos);
                    return true;
                } else if (id == R.id.action_recheck) {
                    menuListener.onRecheck(item, pos);
                    return true;
                }
                return false;
            });

            popup.show();
        });

    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public void setData(List<ScanHistoryItem> newList) {
        list.clear();
        list.addAll(newList);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView url, time;
        Chip statusChip;
        ImageView status;
        ImageButton menuBtn;
        ViewHolder(View itemView) {
            super(itemView);
            url = itemView.findViewById(R.id.tvUrl);
            time = itemView.findViewById(R.id.tvTime);
            statusChip = itemView.findViewById(R.id.statusChip);
            status = itemView.findViewById(R.id.ivStatus);
            menuBtn = itemView.findViewById(R.id.menuBtn);

        }
    }


    public interface OnItemClickListener {
        void onItemClick(ScanHistoryItem item);
    }

    public interface OnMenuActionListener {
        void onDelete(ScanHistoryItem item, int position);
        void onRecheck(ScanHistoryItem item, int position);
    }

}
