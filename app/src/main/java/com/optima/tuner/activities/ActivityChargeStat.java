package com.optima.tuner.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.optima.tuner.R;
import com.optima.tuner.core.Background;
import com.optima.tuner.core.BatteryManager;

/**
 * Charge statistics: live charging state snapshot.
 */
public class ActivityChargeStat extends AppCompatActivity {
    private TextView tvInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_charge_stat);

        tvInfo = findViewById(R.id.tv_info);
        tvInfo.setText("Loading…");

        Button btnRefresh = findViewById(R.id.btn_refresh);
        btnRefresh.setOnClickListener(v -> refresh());

        refresh();
    }

    private void refresh() {
        Background.supply(BatteryManager::readStatus, s -> {
            if (s == null) {
                tvInfo.setText("Unavailable (root required?)");
                return;
            }
            tvInfo.setText("Level: " + s.level + "%\n"
                    + "Temp: " + String.format("%.1f °C", s.temperature) + "\n"
                    + "Voltage: " + s.voltage + " mV\n"
                    + "Current: " + s.current + " mA\n"
                    + "Power: " + s.power + " mW\n"
                    + "Charge counter: " + s.chargeCounter + " µAh\n"
                    + "Cycle count: " + s.cycleCount + "\n"
                    + "Technology: " + (s.technology.isEmpty() ? "-" : s.technology) + "\n"
                    + "Charging: " + s.charging);
        });
    }
}
