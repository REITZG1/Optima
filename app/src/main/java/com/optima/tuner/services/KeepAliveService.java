package com.optima.tuner.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.optima.tuner.Optima;
import com.optima.tuner.core.BatteryManager;
import com.optima.tuner.core.CpuManager;

/**
 * Keep-alive foreground service. Applies stored tuning (charge limit, CPU governor,
 * refresh rate) when the device boots and holds the process alive.
 */
public class KeepAliveService extends Service {
    private static final String TAG = "KeepAliveService";
    private static final int FOREGROUND_ID = 1001;
    private static final String CHANNEL_ID = "optima_keepalive";

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(FOREGROUND_ID, buildNotification("Optima is running", "Optima optimiser active"));
        applyStoredTuning();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        applyStoredTuning();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void applyStoredTuning() {
        // Charge limit
        Integer limit = Optima.charge().getInt("charge_limit_percent", -1);
        if (limit > 0) {
            BatteryManager.setChargeLimit(limit);
        }
        Integer cur = Optima.charge().getInt("current_limit_ma", -1);
        if (cur > 0) {
            BatteryManager.setCurrentLimit(cur);
        }
        // CPU governor / freq
        String gov = Optima.cpu().getString("cpu_governor", null);
        if (gov != null && !gov.isEmpty()) {
            for (CpuManager.Cluster c : CpuManager.getClusters()) {
                CpuManager.setGovernor(c, gov);
            }
        }
    }

    private android.app.Notification buildNotification(String title, String text) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "Optima Keep-Alive",
                    NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Optima keeps its tuning active");
            nm.createNotificationChannel(ch);
        }
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setOngoing(true)
                .build();
    }
}
