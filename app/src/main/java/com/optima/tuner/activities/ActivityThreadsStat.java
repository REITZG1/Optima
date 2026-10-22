package com.optima.tuner.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.optima.tuner.R;

/**
 * Placeholder: threads stat.
 */
public class ActivityThreadsStat extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_threads_stat);

        TextView tv = findViewById(R.id.tv_info);
        tv.setText("Thread Statistics\n\nPer-thread CPU usage.");
    }
}
