package com.optima.tuner.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.optima.tuner.R;

/**
 * Placeholder: fps sessions.
 */
public class ActivityFpsSessions extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fps_sessions);

        TextView tv = findViewById(R.id.tv_info);
        tv.setText("FPS Sessions\n\nFrame rate recording history.");
    }
}
