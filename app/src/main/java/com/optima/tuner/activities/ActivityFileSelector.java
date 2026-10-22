package com.optima.tuner.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.optima.tuner.R;

/**
 * Placeholder: file selector.
 */
public class ActivityFileSelector extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_selector);

        TextView tv = findViewById(R.id.tv_info);
        tv.setText("File Selector\n\nSelect a file or directory.");

        Button btnOk = findViewById(R.id.btn_ok);
        btnOk.setOnClickListener(v -> setResult(RESULT_OK));
    }
}
