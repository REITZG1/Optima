package com.optima.tuner.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.optima.tuner.R;

/**
 * Placeholder: online action page.
 */
public class ActionPageOnline extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.action_page_online);

        TextView tv = findViewById(R.id.tv_info);
        tv.setText("Action Page Online\n\nDownload action pages from the cloud.");
    }
}
