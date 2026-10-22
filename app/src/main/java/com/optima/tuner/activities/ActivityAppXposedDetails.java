package com.optima.tuner.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.optima.tuner.R;

/**
 * Placeholder: app xposed details.
 */
public class ActivityAppXposedDetails extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_xposed_details);

        TextView tv = findViewById(R.id.tv_info);
        tv.setText("App Xposed Details\n\nXposed module information.");
    }
}
