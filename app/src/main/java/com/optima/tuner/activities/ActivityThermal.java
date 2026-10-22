package com.optima.tuner.activities;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.optima.tuner.R;
import com.optima.tuner.core.Background;
import com.optima.tuner.core.ThermalManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Thermal editor: lists thermal zones and current temperature.
 */
public class ActivityThermal extends AppCompatActivity {
    private ListView listView;
    private TextView tvCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thermal);

        listView = findViewById(R.id.list_zones);
        tvCount = findViewById(R.id.tv_count);
        Button btnRefresh = findViewById(R.id.btn_refresh);
        btnRefresh.setOnClickListener(v -> refresh());
        refresh();
    }

    private void refresh() {
        tvCount.setText("Loading…");
        Background.supply(ThermalManager::listZones, zones -> {
            if (zones == null) {
                tvCount.setText("Unavailable (root required?)");
                return;
            }
            List<String> rows = new ArrayList<>();
            for (ThermalManager.Zone z : zones) {
                rows.add("Zone " + z.index + " · " + z.type + " · "
                        + String.format("%.1f °C", z.temperature)
                        + (z.policy.isEmpty() ? "" : " · " + z.policy));
            }
            tvCount.setText(zones.size() + " zones");
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_1, rows);
            listView.setAdapter(adapter);
            if (rows.isEmpty()) {
                Toast.makeText(this, "No thermal zones found (root required)", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
