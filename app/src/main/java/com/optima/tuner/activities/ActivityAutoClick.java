package com.optima.tuner.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.optima.tuner.R;

/**
 * Placeholder: auto click.
 */
public class ActivityAutoClick extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auto_click);

        TextView tv = findViewById(R.id.tv_info);
        tv.setText("Auto Click\n\nAccessibility required. Records taps and replays them.");

        Button btnStart = findViewById(R.id.btn_start);
        btnStart.setOnClickListener(v -> tv.setText("Accessibility service not active."));
    }
}
