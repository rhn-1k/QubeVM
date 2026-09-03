/*
Copyright (C) Max Kastanas 2012
Copyright (C) Rhn 2026
 */
package com.max2idea.android.qube.install;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.qube.emu.lib.R;
import com.max2idea.android.qube.files.FileInstaller;
import com.max2idea.android.qube.toast.ToastUtils;

import java.io.InputStream;
import java.util.ArrayList;

public class Installer extends AsyncTask<Void, Void, Void> {
    private static final String TAG = "Installer";

    private boolean force;
    private Activity activity;
    private androidx.appcompat.app.AlertDialog progDialog;

    private Installer(Activity activity, boolean force) {
        this.activity = activity;
        this.force = force;
    }

    public static String[] getAttrs(Context context, int res) {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            InputStream stream = context.getResources().openRawResource(res);
            byte[] buff = new byte[32768];
            int bytesRead = 0;
            while ((bytesRead = stream.read(buff, 0, buff.length)) > 0) {
                stringBuilder.append(new String(buff, 0, bytesRead));
            }
        } catch(Exception ex) {
            ToastUtils.toastShort(context, context.getString(R.string.CouldNotOpenRawFile) +": " + ex);
        }
        String fileContents = stringBuilder.toString();
        return fileContents.split("\\r\\n|\\n");
    }

    public static void installFiles(Activity activity, boolean force) {
        Installer installer = new Installer(activity, force);
        installer.force = force;
        installer.execute();
    }

    @Override
    protected Void doInBackground(Void... arg0) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int pad = (int) (24 * activity.getResources().getDisplayMetrics().density);

                CircularProgressIndicator progressIndicator = new CircularProgressIndicator(activity);
                progressIndicator.setIndeterminate(true);

                TextView messageView = new TextView(activity);
                messageView.setText(activity.getString(R.string.InstallingBIOS));
                messageView.setPadding(pad, 0, 0, 0);

                LinearLayout layout = new LinearLayout(activity);
                layout.setOrientation(LinearLayout.HORIZONTAL);
                layout.setPadding(pad, pad, pad, pad);
                layout.setGravity(android.view.Gravity.CENTER_VERTICAL);
                layout.addView(progressIndicator);
                layout.addView(messageView);

                progDialog = new MaterialAlertDialogBuilder(activity)
                        .setTitle(activity.getString(R.string.PleaseWait))
                        .setView(layout)
                        .setCancelable(false)
                        .show();
            }
        });
        FileInstaller.installFiles(activity, force);
        return null;
    }

    @Override
    protected void onPostExecute(Void test) {
        ToastUtils.toastShort(activity, "BIOS and Keymap files installed");
        if (progDialog != null && progDialog.isShowing())
            progDialog.dismiss();
    }
}
