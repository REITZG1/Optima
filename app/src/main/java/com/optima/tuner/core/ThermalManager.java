package com.optima.tuner.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Thermal zone discovery and control.
 */
public final class ThermalManager {

    /** A single thermal zone. */
    public static class Zone {
        public int index;
        public String type;
        public String policy;
        public float temperature;
        public List<Integer> availableTripPoints = new ArrayList<>();
    }

    private ThermalManager() {
    }

    /**
     * Lists all thermal zones with their current temperature.
     *
     * <p>Uses a single shell invocation so that devices with 50+ zones do not
     * spawn one process per zone.
     */
    public static List<Zone> listZones() {
        List<Zone> result = new ArrayList<>();
        String out = Shell.execRoot("for z in /sys/class/thermal/thermal_zone*; do "
                + "test -d $z || continue; "
                + "echo \"${z##*_} $(cat $z/type 2>/dev/null) $(cat $z/policy 2>/dev/null) "
                + "$(cat $z/temp 2>/dev/null)\"; done");
        if (out == null) {
            return result;
        }
        int index = 0;
        for (String line : out.split("\n")) {
            String[] p = line.trim().split("\\s+");
            if (p.length < 2) {
                continue;
            }
            Zone z = new Zone();
            try {
                z.index = Integer.parseInt(p[0]);
            } catch (NumberFormatException e) {
                z.index = index;
            }
            index = z.index + 1;
            z.type = p[1];
            z.policy = p.length > 2 ? p[2] : "";
            if (p.length > 3) {
                try {
                    z.temperature = Integer.parseInt(p[3]) / 1000f;
                } catch (NumberFormatException ignored) {
                }
            }
            result.add(z);
        }
        return result;
    }

    /**
     * Reads the trip-point temperatures of one zone.
     *
     * <p>Called on demand only, because it needs one shell call per zone.
     */
    public static List<Integer> getTripPoints(int zone) {
        List<Integer> points = new ArrayList<>();
        String trips = Shell.execRoot("cat /sys/class/thermal/thermal_zone" + zone
                + "/trip_point_*_temp 2>/dev/null");
        if (trips == null) {
            return points;
        }
        for (String t : trips.split("\n")) {
            try {
                points.add(Integer.parseInt(t.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return points;
    }

    /** Sets the thermal trip point (in °C) of a zone. */
    public static boolean setTripPoint(int zone, int tripPointIndex, int celsius) {
        String path = "/sys/class/thermal/thermal_zone" + zone + "/trip_point_" + tripPointIndex + "_temp";
        Shell.execRoot("echo " + (celsius * 1000) + " > " + path);
        String check = Shell.execRoot("cat " + path + " 2>/dev/null").trim();
        try {
            return Math.abs(Integer.parseInt(check) / 1000 - celsius) <= 1;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * MIUI thermal configuration file. The file controls the throttling thresholds
     * per thermal type; it is only present on Xiaomi devices.
     */
    public static final String MIUI_THERMAL_CONFIG = "/data/vendor/thermal/thermal_config";

    public static boolean miuiThermalConfigExists() {
        return "1".equals(Shell.execRoot("test -f " + MIUI_THERMAL_CONFIG + " && echo 1 || echo 0").trim());
    }

    /** Reads the MIUI thermal configuration file. */
    public static String readMiuiThermalConfig() {
        return Shell.execRoot("cat " + MIUI_THERMAL_CONFIG + " 2>/dev/null");
    }

    /**
     * Writes the MIUI thermal configuration file.
     *
     * @param content new file content
     * @return true on success
     */
    public static boolean writeMiuiThermalConfig(String content) {
        if (content == null) {
            return false;
        }
        Shell.execRoot("cp " + MIUI_THERMAL_CONFIG + " " + MIUI_THERMAL_CONFIG + ".bak 2>/dev/null");
        String escaped = content.replace("'", "'\\''");
        Shell.execRoot("echo '" + escaped + "' > " + MIUI_THERMAL_CONFIG);
        return true;
    }

    /** Restores the MIUI thermal configuration backup. */
    public static boolean restoreMiuiThermalConfig() {
        return "1".equals(Shell.execRoot("test -f " + MIUI_THERMAL_CONFIG
                + ".bak && cp " + MIUI_THERMAL_CONFIG + ".bak " + MIUI_THERMAL_CONFIG
                + " && echo 1 || echo 0").trim());
    }

    /**
     * Oplus / ColorOS "orms" configuration directory.
     */
    public static final String[] ORMS_PATHS = {
            "/data/vendor/thermal/oplus_thermal_config",
            "/vendor/etc/thermal/thermal_config",
    };

    public static String findOrmsConfig() {
        for (String path : ORMS_PATHS) {
            if ("1".equals(Shell.execRoot("test -f " + path + " && echo 1 || echo 0").trim())) {
                return path;
            }
        }
        return null;
    }

    /** Finds the vendor thermal configuration file path. */
    public static String findThermalConfig() {
        String[] candidates = {
                "/data/vendor/thermal/thermal_config",
                "/vendor/etc/thermal/thermal_config",
                "/sys/etc/thermal/thermal.conf",
        };
        for (String path : candidates) {
            if ("1".equals(Shell.execRoot("test -f " + path + " && echo 1 || echo 0").trim())) {
                return path;
            }
        }
        return null;
    }
}
