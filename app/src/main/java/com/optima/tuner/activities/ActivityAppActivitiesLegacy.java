package com.optima.tuner.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.optima.tuner.R;

/**
 * Placeholder: applications (legacy).
 */
public class ActivityAppActivitiesLegacy extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_activities_legacy);

        TextView tv = findViewById(R.id.tv_info);
        tv.setText("Applications\n\nList installed applications.");
    }
}
