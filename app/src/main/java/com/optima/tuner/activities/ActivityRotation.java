package com.optima.tuner.activities;

import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.optima.tuner.R;
import com.optima.tuner.core.Background;

/**
 * Rotation control: auto-rotate on, or lock to portrait/landscape.
 */
public class ActivityRotation extends AppCompatActivity {
    private TextView statusView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rotation);

        statusView = findViewById(R.id.tv_rotation_status);

        Button btnAuto = findViewById(R.id.btn_rotation_auto);
        Button btnLocked = findViewById(R.id.btn_rotation_locked);
        Button btnPortrait = findViewById(R.id.btn_rotation_portrait);
        Button btnLandscape = findViewById(R.id.btn_rotation_landscape);

        btnAuto.setOnClickListener(v -> setRotation(1, -1));
        btnLocked.setOnClickListener(v -> setRotation(0, -1));
        btnPortrait.setOnClickListener(v -> setRotation(0, 1));
        btnLandscape.setOnClickListener(v -> setRotation(0, 3));

        refresh();
    }

    private void setRotation(int accelerometer, int userRotation) {
        try {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.ACCELEROMETER_ROTATION, accelerometer);
            if (userRotation >= 0) {
                Settings.System.putInt(getContentResolver(),
                        Settings.System.USER_ROTATION, userRotation);
            }
            refresh();
            Toast.makeText(this, "Rotation updated", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void refresh() {
        try {
            int auto = Settings.System.getInt(getContentResolver(),
                    Settings.System.ACCELEROMETER_ROTATION, -1);
            int user = Settings.System.getInt(getContentResolver(),
                    Settings.System.USER_ROTATION, -1);
            StringBuilder sb = new StringBuilder();
            if (auto == 1) {
                sb.append("Auto-rotate: ON");
            } else {
                sb.append("Auto-rotate: OFF\n");
                switch (user) {
                    case 0: sb.append("Rotation: portrait"); break;
                    case 1: sb.append("Rotation: portrait"); break;
                    case 2: sb.append("Rotation: landscape"); break;
                    case 3: sb.append("Rotation: landscape"); break;
                    default: sb.append("Rotation: locked"); break;
                }
            }
            statusView.setText(sb.toString());
        } catch (Exception e) {
            statusView.setText("Could not read rotation settings");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }
}
