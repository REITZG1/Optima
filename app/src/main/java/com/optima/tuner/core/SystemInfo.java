package com.optima.tuner.core;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Static device / SoC information, discovered once and cached.
 */
public final class SystemInfo {
    private static final String TAG = "OptimaSystemInfo";

    /** Path prefix of the cpufreq sysfs tree. */
    public static final String CPU_ROOT = "/sys/devices/system/cpu/";
    public static final String CPUFREQ_ROOT = "/sys/devices/system/cpu/cpu%d/cpufreq/";

    private static final Map<String, String> props = new HashMap<>();
    private static boolean loaded;

    private static int cpuCount;
    private static String socModel = "";
    private static String kernelVersion = "";
    private static final List<Integer> coreClusters = new ArrayList<>();

    private SystemInfo() {
    }

    public static synchronized void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        cpuCount = Build.SUPPORTED_ABIS.length > 0 ? 0 : 0;

        // core count
        String online = Shell.execRoot("cat /sys/devices/system/cpu/online 2>/dev/null");
        if (online != null && !online.isEmpty()) {
            String first = online.trim().split(",")[0].split("-")[0];
            try {
                cpuCount = Integer.parseInt(first);
            } catch (NumberFormatException ignored) {
            }
        }
        if (cpuCount <= 0) {
            File[] files = new File(CPU_ROOT).listFiles();
            int max = 0;
            if (files != null) {
                for (File f : files) {
                    String n = f.getName();
                    if (n.startsWith("cpu") && n.length() > 3) {
                        try {
                            max = Math.max(max, Integer.parseInt(n.substring(3)));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }
            cpuCount = max + 1;
        }

        // SoC model
        for (String p : new String[]{
                "ro.soc.model", "ro.hardware", "ro.board.platform", "ro.product.board"}) {
            String v = getProp(p);
            if (v != null && !v.isEmpty() && !"unknown".equals(v)) {
                socModel = v;
                break;
            }
        }
        if (socModel.isEmpty()) {
            socModel = Build.HARDWARE;
        }
        kernelVersion = getProp("ro.build.version.release") + " / " + getProp("ro.build.display.id");
        loadClusters();
    }

    /** Reads {@code getprop <name>} with root, falling back to the plain shell. */
    public static String getProp(String name) {
        synchronized (props) {
            if (props.containsKey(name)) {
                return props.get(name);
            }
        }
        String value = Shell.execRoot("getprop " + name);
        if (value == null || value.trim().isEmpty()) {
            value = Shell.exec("getprop " + name);
        }
        value = value == null ? "" : value.trim();
        synchronized (props) {
            props.put(name, value);
        }
        return value;
    }

    public static void reloadProps() {
        synchronized (props) {
            props.clear();
        }
    }

    /**
     * Determines which CPU cores belong to the same cpufreq policy.
     * Cores sharing a {@code scaling_governor} directory (i.e. a policy) form a cluster.
     */
    private static void loadClusters() {
        coreClusters.clear();
        Map<String, Integer> policyFirst = new HashMap<>();
        for (int i = 0; i < cpuCount; i++) {
            String path = String.format(CPUFREQ_ROOT, i) + "scaling_governor";
            String exists = Shell.execRoot("test -f " + path + " && echo 1 || echo 0").trim();
            if (!"1".equals(exists)) {
                continue;
            }
            // policy shared device: use the "related_cpus" / "affected_cpus" node
            String related = Shell.execRoot("cat " + String.format(CPUFREQ_ROOT, i) + "related_cpus 2>/dev/null").trim();
            String key;
            if (related.isEmpty()) {
                related = Shell.execRoot("cat " + String.format(CPUFREQ_ROOT, i) + "affected_cpus 2>/dev/null").trim();
            }
            if (!related.isEmpty()) {
                key = related;
            } else {
                key = String.valueOf(i);
            }
            Integer first = policyFirst.get(key);
            if (first == null) {
                policyFirst.put(key, i);
                coreClusters.add(i);
            }
        }
    }

    public static int getCpuCount() {
        load();
        return cpuCount;
    }

    public static String getSocModel() {
        load();
        return socModel;
    }

    public static String getKernelVersion() {
        load();
        return kernelVersion;
    }

    /** First core index of every CPU cluster/policy. */
    public static List<Integer> getClusterFirstCores() {
        load();
        return new ArrayList<>(coreClusters);
    }

    public static String getDeviceModel() {
        return Build.MANUFACTURER + " " + Build.MODEL;
    }

    public static int getAndroidSdkInt() {
        return Build.VERSION.SDK_INT;
    }

    /** Battery capacity in mAh, estimated from the charge-full node. */
    public static int getBatteryCapacity(Context context) {
        String chargeFull = Shell.execRoot("cat /sys/class/power_supply/battery/charge_full 2>/dev/null").trim();
        if (!chargeFull.isEmpty()) {
            try {
                // charge_full is in uAh on most devices
                long value = Long.parseLong(chargeFull);
                if (value > 100000) {
                    return (int) (value / 1000);
                }
                return (int) value;
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
    }

    public static boolean isMiui() {
        String v = getProp("ro.miui.ui.version.name");
        return !v.isEmpty();
    }

    public static boolean isOplus() {
        String v = getProp("ro.build.version.opporom");
        return !v.isEmpty() || "oplus".equalsIgnoreCase(getProp("ro.product.brand"));
    }

    public static boolean isVivo() {
        String brand = getProp("ro.product.brand").toLowerCase();
        return brand.contains("vivo") || brand.contains("iqoo");
    }

    public static boolean isSamsung() {
        return "samsung".equalsIgnoreCase(getProp("ro.product.brand"));
    }

    public static boolean isOnePlus() {
        return "oneplus".equalsIgnoreCase(getProp("ro.product.brand"));
    }

    public static void logAll() {
        Log.d(TAG, "soc=" + getSocModel() + " cores=" + getCpuCount() + " clusters=" + coreClusters);
    }
}
