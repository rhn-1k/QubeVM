/*
Copyright (C) Max Kastanas 2012
Copyright (C) Rhn 2026
 */
package com.max2idea.android.qube.updates;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.qube.emu.lib.R;
import com.max2idea.android.qube.main.Config;
import com.max2idea.android.qube.main.QubeApplication;
import com.max2idea.android.qube.main.QubeSettingsManager;
import com.max2idea.android.qube.network.NetworkUtils;

/** Software Update notifier for checking if a new version is published.
  */
public class UpdateChecker {
    private static final String TAG = "UpdateChecker";

    public static void checkNewVersion(final Activity activity) {
        if (!QubeSettingsManager.getPromptUpdateVersion(activity)) {
            return;
        }

        try {
            byte[] streamData = NetworkUtils.getContentFromUrl(Config.newVersionLink);
            final String versionStr = new String(streamData).trim();
            float version = Float.parseFloat(versionStr);
            String versionName = getVersionName(versionStr);

            int versionCheck = (int) (version * 100);
            if (versionCheck > QubeApplication.getQubeVersion()) {
                final String finalVersionName = versionName;
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        promptNewVersion(activity, finalVersionName);
                    }
                });
            }
        } catch (Exception ex) {
            Log.w(TAG, "Could not get new version: " + ex.getMessage());
            if (Config.debug)
                ex.printStackTrace();
        }
    }

    private static String getVersionName(String versionStr) {
        String[] versionSegments = versionStr.split("\\.");
        int maj = Integer.parseInt(versionSegments[0]) / 100;
        int min = Integer.parseInt(versionSegments[0]) % 100;
        int mic = 0;
        if (versionSegments.length > 1) {
            mic = Integer.parseInt(versionSegments[1]);
        }
        String versionName = maj + "." + min + "." + mic;
        return versionName;
    }

    public static void promptNewVersion(final Activity activity, String version) {

        TextView stateView = new TextView(activity);
        stateView.setText(R.string.NewVersionWarning);
        stateView.setPadding(20, 20, 20, 20);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity)
                .setTitle(activity.getString(R.string.NewVersion) + " " + version)
                .setIcon(R.drawable.info_24px)
                .setView(stateView)
                .setPositiveButton(activity.getString(R.string.GenNewVersion),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                NetworkUtils.openURL(activity, Config.downloadLink);
                            }
                        })
                .setNegativeButton(activity.getString(R.string.DoNotShowAgain),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                QubeSettingsManager.setPromptUpdateVersion(activity, false);
                            }
                        });
        builder.show();
    }
}
