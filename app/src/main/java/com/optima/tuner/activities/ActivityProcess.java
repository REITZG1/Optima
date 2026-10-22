package com.optima.tuner.activities;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.optima.tuner.R;
import com.optima.tuner.core.Background;
import com.optima.tuner.core.ProcessManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Process list: shows running processes with CPU and memory usage.
 */
public class ActivityProcess extends AppCompatActivity {
    private ListView listView;
    private TextView tvCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_process);

        listView = findViewById(R.id.list_processes);
        tvCount = findViewById(R.id.tv_count);
        tvCount.setText("Loading…");

        Background.supply(ProcessManager::listProcesses, procs -> {
            if (procs == null) {
                tvCount.setText("Failed to read /proc (root required?)");
                return;
            }
            tvCount.setText(procs.size() + " processes");
            List<String> rows = new ArrayList<>();
            for (ProcessManager.ProcessInfo p : procs) {
                rows.add(p.pid + " · " + p.name + " · CPU " + (int) p.cpuPercent + "% · "
                        + (p.rssKb / 1024) + " MB · uid " + p.uid);
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_1, rows);
            listView.setAdapter(adapter);
            if (rows.isEmpty()) {
                Toast.makeText(this, "No processes (root required)", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
