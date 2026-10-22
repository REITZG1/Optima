package com.optima.tuner.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.optima.tuner.R;
import com.optima.tuner.core.Background;
import com.optima.tuner.core.Shell;

/**
 * Magisk module management: lists installed Magisk modules.
 */
public class ActivityMagisk extends AppCompatActivity {
    private TextView tvInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_magisk);

        tvInfo = findViewById(R.id.tv_info);
        tvInfo.setText("Magisk Modules\n\nRoot required. List and toggle Magisk modules here.");

        Button btnList = findViewById(R.id.btn_list);
        btnList.setOnClickListener(v -> {
            tvInfo.setText("Loading…");
            Background.supply(() -> Shell.execRoot("ls /data/adb/modules 2>/dev/null"), out ->
                    tvInfo.setText(out == null || out.isEmpty()
                            ? "No modules found (root required)" : out));
        });
    }
}
