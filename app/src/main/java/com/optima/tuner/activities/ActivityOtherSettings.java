package com.optima.tuner.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.optima.tuner.R;
import com.optima.tuner.core.Background;
import com.optima.tuner.core.ProcessManager;
import com.optima.tuner.core.Shell;
import com.optima.tuner.core.SystemInfo;

/**
 * System information: memory, SoC and CPU topology.
 */
public class ActivityOtherSettings extends AppCompatActivity {
    private TextView tvInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_other_settings);

        tvInfo = findViewById(R.id.tv_info);
        tvInfo.setText("Loading…");

        Button btnRefresh = findViewById(R.id.btn_refresh);
        btnRefresh.setOnClickListener(v -> load());

        load();
    }

    private void load() {
        Background.supply(() -> {
            StringBuilder sb = new StringBuilder();
            sb.append("Device: ").append(android.os.Build.MANUFACTURER).append(" ")
                    .append(android.os.Build.MODEL).append("\n");
            sb.append("Android: ").append(android.os.Build.VERSION.RELEASE).append(" (API ")
                    .append(android.os.Build.VERSION.SDK_INT).append(")\n");
            sb.append("SoC: ").append(SystemInfo.getSocModel()).append("\n");
            sb.append("Cores: ").append(SystemInfo.getCpuCount()).append("\n");
            sb.append("Clusters: ").append(SystemInfo.getClusterFirstCores().size()).append("\n");
            sb.append("Total RAM: ").append(ProcessManager.getTotalMemory() / 1024 / 1024)
                    .append(" MB\n");
            sb.append("Avail RAM: ").append(ProcessManager.getAvailableMemory() / 1024 / 1024)
                    .append(" MB\n");
            sb.append("Miui: ").append(SystemInfo.isMiui()).append("\n");
            sb.append("Oplus: ").append(SystemInfo.isOplus()).append("\n");
            Shell.RootStatus root = Shell.getRootStatus();
            sb.append("Root: ").append(root.granted ? "Granted" : "Denied").append("\n");
            sb.append("  su: ").append(root.method).append("\n");
            sb.append("  ").append(root.detail).append("\n");
            return sb.toString();
        }, text -> {
            if (text != null) {
                tvInfo.setText(text);
            }
        });
    }
}
