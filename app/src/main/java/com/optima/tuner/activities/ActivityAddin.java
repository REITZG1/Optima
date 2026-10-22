package com.optima.tuner.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.optima.tuner.R;

/**
 * Placeholder: add-in.
 */
public class ActivityAddin extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_addin);

        TextView tv = findViewById(R.id.tv_info);
        tv.setText("Add-in\n\nDevice model modifier, MAC address, DPI, etc.");
    }
}
