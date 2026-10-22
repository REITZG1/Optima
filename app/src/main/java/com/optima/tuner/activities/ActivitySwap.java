package com.optima.tuner.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.optima.tuner.R;
import com.optima.tuner.core.Background;
import com.optima.tuner.core.Shell;

/**
 * Swap control: shows the current swap / memory configuration.
 */
public class ActivitySwap extends AppCompatActivity {
    private TextView tvInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_swap);

        tvInfo = findViewById(R.id.tv_info);
        tvInfo.setText("Swap Control\n\nRoot required. Configure zram/swap.");

        Button btnInfo = findViewById(R.id.btn_info);
        btnInfo.setOnClickListener(v -> {
            tvInfo.setText("Loading…");
            Background.supply(() -> Shell.execRoot(
                    "cat /proc/swaps 2>/dev/null; free 2>/dev/null"), out ->
                    tvInfo.setText(out == null || out.isEmpty()
                            ? "Unavailable (root required)" : out));
        });
    }
}
