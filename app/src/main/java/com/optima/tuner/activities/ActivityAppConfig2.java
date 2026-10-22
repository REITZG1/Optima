package com.optima.tuner.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.optima.tuner.R;

/**
 * Placeholder: app config 2.
 */
public class ActivityAppConfig2 extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_config2);

        TextView tv = findViewById(R.id.tv_info);
        tv.setText("App Config\n\nPer-app settings.");
    }
}
