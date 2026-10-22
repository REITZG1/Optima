package com.optima.tuner.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.optima.tuner.R;

/**
 * Placeholder: app xposed config.
 */
public class ActivityAppXposedConfig extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_xposed_config);

        TextView tv = findViewById(R.id.tv_info);
        tv.setText("App Xposed Config\n\nPer-app Xposed settings.");
    }
}
