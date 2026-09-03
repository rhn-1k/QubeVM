/*
Copyright (C) Max Kastanas 2012
Copyright (C) Rhn 2026
 */
package com.max2idea.android.qube.toast;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;

/** ToastUtils offers wrappers for showing toast notifications
 *
 */
public class ToastUtils {
    private static final String TAG = "ToastUtils";

    public static void toastLong(final Context activity, final String msg) {
        toastLong(activity, Gravity.CENTER, msg);
    }

    public static void toastLong(final Context activity, final int gravity, final String msg) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                android.widget.Toast toast = android.widget.Toast.makeText(activity, msg, android.widget.Toast.LENGTH_LONG);
                toast.setGravity(gravity, 0, 0);
                toast.show();
            }
        });
    }

    public static void toastShortTop(final Activity activity, final String msg) {
        toast(activity, msg, Gravity.TOP | Gravity.CENTER, android.widget.Toast.LENGTH_SHORT);
    }

    public static void toast(final Context context, final String msg, final int gravity, final int length) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (context instanceof Activity && ((Activity) context).isFinishing()) {
                    return;
                }
                android.widget.Toast toast = android.widget.Toast.makeText(context, msg, length);
                toast.setGravity(gravity, 0, 0);
                toast.show();
            }
        });
    }

    public static void toastShort(final Context context, final String msg) {
        toast(context, msg, Gravity.CENTER, android.widget.Toast.LENGTH_SHORT);
    }
}
