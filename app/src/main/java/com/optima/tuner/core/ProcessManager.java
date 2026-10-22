package com.optima.tuner.core;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Process and thread statistics read from /proc.
 */
public final class ProcessManager {

    /** A running process. */
    public static class ProcessInfo {
        public int pid;
        public String name;
        public int uid;
        public long rssKb;
        public float cpuPercent;
        public int threads;
        public String state;
        public String cmdline;
    }

    /** A thread of a process. */
    public static class ThreadInfo {
        public int tid;
        public String name;
        public int state;
        public long rssKb;
        public String schedPolicy;
        public String priority;
    }

    private ProcessManager() {
    }

    /** Lists all processes with their memory usage. */
    public static List<ProcessInfo> listProcesses() {
        List<ProcessInfo> result = new ArrayList<>();
        File[] dirs = new File("/proc").listFiles();
        if (dirs == null) {
            return result;
        }
        Map<Integer, Float> cpuMap = readCpuTimes();
        for (File dir : dirs) {
            String name = dir.getName();
            if (!name.matches("\\d+")) {
                continue;
            }
            int pid;
            try {
                pid = Integer.parseInt(name);
            } catch (NumberFormatException e) {
                continue;
            }
            if (pid == 0) {
                continue;
            }
            ProcessInfo p = new ProcessInfo();
            p.pid = pid;
            p.cmdline = readCmdline(pid);
            p.name = resolveName(pid, p.cmdline);
            p.rssKb = readRss(pid);
            p.threads = countThreads(pid);
            p.state = readState(pid);
            p.uid = readUid(pid);
            Float cpu = cpuMap.get(pid);
            p.cpuPercent = cpu == null ? 0 : cpu;
            result.add(p);
        }
        result.sort(Comparator.comparingDouble((ProcessInfo p) -> p.cpuPercent).reversed());
        return result;
    }

    private static String readCmdline(int pid) {
        try {
            byte[] data = java.nio.file.Files.readAllBytes(new File("/proc/" + pid + "/cmdline").toPath());
            StringBuilder sb = new StringBuilder();
            for (byte b : data) {
                if (b == 0) {
                    break;
                }
                sb.append((char) (b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private static String resolveName(int pid, String cmdline) {
        if (cmdline != null && !cmdline.isEmpty()) {
            return cmdline;
        }
        try {
            byte[] data = java.nio.file.Files.readAllBytes(new File("/proc/" + pid + "/comm").toPath());
            return new String(data).trim();
        } catch (Exception e) {
            return "pid:" + pid;
        }
    }

    private static long readRss(int pid) {
        try {
            byte[] data = java.nio.file.Files.readAllBytes(new File("/proc/" + pid + "/statm").toPath());
            String[] parts = new String(data).trim().split("\\s+");
            if (parts.length > 1) {
                return Long.parseLong(parts[1]) * 4; // pages -> kB
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    private static int countThreads(int pid) {
        try {
            byte[] data = java.nio.file.Files.readAllBytes(new File("/proc/" + pid + "/status").toPath());
            for (String line : new String(data).split("\n")) {
                if (line.startsWith("Threads:")) {
                    return Integer.parseInt(line.substring(8).trim());
                }
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    private static String readState(int pid) {
        try {
            byte[] data = java.nio.file.Files.readAllBytes(new File("/proc/" + pid + "/stat").toPath());
            String s = new String(data);
            int start = s.indexOf('(');
            int end = s.lastIndexOf(')');
            if (start > 0 && end > start) {
                String[] parts = s.substring(end + 2).split("\\s+");
                if (parts.length > 0) {
                    return parts[0];
                }
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private static int readUid(int pid) {
        try {
            java.io.File status = new java.io.File("/proc/" + pid + "/status");
            byte[] data = java.nio.file.Files.readAllBytes(status.toPath());
            for (String line : new String(data).split("\n")) {
                if (line.startsWith("Uid:")) {
                    String[] p = line.trim().split("\\s+");
                    return Integer.parseInt(p[1]);
                }
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    /** Computes per-process CPU usage by diffing two /proc/stat samples. */
    private static Map<Integer, Float> readCpuTimes() {
        Map<Integer, Float> result = new LinkedHashMap<>();
        long total1 = 0;
        long total2 = 0;
        Map<Integer, long[]> before = readAllProcessCpu();
        long t1 = SystemClock();
        try {
            Thread.sleep(400);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        Map<Integer, long[]> after = readAllProcessCpu();
        long t2 = SystemClock();
        long elapsed = Math.max(1, t2 - t1);
        for (Map.Entry<Integer, long[]> e : after.entrySet()) {
            long[] a = e.getValue();
            long[] b = before.get(e.getKey());
            if (b == null) {
                continue;
            }
            long totalDelta = (a[0] - b[0]) + (a[1] - b[1]) + (a[2] - b[2]);
            long idleDelta = (a[3] - b[3]) + (a[4] - b[4]);
            if (totalDelta <= 0) {
                continue;
            }
            result.put(e.getKey(), (float) ((totalDelta - idleDelta) * 100.0 / totalDelta));
        }
        return result;
    }

    private static long SystemClock() {
        return android.os.SystemClock.elapsedRealtime();
    }

    private static Map<Integer, long[]> readAllProcessCpu() {
        Map<Integer, long[]> result = new LinkedHashMap<>();
        File[] dirs = new File("/proc").listFiles();
        if (dirs == null) {
            return result;
        }
        for (File dir : dirs) {
            String name = dir.getName();
            if (!name.matches("\\d+")) {
                continue;
            }
            try {
                byte[] data = java.nio.file.Files.readAllBytes(new File(dir, "stat").toPath());
                String s = new String(data);
                int end = s.lastIndexOf(')');
                if (end < 0) {
                    continue;
                }
                String[] parts = s.substring(end + 2).split("\\s+");
                // after "state" the fields are: ppid pgrp session tty tpgid flags minflt cminflt majflt cmajflt utime stime
                long utime = Long.parseLong(parts[11]);
                long stime = Long.parseLong(parts[12]);
                long cutime = Long.parseLong(parts[13]);
                long cstime = Long.parseLong(parts[14]);
                long[] v = new long[]{utime + stime, cutime + cstime, 0, 0, 0};
                result.put(Integer.parseInt(name), v);
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    /** Lists the threads of a process with their scheduling state. */
    public static List<ThreadInfo> listThreads(int pid) {
        List<ThreadInfo> result = new ArrayList<>();
        File dir = new File("/proc/" + pid + "/task");
        File[] tasks = dir.listFiles();
        if (tasks == null) {
            return result;
        }
        for (File task : tasks) {
            ThreadInfo t = new ThreadInfo();
            try {
                t.tid = Integer.parseInt(task.getName());
            } catch (NumberFormatException e) {
                continue;
            }
            try {
                byte[] comm = java.nio.file.Files.readAllBytes(new File(task, "comm").toPath());
                t.name = new String(comm).trim();
            } catch (Exception ignored) {
            }
            try {
                byte[] stat = java.nio.file.Files.readAllBytes(new File(task, "stat").toPath());
                String s = new String(stat);
                int end = s.lastIndexOf(')');
                String[] parts = s.substring(end + 2).split("\\s+");
                t.state = Integer.parseInt(parts[0]);
            } catch (Exception ignored) {
            }
            t.rssKb = readRss(t.tid);
            t.schedPolicy = readFile(new File(task, "sched_policy")).trim();
            t.priority = readFile(new File(task, "stat")).trim();
            result.add(t);
        }
        result.sort(Comparator.comparingLong((ThreadInfo t) -> t.rssKb).reversed());
        return result;
    }

    private static String readFile(File f) {
        try {
            return new String(java.nio.file.Files.readAllBytes(f.toPath()));
        } catch (Exception e) {
            return "";
        }
    }

    /** Total system memory in bytes. */
    public static long getTotalMemory() {
        try {
            byte[] data = java.nio.file.Files.readAllBytes(new File("/proc/meminfo").toPath());
            for (String line : new String(data).split("\n")) {
                if (line.startsWith("MemTotal:")) {
                    String[] p = line.trim().split("\\s+");
                    return Long.parseLong(p[1]) * 1024;
                }
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    /** Available system memory in bytes. */
    public static long getAvailableMemory() {
        try {
            byte[] data = java.nio.file.Files.readAllBytes(new File("/proc/meminfo").toPath());
            for (String line : new String(data).split("\n")) {
                if (line.startsWith("MemAvailable:")) {
                    String[] p = line.trim().split("\\s+");
                    return Long.parseLong(p[1]) * 1024;
                }
            }
        } catch (Exception ignored) {
        }
        return 0;
    }
}
