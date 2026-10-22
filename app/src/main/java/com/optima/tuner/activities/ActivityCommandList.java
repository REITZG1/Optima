package com.optima.tuner.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.optima.tuner.R;
import com.optima.tuner.core.Background;
import com.optima.tuner.core.Shell;

/**
 * Command list of quick shell shortcuts.
 */
public class ActivityCommandList extends AppCompatActivity {
    private TextView tvInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_command_list);

        tvInfo = findViewById(R.id.tv_info);
        tvInfo.setText("Command List\n\nQuick shell commands (require root).");

        Button btnDmesg = findViewById(R.id.btn_dmesg);
        Button btnProp = findViewById(R.id.btn_prop);
        Button btnMount = findViewById(R.id.btn_mount);
        btnDmesg.setOnClickListener(v -> run("dmesg | tail -n 50"));
        btnProp.setOnClickListener(v -> run("getprop | head -n 50"));
        btnMount.setOnClickListener(v -> run("mount | head -n 30"));
    }

    private void run(String cmd) {
        tvInfo.setText("Running…");
        Background.supply(() -> Shell.execRoot(cmd), out ->
                tvInfo.setText(out == null || out.isEmpty() ? "(no output)" : out));
    }
}
