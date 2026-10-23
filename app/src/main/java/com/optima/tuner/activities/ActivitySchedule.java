package com.optima.tuner.activities;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.optima.tuner.R;
import com.optima.tuner.core.Background;
import com.optima.tuner.core.Shell;

import java.util.ArrayList;
import java.util.List;

/**
 * Schedule: store simple shell tasks (name + command) so they can be re-run
 * or triggered later. Persisted in shared preferences.
 */
public class ActivitySchedule extends AppCompatActivity {
    private ListView listView;
    private final List<String> tasks = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);

        listView = findViewById(R.id.list_tasks);
        Button btnAdd = findViewById(R.id.btn_schedule_add);

        load();

        btnAdd.setOnClickListener(v -> {
            EditText name = new EditText(this);
            name.setHint("Task name");
            EditText cmd = new EditText(this);
            cmd.setHint("Shell command");
            android.app.AlertDialog dlg = new android.app.AlertDialog.Builder(this)
                    .setTitle("Add scheduled task")
                    .setView(name)
                    .setPositiveButton("Next", (d, which) -> {
                        android.app.AlertDialog dlg2 = new android.app.AlertDialog.Builder(ActivitySchedule.this)
                                .setView(cmd)
                                .setMessage("Command for " + name.getText())
                                .setPositiveButton("Save", (d2, w2) -> {
                                    String n = name.getText().toString().trim();
                                    String c = cmd.getText().toString().trim();
                                    if (n.isEmpty() || c.isEmpty()) {
                                        Toast.makeText(ActivitySchedule.this,
                                                "Name and command required", Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    Background.supply(() -> saveTask(n, c), ok ->
                                            Toast.makeText(ActivitySchedule.this,
                                                    ok ? "Saved" : "Save failed", Toast.LENGTH_SHORT).show());
                                    load();
                                })
                                .setNegativeButton("Cancel", null)
                                .create();
                        dlg2.show();
                    })
                    .setNegativeButton("Cancel", null)
                    .create();
            dlg.show();
        });
    }

    private void load() {
        tasks.clear();
        String raw = getSharedPreferences("optima_schedule", MODE_PRIVATE)
                .getString("tasks", "");
        if (!raw.isEmpty()) {
            for (String part : raw.split("\\|")) {
                tasks.add(part);
            }
        }
        refreshList();
    }

    private void refreshList() {
        if (tasks.isEmpty()) {
            tasks.add("No scheduled tasks");
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, tasks);
        listView.setAdapter(adapter);
    }

    private boolean saveTask(String name, String cmd) {
        try {
            android.content.SharedPreferences prefs =
                    getSharedPreferences("optima_schedule", MODE_PRIVATE);
            android.content.SharedPreferences.Editor ed = prefs.edit();
            String raw = prefs.getString("tasks", "");
            String entry = name + "=" + cmd;
            if (raw.isEmpty()) {
                raw = entry;
            } else {
                raw = raw + "|" + entry;
            }
            ed.putString("tasks", raw);
            ed.apply();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
