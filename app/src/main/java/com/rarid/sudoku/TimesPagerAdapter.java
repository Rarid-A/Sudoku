package com.rarid.sudoku;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TimesPagerAdapter extends FragmentStateAdapter {
    private final String[] difficulties;
    private static final String PREFS_NAME = "SudokuTimes";
    private static final int MAX_SCORES = 10;

    public TimesPagerAdapter(Context context, String[] difficulties) {
        super((androidx.fragment.app.FragmentActivity) context);
        this.difficulties = difficulties;
    }

    @NonNull
    @Override
    public androidx.fragment.app.Fragment createFragment(int position) {
        return TimesFragment.newInstance(difficulties[position]);
    }

    @Override
    public int getItemCount() {
        return difficulties.length;
    }
}