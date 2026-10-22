package com.optima.tuner.core;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Root / shell access layer.
 *
 * <p>Two execution paths are provided:
 * <ul>
 *     <li>a <b>persistent</b> root shell ({@code su}) kept alive for the whole process
 *         lifetime, used for the frequent polling loops,</li>
 *     <li>one-shot {@code su -c <cmd>} invocations, used for infrequent commands.</li>
 * </ul>
 *
 * <p>Automatically discovers the {@code su} binary in common locations
 * (/system/bin/su, /system/xbin/su, /sbin/su, /vendor/bin/su, /system/sbin/su)
 * to work with Magisk, KernelSU, SuperSU, and other root solutions.
 */
public final class Shell {
    private static final String TAG = "OptimaShell";

    /** Marker used to detect the end of a command's output on the persistent shell. */
    private static final String ECHO_MARKER = "OPTIMA_EOF_MARKER";

    /** Common locations for the su binary. */
    private static final String[] SU_PATHS = {
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/vendor/bin/su",
            "/system/sbin/su",
            "/system/bin/.magisk/busybox",
            "/data/local/tmp/su",
            "/data/adb/magisk/su",
    };

    private static final Object SU_LOCK = new Object();

    /** Minimum delay between two failed su lookups. */
    private static final long SU_RETRY_INTERVAL_MS = 2000;

    private static String suBinary = null;
    private static boolean suProbeDone;
    private static long suProbeTime;
    private static Process process;
    private static BufferedWriter shellIn;
    private static BufferedReader shellOut;
    private static boolean rootGranted;
    private static boolean rootChecked;
    private static RootStatus rootStatus;

    /** Output plus exit status of a finished command. */
    public static final class Result {
        public final String output;
        public final int exitCode;

        Result(String output, int exitCode) {
            this.output = output;
            this.exitCode = exitCode;
        }
    }

    /** Outcome of a root probe. */
    public static final class RootStatus {
        /** True when the app can run commands as uid 0. */
        public final boolean granted;
        /** The su invocation that succeeded (or was attempted). */
        public final String method;
        /** Human readable explanation of the outcome. */
        public final String detail;
        /** Exit code of the last attempt. */
        public final int exitCode;
        /** Actionable next step for the user, or an empty string. */
        public final String hint;

        RootStatus(boolean granted, String method, String detail, int exitCode) {
            this(granted, method, detail, exitCode, "");
        }

        RootStatus(boolean granted, String method, String detail, int exitCode, String hint) {
            this.granted = granted;
            this.method = method;
            this.detail = detail;
            this.exitCode = exitCode;
            this.hint = hint;
        }

        /** Short label for the UI, e.g. "Granted" or "Denied". */
        public String label() {
            return granted ? "Granted" : "Denied";
        }
    }

    private static final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "optima-shell");
        t.setDaemon(true);
        return t;
    });

    private Shell() {
    }

    public interface Callback {
        void onResult(String result);
    }

    public interface LineCallback {
        void onLine(String line);
    }

    // ------------------------------------------------------------------ root

    /**
     * Returns the discovered su binary path, or "su" as fallback.
     *
     * <p>Root managers such as KernelSU only make the binary visible to the
     * app once root has been granted, and the visibility can lag a little
     * behind the process start. A failed lookup is therefore retried every
     * {@link #SU_RETRY_INTERVAL_MS} milliseconds instead of being cached for
     * the whole process lifetime, so granting root in the root manager is
     * picked up automatically.
     */
    public static String getSuBinary() {
        synchronized (SU_LOCK) {
            if (suBinary != null) {
                return suBinary;
            }
            long now = android.os.SystemClock.elapsedRealtime();
            if (suProbeDone && now - suProbeTime < SU_RETRY_INTERVAL_MS) {
                return "su";
            }
            suProbeTime = now;
            for (String path : SU_PATHS) {
                java.io.File f = new java.io.File(path);
                if (f.exists() && f.canExecute()) {
                    suBinary = path;
                    suProbeDone = true;
                    Log.d(TAG, "Found su binary at: " + path);
                    return suBinary;
                }
            }
            // Some ROMs ship a non-executable placeholder; fall back to a
            // plain existence check before giving up.
            for (String path : SU_PATHS) {
                if (new java.io.File(path).exists()) {
                    suBinary = path;
                    suProbeDone = true;
                    Log.d(TAG, "Found su binary at: " + path + " (not executable, using anyway)");
                    return suBinary;
                }
            }
            suProbeDone = true;
            Log.w(TAG, "su binary not found in known locations, falling back to 'su'");
            return "su";
        }
    }

    /**
     * Discards the cached su location so the next call probes again.
     */
    public static void resetSuBinary() {
        synchronized (SU_LOCK) {
            suBinary = null;
            suProbeDone = false;
            suProbeTime = 0;
        }
    }

    public static boolean isRootAvailable() {
        if (rootChecked) {
            return rootGranted;
        }
        rootStatus = probeRoot();
        rootGranted = rootStatus.granted;
        rootChecked = true;
        return rootGranted;
    }

    /** Detailed result of the last root probe. */
    public static RootStatus getRootStatus() {
        if (!rootChecked) {
            isRootAvailable();
        }
        return rootStatus;
    }

    /**
     * Probes for root access.
     *
     * <p>Different root solutions accept different argument styles, so several
     * are tried in turn: {@code su -c <cmd>}, {@code su 0 <cmd>} and
     * {@code su -c <abs path>}.
     */
    private static RootStatus probeRoot() {
        String su = getSuBinary();

        String[][] attempts = {
                {su, "-c", "id"},
                {su, "0", "id"},
                {su, "-c", "/system/bin/id"},
                {su, "-c", "id -u"},
        };

        Result last = null;
        for (String[] argv : attempts) {
            Result r = execWithCode(argv);
            last = r;
            String out = r.output.trim();
            if (out.contains("uid=0") || out.equals("0")) {
                return new RootStatus(true, argv[0] + " " + argv[1], out, r.exitCode);
            }
        }

        // The su binary may only become visible after root was granted in the
        // root manager, so probe the known locations once more before giving up.
        if (last != null && last.exitCode == -1) {
            resetSuBinary();
            String again = getSuBinary();
            if (!again.equals(su)) {
                Result r = execWithCode(new String[]{again, "-c", "id"});
                if (r.output.contains("uid=0")) {
                    return new RootStatus(true, again + " -c", r.output.trim(), r.exitCode);
                }
            }
        }

        String detail;
        String hint;
        if (last == null) {
            detail = "su could not be started";
            hint = "Open your root manager and allow Optima, then tap Re-check.";
        } else if (last.exitCode == -1) {
            detail = "su binary not visible to the app yet";
            hint = "Open your root manager, allow Optima, then tap Re-check.";
        } else if (last.output.contains("not found") || last.output.contains("No such file")) {
            detail = "su binary not found";
            hint = "Install or enable a root solution, then tap Re-check.";
        } else if (!last.output.isEmpty()) {
            detail = "su replied: " + last.output.trim();
            hint = "Open your root manager and allow Optima, then tap Re-check.";
        } else {
            detail = "root request denied (exit code " + last.exitCode + ")";
            hint = "Open your root manager and allow Optima, then tap Re-check.";
        }
        return new RootStatus(false, su, detail, last == null ? -1 : last.exitCode, hint);
    }

    public static void resetRootCheck() {
        rootChecked = false;
        rootGranted = false;
        rootStatus = null;
        resetSuBinary();
    }

    // ------------------------------------------------------------ one shot

    /** Runs {@code cmd} through the device shell (no root). */
    public static String exec(String cmd) {
        return execWith(new String[]{"sh", "-c", cmd});
    }

    /** Runs {@code cmd} as root via {@code su -c}. */
    public static String execRoot(String cmd) {
        String su = getSuBinary();
        return execWith(new String[]{su, "-c", cmd});
    }

    private static String execWith(String[] argv) {
        return execWithCode(argv).output;
    }

    /**
     * Runs a command and returns its combined output together with the exit code.
     *
     * <p>Never throws: a failure to start the process yields an empty output
     * and exit code {@code -1}.
     */
    private static Result execWithCode(String[] argv) {
        Process p = null;
        try {
            p = new ProcessBuilder(argv).redirectErrorStream(true).start();
            String out = readAll(p.getInputStream());
            int code = p.waitFor();
            return new Result(out, code);
        } catch (Exception e) {
            Log.d(TAG, "exec failed: " + e.getMessage() + " (cmd: " + Arrays.toString(argv) + ")");
            return new Result("", -1);
        } finally {
            if (p != null) {
                p.destroy();
            }
        }
    }

    private static String readAll(java.io.InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }

    /** Root exec returning the trimmed first line, or {@code null}. */
    public static String execRootLine(String cmd) {
        String out = execRoot(cmd);
        if (out == null || out.isEmpty()) {
            return null;
        }
        String line = out.split("\n")[0].trim();
        return line.isEmpty() ? null : line;
    }

    /** Root exec returning every non-empty trimmed line. */
    public static List<String> execRootLines(String cmd) {
        List<String> result = new ArrayList<>();
        String out = execRoot(cmd);
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

    // --------------------------------------------------------- persistent

    /** Runs {@code cmd} on the long-lived root shell. */
    public static synchronized String execPersistent(String cmd) {
        try {
            ensureShell();
            String marker = ECHO_MARKER + "_" + UUID.randomUUID().toString().replace("-", "");
            shellIn.write(cmd);
            shellIn.write("\n");
            shellIn.write("echo " + marker + "\n");
            shellIn.flush();

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = shellOut.readLine()) != null) {
                if (line.startsWith(ECHO_MARKER)) {
                    break;
                }
                sb.append(line).append('\n');
            }
            return sb.toString();
        } catch (Exception e) {
            Log.w(TAG, "persistent exec failed: " + e.getMessage());
            closeShell();
            return "";
        }
    }

    public static synchronized String execPersistentLine(String cmd) {
        String out = execPersistent(cmd);
        if (out == null || out.isEmpty()) {
            return null;
        }
        String line = out.split("\n")[0].trim();
        return line.isEmpty() ? null : line;
    }

    private static void ensureShell() throws IOException {
        if (process != null && process.isAlive() && shellIn != null && shellOut != null) {
            return;
        }
        closeShell();
        String su = getSuBinary();
        process = new ProcessBuilder(su).redirectErrorStream(false).start();
        shellIn = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
        shellOut = new BufferedReader(new InputStreamReader(process.getInputStream()));
        java.io.InputStream errStream = process.getErrorStream();
        // Drain stderr so the pipe never blocks the root shell.
        Thread drain = new Thread(() -> {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(errStream))) {
                while (br.readLine() != null) {
                    // discard
                }
            } catch (IOException ignored) {
            }
        }, "optima-shell-err");
        drain.setDaemon(true);
        drain.start();
    }

    public static synchronized void closeShell() {
        if (process != null) {
            try {
                process.destroy();
            } catch (Exception ignored) {
            }
        }
        process = null;
        shellIn = null;
        shellOut = null;
    }

    // -------------------------------------------------------------- async

    public static void execAsync(String cmd, Callback callback) {
        executor.execute(() -> {
            String result = execRoot(cmd);
            if (callback != null) {
                callback.onResult(result);
            }
        });
    }

    public static void execPersistentAsync(String cmd, Callback callback) {
        executor.execute(() -> {
            String result = execPersistent(cmd);
            if (callback != null) {
                callback.onResult(result);
            }
        });
    }

    /** Streams each output line of a root command to the callback. */
    public static void execStream(String cmd, LineCallback callback) {
        executor.execute(() -> {
            try {
                ensureShell();
                shellIn.write(cmd);
                shellIn.write("\n");
                shellIn.flush();
                String line;
                while ((line = shellOut.readLine()) != null) {
                    callback.onLine(line);
                }
            } catch (Exception e) {
                Log.w(TAG, "stream failed: " + e.getMessage());
                closeShell();
            }
        });
    }
}