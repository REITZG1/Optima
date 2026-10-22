package com.optima.tuner.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.optima.tuner.R;

/**
 * Placeholder: activity components.
 */
public class ActivityAppComponents extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_components);

        TextView tv = findViewById(R.id.tv_info);
        tv.setText("App Components\n\nList activities/services/receivers.");
    }
}
