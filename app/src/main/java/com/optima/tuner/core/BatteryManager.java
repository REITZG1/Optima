package com.optima.tuner.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Battery / charging information and control.
 */
public final class BatteryManager {

    public static final String BATTERY_ROOT = "/sys/class/power_supply/battery/";

    /** A snapshot of the battery state. */
    public static class Status {
        public int level;
        public float temperature;         // °C
        public int voltage;              // mV
        public int current;              // mA (absolute value)
        public boolean charging;
        public boolean full;
        public int health = -1;          // BatteryManager.BATTERY_HEALTH_*
        public int statusCode = -1;      // BatteryManager.BATTERY_STATUS_*
        public int chargeCounter;        // µAh
        public int chargeFull;           // µAh
        public int chargeFullDesign;     // µAh
        public int cycleCount;
        public String technology = "";
        public String chargeType = "";
        public int power;                // mW, computed from voltage * current
    }

    private BatteryManager() {
    }

    /**
     * Converts a raw battery temperature into degrees Celsius.
     *
     * <p>The Linux power supply class normally reports the temperature in
     * tenths of a degree Celsius (375 → 37.5 °C). Some vendors use
     * milli-degrees instead (37500 → 37.5 °C), which is detected by the
     * magnitude of the raw value.
     */
    static float toCelsius(int raw) {
        if (raw <= 0) {
            return 0f;
        }
        if (raw > 1000) {
            // milli-degrees: 37500 → 37.5
            return raw / 1000f;
        }
        // tenths of a degree: 375 → 37.5
        return raw / 10f;
    }

    /** Reads the current battery state from sysfs (root required for the full set). */
    public static Status readStatus() {
        Status s = new Status();
        s.level = readInt(BATTERY_ROOT + "capacity", 0);
        int temp = readInt(BATTERY_ROOT + "temp", 0);
        s.temperature = toCelsius(temp);
        s.voltage = readInt(BATTERY_ROOT + "voltage_now", 0) / 1000;
        if (s.voltage == 0) {
            s.voltage = readInt(BATTERY_ROOT + "voltage_avg", 0) / 1000;
        }
        int current = readInt(BATTERY_ROOT + "current_now", 0);
        if (current == 0) {
            current = readInt(BATTERY_ROOT + "current_avg", 0);
        }
        s.current = Math.abs(current) / 1000;
        s.chargeCounter = readInt(BATTERY_ROOT + "charge_counter", 0);
        s.chargeFull = readInt(BATTERY_ROOT + "charge_full", 0);
        s.chargeFullDesign = readInt(BATTERY_ROOT + "charge_full_design", 0);
        s.cycleCount = readInt(BATTERY_ROOT + "cycle_count", 0);
        s.technology = readString(BATTERY_ROOT + "technology");
        s.chargeType = readString(BATTERY_ROOT + "charge_type");

        String status = readString(BATTERY_ROOT + "status");
        s.charging = "Charging".equalsIgnoreCase(status) || "Full".equalsIgnoreCase(status);
        s.full = "Full".equalsIgnoreCase(status);

        String health = readString(BATTERY_ROOT + "health");
        s.health = healthCode(health);

        s.power = (s.voltage * s.current) / 1000;
        return s;
    }

    /** Parses {@code dumpsys battery} output. */
    public static Status readStatusFromDump() {
        Status s = new Status();
        String out = Shell.execRoot("dumpsys battery");
        if (out == null || out.isEmpty()) {
            return s;
        }
        for (String line : out.split("\n")) {
            String t = line.trim();
            if (t.startsWith("level:")) {
                s.level = parseInt(t.substring(6));
            } else if (t.startsWith("temperature:")) {
                s.temperature = toCelsius(parseInt(t.substring(11)));
            } else if (t.startsWith("voltage:")) {
                s.voltage = parseInt(t.substring(8)) / 1000;
            } else if (t.startsWith("status:")) {
                s.statusCode = statusCode(t.substring(7).trim());
                s.charging = s.statusCode == 2 || s.statusCode == 5;
                s.full = s.statusCode == 5;
            } else if (t.startsWith("health:")) {
                s.health = healthCode(t.substring(7).trim());
            } else if (t.startsWith("current:")) {
                s.current = Math.abs(parseInt(t.substring(8))) / 1000;
            }
        }
        return s;
    }

    private static int healthCode(String health) {
        if (health == null) {
            return -1;
        }
        switch (health.trim().toLowerCase()) {
            case "good":
                return 2;
            case "overheat":
                return 3;
            case "dead":
                return 4;
            case "over voltage":
                return 5;
            case "failure":
                return 6;
            case "cold":
                return 7;
            case "unknown":
                return 1;
            default:
                return -1;
        }
    }

    private static int statusCode(String status) {
        if (status == null) {
            return -1;
        }
        switch (status.trim().toLowerCase()) {
            case "charging":
                return 2;
            case "discharging":
                return 3;
            case "not charging":
                return 4;
            case "full":
                return 5;
            case "unknown":
                return 1;
            default:
                return -1;
        }
    }

    // ------------------------------------------------------------- helpers

    public static int readInt(String path, int def) {
        String v = Shell.execRoot("cat " + path + " 2>/dev/null").trim();
        if (v.isEmpty()) {
            return def;
        }
        try {
            return Integer.parseInt(v.split("\\s+")[0]);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static String readString(String path) {
        String v = Shell.execRoot("cat " + path + " 2>/dev/null").trim();
        return v;
    }

    private static int parseInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // ------------------------------------------------------- charge limit

    /** Candidate sysfs nodes that expose a charge limit, most specific first. */
    private static final String[] CHARGE_LIMIT_PATHS = {
            BATTERY_ROOT + "charge_limit",
            BATTERY_ROOT + "set_charge_limit",
            BATTERY_ROOT + "charge_limit_max",
            BATTERY_ROOT + "constant_charge_limit",
            "/sys/class/thermal/thermal_zone0/charge_limit",
    };

    public static String findChargeLimitNode() {
        for (String path : CHARGE_LIMIT_PATHS) {
            String exists = Shell.execRoot("test -f " + path + " && echo 1 || echo 0").trim();
            if ("1".equals(exists)) {
                return path;
            }
        }
        return null;
    }

    public static int getChargeLimit() {
        String node = findChargeLimitNode();
        if (node == null) {
            return -1;
        }
        String v = Shell.execRoot("cat " + node + " 2>/dev/null").trim();
        try {
            return Integer.parseInt(v.split("\\s+")[0]);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Sets the charging ceiling. Most kernels expect a percentage.
     *
     * @param percent charge limit 0..100
     */
    public static boolean setChargeLimit(int percent) {
        String node = findChargeLimitNode();
        if (node == null) {
            return false;
        }
        int value = Math.max(0, Math.min(100, percent));
        Shell.execRoot("echo " + value + " > " + node);
        return true;
    }

    // ------------------------------------------------------ current limit

    private static final String[] CURRENT_LIMIT_PATHS = {
            BATTERY_ROOT + "constant_charge_current_max",
            BATTERY_ROOT + "input_current_limit",
            BATTERY_ROOT + "current_limit",
            BATTERY_ROOT + "charge_current_max",
            "/sys/class/power_supply/usb/current_limit",
    };

    public static String findCurrentLimitNode() {
        for (String path : CURRENT_LIMIT_PATHS) {
            String exists = Shell.execRoot("test -f " + path + " && echo 1 || echo 0").trim();
            if ("1".equals(exists)) {
                return path;
            }
        }
        return null;
    }

    /**
     * Sets the maximum charging current.
     *
     * @param mA current in milli-amps; values are written in µA where the node expects it
     */
    public static boolean setCurrentLimit(int mA) {
        String node = findCurrentLimitNode();
        if (node == null) {
            return false;
        }
        long value = mA <= 0 ? 0 : mA * 1000L;
        Shell.execRoot("echo " + value + " > " + node);
        return true;
    }

    public static int getCurrentLimit() {
        String node = findCurrentLimitNode();
        if (node == null) {
            return -1;
        }
        String v = Shell.execRoot("cat " + node + " 2>/dev/null").trim();
        try {
            int raw = Integer.parseInt(v.split("\\s+")[0]);
            return raw > 100000 ? raw / 1000 : raw;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // -------------------------------------------------------------- other

    /** True when the device is currently charging. */
    public static boolean isCharging() {
        String status = readString(BATTERY_ROOT + "status");
        if (!status.isEmpty()) {
            return status.equalsIgnoreCase("Charging") || status.equalsIgnoreCase("Full");
        }
        return readStatusFromDump().charging;
    }

    /** Enables/disables battery charging (bypass). */
    public static boolean setChargingEnabled(boolean enabled) {
        String[] paths = {
                "/sys/class/power_supply/battery/enable_charging",
                "/sys/class/power_supply/battery/charging_enabled",
        };
        for (String path : paths) {
            String exists = Shell.execRoot("test -f " + path + " && echo 1 || echo 0").trim();
            if ("1".equals(exists)) {
                Shell.execRoot("echo " + (enabled ? 1 : 0) + " > " + path);
                return true;
            }
        }
        return false;
    }

    /** Lists all power supply nodes with their type. */
    public static List<String> listPowerSupplies() {
        List<String> result = new ArrayList<>();
        String out = Shell.execRoot("ls /sys/class/power_supply 2>/dev/null");
        if (out == null) {
            return result;
        }
        for (String line : out.split("\n")) {
            String t = line.trim();
            if (!t.isEmpty()) {
                result.add(t);
            }
        }
        return result;
    }
}
