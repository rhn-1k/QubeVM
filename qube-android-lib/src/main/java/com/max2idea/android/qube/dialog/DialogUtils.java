/*
Copyright (C) Max Kastanas 2012
Copyright (C) Rhn 2026
 */
package com.max2idea.android.qube.dialog;

import android.app.Activity;
import androidx.appcompat.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public final class DialogUtils {
    private DialogUtils() {
    }

    public static void UIAlert(Activity activity, String title, String body) {
        TextView textView = new TextView(activity);
        textView.setPadding(20, 20, 20, 20);
        textView.setText(body == null ? "" : body);
        ScrollView scrollView = new ScrollView(activity);
        scrollView.addView(textView);
        new MaterialAlertDialogBuilder(activity)
                .setTitle(title)
                .setView(scrollView)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    public static void UIAlert(Activity activity, String title, String body, int textSize,
                               boolean cancelable, String button1title,
                               DialogInterface.OnClickListener button1Listener,
                               String button2title,
                               DialogInterface.OnClickListener button2Listener,
                               String button3title,
                               DialogInterface.OnClickListener button3Listener) {
        UIAlert(activity, title, body, textSize, cancelable, false, 0,
                button1title, button1Listener, button2title, button2Listener,
                button3title, button3Listener);
    }

    public static void UIAlert(Activity activity, String title, String body, int textSize,
                               boolean cancelable, boolean showIcon, int iconId,
                               String button1title,
                               DialogInterface.OnClickListener button1Listener,
                               String button2title,
                               DialogInterface.OnClickListener button2Listener,
                               String button3title,
                               DialogInterface.OnClickListener button3Listener) {
        TextView textView = new TextView(activity);
        textView.setPadding(20, 20, 20, 20);
        textView.setText(body == null ? "" : body);
        if (textSize > 0) {
            textView.setTextSize(textSize);
        }
        LinearLayout container = new LinearLayout(activity);
        container.setOrientation(LinearLayout.VERTICAL);
        ScrollView scrollView = new ScrollView(activity);
        scrollView.addView(textView);
        container.addView(scrollView);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity)
                .setTitle(title)
                .setView(container);
        if (showIcon && iconId != 0) {
            builder.setIcon(iconId);
        }
        if (button1title != null) {
            builder.setPositiveButton(button1title, button1Listener);
        }
        if (button2title != null) {
            builder.setNegativeButton(button2title, button2Listener);
        }
        if (button3title != null) {
            builder.setNeutralButton(button3title, button3Listener);
        }
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(cancelable);
        dialog.show();
    }
}
