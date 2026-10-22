package com.optima.tuner.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.optima.tuner.Optima;

/**
 * Background service started at boot. Keeps Optima's shell layer initialised.
 */
public class BootService extends Service {
    private static final String TAG = "BootService";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Ensure root layer is initialised at boot
        com.optima.tuner.core.Shell.isRootAvailable();
        return START_STICKY;
    }
}
