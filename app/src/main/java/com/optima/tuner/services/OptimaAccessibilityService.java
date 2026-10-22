package com.optima.tuner.services;

import android.content.Intent;
import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;

import com.optima.tuner.Optima;

/**
 * Accessibility service: monitors window changes and can trigger actions on the
 * foreground app (auto-unfreeze, app-profile switching).
 */
public class OptimaAccessibilityService extends AccessibilityService {
    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Optima.putBool("accessibility_active", true);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int type = event.getEventType();
        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                || type == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            // Record foreground package
            String pkg = event.getPackageName() == null ? "" : event.getPackageName().toString();
            Optima.putString("last_foreground_pkg", pkg);
        }
    }

    @Override
    public void onInterrupt() {
        // no-op
    }
}
