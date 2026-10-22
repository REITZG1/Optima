package com.optima.tuner.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.optima.tuner.R;

/**
 * Placeholder: gesture recorder.
 */
public class ActivityGesture extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gesture);

        TextView tv = findViewById(R.id.tv_info);
        tv.setText("Gesture\n\nRecord and replay touch gestures.");

        Button btnRec = findViewById(R.id.btn_record);
        btnRec.setOnClickListener(v -> tv.setText("Not implemented."));
    }
}
