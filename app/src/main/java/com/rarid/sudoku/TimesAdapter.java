package com.rarid.sudoku;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class TimesAdapter extends RecyclerView.Adapter<TimesAdapter.TimeViewHolder> {
    private final List<Long> times;

    public TimesAdapter(List<Long> times) {
        this.times = times;
    }

    @NonNull
    @Override
    public TimeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_time, parent, false);
        return new TimeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TimeViewHolder holder, int position) {
        long timeInMillis = times.get(position);
        holder.bind(position + 1, timeInMillis);
    }

    @Override
    public int getItemCount() {
        return times.size();
    }

    static class TimeViewHolder extends RecyclerView.ViewHolder {
        private final TextView rankText;
        private final TextView timeText;

        TimeViewHolder(View itemView) {
            super(itemView);
            rankText = itemView.findViewById(R.id.rank_text);
            timeText = itemView.findViewById(R.id.time_text);
        }

        void bind(int rank, long timeInMillis) {
            rankText.setText(String.format("#%d", rank));

            long seconds = timeInMillis / 1000;
            long minutes = seconds / 60;
            seconds = seconds % 60;
            timeText.setText(String.format("%02d:%02d", minutes, seconds));
        }
    }
}