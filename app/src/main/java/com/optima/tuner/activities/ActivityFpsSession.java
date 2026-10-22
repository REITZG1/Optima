package com.optima.tuner.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.optima.tuner.R;

/**
 * Placeholder: fps session detail.
 */
public class ActivityFpsSession extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fps_session);

        TextView tv = findViewById(R.id.tv_info);
        tv.setText("FPS Session\n\nFrame rate graph for one session.");
    }
}
