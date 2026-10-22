package com.optima.tuner.receivers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

import com.optima.tuner.services.KeepAliveService;

/**
 * Boot receiver: starts the keep-alive service and re-applies stored tuning
 * once the device has finished booting.
 */
public class ReceiverBoot extends BroadcastReceiver {
    private static final String TAG = "ReceiverBoot";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_USER_PRESENT.equals(action)) {
            // Start keep-alive service
            Intent svc = new Intent(context, KeepAliveService.class);
            context.startForegroundService(svc);

            // Apply stored charge limit / cpu tuning after a short delay
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am != null) {
                PendingIntent pi = PendingIntent.getService(context, 0x001,
                        new Intent(context, KeepAliveService.class),
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        SystemClock.elapsedRealtime() + 3000, pi);
            }
            Log.d(TAG, "Boot handled, keep-alive service started");
        }
    }
}
