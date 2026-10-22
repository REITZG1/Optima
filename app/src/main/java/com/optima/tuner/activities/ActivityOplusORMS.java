package com.optima.tuner.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.optima.tuner.R;

/**
 * Placeholder: oplus ORMS.
 */
public class ActivityOplusORMS extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_oplus_orms);

        TextView tv = findViewById(R.id.tv_info);
        tv.setText("ORMS Editor\n\nOplus thermal config.");
    }
}
