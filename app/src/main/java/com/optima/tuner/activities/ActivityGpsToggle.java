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
 * GPS location: enable or disable the GPS location provider.
 */
public class ActivityGpsToggle extends AppCompatActivity {
    private TextView statusView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gps);

        statusView = findViewById(R.id.tv_gps_status);

        Button btnOn = findViewById(R.id.btn_gps_on);
        Button btnOff = findViewById(R.id.btn_gps_off);

        btnOn.setOnClickListener(v -> setGps(true));
        btnOff.setOnClickListener(v -> setGps(false));

        refresh();
    }

    private void setGps(boolean enable) {
        try {
            String providers = Settings.Secure.getString(getContentResolver(),
                    Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            if (providers == null) {
                providers = "";
            }
            String updated;
            if (enable) {
                if (!providers.contains("gps")) {
                    updated = providers.isEmpty() ? "gps" : providers + ",gps";
                } else {
                    updated = providers;
                }
            } else {
                updated = providers.replace("gps", "");
                updated = updated.replace(",,", ",").replace("^,", "").replace(",$", "");
            }
            Settings.Secure.putString(getContentResolver(),
                    Settings.Secure.LOCATION_PROVIDERS_ALLOWED, updated);
            refresh();
            Toast.makeText(this, "GPS " + (enable ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void refresh() {
        try {
            String providers = Settings.Secure.getString(getContentResolver(),
                    Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            boolean gps = providers != null && providers.contains("gps");
            statusView.setText("GPS: " + (gps ? "enabled" : "disabled"));
        } catch (Exception e) {
            statusView.setText("Could not read GPS settings");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }
}
