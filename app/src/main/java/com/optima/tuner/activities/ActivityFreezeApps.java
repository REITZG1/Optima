package com.optima.tuner.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.optima.tuner.R;

/**
 * Placeholder: freeze apps.
 */
public class ActivityFreezeApps extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_freeze_apps);

        TextView tv = findViewById(R.id.tv_info);
        tv.setText("App Freezer\n\nOpen Applications to freeze apps.");
    }
}
