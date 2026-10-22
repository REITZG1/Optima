package com.optima.tuner.core;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Installed-application management: listing, freezing, permissions, dex2oat, APK extraction.
 */
public final class AppManager {

    /** A single installed application. */
    public static class AppEntry {
        public String packageName;
        public String label;
        public boolean system;
        public boolean enabled = true;
        public boolean frozen;
        public int uid;
        public long installTime;
        public long updateTime;
        public String sourceDir;
        public String versionName;
        public int versionCode;
    }

    private AppManager() {
    }

    /** Lists installed applications, optionally filtered by system/user. */
    public static List<AppEntry> listApps(Context context, boolean showSystem) {
        List<AppEntry> result = new ArrayList<>();
        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> installed;
        try {
            installed = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        } catch (Exception e) {
            return result;
        }
        for (ApplicationInfo info : installed) {
            boolean isSystem = (info.flags & ApplicationInfo.FLAG_SYSTEM) != 0
                    || (info.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0;
            if (isSystem && !showSystem) {
                continue;
            }
            AppEntry e = new AppEntry();
            e.packageName = info.packageName;
            e.system = isSystem;
            e.uid = info.uid;
            e.sourceDir = info.sourceDir;
            try {
                e.label = pm.getApplicationLabel(info).toString();
            } catch (Exception ex) {
                e.label = info.packageName;
            }
            try {
                e.versionName = pm.getPackageInfo(info.packageName, 0).versionName;
                e.versionCode = pm.getPackageInfo(info.packageName, 0).versionCode;
            } catch (Exception ignored) {
            }
            e.enabled = info.enabled;
            result.add(e);
        }
        return result;
    }

    /** True when the package is currently disabled (frozen) by the package manager. */
    public static boolean isFrozen(String packageName) {
        String out = Shell.execRoot("pm list packages -d " + packageName + " 2>/dev/null");
        if (out == null || out.isEmpty()) {
            return false;
        }
        for (String line : out.split("\n")) {
            if (line.trim().equals("package:" + packageName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Freezes an app by disabling its launcher components.
     *
     * <p>Uses {@code cmd package set-enabled-setting} when available (API 24+) and falls back
     * to {@code pm disable} for the individual launcher entries.
     */
    public static boolean freeze(String packageName) {
        Shell.execRoot("cmd package set-enabled-setting " + packageName + " 0 0 2>&1");
        if (isFrozen(packageName)) {
            return true;
        }
        // Fallback: disable the launcher activities individually.
        String out = Shell.execRoot("cmd package resolve-activity --brief -a android.intent.action.MAIN "
                + "-c android.intent.category.LAUNCHER " + packageName + " 2>/dev/null");
        if (out != null && out.contains("/")) {
            String component = out.trim().split("\n")[0].trim();
            if (component.contains("/")) {
                Shell.execRoot("pm disable --user 0 " + component);
                return true;
            }
        }
        Shell.execRoot("pm disable --user 0 " + packageName);
        return isFrozen(packageName);
    }

    /** Unfreezes an app. */
    public static boolean unfreeze(String packageName) {
        Shell.execRoot("cmd package set-enabled-setting " + packageName + " 1 0 2>&1");
        if (!isFrozen(packageName)) {
            return true;
        }
        Shell.execRoot("pm enable " + packageName);
        return !isFrozen(packageName);
    }

    /** Force-stops an application. */
    public static void forceStop(String packageName) {
        Shell.execRoot("am force-stop " + packageName);
    }

    /** Clears the application data. */
    public static boolean clearData(String packageName) {
        String out = Shell.execRoot("pm clear " + packageName + " 2>&1");
        return out != null && out.toLowerCase().contains("success");
    }

    // ---------------------------------------------------------- app ops

    /** All AppOps operation names supported by this Android version. */
    public static List<String> getAppOps() {
        List<String> result = new ArrayList<>();
        String out = Shell.execRoot("cmd appops get 2>&1");
        if (out == null) {
            return result;
        }
        for (String line : out.split("\n")) {
            String t = line.trim();
            if (t.startsWith("OP_") || t.startsWith("android:")) {
                result.add(t);
            }
        }
        if (result.isEmpty()) {
            result.addAll(defaultAppOps());
        }
        return result;
    }

    private static List<String> defaultAppOps() {
        List<String> ops = new ArrayList<>();
        ops.add("android:coarse_location");
        ops.add("android:fine_location");
        ops.add("android:background_location");
        ops.add("android:read_contacts");
        ops.add("android:write_contacts");
        ops.add("android:get_accounts");
        ops.add("android:read_phone_state");
        ops.add("android:call_phone");
        ops.add("android:answer_phone_calls");
        ops.add("android:read_call_log");
        ops.add("android:write_call_log");
        ops.add("android:add_voicemail");
        ops.add("android:use_sip");
        ops.add("android:process_outgoing_calls");
        ops.add("android:body_sensors");
        ops.add("android:send_sms");
        ops.add("android:receive_sms");
        ops.add("android:read_sms");
        ops.add("android:receive_wap_push");
        ops.add("android:receive_mms");
        ops.add("android:read_excel");
        ops.add("android:write_excel");
        ops.add("android:accept_handover");
        ops.add("android:access_mock_location");
        ops.add("android:read_external_storage");
        ops.add("android:write_external_storage");
        ops.add("android:access_media_location");
        ops.add("android:activity_recognition");
        ops.add("android:post_notification");
        ops.add("android:run_in_background");
        ops.add("android:run_any_in_background");
        ops.add("android:change_background_restrictions");
        return ops;
    }

    /** Reads the current mode of one app op for a package. */
    public static String getAppOp(String packageName, String op) {
        String out = Shell.execRoot("cmd appops get " + packageName + " " + op + " 2>&1");
        if (out == null) {
            return null;
        }
        for (String line : out.split("\n")) {
            String t = line.trim();
            if (t.startsWith(op + ":")) {
                return t.substring(op.length() + 1).trim();
            }
        }
        return null;
    }

    /**
     * Sets an app op mode.
     *
     * @param mode one of allow / ignore / deny / default
     */
    public static boolean setAppOp(String packageName, String op, String mode) {
        String out = Shell.execRoot("cmd appops set " + packageName + " " + op + " " + mode + " 2>&1");
        return out == null || out.toLowerCase().contains("success") || out.trim().isEmpty();
    }

    // ---------------------------------------------------------- dex2oat

    /** Dex2oat compilation modes. */
    public enum DexMode {
        SPEED("speed"),
        VERIFY("verify"),
        EVERYTHING("everything"),
        PROFILE("profile"),
        COMPACT("compact");

        public final String value;

        DexMode(String value) {
            this.value = value;
        }
    }

    /** Runs a dex2oat compilation for the given packages. */
    public static String dex2oat(List<String> packages, DexMode mode, boolean force) {
        StringBuilder sb = new StringBuilder();
        for (String pkg : packages) {
            String path = "/data/app/" + pkg + "/";
            // resolve the real apk path
            String resolved = Shell.execRoot("pm path " + pkg + " 2>/dev/null | head -n 1")
                    .replace("package:", "").trim();
            if (resolved.isEmpty()) {
                continue;
            }
            String out = Shell.execRoot("dex2oat --dex-file=" + resolved
                    + " --oat-file=" + path + "oat/" + pkg + ".odex"
                    + " --instruction-set=arm64 --compiler-filter=" + mode.value
                    + (force ? " --force-dex2oat" : "") + " 2>&1");
            sb.append(out).append('\n');
        }
        return sb.toString();
    }

    // ------------------------------------------------------ APK handling

    /** Copies the base APK of a package into the given target directory. */
    public static boolean extractApk(Context context, String packageName, File targetDir) {
        try {
            PackageManager pm = context.getPackageManager();
            String source = pm.getApplicationInfo(packageName, 0).sourceDir;
            if (source == null || !new File(source).exists()) {
                return false;
            }
            if (!targetDir.exists() && !targetDir.mkdirs()) {
                return false;
            }
            File target = new File(targetDir, packageName + ".apk");
            java.nio.file.Files.copy(new File(source).toPath(), target.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return target.exists();
        } catch (Exception e) {
            return false;
        }
    }

    /** Installs an APK file with root. */
    public static boolean installApk(File apk) {
        String out = Shell.execRoot("pm install -r " + apk.getAbsolutePath() + " 2>&1");
        return out != null && out.toLowerCase().contains("success");
    }

    /** Returns the launch intent for a package. */
    public static android.content.Intent getLaunchIntent(Context context, String packageName) {
        try {
            return context.getPackageManager().getLaunchIntentForPackage(packageName);
        } catch (Exception e) {
            return null;
        }
    }

    /** Total size in bytes of the installed package. */
    public static long getAppSize(Context context, String packageName) {
        try {
            android.content.pm.PackageInfo info = context.getPackageManager()
                    .getPackageInfo(packageName, 0);
            File apk = new File(info.applicationInfo.sourceDir);
            return apk.exists() ? apk.length() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /** Whether the device supports the package manager commands used above. */
    public static boolean supportsSetEnabledSetting() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
    }
}
