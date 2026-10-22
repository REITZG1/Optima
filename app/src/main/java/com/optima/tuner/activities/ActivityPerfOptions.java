package com.optima.tuner.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.optima.tuner.R;

/**
 * Placeholder: perf options.
 */
public class ActivityPerfOptions extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perf_options);

        TextView tv = findViewById(R.id.tv_info);
        tv.setText("Perf Options\n\nPerformance mode options.");
    }
}
