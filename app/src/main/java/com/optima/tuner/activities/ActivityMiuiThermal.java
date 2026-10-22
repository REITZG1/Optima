package com.optima.tuner.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.optima.tuner.R;

/**
 * Placeholder: miui thermal.
 */
public class ActivityMiuiThermal extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_miui_thermal);

        TextView tv = findViewById(R.id.tv_info);
        tv.setText("Thermal Editor\n\nMIUI thermal config.");
    }
}
