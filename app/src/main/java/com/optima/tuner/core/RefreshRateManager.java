package com.optima.tuner.core;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.view.Display;

import java.util.ArrayList;
import java.util.List;

/**
 * Display refresh-rate control.
 */
public final class RefreshRateManager {

    /** A display mode offered by the device. */
    public static class Mode {
        public int id;
        public float refreshRate;
        public int width;
        public int height;
        public String modeId;   // SurfaceFlinger mode id, e.g. "1"

        @Override
        public String toString() {
            return refreshRate + " Hz";
        }
    }

    private RefreshRateManager() {
    }

    /**
     * Lists the supported display modes using the hidden {@code SurfaceFlinger} API.
     */
    @SuppressWarnings("deprecation")
    public static List<Mode> listModes(Context context) {
        List<Mode> result = new ArrayList<>();
        try {
            Display display = context.getDisplay();
            android.view.SurfaceView.class.getName();
            Class<?> sf = Class.forName("android.view.Surface");
            Object displayInfo = sf.getMethod("getDisplayInfo").invoke(null);
            Class<?> diClass = Class.forName("android.view.DisplayInfo");
            Object[] modes = (Object[]) diClass.getMethod("getSupportedModes").invoke(displayInfo);
            Class<?> modeClass = Class.forName("android.view.Display$Mode");
            int idField = modeClass.getField("modeId").getModifiers() == java.lang.reflect.Modifier.PUBLIC
                    ? 1 : 0;
            for (Object mode : modes) {
                Mode m = new Mode();
                m.id = (int) modeClass.getField("modeId").get(mode);
                m.refreshRate = (float) modeClass.getField("refreshRate").get(mode);
                m.width = (int) modeClass.getField("physicalWidth").get(mode);
                m.height = (int) modeClass.getField("physicalHeight").get(mode);
                result.add(m);
            }
            if (idField == 0) {
                // nothing to do, placeholder to keep the reflection branch explicit
                result.clear();
                result.addAll(queryModesFallback());
            }
        } catch (Throwable t) {
            result.clear();
            result.addAll(queryModesFallback());
        }
        return result;
    }

    /** Fallback that reads the supported modes from {@code dumpsys display}. */
    private static List<Mode> queryModesFallback() {
        List<Mode> result = new ArrayList<>();
        String out = Shell.execRoot("dumpsys display 2>/dev/null | head -n 400");
        if (out == null) {
            return result;
        }
        boolean inModes = false;
        for (String line : out.split("\n")) {
            String t = line.trim();
            if (t.startsWith("modes:")) {
                inModes = true;
                continue;
            }
            if (inModes) {
                if (t.isEmpty() || t.endsWith(":")) {
                    if (!t.startsWith("m")) {
                        inModes = false;
                    }
                }
                // e.g. "m0 720x1280 @ 60.0 fps (DEFAULT) ..."
                String[] parts = t.split("\\s+");
                if (parts.length >= 5 && parts[0].startsWith("m")
                        && parts[0].substring(1).matches("\\d+")) {
                    Mode m = new Mode();
                    try {
                        m.id = Integer.parseInt(parts[0].substring(1));
                        String[] wh = parts[1].split("x");
                        m.width = Integer.parseInt(wh[0]);
                        m.height = Integer.parseInt(wh[1]);
                        m.refreshRate = Float.parseFloat(parts[3]);
                        result.add(m);
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        return result;
    }

    /** Highest refresh rate supported by the display. */
    public static float getMaxRefreshRate(Context context) {
        float max = 0;
        for (Mode m : listModes(context)) {
            if (m.refreshRate > max) {
                max = m.refreshRate;
            }
        }
        return max;
    }

    /**
     * Applies a refresh rate using the {@code min_fps} / {@code peak_fps} system settings.
     *
     * @param refreshRate target rate; {@code 0} restores the automatic behaviour
     */
    public static boolean setRefreshRate(Context context, float refreshRate) {
        try {
            if (refreshRate <= 0) {
                Settings.System.putFloat(context.getContentResolver(), "peak_fps", 0f);
                Settings.System.putFloat(context.getContentResolver(), "min_fps", 0f);
                return true;
            }
            int value = Math.round(refreshRate);
            Settings.System.putInt(context.getContentResolver(), "peak_fps", value);
            Settings.System.putInt(context.getContentResolver(), "min_fps", value);
            return true;
        } catch (Exception e) {
            // Fall back to the shell command which works on more devices
            if (refreshRate <= 0) {
                Shell.execRoot("settings put system peak_fps 0.0");
                Shell.execRoot("settings put system min_fps 0.0");
            } else {
                int value = Math.round(refreshRate);
                Shell.execRoot("settings put system peak_fps " + value + ".0");
                Shell.execRoot("settings put system min_fps " + value + ".0");
            }
            return true;
        }
    }

    /** Reads the currently enforced refresh rate from the system settings. */
    public static float getCurrentRefreshRate(Context context) {
        try {
            float peak = Settings.System.getFloat(context.getContentResolver(), "peak_fps", 0f);
            if (peak > 0) {
                return peak;
            }
        } catch (Exception ignored) {
        }
        String v = Shell.execRoot("settings get system peak_fps 2>/dev/null").trim();
        try {
            return Float.parseFloat(v);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Selects a specific display mode id.
     */
    public static boolean setModeId(int modeId) {
        // "cmd display set-user-preferred-display-mode" exists on Android 11+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            String out = Shell.execRoot("cmd display set-user-preferred-display-mode " + modeId
                    + " 0 0 2>&1");
            if (out == null || !out.toLowerCase().contains("exception")) {
                return true;
            }
        }
        return false;
    }
}
