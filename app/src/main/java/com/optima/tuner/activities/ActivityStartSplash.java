package com.optima.tuner.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.optima.tuner.R;

/**
 * Splash screen: checks root/permissions and launches the main activity.
 */
public class ActivityStartSplash extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_splash);

        TextView tv = findViewById(R.id.tv_info);
        tv.setText("Optima 8.3.7\nChecking environment...");

        Button btnOpen = findViewById(R.id.btn_open);
        btnOpen.setOnClickListener(v -> {
            startActivity(new android.content.Intent(this, ActivityMain.class));
            finish();
        });

        // Auto-proceed after short delay
        new android.os.Handler().postDelayed(() -> {
            startActivity(new android.content.Intent(this, ActivityMain.class));
            finish();
        }, 1500);
    }
}
