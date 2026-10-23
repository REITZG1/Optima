package com.optima.tuner.activities;

import android.os.Bundle;
import android.provider.Settings;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.optima.tuner.R;
import com.optima.tuner.core.Background;

/**
 * Screen brightness: manual level or auto brightness.
 */
public class ActivityBrightness extends AppCompatActivity {
    private SeekBar seekBar;
    private TextView statusView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_brightness);

        seekBar = findViewById(R.id.seek_brightness);
        statusView = findViewById(R.id.tv_brightness_status);

        Button btnAuto = findViewById(R.id.btn_brightness_auto);

        seekBar.setMax(255);
        seekBar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (fromUser) {
                    applyBrightness(progress, false);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });

        btnAuto.setOnClickListener(v -> applyBrightness(-1, true));

        refresh();
    }

    private void applyBrightness(int level, boolean auto) {
        try {
            if (auto) {
                Settings.System.putInt(getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS_MODE, 1);
                WindowManager.LayoutParams params = getWindow().getAttributes();
                params.screenBrightness = -1f;
                getWindow().setAttributes(params);
            } else {
                Settings.System.putInt(getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS_MODE, 0);
                Settings.System.putInt(getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS, level);
                WindowManager.LayoutParams params = getWindow().getAttributes();
                params.screenBrightness = level / 255f;
                getWindow().setAttributes(params);
            }
            refresh();
        } catch (Exception e) {
            Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void refresh() {
        try {
            int mode = Settings.System.getInt(getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE, -1);
            int level = Settings.System.getInt(getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS, -1);
            if (mode == 1) {
                statusView.setText("Auto brightness: ON");
                seekBar.setEnabled(false);
            } else {
                statusView.setText("Manual brightness: " + level + " / 255");
                seekBar.setProgress(level);
                seekBar.setEnabled(true);
            }
        } catch (Exception e) {
            statusView.setText("Could not read brightness settings");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }
}
