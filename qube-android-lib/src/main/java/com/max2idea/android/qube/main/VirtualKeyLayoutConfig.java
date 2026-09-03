/*
This is a ported code from the AVNC project
Copyright (C) Gaurav Ujjwal 2020
Copyright (C) Rhn 2026
*/
package com.max2idea.android.qube.main;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VirtualKeyLayoutConfig {
    private static final String TAG = "VirtualKeyLayoutConfig";

    private static final List<VirtualKey> DEFAULT_LAYOUT = Arrays.asList(
            VirtualKey.ToggleKeyboard, VirtualKey.CloseKeys, VirtualKey.Esc, VirtualKey.LeftSuper,
            VirtualKey.Tab, VirtualKey.LeftCtrl, VirtualKey.LeftShift, VirtualKey.LeftAlt,
            VirtualKey.Home, VirtualKey.Left, VirtualKey.Up, VirtualKey.Down, VirtualKey.End,
            VirtualKey.Right, VirtualKey.PgUp, VirtualKey.PgDn);

    // In older versions, before users could customize key layout, there was a pref to
    // 'Show all' keys. This layout is used for compatibility with that pref.
    private static final List<VirtualKey> DEFAULT_LAYOUT_ALL = new ArrayList<>(DEFAULT_LAYOUT);
    static {
        DEFAULT_LAYOUT_ALL.addAll(Arrays.asList(
                VirtualKey.Insert, VirtualKey.Delete, VirtualKey.F1, VirtualKey.F2, VirtualKey.F3,
                VirtualKey.F4, VirtualKey.F5, VirtualKey.F6, VirtualKey.F7, VirtualKey.F8,
                VirtualKey.F9, VirtualKey.F10, VirtualKey.F11, VirtualKey.F12));
    }

    public static List<VirtualKey> getDefaultLayout(Context context) {
        return QubeSettingsManager.getVkShowAll(context) ? DEFAULT_LAYOUT_ALL : DEFAULT_LAYOUT;
    }

    public static List<VirtualKey> getLayout(Context context) {
        String saved = QubeSettingsManager.getVkLayout(context);
        if (saved != null) {
            try {
                List<VirtualKey> keys = new ArrayList<>();
                for (String name : saved.split(","))
                    keys.add(VirtualKey.valueOf(name));
                if (!keys.isEmpty())
                    return keys;
            } catch (Exception e) {
                Log.e(TAG, "Error parsing key layout [" + saved + "]: ", e);
            }
        }
        return getDefaultLayout(context);
    }

    public static void setLayout(Context context, List<VirtualKey> keys) {
        boolean hasCustomLayout = QubeSettingsManager.getVkLayout(context) != null;
        if (keys.equals(getDefaultLayout(context)) && hasCustomLayout) {
            // Restoring the defaults, so simply remove the pref.
            QubeSettingsManager.setVkLayout(context, null);
            return;
        }

        if (keys.equals(getLayout(context)))
            return; // Nothing changed

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keys.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(keys.get(i).name());
        }
        QubeSettingsManager.setVkLayout(context, sb.toString());
    }
}
