package com.optima.tuner.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.optima.tuner.R;

/**
 * Placeholder: app module upload.
 */
public class ActivityModuleUploadLegacy extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_module_upload_legacy);

        TextView tv = findViewById(R.id.tv_info);
        tv.setText("Module Upload\n\nUpload Magisk modules.");
    }
}
