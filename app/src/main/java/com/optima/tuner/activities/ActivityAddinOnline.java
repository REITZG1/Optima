package com.optima.tuner.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.optima.tuner.R;

/**
 * Placeholder: online add-in.
 */
public class ActivityAddinOnline extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_addin_online);

        TextView tv = findViewById(R.id.tv_info);
        tv.setText("Add-in Online\n\nDownload add-ins from the cloud.");
    }
}
