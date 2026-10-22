package com.optima.tuner.core;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Small helper to run shell-backed work off the UI thread.
 */
public final class Background {
    private static final ExecutorService POOL = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "optima-bg");
        t.setDaemon(true);
        return t;
    });

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private Background() {
    }

    /** Runs {@code work} on a background thread. */
    public static void run(Runnable work) {
        POOL.execute(() -> {
            try {
                work.run();
            } catch (Throwable t) {
                // never let a background failure kill the worker
            }
        });
    }

    /**
     * Runs {@code work} on a background thread and delivers its result on the UI thread.
     */
    public static <T> void supply(Supplier<T> work, Consumer<T> onResult) {
        POOL.execute(() -> {
            T result = null;
            try {
                result = work.get();
            } catch (Throwable t) {
                result = null;
            }
            final T value = result;
            MAIN.post(() -> onResult.accept(value));
        });
    }

    /** Posts {@code action} to the UI thread. */
    public static void onMain(Runnable action) {
        MAIN.post(action);
    }

    public interface Supplier<T> {
        T get();
    }

    public interface Consumer<T> {
        void accept(T value);
    }
}
