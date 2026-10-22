package com.optima.tuner.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.optima.tuner.R;

/**
 * Placeholder: charge control (legacy).
 */
public class ActivityChargeControlLegacy extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_charge_control_legacy);

        TextView tv = findViewById(R.id.tv_info);
        tv.setText("Charge Control\n\nSet charging limits.");
    }
}
