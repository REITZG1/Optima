package com.optima.tuner.activities;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.optima.tuner.R;
import com.optima.tuner.core.Background;

/**
 * Immersive mode: hide system bars (navigation / status) for full-screen apps.
 */
public class ActivityImmersive extends AppCompatActivity {
    private TextView statusView;
    private int flags = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_immerse);

        statusView = findViewById(R.id.tv_immerse_status);

        Button btnHideNav = findViewById(R.id.btn_hide_nav);
        Button btnHideStatus = findViewById(R.id.btn_hide_status);
        Button btnShowAll = findViewById(R.id.btn_show_all);

        btnHideNav.setOnClickListener(v -> applyImmersive(true, false));
        btnHideStatus.setOnClickListener(v -> applyImmersive(false, true));
        btnShowAll.setOnClickListener(v -> applyImmersive(false, false));

        refresh();
    }

    private void applyImmersive(boolean hideNav, boolean hideStatus) {
        View decor = getWindow().getDecorView();
        int newFlags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        if (hideNav) {
            newFlags |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        }
        if (hideStatus) {
            newFlags |= View.SYSTEM_UI_FLAG_FULLSCREEN;
        }
        newFlags |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decor.setSystemUiVisibility(newFlags);
        flags = newFlags;
        refresh();
        Toast.makeText(this, "Immersive mode applied", Toast.LENGTH_SHORT).show();
    }

    private void refresh() {
        StringBuilder sb = new StringBuilder();
        if ((flags & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) != 0) {
            sb.append("Navigation bar: hidden\n");
        }
        if ((flags & View.SYSTEM_UI_FLAG_FULLSCREEN) != 0) {
            sb.append("Status bar: hidden\n");
        }
        if (sb.length() == 0) {
            sb.append("All bars visible");
        }
        statusView.setText(sb.toString());
    }
}
