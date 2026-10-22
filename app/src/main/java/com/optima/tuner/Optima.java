package com.optima.tuner;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;

/**
 * Application entry point. Initialises the shell layer and exposes shared state.
 */
public class Optima extends Application {
    private static final String TAG = "OptimaApp";
    private static Optima instance;

    private static final String PREF_MODE = "optima_mode";
    private static final String PREF_CPU = "optima_cpu";
    private static final String PREF_CHARGE = "optima_charge";
    private static final String PREF_POWER = "optima_power";
    private static final String PREF_THERMAL = "optima_thermal";
    private static final String PREF_REFRESH = "optima_refresh";
    private static final String PREF_APP_CONFIG = "optima_app_config";
    private static final String PREF_XPOSED = "optima_xposed";
    private static final String PREF_OTHER = "optima_other";

    private static SharedPreferences modePref;
    private static SharedPreferences cpuPref;
    private static SharedPreferences chargePref;
    private static SharedPreferences powerPref;
    private static SharedPreferences thermalPref;
    private static SharedPreferences refreshPref;
    private static SharedPreferences appConfigPref;
    private static SharedPreferences xposedPref;
    private static SharedPreferences otherPref;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        try {
            Log.d(TAG, "Optima application starting");
        } catch (Exception ignored) {
        }
    }

    public static Optima get() {
        return instance;
    }

    public static Context context() {
        return instance == null ? null : instance.getApplicationContext();
    }

    public static SharedPreferences mode() {
        if (modePref == null) {
            modePref = instance.getSharedPreferences(PREF_MODE, Context.MODE_PRIVATE);
        }
        return modePref;
    }

    public static SharedPreferences cpu() {
        if (cpuPref == null) {
            cpuPref = instance.getSharedPreferences(PREF_CPU, Context.MODE_PRIVATE);
        }
        return cpuPref;
    }

    public static SharedPreferences charge() {
        if (chargePref == null) {
            chargePref = instance.getSharedPreferences(PREF_CHARGE, Context.MODE_PRIVATE);
        }
        return chargePref;
    }

    public static SharedPreferences power() {
        if (powerPref == null) {
            powerPref = instance.getSharedPreferences(PREF_POWER, Context.MODE_PRIVATE);
        }
        return powerPref;
    }

    public static SharedPreferences thermal() {
        if (thermalPref == null) {
            thermalPref = instance.getSharedPreferences(PREF_THERMAL, Context.MODE_PRIVATE);
        }
        return thermalPref;
    }

    public static SharedPreferences refresh() {
        if (refreshPref == null) {
            refreshPref = instance.getSharedPreferences(PREF_REFRESH, Context.MODE_PRIVATE);
        }
        return refreshPref;
    }

    public static SharedPreferences appConfig() {
        if (appConfigPref == null) {
            appConfigPref = instance.getSharedPreferences(PREF_APP_CONFIG, Context.MODE_PRIVATE);
        }
        return appConfigPref;
    }

    public static SharedPreferences xposed() {
        if (xposedPref == null) {
            xposedPref = instance.getSharedPreferences(PREF_XPOSED, Context.MODE_PRIVATE);
        }
        return xposedPref;
    }

    public static SharedPreferences other() {
        if (otherPref == null) {
            otherPref = instance.getSharedPreferences(PREF_OTHER, Context.MODE_PRIVATE);
        }
        return otherPref;
    }

    public static SharedPreferences globalPrefs() {
        return PreferenceManager.getDefaultSharedPreferences(instance);
    }

    public static String getString(String key, String def) {
        SharedPreferences p = instance.getSharedPreferences("optima", Context.MODE_PRIVATE);
        return p.getString(key, def);
    }

    public static boolean getBool(String key, boolean def) {
        SharedPreferences p = instance.getSharedPreferences("optima", Context.MODE_PRIVATE);
        return p.getBoolean(key, def);
    }

    public static void putString(String key, String value) {
        SharedPreferences p = instance.getSharedPreferences("optima", Context.MODE_PRIVATE);
        p.edit().putString(key, value).apply();
    }

    public static void putBool(String key, boolean value) {
        SharedPreferences p = instance.getSharedPreferences("optima", Context.MODE_PRIVATE);
        p.edit().putBoolean(key, value).apply();
    }

    public static void putInt(String key, int value) {
        SharedPreferences p = instance.getSharedPreferences("optima", Context.MODE_PRIVATE);
        p.edit().putInt(key, value).apply();
    }

    public static int getInt(String key, int def) {
        SharedPreferences p = instance.getSharedPreferences("optima", Context.MODE_PRIVATE);
        return p.getInt(key, def);
    }

    public static void putStringSet(String key, Set<String> value) {
        SharedPreferences p = instance.getSharedPreferences("optima", Context.MODE_PRIVATE);
        p.edit().putStringSet(key, value).apply();
    }

    public static Set<String> getStringSet(String key, Set<String> def) {
        SharedPreferences p = instance.getSharedPreferences("optima", Context.MODE_PRIVATE);
        Set<String> s = p.getStringSet(key, def);
        return s == null ? new HashSet<>() : s;
    }

    public static void remove(String key) {
        SharedPreferences p = instance.getSharedPreferences("optima", Context.MODE_PRIVATE);
        p.edit().remove(key).apply();
    }

    public static void commit() {
        SharedPreferences p = instance.getSharedPreferences("optima", Context.MODE_PRIVATE);
        p.edit().commit();
    }
}
