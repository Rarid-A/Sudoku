package com.rarid.sudoku;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TimesFragment extends Fragment {
    private static final String ARG_DIFFICULTY = "difficulty";
    private static final String PREFS_NAME = "SudokuTimes";
    private static final int MAX_SCORES = 10;

    public static TimesFragment newInstance(String difficulty) {
        TimesFragment fragment = new TimesFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DIFFICULTY, difficulty);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_times, container, false);

        String difficulty = getArguments() != null ? getArguments().getString(ARG_DIFFICULTY) : "easy";

        TextView difficultyTitle = view.findViewById(R.id.difficulty_title);
        TextView noTimesMessage = view.findViewById(R.id.no_times_message);
        RecyclerView recyclerView = view.findViewById(R.id.times_recycler_view);

        if (difficultyTitle != null) {
            difficultyTitle.setText(difficulty.substring(0, 1).toUpperCase() + difficulty.substring(1));
        }

        List<Long> times = ProgressManager.loadBestTimes(requireContext(), difficulty);
        if (times.isEmpty()) {
            if (noTimesMessage != null) {
                noTimesMessage.setVisibility(View.VISIBLE);
            }
            if (recyclerView != null) {
                recyclerView.setVisibility(View.GONE);
            }
        } else {
            if (noTimesMessage != null) {
                noTimesMessage.setVisibility(View.GONE);
            }
            if (recyclerView != null) {
                recyclerView.setVisibility(View.VISIBLE);
                recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                TimesAdapter adapter = new TimesAdapter(times);
                recyclerView.setAdapter(adapter);
            }
        }

        return view;
    }

    private List<Long> loadTimes(String difficulty) {
        if (difficulty == null) {
            return new ArrayList<>();
        }

        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME,
                android.content.Context.MODE_PRIVATE);
        List<Long> times = new ArrayList<>();

        for (int i = 0; i < MAX_SCORES; i++) {
            long time = prefs.getLong(difficulty + "_time_" + i, 0);
            if (time > 0) {
                times.add(time);
            }
        }

        Collections.sort(times);
        return times;
    }
}