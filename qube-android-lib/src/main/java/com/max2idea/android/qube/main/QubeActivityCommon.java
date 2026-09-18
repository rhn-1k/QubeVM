/*
Copyright (C) Max Kastanas 2012
Copyright (C) Rhn 2026
 */
package com.max2idea.android.qube.main;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.qube.emu.lib.R;
import com.max2idea.android.qube.dialog.DialogUtils;
import com.max2idea.android.qube.files.FileUtils;
import com.max2idea.android.qube.help.Help;
import com.max2idea.android.qube.install.Installer;
import com.max2idea.android.qube.machine.Machine;
import com.max2idea.android.qube.machine.MachineAction;
import com.max2idea.android.qube.network.NetworkUtils;

import java.io.IOException;
import java.util.ArrayList;

public final class QubeActivityCommon {
    private QubeActivityCommon() {
    }

    public static void promptStopVM(final Activity activity, final ViewListener viewListener) {
        activity.runOnUiThread(() -> new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.ShutdownVM)
                .setIcon(R.drawable.power_settings_new_24px)
                .setMessage(R.string.ShutdownVMWarning)
                .setPositiveButton(android.R.string.ok,
                        (dialog, which) -> viewListener.onAction(MachineAction.STOP_VM, null))
                .setNegativeButton(android.R.string.cancel, null)
                .show());
    }

    public static void promptResetVM(final Activity activity, final ViewListener viewListener) {
        activity.runOnUiThread(() -> new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.ResetVM)
                .setIcon(R.drawable.refresh_24px)
                .setMessage(R.string.ResetVMWarning)
                .setPositiveButton(R.string.Yes,
                        (dialog, which) -> viewListener.onAction(MachineAction.RESET_VM, null))
                .setNegativeButton(R.string.No, null)
                .show());
    }

    public static void promptVNCServer(final Context context, final String msg,
                                       final ViewListener viewListener) {
        new Handler(Looper.getMainLooper()).post(() -> new MaterialAlertDialogBuilder(context)
                .setTitle(context.getString(R.string.vncServer))
                .setIcon(R.drawable.cast_24px)
                .setMessage(context.getString(R.string.vncServer) + ": "
                        + NetworkUtils.getVNCAddress(context) + ":" + Config.defaultVNCPort
                        + "\n\n" + (msg == null ? "" : msg))
                .setPositiveButton(android.R.string.ok,
                        (dialog, which) -> viewListener.onAction(MachineAction.START_VM, null))
                .setNegativeButton(R.string.Cancel, null)
                .show());
    }

    public static void promptLicense(final Activity activity, final String title, final String body) {
        promptLicense(activity, title, body, null);
    }

    public static void promptLicense(final Activity activity, final String title, final String body, final Runnable onFirstLaunchComplete) {
        activity.runOnUiThread(() -> {
            TextView textView = new TextView(activity);
            textView.setText(body == null ? "" : body);
            textView.setTextSize(10f);
            textView.setPadding(20, 20, 20, 20);
            ScrollView scrollView = new ScrollView(activity);
            scrollView.addView(textView);
            final androidx.appcompat.app.AlertDialog alertDialog = new MaterialAlertDialogBuilder(activity)
                    .setTitle(title)
                    .setIcon(R.drawable.copyright_24px)
                    .setView(scrollView)
                    .setPositiveButton(R.string.IAcknowledge, (dialog, which) -> {
                        if (QubeSettingsManager.isFirstLaunch(activity)) {
                            Installer.installFiles(activity, true);
                            Help.showHelp(activity, onFirstLaunchComplete);
                        }
                        QubeSettingsManager.setFirstLaunch(activity);
                    })
                    .create();
            alertDialog.setOnCancelListener(dialog -> {
                if (QubeSettingsManager.isFirstLaunch(activity)) {
                    finishParentOrActivity(activity);
                }
            });
            alertDialog.show();
        });
    }

    public static void tapNotSupported(final Activity activity, final String userId) {
        activity.runOnUiThread(() -> DialogUtils.UIAlert(
                activity,
                activity.getString(R.string.tapUserId) + ": " + (userId == null ? "" : userId),
                activity.getString(R.string.tapNotSupportInstructions),
                0,
                true,
                true,
                R.drawable.error_24px,
                activity.getString(android.R.string.ok),
                null,
                null,
                null,
                null,
                null));
    }

    public static void promptTap(final Activity activity, final String userId) {
        DialogInterface.OnClickListener okListener = (dialog, which) -> {
        };
        DialogInterface.OnClickListener helpListener = (dialog, which) ->
                goToURL(activity, Config.NetworkLink);
        activity.runOnUiThread(() -> DialogUtils.UIAlert(
                activity,
                activity.getString(R.string.TapDeviceFound),
                activity.getString(R.string.tunDeviceWarning) + ": "
                        + (userId == null ? "" : userId) + "\n",
                16,
                false,
                true,
                R.drawable.settings_ethernet_24px,
                activity.getString(android.R.string.ok),
                okListener,
                null,
                null,
                activity.getString(R.string.TAPHelp),
                helpListener));
    }

    public static void goToURL(Context context, String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        context.startActivity(intent);
    }

    public static void onNetworkUser(final Activity activity) {
        DialogInterface.OnClickListener okListener = (dialog, which) -> {
        };
        DialogInterface.OnClickListener helpListener = (dialog, which) ->
                goToURL(activity, Config.NetworkLink);
        activity.runOnUiThread(() -> DialogUtils.UIAlert(
                activity,
                activity.getString(R.string.network),
                activity.getString(R.string.externalNetworkWarning),
                16,
                false,
                true,
                R.drawable.settings_ethernet_24px,
                activity.getString(android.R.string.ok),
                okListener,
                null,
                null,
                activity.getString(R.string.faq),
                helpListener));
    }

    public static void showChangelog(final Activity activity) {
        try {
            TextView textView = new TextView(activity);
            textView.setPadding(20, 20, 20, 20);
            textView.setText(FileUtils.LoadFile(activity, "CHANGELOG", false));
            ScrollView scrollView = new ScrollView(activity);
            scrollView.addView(textView);
            androidx.appcompat.app.AlertDialog alertDialog = new MaterialAlertDialogBuilder(activity)
                    .setTitle(activity.getString(R.string.CHANGELOG))
                    .setIcon(R.drawable.notes_24px)
                    .setView(scrollView)
                    .setPositiveButton(android.R.string.ok, null)
                    .create();
            alertDialog.setCanceledOnTouchOutside(false);
            alertDialog.show();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @SuppressWarnings("deprecation")
    private static void finishParentOrActivity(Activity activity) {
        if (activity.getParent() != null) {
            activity.getParent().finish();
        } else {
            activity.finish();
        }
    }
}
