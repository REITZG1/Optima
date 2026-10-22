package com.optima.tuner.activities;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.optima.tuner.R;
import com.optima.tuner.core.AppManager;
import com.optima.tuner.core.Background;

import java.util.ArrayList;
import java.util.List;

/**
 * Application list: freeze / unfreeze per package.
 */
public class ActivityApplications extends AppCompatActivity {
    private ListView listView;
    private TextView tvCount;
    private boolean showSystem = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_applications);

        listView = findViewById(R.id.list_apps);
        tvCount = findViewById(R.id.tv_count);
        Button btnRefresh = findViewById(R.id.btn_refresh);
        Button btnToggleSystem = findViewById(R.id.btn_toggle_system);

        btnRefresh.setOnClickListener(v -> refresh());
        btnToggleSystem.setOnClickListener(v -> {
            showSystem = !showSystem;
            btnToggleSystem.setText(showSystem ? "Hide System Apps" : "Show System Apps");
            refresh();
        });

        refresh();
    }

    private void refresh() {
        tvCount.setText("Loading…");
        final boolean sys = showSystem;
        Background.supply(() -> {
            List<AppManager.AppEntry> apps = AppManager.listApps(this, sys);
            // Resolve the frozen state in one batch instead of one call per app.
            String disabled = com.optima.tuner.core.Shell.execRoot("pm list packages -d 2>/dev/null");
            List<String> frozen = new ArrayList<>();
            if (disabled != null) {
                for (String line : disabled.split("\n")) {
                    String t = line.trim();
                    if (t.startsWith("package:")) {
                        frozen.add(t.substring(8).trim());
                    }
                }
            }
            return new Object[]{apps, frozen};
        }, result -> {
            if (result == null) {
                tvCount.setText("Failed to load");
                return;
            }
            @SuppressWarnings("unchecked")
            List<AppManager.AppEntry> apps = (List<AppManager.AppEntry>) result[0];
            @SuppressWarnings("unchecked")
            List<String> frozen = (List<String>) result[1];

            for (AppManager.AppEntry e : apps) {
                e.frozen = frozen.contains(e.packageName);
            }

            tvCount.setText(apps.size() + " apps");
            List<String> rows = new ArrayList<>();
            for (AppManager.AppEntry e : apps) {
                rows.add((e.system ? "[S] " : "") + e.label + "  (" + e.packageName + ")"
                        + (e.frozen ? "  [FROZEN]" : ""));
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_1, rows);
            listView.setAdapter(adapter);

            listView.setOnItemClickListener((parent, view, position, id) -> {
                AppManager.AppEntry e = apps.get(position);
                Background.supply(() -> e.frozen ? AppManager.unfreeze(e.packageName)
                        : AppManager.freeze(e.packageName), ok -> {
                    Toast.makeText(this, ok ? (e.frozen ? "Unfroze " : "Froze ") + e.packageName
                            : "Operation failed (root required?)", Toast.LENGTH_SHORT).show();
                    refresh();
                });
            });
        });
    }
}
