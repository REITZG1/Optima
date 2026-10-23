package com.optima.tuner.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.optima.tuner.R;
import com.optima.tuner.core.Background;
import com.optima.tuner.core.BatteryManager;
import com.optima.tuner.core.CpuManager;
import com.optima.tuner.core.ProcessManager;
import com.optima.tuner.core.RootManager;
import com.optima.tuner.core.Shell;

/**
 * Main overview dashboard: battery, CPU load, memory and quick toggles.
 *
 * <p>All shell access happens on a background thread; only the resulting
 * text is applied to the views on the main thread.
 */
public class ActivityMain extends AppCompatActivity {
    private TextView batteryView, cpuView, memView, rootView;
    private LinearLayout toggleContainer;
    private Button btnRecheckRoot, btnRootManager;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Thread worker;
    private volatile boolean running = true;

    private final Runnable poll = new Runnable() {
        @Override
        public void run() {
            if (!running) {
                return;
            }
            pollStatus();
            mainHandler.postDelayed(this, 2000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        batteryView = findViewById(R.id.tv_battery);
        cpuView = findViewById(R.id.tv_cpu);
        memView = findViewById(R.id.tv_mem);
        rootView = findViewById(R.id.tv_root);
        btnRecheckRoot = findViewById(R.id.btn_recheck_root);
        toggleContainer = findViewById(R.id.ll_toggles);

        btnRecheckRoot.setOnClickListener(v -> {
            rootView.setText("Root: checking…");
            Background.supply(() -> {
                Shell.resetRootCheck();
                return Shell.getRootStatus();
            }, this::showRootStatus);
        });

        btnRootManager = findViewById(R.id.btn_root_manager);
        btnRootManager.setOnClickListener(v -> {
            if (!RootManager.open(this)) {
                Toast.makeText(this, "No root manager found on this device", Toast.LENGTH_SHORT).show();
            }
        });

        buildToggles();
        refreshRootStatus();
    }

    /** Probes root access on a background thread and shows the outcome. */
    private void refreshRootStatus() {
        Background.supply(Shell::getRootStatus, this::showRootStatus);
    }

    /** Renders the root status and the hint for the next step. */
    private void showRootStatus(Shell.RootStatus status) {
        if (status == null) {
            rootView.setText("Root: unknown");
            return;
        }
        StringBuilder sb = new StringBuilder("Root: ").append(status.label())
                .append('\n').append(status.detail);
        if (!status.granted && !status.hint.isEmpty()) {
            sb.append('\n').append(status.hint);
        }
        rootView.setText(sb.toString());
        rootView.setTextColor(getColor(status.granted
                ? R.color.colorTextPrimary : R.color.colorError));
        // Offer a shortcut to the root manager while root is missing.
        btnRootManager.setVisibility(status.granted ? View.GONE : View.VISIBLE);
    }

    private void buildToggles() {
        toggleContainer.removeAllViews();
        addToggle("CPU Tuning", "Set CPU governor & frequencies", v ->
                startActivity(new Intent(this, ActivityCpuControl.class)));
        addToggle("Charge Control", "Limit charge % & current", v ->
                startActivity(new Intent(this, ActivityChargeControl.class)));
        addToggle("Thermal Editor", "Edit thermal thresholds", v ->
                startActivity(new Intent(this, ActivityThermal.class)));
        addToggle("Refresh Rate", "Set display fps", v ->
                startActivity(new Intent(this, ActivityRefreshRate.class)));
        addToggle("App Freezer", "Freeze/disable apps", v ->
                startActivity(new Intent(this, ActivityFreezeApps.class)));
        addToggle("Process List", "Running processes", v ->
                startActivity(new Intent(this, ActivityProcess.class)));
        addToggle("Power Stats", "Battery power draw", v ->
                startActivity(new Intent(this, ActivityPowerStat.class)));
        addToggle("Immersive Mode", "Hide system bars", v ->
                startActivity(new Intent(this, ActivityImmersive.class)));
        addToggle("Rotation Control", "Lock screen orientation", v ->
                startActivity(new Intent(this, ActivityRotation.class)));
        addToggle("Brightness", "Set screen brightness", v ->
                startActivity(new Intent(this, ActivityBrightness.class)));
        addToggle("GPS Location", "Enable/disable GPS", v ->
                startActivity(new Intent(this, ActivityGpsToggle.class)));
        addToggle("Other Settings", "Device information", v ->
                startActivity(new Intent(this, ActivityOtherSettings.class)));
    }

    private void addToggle(String title, String sub, View.OnClickListener listener) {
        TextView tv = new TextView(this);
        tv.setText(title + "\n" + sub);
        tv.setTextSize(14f);
        tv.setPadding(12, 12, 12, 12);
        tv.setClickable(true);
        tv.setOnClickListener(listener);
        toggleContainer.addView(tv);
    }

    /** Collects the status on a background thread and applies it on the main thread. */
    private void pollStatus() {
        if (worker != null && worker.isAlive()) {
            return;
        }
        worker = new Thread(() -> {
            String bat = readBattery();
            String cpu = readCpu();
            String mem = readMemory();
            mainHandler.post(() -> {
                batteryView.setText(bat);
                cpuView.setText(cpu);
                memView.setText(mem);
            });
        }, "optima-status");
        worker.start();
    }

    private String readBattery() {
        try {
            BatteryManager.Status s = BatteryManager.readStatus();
            return s.level + "% · " + (s.charging ? "Charging" : "Discharging")
                    + "\n" + String.format("%.1f °C · ", s.temperature) + s.voltage + " mV";
        } catch (Throwable t) {
            return "Battery: unavailable";
        }
    }

    private String readCpu() {
        try {
            int[] usage = CpuManager.getCpuUsage();
            StringBuilder sb = new StringBuilder("CPU: ");
            for (int i = 0; i < usage.length && i < 8; i++) {
                sb.append(i).append(':').append(usage[i]).append("% ");
            }
            return sb.toString();
        } catch (Throwable t) {
            return "CPU: unavailable";
        }
    }

    private String readMemory() {
        try {
            long total = ProcessManager.getTotalMemory();
            long avail = ProcessManager.getAvailableMemory();
            long used = total - avail;
            int pct = total > 0 ? (int) (used * 100 / total) : 0;
            return pct + "% RAM\n" + (used / 1024 / 1024) + " / "
                    + (total / 1024 / 1024) + " MB";
        } catch (Throwable t) {
            return "RAM: unavailable";
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        running = true;
        mainHandler.post(poll);
    }

    @Override
    protected void onPause() {
        super.onPause();
        running = false;
        mainHandler.removeCallbacks(poll);
    }
}
