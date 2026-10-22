package com.optima.tuner.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.optima.tuner.R;

/**
 * Placeholder: module upload.
 */
public class ActivityModuleUpload extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_module_upload);

        TextView tv = findViewById(R.id.tv_info);
        tv.setText("Module Share\n\nUpload/download Magisk modules.");
    }
}
