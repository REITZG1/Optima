package com.optima.tuner.activities;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.optima.tuner.R;
import com.optima.tuner.core.Background;
import com.optima.tuner.core.RefreshRateManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Refresh rate control: lists supported display modes and applies one.
 */
public class ActivityRefreshRate extends AppCompatActivity {
    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_refresh_rate);

        listView = findViewById(R.id.list_modes);
        Button btnApplyAuto = findViewById(R.id.btn_apply_auto);

        btnApplyAuto.setOnClickListener(v -> Background.supply(() -> RefreshRateManager.setRefreshRate(this, 0f),
                ok -> Toast.makeText(this, ok ? "Refresh rate reset to auto" : "Reset failed",
                        Toast.LENGTH_SHORT).show()));

        load();
    }

    private void load() {
        Background.supply(() -> {
            List<RefreshRateManager.Mode> modes = RefreshRateManager.listModes(this);
            float cur = RefreshRateManager.getCurrentRefreshRate(this);
            float max = RefreshRateManager.getMaxRefreshRate(this);
            return new Object[]{modes, cur, max};
        }, result -> {
            if (result == null) {
                Toast.makeText(this, "Could not read display modes", Toast.LENGTH_SHORT).show();
                return;
            }
            @SuppressWarnings("unchecked")
            List<RefreshRateManager.Mode> modes = (List<RefreshRateManager.Mode>) result[0];
            float cur = (Float) result[1];
            float max = (Float) result[2];

            List<String> rows = new ArrayList<>();
            for (RefreshRateManager.Mode m : modes) {
                rows.add("Mode " + m.id + " · " + m.width + "x" + m.height
                        + " · " + m.refreshRate + " Hz");
            }
            if (rows.isEmpty()) {
                rows.add("No display modes reported");
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_1, rows);
            listView.setAdapter(adapter);

            Toast.makeText(this, "Current: " + (cur > 0 ? cur : "auto") + " Hz  Max: "
                    + max + " Hz", Toast.LENGTH_LONG).show();

            listView.setOnItemClickListener((parent, view, position, id) -> {
                if (position >= modes.size()) {
                    return;
                }
                RefreshRateManager.Mode m = modes.get(position);
                Background.supply(() -> RefreshRateManager.setModeId(m.id), ok ->
                        Toast.makeText(this, ok ? "Mode " + m.id + " applied" : "Apply failed",
                                Toast.LENGTH_SHORT).show());
            });
        });
    }
}
