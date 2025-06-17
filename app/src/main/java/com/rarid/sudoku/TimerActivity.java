package com.rarid.sudoku;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TimerActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "SudokuTimes";
    private static final int MAX_SCORES = 10;
    private ViewPager2 viewPager;
    private String[] difficulties = { "easy", "medium", "hard" };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer);

        // Enable back button in action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Best Times");
        }

        viewPager = findViewById(R.id.view_pager);
        TimesPagerAdapter pagerAdapter = new TimesPagerAdapter(this, difficulties);
        viewPager.setAdapter(pagerAdapter);

        // Set initial page based on intent
        String initialDifficulty = getIntent() != null ? getIntent().getStringExtra("difficulty") : "easy";
        for (int i = 0; i < difficulties.length; i++) {
            if (difficulties[i].equals(initialDifficulty)) {
                viewPager.setCurrentItem(i, false);
                break;
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}