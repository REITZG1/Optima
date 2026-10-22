package com.optima.tuner.activities;

import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.optima.tuner.R;
import com.optima.tuner.Optima;
import com.optima.tuner.core.Background;
import com.optima.tuner.core.BatteryManager;

/**
 * Charge control: charge limit %, current limit mA and charge on/off.
 */
public class ActivityChargeControl extends AppCompatActivity {
    private EditText etLimitPercent, etCurrentMa;
    private TextView tvStatus, tvTemp, tvVoltage;
    private Button btnSetLimit, btnSetCurrent, btnChargeOn, btnChargeOff;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_charge_control);

        etLimitPercent = findViewById(R.id.et_limit_percent);
        etCurrentMa = findViewById(R.id.et_current_ma);
        tvStatus = findViewById(R.id.tv_status);
        tvTemp = findViewById(R.id.tv_temp);
        tvVoltage = findViewById(R.id.tv_voltage);
        btnSetLimit = findViewById(R.id.btn_set_limit);
        btnSetCurrent = findViewById(R.id.btn_set_current);
        btnChargeOn = findViewById(R.id.btn_charge_on);
        btnChargeOff = findViewById(R.id.btn_charge_off);

        // Load saved values
        etLimitPercent.setText(String.valueOf(Optima.charge().getInt("charge_limit_percent", 0)));
        etCurrentMa.setText(String.valueOf(Optima.charge().getInt("current_limit_ma", 0)));

        btnSetLimit.setOnClickListener(v -> {
            try {
                int pct = Integer.parseInt(etLimitPercent.getText().toString());
                if (pct < 0 || pct > 100) {
                    throw new NumberFormatException();
                }
                Background.supply(() -> BatteryManager.setChargeLimit(pct), ok -> {
                    Optima.charge().edit().putInt("charge_limit_percent", pct).apply();
                    Toast.makeText(this, ok ? "Charge limit set to " + pct + "%"
                            : "Failed (no charge_limit node on this device)",
                            Toast.LENGTH_SHORT).show();
                });
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Enter 0-100", Toast.LENGTH_SHORT).show();
            }
        });

        btnSetCurrent.setOnClickListener(v -> {
            try {
                int ma = Integer.parseInt(etCurrentMa.getText().toString());
                Background.supply(() -> BatteryManager.setCurrentLimit(ma), ok -> {
                    Optima.charge().edit().putInt("current_limit_ma", ma).apply();
                    Toast.makeText(this, ok ? "Current limit set to " + ma + " mA"
                            : "Failed (no current limit node on this device)",
                            Toast.LENGTH_SHORT).show();
                });
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid mA", Toast.LENGTH_SHORT).show();
            }
        });

        btnChargeOn.setOnClickListener(v ->
                Background.supply(() -> BatteryManager.setChargingEnabled(true), ok ->
                        Toast.makeText(this, ok ? "Charging enabled" : "Not supported",
                                Toast.LENGTH_SHORT).show()));
        btnChargeOff.setOnClickListener(v ->
                Background.supply(() -> BatteryManager.setChargingEnabled(false), ok ->
                        Toast.makeText(this, ok ? "Charging disabled" : "Not supported",
                                Toast.LENGTH_SHORT).show()));

        etLimitPercent.setInputType(InputType.TYPE_CLASS_NUMBER);
        etCurrentMa.setInputType(InputType.TYPE_CLASS_NUMBER);
        refreshStatus();
    }

    private void refreshStatus() {
        Background.supply(BatteryManager::readStatus, s -> {
            if (s == null) {
                tvStatus.setText("Status: unavailable");
                return;
            }
            tvStatus.setText((s.charging ? "Charging" : "Discharging")
                    + " · " + s.level + "% · " + String.format("%.1f °C", s.temperature));
            tvTemp.setText("Temp: " + String.format("%.1f °C", s.temperature));
            tvVoltage.setText("Voltage: " + s.voltage + " mV");
        });
    }
}
