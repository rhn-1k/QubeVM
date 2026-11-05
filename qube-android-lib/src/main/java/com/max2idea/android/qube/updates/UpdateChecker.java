/*
Copyright (C) Max Kastanas 2012
Copyright (C) Rhn 2026
 */
package com.max2idea.android.qube.updates;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.qube.emu.lib.R;
import com.max2idea.android.qube.main.Config;
import com.max2idea.android.qube.main.QubeApplication;
import com.max2idea.android.qube.main.QubeSettingsManager;
import com.max2idea.android.qube.network.NetworkUtils;

import java.nio.charset.StandardCharsets;

/** Software Update notifier for checking if a new version is published.
  */
public class UpdateChecker {
    private static final String TAG = "UpdateChecker";

    public static void checkNewVersion(final Activity activity) {
        if (!QubeSettingsManager.getPromptUpdateVersion(activity)) {
            return;
        }

        try {
            byte[] streamData = NetworkUtils.getContentFromUrl(Config.versionLink);
            final String versionStr = new String(streamData).trim();
            String versionName = getVersionName(versionStr);

            int versionCheck = getVersionCode(versionStr);
            if (versionCheck > QubeApplication.getQubeVersion()) {
                final String finalVersionName = versionName;
                final String changelog = getChangelogForVersion(getChangelog(), finalVersionName);
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        promptNewVersion(activity, finalVersionName, changelog);
                    }
                });
            }
        } catch (Exception ex) {
            Log.w(TAG, "Could not get new version: " + ex.getMessage());
            if (Config.debug)
                ex.printStackTrace();
        }
    }

    private static String getChangelog() {
        try {
            byte[] changelogData = NetworkUtils.getContentFromUrl(Config.changelogLink);
            String changelog = new String(changelogData, StandardCharsets.UTF_8).trim();
            if (!changelog.isEmpty()) {
                return changelog;
            }
        } catch (Exception ex) {
            Log.w(TAG, "Could not get changelog: " + ex.getMessage());
            if (Config.debug)
                ex.printStackTrace();
        }
        return QubeApplication.getInstance().getString(R.string.ChangelogFetchFailed);
    }

    private static String getChangelogForVersion(String changelog, String versionName) {
        String[] lines = changelog.split("\\R");
        String versionHeader = Config.APP_NAME + " v" + versionName;
        int start = -1;

        for (int i = 0; i < lines.length; i++) {
            if (lines[i].trim().contains(versionHeader)) {
                start = i;
                break;
            }
        }

        if (start == -1) {
            return QubeApplication.getInstance().getString(R.string.ChangelogNotFound);
        }

        int end = start + 1;
        while (end < lines.length
                && !lines[end].trim().startsWith("* " + Config.APP_NAME + " v")) {
            end++;
        }

        StringBuilder releaseChangelog = new StringBuilder();
        for (int i = start; i < end; i++) {
            if (releaseChangelog.length() > 0) {
                releaseChangelog.append('\n');
            }
            releaseChangelog.append(lines[i]);
        }
        return releaseChangelog.toString().trim();
    }

    private static int getVersionCode(String versionStr) {
        String[] versionSegments = versionStr.split("\\.");
        int majorMinor = Integer.parseInt(versionSegments[0]);
        int micro = versionSegments.length > 1 ? Integer.parseInt(versionSegments[1]) : 0;
        return majorMinor * 100 + micro;
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

    public static void promptNewVersion(final Activity activity, String version, String changelog) {

        TextView stateView = new TextView(activity);
        stateView.setText(changelog);
        stateView.setPadding(20, 20, 20, 20);
        stateView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        ScrollView changelogView = new ScrollView(activity);
        changelogView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        changelogView.addView(stateView);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity)
                .setTitle(activity.getString(R.string.NewVersion) + " " + version)
                .setIcon(R.drawable.info_24px)
                .setView(changelogView)
                .setPositiveButton(activity.getString(R.string.GenNewVersion),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                NetworkUtils.openURL(activity, Config.newVersionLink + version);
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
