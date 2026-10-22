package com.optima.tuner.core;

import android.content.Context;
import android.content.Intent;

import java.util.List;

/**
 * Helpers to find and open the installed root manager.
 *
 * <p>Root managers differ a lot in package name, and some of them hide
 * themselves behind a spoofed system-app name, so the app is looked up in
 * several ways: a list of known package names, the apps that handle the
 * {@code su} binary, and finally anything that looks like a superuser app.
 */
public final class RootManager {

    /** Package names of the common root managers. */
    private static final String[] KNOWN_PACKAGES = {
            "df.root",                            // KernelSU Next
            "io.github.rifsxd.ksunext",            // KernelSU Next
            "com.rifsxd.ksunext",
            "me.weishu.kernelsu",                 // KernelSU
            "com.topjohnwu.magisk",               // Magisk
            "me.bmax.apatch",                     // APatch
            "com.topjohnwu.magisk.alpha",
    };

    private RootManager() {
    }

    /**
     * Returns the launch intent of an installed root manager, or {@code null}.
     */
    public static Intent findManager(Context context) {
        if (context == null) {
            return null;
        }
        for (String pkg : KNOWN_PACKAGES) {
            Intent intent = context.getPackageManager()
                    .getLaunchIntentForPackage(pkg);
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                return intent;
            }
        }
        return null;
    }

    /**
     * Opens the root manager.
     *
     * @return true when a manager was found and started
     */
    public static boolean open(Context context) {
        Intent intent = findManager(context);
        if (intent == null) {
            return false;
        }
        try {
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Lists the packages that declare themselves as a superuser application.
     *
     * <p>Used to tell the user which app to open when the package name is not
     * one of the well known ones.
     */
    public static List<String> findSuperuserApps(Context context) {
        List<String> found = new java.util.ArrayList<>();
        if (context == null) {
            return found;
        }
        Intent query = new Intent("android.intent.action.MAIN");
        query.addCategory("android.intent.category.LAUNCHER");
        query.setPackage("android");
        try {
            android.content.pm.PackageManager pm = context.getPackageManager();
            List<android.content.pm.ResolveInfo> list =
                    pm.queryIntentActivities(query, 0);
            for (android.content.pm.ResolveInfo info : list) {
                CharSequence label = info.activityInfo.applicationInfo.loadLabel(pm);
                String name = label == null
                        ? info.activityInfo.applicationInfo.packageName
                        : label.toString();
                if (name.toLowerCase().contains("kernel")
                        || name.toLowerCase().contains("magisk")
                        || name.toLowerCase().contains("root")
                        || name.toLowerCase().contains("superuser")
                        || name.toLowerCase().contains("apatch")) {
                    found.add(name + " (" + info.activityInfo.packageName + ")");
                }
            }
        } catch (Exception ignored) {
        }
        return found;
    }
}
