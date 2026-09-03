/*
Copyright (C) Max Kastanas 2012
Copyright (C) Rhn 2026
 */
package com.max2idea.android.qube.keyboard;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.max2idea.android.qube.main.Config;
import com.max2idea.android.qube.main.QubeActivity;

public class KeyboardUtils {
    private static final String TAG = "KeyboardUtils";

    public static boolean showKeyboard(Activity activity, boolean toggle, View view) {
        InputMethodManager inputMgr = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (toggle || !Config.enableToggleKeyboard) {
            if (view != null) {
                view.requestFocus();
                inputMgr.showSoftInput(view, InputMethodManager.SHOW_FORCED);
            }
        } else {
            if (view != null) {
                inputMgr.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
        return !toggle;
    }

    public static void hideKeyboard(Activity activity, View view) {
        InputMethodManager inputMgr = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (view != null) {
            inputMgr.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
