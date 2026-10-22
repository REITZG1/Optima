package com.optima.tuner.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.optima.tuner.R;
import com.optima.tuner.core.Background;
import com.optima.tuner.core.Shell;

/**
 * Activities / Services inspector.
 */
public class ActivityAppActivities extends AppCompatActivity {
    private TextView tvInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_activities);

        tvInfo = findViewById(R.id.tv_info);
        tvInfo.setText("Activities / Services\n\n");

        Button btnDump = findViewById(R.id.btn_dump);
        btnDump.setOnClickListener(v -> {
            tvInfo.setText("Loading…");
            Background.supply(() -> Shell.execRoot(
                    "dumpsys activity services 2>/dev/null | head -n 100"), out ->
                    tvInfo.setText(out == null || out.isEmpty()
                            ? "Unavailable (root required)" : out));
        });
    }
}
