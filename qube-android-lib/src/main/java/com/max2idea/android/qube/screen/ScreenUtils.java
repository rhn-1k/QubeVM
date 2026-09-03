/*
Copyright (C) Max Kastanas 2012
Copyright (C) Rhn 2026
 */
package com.max2idea.android.qube.screen;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.view.Display;

import com.max2idea.android.qube.main.QubeSettingsManager;

/** Utility Class for Screen Orientation changes
 */
public class ScreenUtils {
    private static final String TAG = "ScreenUtils";
    public static void updateOrientation(Activity activity, int lastOrientation) {
        int orientation = QubeSettingsManager.getOrientationSetting(activity);
        switch (orientation) {
            case 0:
                if(lastOrientation >= 0)
                    activity.setRequestedOrientation(lastOrientation);
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                break;
            case 1:
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                break;
            case 2:
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                break;
            case 3:
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;
            case 4:
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                break;
        }
    }

    public static boolean isLandscapeOrientation(Activity activity) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        Point screenSize = new Point();
        display.getSize(screenSize);
        return screenSize.x >= screenSize.y;
    }

}
