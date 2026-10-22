package com.optima.tuner.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.optima.tuner.R;
import com.optima.tuner.core.Background;
import com.optima.tuner.core.Shell;

/**
 * Text editor for viewing/editing system config files.
 */
public class ActivityEditor extends AppCompatActivity {
    private TextView etText;
    private String path;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        etText = findViewById(R.id.et_text);
        path = getIntent().getStringExtra("path");
        if (path == null) {
            path = "/system/build.prop";
        }

        Button btnLoad = findViewById(R.id.btn_load);
        Button btnSave = findViewById(R.id.btn_save);

        btnLoad.setOnClickListener(v -> load());
        btnSave.setOnClickListener(v -> save());

        load();
    }

    private void load() {
        etText.setText("Loading…");
        Background.supply(() -> Shell.execRoot("cat " + path + " 2>/dev/null"), out ->
                etText.setText(out == null || out.isEmpty()
                        ? "Failed to read (root required?)" : out));
    }

    private void save() {
        String content = etText.getText().toString().replace("'", "'\\''");
        Background.supply(() -> Shell.execRoot("echo '" + content + "' > " + path), out ->
                etText.setText("Saved."));
    }
}
