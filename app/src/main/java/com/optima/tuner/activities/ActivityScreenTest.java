package com.optima.tuner.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.optima.tuner.R;

/**
 * Placeholder: screen test patterns.
 */
public class ActivityScreenTest extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_test);

        TextView tv = findViewById(R.id.tv_info);
        tv.setText("Screen test - displays colour bands to check the panel.");

        Button btnRed = findViewById(R.id.btn_red);
        Button btnGreen = findViewById(R.id.btn_green);
        Button btnBlue = findViewById(R.id.btn_blue);
        btnRed.setOnClickListener(v -> getWindow().getDecorView().setBackgroundColor(0xFFFF0000));
        btnGreen.setOnClickListener(v -> getWindow().getDecorView().setBackgroundColor(0xFF00FF00));
        btnBlue.setOnClickListener(v -> getWindow().getDecorView().setBackgroundColor(0xFF0000FF));
    }
}
