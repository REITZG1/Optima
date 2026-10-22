package com.optima.tuner.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.optima.tuner.R;
import com.optima.tuner.Optima;
import com.optima.tuner.core.Background;
import com.optima.tuner.core.CpuManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * CPU control: per-cluster governor and frequency limits.
 *
 * <p>Reading the cpufreq sysfs tree requires root shell calls, so all probing
 * happens on a background thread.
 *
 * <p>Some vendors make the frequency nodes read-only so their own governor
 * stays in charge; those controls are then disabled instead of silently failing.
 */
public class ActivityCpuControl extends AppCompatActivity {
    private Spinner spinnerClusters, spinnerGovernor, spinnerMinFreq, spinnerMaxFreq;
    private TextView tvGov, tvMinFreq, tvMaxFreq, tvCurFreq, tvCores, tvLockNote, tvRateTitle;
    private Button btnApply, btnReset, btnApplyRate;
    private LinearLayout llRateLimits;
    private EditText etUpRate, etDownRate;

    private List<CpuManager.Cluster> clusters = new ArrayList<>();
    private List<String> governorLabels = new ArrayList<>();
    private List<String> minLabels = new ArrayList<>();
    private List<String> maxLabels = new ArrayList<>();
    private List<Integer> minValues = new ArrayList<>();
    private List<Integer> maxValues = new ArrayList<>();

    private int selectedClusterIndex = 0;
    private boolean loaded;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cpu_control);

        spinnerClusters = findViewById(R.id.spinner_clusters);
        spinnerGovernor = findViewById(R.id.spinner_governor);
        spinnerMinFreq = findViewById(R.id.spinner_min_freq);
        spinnerMaxFreq = findViewById(R.id.spinner_max_freq);
        tvGov = findViewById(R.id.tv_governor);
        tvMinFreq = findViewById(R.id.tv_min_freq);
        tvMaxFreq = findViewById(R.id.tv_max_freq);
        tvCurFreq = findViewById(R.id.tv_cur_freq);
        tvCores = findViewById(R.id.tv_cores);
        tvLockNote = findViewById(R.id.tv_lock_note);
        tvRateTitle = findViewById(R.id.tv_rate_title);
        llRateLimits = findViewById(R.id.ll_rate_limits);
        etUpRate = findViewById(R.id.et_up_rate);
        etDownRate = findViewById(R.id.et_down_rate);
        btnApply = findViewById(R.id.btn_apply);
        btnReset = findViewById(R.id.btn_reset);
        btnApplyRate = findViewById(R.id.btn_apply_rate);

        spinnerClusters.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedClusterIndex = position;
                updateInfo(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        btnApply.setOnClickListener(v -> apply());
        btnReset.setOnClickListener(v -> resetToHardware());
        btnApplyRate.setOnClickListener(v -> applyRateLimits());

        loadClusters();
    }

    /** Probes the cpufreq tree in the background. */
    private void loadClusters() {
        tvCores.setText("Reading CPU policies…");
        Background.supply(() -> {
            CpuManager.load();
            return CpuManager.getClusters();
        }, result -> {
            clusters = result == null ? new ArrayList<>() : result;
            List<String> names = new ArrayList<>();
            for (CpuManager.Cluster c : clusters) {
                names.add("Cluster " + c.firstCore + " · cores " + c.cores
                        + " · " + formatFreq(c.hardwareMax));
            }
            if (names.isEmpty()) {
                names.add("No cpufreq policy found");
            }
            setSpinnerItems(spinnerClusters, names);
            loaded = true;
            if (!clusters.isEmpty()) {
                updateInfo(0);
            } else {
                tvGov.setText("Unavailable (root required?)");
                tvCores.setText("No cpufreq policy found. Root access is needed to read "
                        + "/sys/devices/system/cpu/*/cpufreq.");
            }
        });
    }

    /** Refreshes the info card and the three selection spinners. */
    private void updateInfo(int idx) {
        if (!loaded || idx < 0 || idx >= clusters.size()) {
            return;
        }
        CpuManager.Cluster c = clusters.get(idx);
        tvGov.setText("Governor: " + (c.governor.isEmpty() ? "-" : c.governor)
                + (c.governorWritable ? "" : "  (read-only)"));
        tvMinFreq.setText("Min freq: " + formatFreq(c.minFreq)
                + "  (hw min " + formatFreq(c.hardwareMin) + ")");
        tvMaxFreq.setText("Max freq: " + formatFreq(c.maxFreq)
                + "  (hw max " + formatFreq(c.hardwareMax) + ")");
        tvCurFreq.setText("Cur freq: " + formatFreq(c.currentFreq));
        tvCores.setText("Cores: " + c.cores
                + (c.relatedCpus.isEmpty() ? "" : "  (policy: " + c.relatedCpus + ")"));

        // Governor list
        governorLabels = new ArrayList<>(c.availableGovernors);
        if (governorLabels.isEmpty() && !c.governor.isEmpty()) {
            governorLabels.add(c.governor);
        }
        setSpinnerItems(spinnerGovernor, governorLabels);
        int govIndex = governorLabels.indexOf(c.governor);
        if (govIndex >= 0) {
            spinnerGovernor.setSelection(govIndex);
        }
        spinnerGovernor.setEnabled(c.governorWritable);

        // Frequency lists, ascending / descending
        List<Integer> freqs = new ArrayList<>(c.availableFrequencies);
        Collections.sort(freqs);
        minLabels = new ArrayList<>();
        maxLabels = new ArrayList<>();
        minValues = new ArrayList<>();
        maxValues = new ArrayList<>();
        for (int f : freqs) {
            minLabels.add(formatFreq(f));
            minValues.add(f);
        }
        for (int i = freqs.size() - 1; i >= 0; i--) {
            maxLabels.add(formatFreq(freqs.get(i)));
            maxValues.add(freqs.get(i));
        }
        if (minLabels.isEmpty()) {
            minLabels.add(formatFreq(c.hardwareMin));
            minValues.add(c.hardwareMin);
            maxLabels.add(formatFreq(c.hardwareMax));
            maxValues.add(c.hardwareMax);
        }
        setSpinnerItems(spinnerMinFreq, minLabels);
        setSpinnerItems(spinnerMaxFreq, maxLabels);
        selectValue(spinnerMinFreq, c.minFreq, minValues);
        selectValue(spinnerMaxFreq, c.maxFreq, maxValues);
        spinnerMinFreq.setEnabled(c.minWritable);
        spinnerMaxFreq.setEnabled(c.maxWritable);

        // Explain what the kernel refuses to let us change.
        StringBuilder locked = new StringBuilder();
        if (!c.governorWritable) {
            locked.append("Governor node is read-only on this device.\n");
        }
        if (!c.minWritable || !c.maxWritable) {
            locked.append("Frequency limits are locked by the kernel (")
                    .append(c.governor).append(" governor manages them). ")
                    .append("Only the governor can be changed here.");
        }
        if (locked.length() > 0) {
            tvLockNote.setText(locked.toString().trim());
            tvLockNote.setVisibility(View.VISIBLE);
        } else {
            tvLockNote.setVisibility(View.GONE);
        }

        // MTK sugov_ext ramp limits
        boolean hasRate = CpuManager.supportsRateLimits(c);
        tvRateTitle.setVisibility(hasRate ? View.VISIBLE : View.GONE);
        llRateLimits.setVisibility(hasRate ? View.VISIBLE : View.GONE);
        btnApplyRate.setVisibility(hasRate ? View.VISIBLE : View.GONE);
        if (hasRate) {
            etUpRate.setText(String.valueOf(c.upRateLimit));
            etDownRate.setText(String.valueOf(c.downRateLimit));
        }
    }

    private void apply() {
        if (!loaded || selectedClusterIndex >= clusters.size()) {
            return;
        }
        CpuManager.Cluster c = clusters.get(selectedClusterIndex);
        int governorPos = spinnerGovernor.getSelectedItemPosition();
        int minPos = spinnerMinFreq.getSelectedItemPosition();
        int maxPos = spinnerMaxFreq.getSelectedItemPosition();

        String governor = governorPos >= 0 && governorPos < governorLabels.size()
                ? governorLabels.get(governorPos) : c.governor;
        int minKhz = minPos >= 0 && minPos < minValues.size() ? minValues.get(minPos) : c.minFreq;
        int maxKhz = maxPos >= 0 && maxPos < maxValues.size() ? maxValues.get(maxPos) : c.maxFreq;

        if (c.minWritable && c.maxWritable && minKhz > maxKhz) {
            Toast.makeText(this, "Minimum cannot be higher than maximum", Toast.LENGTH_SHORT).show();
            return;
        }

        final String gov = governor;
        final int min = minKhz;
        final int max = maxKhz;
        Background.supply(() -> {
            boolean govOk = !c.governorWritable || CpuManager.setGovernor(c, gov);
            boolean freqOk = true;
            if (c.minWritable && c.maxWritable) {
                freqOk = CpuManager.setFrequencyRange(c, min, max);
            }
            return new boolean[]{govOk, freqOk};
        }, result -> {
            if (result == null) {
                Toast.makeText(this, "Failed to apply", Toast.LENGTH_LONG).show();
                return;
            }
            boolean govOk = result[0];
            boolean freqOk = result[1];
            if (govOk && freqOk) {
                Optima.cpu().edit()
                        .putString("cpu_governor", gov)
                        .putInt("cpu_min_khz", min)
                        .putInt("cpu_max_khz", max)
                        .apply();
                Toast.makeText(this, "Applied: " + gov
                        + (c.minWritable && c.maxWritable
                        ? "  " + formatFreq(min) + " – " + formatFreq(max) : ""),
                        Toast.LENGTH_LONG).show();
            } else if (govOk) {
                Toast.makeText(this, "Governor changed to " + gov
                        + ", but the frequency limits are locked by the kernel",
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "The kernel refused the governor change", Toast.LENGTH_LONG).show();
            }
            reload();
        });
    }

    private void resetToHardware() {
        if (!loaded || selectedClusterIndex >= clusters.size()) {
            return;
        }
        CpuManager.Cluster c = clusters.get(selectedClusterIndex);
        if (!c.minWritable || !c.maxWritable) {
            Toast.makeText(this, "Frequency limits are locked by the kernel on this device",
                    Toast.LENGTH_LONG).show();
            return;
        }
        final int hwMin = c.hardwareMin;
        final int hwMax = c.hardwareMax;
        Background.supply(() -> CpuManager.setFrequencyRange(c, hwMin, hwMax), ok -> {
            Toast.makeText(this, ok == null || !ok
                            ? "Reset failed" : "Reset to hardware limits",
                    Toast.LENGTH_SHORT).show();
            reload();
        });
    }

    private void applyRateLimits() {
        if (!loaded || selectedClusterIndex >= clusters.size()) {
            return;
        }
        CpuManager.Cluster c = clusters.get(selectedClusterIndex);
        int up, down;
        try {
            up = Integer.parseInt(etUpRate.getText().toString().trim());
            down = Integer.parseInt(etDownRate.getText().toString().trim());
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Enter whole microseconds", Toast.LENGTH_SHORT).show();
            return;
        }
        if (up < 0 || down < 0) {
            Toast.makeText(this, "Values cannot be negative", Toast.LENGTH_SHORT).show();
            return;
        }
        Background.supply(() -> CpuManager.setUpRateLimit(c, up)
                && CpuManager.setDownRateLimit(c, down), ok -> {
            Toast.makeText(this, ok != null && ok
                            ? "Ramp limits applied" : "The kernel refused the value",
                    Toast.LENGTH_SHORT).show();
            reload();
        });
    }

    /** Re-reads the live values after a change. */
    private void reload() {
        Background.supply(() -> {
            CpuManager.refresh();
            return CpuManager.getClusters();
        }, result -> {
            clusters = result == null ? clusters : result;
            updateInfo(selectedClusterIndex);
        });
    }

    private void setSpinnerItems(Spinner spinner, List<String> items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void selectValue(Spinner spinner, int value, List<Integer> values) {
        int index = values.indexOf(value);
        if (index >= 0) {
            spinner.setSelection(index);
        }
    }

    private String formatFreq(int khz) {
        if (khz <= 0) {
            return "-";
        }
        if (khz >= 1000) {
            return (khz / 1000f) + " GHz";
        }
        return khz + " MHz";
    }
}
