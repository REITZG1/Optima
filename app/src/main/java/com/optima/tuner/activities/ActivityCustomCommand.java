package com.optima.tuner.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.optima.tuner.R;
import com.optima.tuner.core.Background;
import com.optima.tuner.core.Shell;

/**
 * Custom command runner: execute arbitrary shell commands.
 */
public class ActivityCustomCommand extends AppCompatActivity {
    private EditText etCommand;
    private TextView tvOutput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_command);

        etCommand = findViewById(R.id.et_command);
        tvOutput = findViewById(R.id.tv_output);

        Button btnRun = findViewById(R.id.btn_run);
        Button btnRoot = findViewById(R.id.btn_root);

        btnRun.setOnClickListener(v -> execute(false));
        btnRoot.setOnClickListener(v -> execute(true));
    }

    private void execute(boolean asRoot) {
        String cmd = etCommand.getText().toString();
        if (cmd.trim().isEmpty()) {
            return;
        }
        tvOutput.setText("Running…");
        Background.supply(() -> asRoot ? Shell.execRoot(cmd) : Shell.exec(cmd), out -> {
            String text = out == null || out.isEmpty() ? "(no output)" : out;
            tvOutput.setText(text);
            if (asRoot && text.contains("not found")) {
                Toast.makeText(this, "Root not available or command failed", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
