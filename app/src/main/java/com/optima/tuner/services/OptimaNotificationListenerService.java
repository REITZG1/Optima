package com.optima.tuner.services;

import android.service.notification.StatusBarNotification;

import com.optima.tuner.Optima;

/**
 * Notification listener: records notifications so Optima can filter/block them
 * during performance modes.
 */
public class OptimaNotificationListenerService extends android.service.notification.NotificationListenerService {
    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        Optima.putBool("notification_listener_active", true);
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        Optima.putBool("notification_listener_active", false);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn,
            android.service.notification.NotificationListenerService.RankingMap rankingMap) {
        super.onNotificationPosted(sbn, rankingMap);
        String pkg = sbn.getPackageName();
        Optima.putString("last_notification_pkg", pkg);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);
    }
}
