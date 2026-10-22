package com.optima.tuner.receivers;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.optima.tuner.R;
import com.optima.tuner.activities.ActivityMain;
import com.optima.tuner.core.BatteryManager;
import com.optima.tuner.core.CpuManager;

/**
 * Home-screen widget showing a short device summary (battery and CPU load).
 */
public class OptimaAppWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] widgetIds) {
        for (int id : widgetIds) {
            manager.updateAppWidget(id, buildViews(context));
        }
        refreshAll(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent == null ? null : intent.getAction();
        if (action != null && AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(action)) {
            refreshAll(context);
        }
    }

    /** Re-reads the values and pushes them to every placed widget. */
    private void refreshAll(Context context) {
        final Context ctx = context.getApplicationContext();
        new Thread(() -> {
            String text = "Loading…";
            try {
                BatteryManager.Status s = BatteryManager.readStatus();
                int[] usage = CpuManager.getCpuUsage();
                int avg = 0;
                for (int u : usage) {
                    avg += u;
                }
                avg = usage.length == 0 ? 0 : avg / usage.length;
                text = s.level + "% · " + (s.charging ? "charging" : "discharging")
                        + "\nCPU " + avg + "% · " + String.format("%.1f °C", s.temperature);
            } catch (Throwable t) {
                text = "Unavailable";
            }
            final String value = text;
            AppWidgetManager manager = AppWidgetManager.getInstance(ctx);
            int[] ids = manager.getAppWidgetIds(
                    new ComponentName(ctx, OptimaAppWidgetProvider.class));
            if (ids == null || ids.length == 0) {
                return;
            }
            RemoteViews views = buildViews(ctx);
            views.setTextViewText(R.id.widget_text, value);
            manager.updateAppWidget(ids, views);
        }, "optima-widget").start();
    }

    private RemoteViews buildViews(Context context) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.appwidget);
        views.setTextViewText(R.id.widget_title, context.getString(R.string.app_name));

        Intent open = new Intent(context, ActivityMain.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        views.setOnClickPendingIntent(R.id.widget_root,
                PendingIntent.getActivity(context, 0, open, flags));

        return views;
    }
}
