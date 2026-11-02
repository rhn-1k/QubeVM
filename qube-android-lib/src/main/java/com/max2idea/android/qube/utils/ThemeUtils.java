package com.max2idea.android.qube.utils;

import android.app.Activity;
import android.content.res.Configuration;

import java.util.WeakHashMap;

/**
 * Activities in this app declare uiMode in android:configChanges, so the
 * system does not recreate them automatically when the light/dark theme
 * changes while they're open - it only calls onConfigurationChanged().
 * This class tracks the theme each activity was created with and recreates
 * it if that theme changes, so colors, the status bar, etc. pick it up.
 */
public class ThemeUtils {

    private static final WeakHashMap<Activity, Integer> sNightModeAtCreate = new WeakHashMap<>();

    private ThemeUtils() {
    }

    /** Call once from the activity's onCreate, after super.onCreate(). */
    public static void trackNightMode(Activity activity) {
        sNightModeAtCreate.put(activity, nightMode(activity.getResources().getConfiguration()));
    }

    /**
     * Call from the activity's onConfigurationChanged(newConfig), after
     * super.onConfigurationChanged(newConfig).
     *
     * @return true if the theme changed and activity.recreate() was called.
     */
    public static boolean recreateIfThemeChanged(Activity activity, Configuration newConfig) {
        Integer previous = sNightModeAtCreate.get(activity);
        if (previous != null && previous != nightMode(newConfig)) {
            activity.recreate();
            return true;
        }
        return false;
    }

    private static int nightMode(Configuration config) {
        return config.uiMode & Configuration.UI_MODE_NIGHT_MASK;
    }
}
