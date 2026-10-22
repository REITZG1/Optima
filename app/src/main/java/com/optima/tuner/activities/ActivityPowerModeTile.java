package com.optima.tuner.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.optima.tuner.R;

/**
 * Placeholder: activity power mode tile.
 */
public class ActivityPowerModeTile extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_power_mode_tile);

        TextView tv = findViewById(R.id.tv_info);
        tv.setText("Power Mode Tile\n\nQuick settings tile configuration.");
    }
}
