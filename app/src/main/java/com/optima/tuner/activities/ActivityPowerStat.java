package com.optima.tuner.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.optima.tuner.R;
import com.optima.tuner.core.Background;
import com.optima.tuner.core.BatteryManager;

/**
 * Power statistics: current power draw and battery health.
 */
public class ActivityPowerStat extends AppCompatActivity {
    private TextView tvPower, tvVoltage, tvCurrent, tvHealth, tempView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_power_stat);

        tvPower = findViewById(R.id.tv_power);
        tvVoltage = findViewById(R.id.tv_voltage);
        tvCurrent = findViewById(R.id.tv_current);
        tvHealth = findViewById(R.id.tv_health);
        tempView = findViewById(R.id.tv_temp);

        Button btnRefresh = findViewById(R.id.btn_refresh);
        btnRefresh.setOnClickListener(v -> refresh());
        refresh();
    }

    private void refresh() {
        Background.supply(BatteryManager::readStatus, s -> {
            if (s == null) {
                tvPower.setText("Power: unavailable");
                return;
            }
            tvPower.setText(s.power + " mW");
            tvVoltage.setText(s.voltage + " mV");
            tvCurrent.setText(s.current + " mA");
            tvHealth.setText(healthName(s.health));
            tempView.setText(String.format("%.1f °C", s.temperature));
        });
    }

    private String healthName(int health) {
        switch (health) {
            case 2: return "Good";
            case 3: return "Overheating";
            case 4: return "Dead";
            case 5: return "Over voltage";
            case 6: return "Failure";
            case 7: return "Cold";
            default: return "Unknown";
        }
    }
}
