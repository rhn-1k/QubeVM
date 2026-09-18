/*
Copyright (C) Max Kastanas 2012
Copyright (C) Rhn 2026
 */
package com.max2idea.android.qube.help;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.qube.emu.lib.R;
import com.max2idea.android.qube.main.Config;
import com.max2idea.android.qube.main.QubeApplication;
import com.max2idea.android.qube.main.QubeSettingsManager;
import com.max2idea.android.qube.network.NetworkUtils;

public class Help {
    private static final String TAG = "Help";

    public static void showHelp(final Activity activity, final Runnable onDismiss) {
        LinearLayout mLayout = new LinearLayout(activity);
        mLayout.setOrientation(LinearLayout.VERTICAL);
        TextView textView = new TextView(activity);
        textView.setTextSize(15);
        textView.setText(activity.getResources().getString(R.string.welcomeText));
        textView.setPadding(20, 20, 20, 20);
        mLayout.addView(textView);

        CheckBox checkUpdates = new CheckBox(activity);
        checkUpdates.setText(R.string.checkForUpdates);
        checkUpdates.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                QubeSettingsManager.setPromptUpdateVersion(activity, b);
            }
        });
        checkUpdates.setChecked(QubeSettingsManager.getPromptUpdateVersion(activity));
        mLayout.addView(checkUpdates);

        new MaterialAlertDialogBuilder(activity)
                .setTitle(Config.APP_NAME + " " + QubeApplication.getQubeVersionString()
                        + " " + "QEMU" + " " + QubeApplication.getQemuVersionString())
                .setIcon(R.drawable.help_24px)
                .setView(mLayout)
                .setPositiveButton(activity.getString(R.string.GoToWiki),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                NetworkUtils.openURL(activity, Config.guidesLink);
                                if (onDismiss != null) onDismiss.run();
                            }
                        })
                .setNegativeButton(activity.getString(android.R.string.ok),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                if (onDismiss != null) onDismiss.run();
                            }
                        })
                .show();
    }

    public static void showHelp(final Activity activity) {
        showHelp(activity, null);
    }
}
