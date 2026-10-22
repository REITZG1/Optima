package com.optima.tuner.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CPU frequency / governor / online-core control via sysfs.
 *
 * <p>All frequencies exposed by this class are in <b>kHz</b>; the kernel sysfs
 * nodes use Hz, so the conversion happens here.
 */
public final class CpuManager {

    /** One cpufreq policy group (a "cluster" of cores sharing a policy). */
    public static class Cluster {
        /** Lowest core number belonging to this cluster. */
        public final int firstCore;
        /** Representative sysfs directory of the cluster. */
        public final String policyPath;
        /** Every sysfs directory of the cluster; writes go to all of them. */
        public final List<String> policyPaths = new ArrayList<>();
        /** Cores belonging to this cluster. */
        public final List<Integer> cores = new ArrayList<>();

        public String governor = "";
        /** kHz */
        public int minFreq;
        /** kHz */
        public int maxFreq;
        /** kHz */
        public int hardwareMin;
        /** kHz */
        public int hardwareMax;
        /** kHz */
        public int currentFreq;
        /** kHz */
        public List<Integer> availableFrequencies = new ArrayList<>();
        public List<String> availableGovernors = new ArrayList<>();
        /** Raw {@code related_cpus} value, e.g. "0 1 2 3". */
        public String relatedCpus = "";
        /** True when the governor node accepts writes. */
        public boolean governorWritable = true;
        /** True when the minimum-frequency node accepts writes. */
        public boolean minWritable = true;
        /** True when the maximum-frequency node accepts writes. */
        public boolean maxWritable = true;
        /** MTK "sugov_ext" ramp-up limit in microseconds (-1 when unsupported). */
        public int upRateLimit = -1;
        /** MTK "sugov_ext" ramp-down limit in microseconds (-1 when unsupported). */
        public int downRateLimit = -1;

        Cluster(int firstCore, String policyPath) {
            this.firstCore = firstCore;
            this.policyPath = policyPath;
        }

        public boolean isValid() {
            return !governor.isEmpty();
        }

        /** Highest available frequency in kHz, or 0 when unknown. */
        public int getTopFrequency() {
            int top = 0;
            for (int f : availableFrequencies) {
                if (f > top) {
                    top = f;
                }
            }
            return top > 0 ? top : hardwareMax;
        }

        /** Lowest available frequency in kHz, or 0 when unknown. */
        public int getBottomFrequency() {
            int low = 0;
            for (int f : availableFrequencies) {
                if (low == 0 || f < low) {
                    low = f;
                }
            }
            return low > 0 ? low : hardwareMin;
        }
    }

    private static final List<Cluster> clusters = new ArrayList<>();
    private static boolean loaded;

    private CpuManager() {
    }

    public static synchronized List<Cluster> getClusters() {
        load();
        return new ArrayList<>(clusters);
    }

    /**
     * Reads every cpufreq policy of the device.
     *
     * <p>Uses a single shell invocation so that devices with many cores do not
     * spawn one process per core. Cores are grouped into clusters by their
     * {@code related_cpus} value, because the kernel shares one policy between
     * all cores of a cluster even though each core has its own sysfs folder.
     */
    public static synchronized void load() {
        clusters.clear();
        loaded = true;

        String out = Shell.execRoot(
                "for c in /sys/devices/system/cpu/cpu[0-9]*/cpufreq "
                        + "/sys/devices/system/cpu/cpufreq/policy[0-9]*; do "
                        + "test -d \"$c\" || continue; "
                        + "echo \"${c%/cpufreq}|${c%/policy}|"
                        + "$(cat $c/scaling_governor 2>/dev/null)|"
                        + "$(cat $c/scaling_min_freq 2>/dev/null)|"
                        + "$(cat $c/scaling_max_freq 2>/dev/null)|"
                        + "$(cat $c/cpuinfo_min_freq 2>/dev/null)|"
                        + "$(cat $c/cpuinfo_max_freq 2>/dev/null)|"
                        + "$(cat $c/scaling_cur_freq 2>/dev/null)|"
                        + "$(cat $c/related_cpus 2>/dev/null)|"
                        + "$(cat $c/scaling_available_governors 2>/dev/null)|"
                        + "$(cat $c/scaling_available_frequencies 2>/dev/null)|"
                        + "$(stat -c %A $c/scaling_governor 2>/dev/null)|"
                        + "$(stat -c %A $c/scaling_min_freq 2>/dev/null)|"
                        + "$(stat -c %A $c/scaling_max_freq 2>/dev/null)|"
                        + "$(cat $c/sugov_ext/up_rate_limit_us 2>/dev/null)|"
                        + "$(cat $c/sugov_ext/down_rate_limit_us 2>/dev/null)\"; done");

        if (out == null || out.isEmpty()) {
            loadPerCore();
            return;
        }

        // Key: the set of cores that share a policy.
        Map<String, Cluster> byGroup = new LinkedHashMap<>();
        for (String line : out.split("\n")) {
            String t = line.trim();
            if (t.isEmpty()) {
                continue;
            }
            String[] p = t.split("\\|");
            if (p.length < 3 || p[2].isEmpty()) {
                continue;
            }
            // p[0] and p[1] are the two spellings of the same directory.
            String policyPath = !p[0].isEmpty() ? p[0] : p[1];
            int core = coreOf(policyPath);
            if (core < 0) {
                continue;
            }
            String related = p.length > 8 ? p[8].trim() : "";
            String key = related.isEmpty() ? policyPath : related;

            Cluster c = byGroup.get(key);
            if (c == null) {
                c = new Cluster(core, policyPath);
                byGroup.put(key, c);
            }
            if (!c.policyPaths.contains(policyPath)) {
                c.policyPaths.add(policyPath);
            }
            if (!c.cores.contains(core)) {
                c.cores.add(core);
            }
            c.governor = p[2];
            c.minFreq = intAt(p, 3) / 1000;
            c.maxFreq = intAt(p, 4) / 1000;
            c.hardwareMin = intAt(p, 5) / 1000;
            c.hardwareMax = intAt(p, 6) / 1000;
            c.currentFreq = intAt(p, 7) / 1000;
            if (!related.isEmpty()) {
                c.relatedCpus = related;
            }
            if (p.length > 9) {
                for (String g : p[9].split("\\s+")) {
                    if (!g.isEmpty() && !c.availableGovernors.contains(g)) {
                        c.availableGovernors.add(g);
                    }
                }
            }
            if (p.length > 10) {
                for (String f : p[10].split("\\s+")) {
                    try {
                        int khz = Integer.parseInt(f) / 1000;
                        if (khz > 0 && !c.availableFrequencies.contains(khz)) {
                            c.availableFrequencies.add(khz);
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            if (p.length > 13) {
                c.governorWritable = isWritable(p[11]);
                c.minWritable = isWritable(p[12]);
                c.maxWritable = isWritable(p[13]);
            }
            if (p.length > 15) {
                c.upRateLimit = p[14].isEmpty() ? -1 : intAt(p, 14);
                c.downRateLimit = p[15].isEmpty() ? -1 : intAt(p, 15);
            }
        }
        clusters.addAll(byGroup.values());
        for (Cluster c : clusters) {
            c.cores.sort(Integer::compare);
        }
    }

    /**
     * Interprets the permission string of {@code stat -c %A}.
     *
     * <p>The owner write bit is what matters: the vendor often turns the
     * frequency nodes read-only to keep its own governor in charge.
     *
     * <p>Layout: {@code -rw-rw----} → index 0 is the type, index 2 the
     * owner write bit.
     */
    static boolean isWritable(String mode) {
        if (mode == null || mode.length() < 10) {
            return true;
        }
        return mode.charAt(2) == 'w';
    }

    /** Fallback probing when the bulk read fails. */
    private static void loadPerCore() {
        Map<String, Cluster> byGroup = new LinkedHashMap<>();
        int cpuCount = SystemInfo.getCpuCount();
        for (int i = 0; i < cpuCount; i++) {
            String base = String.format(SystemInfo.CPUFREQ_ROOT, i);
            String governor = Shell.execRoot("cat " + base + "scaling_governor 2>/dev/null").trim();
            if (governor.isEmpty()) {
                continue;
            }
            String policyPath = base.substring(0, base.length() - 1);
            String related = Shell.execRoot("cat " + base + "related_cpus 2>/dev/null").trim();
            String key = related.isEmpty() ? policyPath : related;

            Cluster c = byGroup.get(key);
            if (c == null) {
                c = new Cluster(i, policyPath);
                byGroup.put(key, c);
            }
            if (!c.policyPaths.contains(policyPath)) {
                c.policyPaths.add(policyPath);
            }
            if (!c.cores.contains(i)) {
                c.cores.add(i);
            }
            c.governor = governor;
            c.relatedCpus = related;
        }
        clusters.addAll(byGroup.values());
    }

    /**
     * Extracts the core number from a cpufreq path such as
     * {@code /sys/devices/system/cpu/cpu3/cpufreq} or
     * {@code /sys/devices/system/cpu/policy2}.
     */
    static int coreOf(String policyPath) {
        if (policyPath == null) {
            return -1;
        }
        int slash = policyPath.lastIndexOf('/');
        if (slash < 0) {
            return -1;
        }
        String name = policyPath.substring(slash + 1);
        int digits = -1;
        if (name.startsWith("cpu")) {
            digits = 3;
        } else if (name.startsWith("policy")) {
            digits = 6;
        }
        if (digits < 0) {
            return -1;
        }
        try {
            return Integer.parseInt(name.substring(digits));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static int intAt(String[] parts, int index) {
        if (index >= parts.length) {
            return 0;
        }
        try {
            return Integer.parseInt(parts[index].trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Re-reads the live values of every cluster. */
    public static synchronized void refresh() {
        if (!loaded) {
            load();
            return;
        }
        for (Cluster c : clusters) {
            String base = c.policyPath;
            c.governor = Shell.execRoot("cat " + base + "/scaling_governor 2>/dev/null").trim();
            c.minFreq = readInt(base + "/scaling_min_freq") / 1000;
            c.maxFreq = readInt(base + "/scaling_max_freq") / 1000;
            c.hardwareMin = readInt(base + "/cpuinfo_min_freq") / 1000;
            c.hardwareMax = readInt(base + "/cpuinfo_max_freq") / 1000;
            c.currentFreq = readInt(base + "/scaling_cur_freq") / 1000;
            if (c.currentFreq == 0) {
                c.currentFreq = readInt(base + "/cpuinfo_cur_freq") / 1000;
            }
            c.availableFrequencies.clear();
            String freqs = Shell.execRoot("cat " + base + "/scaling_available_frequencies 2>/dev/null").trim();
            if (!freqs.isEmpty()) {
                for (String f : freqs.split("\\s+")) {
                    try {
                        c.availableFrequencies.add(Integer.parseInt(f) / 1000);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            c.availableGovernors.clear();
            String govs = Shell.execRoot("cat " + base + "/scaling_available_governors 2>/dev/null").trim();
            if (!govs.isEmpty()) {
                for (String g : govs.split("\\s+")) {
                    if (!g.isEmpty()) {
                        c.availableGovernors.add(g);
                    }
                }
            }
        }
    }

    private static int readInt(String path) {
        String v = Shell.execRoot("cat " + path + " 2>/dev/null").trim();
        if (v.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(v.split("\\s+")[0]);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // ------------------------------------------------------------- setters

    /**
     * Applies a governor to every policy of the cluster.
     *
     * @return true when the new governor could be read back
     */
    public static boolean setGovernor(Cluster cluster, String governor) {
        if (cluster == null || governor == null || governor.isEmpty()) {
            return false;
        }
        StringBuilder sb = new StringBuilder();
        for (String path : cluster.policyPaths) {
            sb.append("echo ").append(governor).append(" > ").append(path)
                    .append("/scaling_governor 2>/dev/null; ");
        }
        Shell.execRoot(sb.toString());
        String check = Shell.execRoot("cat " + cluster.policyPath + "/scaling_governor").trim();
        return governor.equals(check);
    }

    /** Sets the minimum frequency of the cluster. @param khz frequency in kHz */
    public static boolean setMinFrequency(Cluster cluster, int khz) {
        return writeAll(cluster, "scaling_min_freq", khz * 1000L);
    }

    /** Sets the maximum frequency of the cluster. @param khz frequency in kHz */
    public static boolean setMaxFrequency(Cluster cluster, int khz) {
        return writeAll(cluster, "scaling_max_freq", khz * 1000L);
    }

    /**
     * Sets both limits of the cluster.
     *
     * <p>The upper limit is written first, because lowering the ceiling fails
     * when the current frequency is above the new maximum.
     *
     * @param minKhz minimum frequency in kHz
     * @param maxKhz maximum frequency in kHz
     */
    public static boolean setFrequencyRange(Cluster cluster, int minKhz, int maxKhz) {
        if (cluster == null) {
            return false;
        }
        boolean ok = setMaxFrequency(cluster, maxKhz);
        ok &= setMinFrequency(cluster, minKhz);
        return ok;
    }

    /** Writes a value to every policy directory of the cluster. */
    private static boolean writeAll(Cluster cluster, String node, long value) {
        if (cluster == null) {
            return false;
        }
        StringBuilder sb = new StringBuilder();
        for (String path : cluster.policyPaths) {
            sb.append("echo ").append(value).append(" > ").append(path)
                    .append("/").append(node).append(" 2>/dev/null; ");
        }
        Shell.execRoot(sb.toString());
        long check = readInt(cluster.policyPath + "/" + node);
        return check == value;
    }

    /**
     * Sets the MTK "sugov_ext" ramp-up limit.
     *
     * <p>Lower values make the governor react faster to load increases.
     *
     * @param microseconds ramp-up limit in microseconds
     */
    public static boolean setUpRateLimit(Cluster cluster, int microseconds) {
        return writeAll(cluster, "sugov_ext/up_rate_limit_us", microseconds);
    }

    /**
     * Sets the MTK "sugov_ext" ramp-down limit.
     *
     * @param microseconds ramp-down limit in microseconds
     */
    public static boolean setDownRateLimit(Cluster cluster, int microseconds) {
        return writeAll(cluster, "sugov_ext/down_rate_limit_us", microseconds);
    }

    /** True when the cluster exposes the MTK sugov_ext rate limits. */
    public static boolean supportsRateLimits(Cluster cluster) {
        return cluster != null && cluster.upRateLimit >= 0 && cluster.downRateLimit >= 0;
    }

    /** Sets the governor of a single core. */
    public static boolean setGovernor(int core, String governor) {
        String path = String.format(SystemInfo.CPUFREQ_ROOT, core) + "scaling_governor";
        Shell.execRoot("echo " + governor + " > " + path);
        return true;
    }

    // -------------------------------------------------------- online cores

    public static List<Integer> getOnlineCores() {
        List<Integer> cores = new ArrayList<>();
        String out = Shell.execRoot("cat /sys/devices/system/cpu/online 2>/dev/null").trim();
        if (out.isEmpty()) {
            out = Shell.execRoot("for i in /sys/devices/system/cpu/cpu[0-9]*; do " +
                    "test -f $i/online && echo $(basename $i) | cut -c4-; done").trim();
        }
        for (String s : out.split("[\\s,]+")) {
            if (s.contains("-")) {
                try {
                    String[] p = s.split("-");
                    int from = Integer.parseInt(p[0]);
                    int to = Integer.parseInt(p[1]);
                    for (int i = from; i <= to; i++) {
                        cores.add(i);
                    }
                } catch (NumberFormatException ignored) {
                }
            } else if (!s.isEmpty()) {
                try {
                    cores.add(Integer.parseInt(s));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return cores;
    }

    public static boolean setOnlineCores(List<Integer> cores) {
        if (cores == null || cores.isEmpty()) {
            return false;
        }
        // offline first (high to low), then online
        int cpuCount = SystemInfo.getCpuCount();
        List<Integer> sorted = new ArrayList<>(cores);
        sorted.sort((a, b) -> Integer.compare(b, a));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cpuCount; i++) {
            if (!cores.contains(i)) {
                sb.append("echo 0 > /sys/devices/system/cpu/cpu").append(i).append("/online 2>/dev/null; ");
            }
        }
        for (int i : sorted) {
            sb.append("echo 1 > /sys/devices/system/cpu/cpu").append(i).append("/online 2>/dev/null; ");
        }
        Shell.execRoot(sb.toString());
        return true;
    }

    // -------------------------------------------------------------- loads

    /**
     * Per-core utilisation percentages, computed from two samples of /proc/stat.
     *
     * <p>Must be called from a background thread: it takes two samples 320 ms apart.
     */
    public static int[] getCpuUsage() {
        long[][] first = readStat();
        try {
            Thread.sleep(320);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
            return new int[0];
        }
        long[][] second = readStat();

        int count = Math.min(first.length, second.length);
        int[] usage = new int[count];
        for (int i = 0; i < count; i++) {
            long[] a = first[i];
            long[] b = second[i];
            int n = Math.min(a.length, b.length);
            if (n < 4) {
                usage[i] = 0;
                continue;
            }
            long total = 0;
            for (int j = 0; j < n; j++) {
                total += b[j] - a[j];
            }
            // fields: user, nice, system, idle, iowait, irq, softirq, steal, ...
            long idle = (b[3] - a[3]) + (n > 4 ? b[4] - a[4] : 0);
            if (total <= 0) {
                usage[i] = 0;
            } else {
                usage[i] = (int) Math.max(0, Math.min(100, ((total - idle) * 100) / total));
            }
        }
        return usage;
    }

    /**
     * Reads the per-CPU counters from /proc/stat.
     *
     * @return one array per core: {user, nice, system, idle, iowait, ...}
     */
    private static long[][] readStat() {
        String out = Shell.execRoot("cat /proc/stat");
        if (out == null || out.isEmpty()) {
            return new long[0][];
        }
        List<long[]> rows = new ArrayList<>();
        for (String line : out.split("\n")) {
            String t = line.trim();
            if (!t.startsWith("cpu")) {
                continue;
            }
            String[] parts = t.split("\\s+");
            if (parts.length < 5) {
                continue;
            }
            // Skip the aggregate "cpu " line, keep only "cpu0", "cpu1", ...
            if (!parts[0].matches("cpu\\d+")) {
                continue;
            }
            long[] v = new long[parts.length - 1];
            for (int i = 1; i < parts.length; i++) {
                try {
                    v[i - 1] = Long.parseLong(parts[i]);
                } catch (NumberFormatException ignored) {
                }
            }
            rows.add(v);
        }
        return rows.toArray(new long[0][]);
    }
}
