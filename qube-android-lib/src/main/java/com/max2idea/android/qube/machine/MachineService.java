/*
Copyright (C) Max Kastanas 2012
Copyright (C) Rhn 2026
 */
package com.max2idea.android.qube.machine;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.qube.emu.lib.R;
import com.max2idea.android.qube.files.FileUtils;
import com.max2idea.android.qube.main.Config;
import com.max2idea.android.qube.main.QubeActivity;
import com.max2idea.android.qube.main.QubeSettingsManager;
import com.max2idea.android.qube.network.NetworkUtils;
import com.max2idea.android.qube.toast.ToastUtils;

/** MachineService is responsible only for owning and starting the thread that executes the jni part
 * This implementation is needed for Android to make sure that the process is started from a
 * foreground service so it will remain alive but with its thread running in the background.
 * All other communications with the native process will happen via MachineController -> MachineExecutor.
  */
public class MachineService extends Service {
    public static final int notifID = 1000;
    private static final String TAG = "MachineService";
    private static MachineService service;
    public Thread qubeThread;
    private NotificationCompat.Builder builder;
    private Notification mNotification;
    private WifiLock mWifiLock;
    private WakeLock mWakeLock;

    public static MachineService getService() {
        return service;
    }
    @Override
    public void onCreate() {
        Log.d(TAG, "Creating Service");
        service = this;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final String action = intent.getAction();
        final Bundle b = intent.getExtras();

        if (qubeThread != null)
            return START_NOT_STICKY;

        if (action.equals(Config.ACTION_START)) {
            if (MachineController.getInstance().getMachine() == null)
                return START_NOT_STICKY;

            String text = MachineController.getInstance().getMachine().getName() + ": VM Running";
            if (MachineController.getInstance().isVNCEnabled()) {
                text += " - " + service.getString(R.string.vncServer);
                text += ": " + NetworkUtils.getVNCAddress(service) + ":" + Config.defaultVNCPort;
            }
            // we need to start the service in the foreground
            setUpAsForeground(text);

            // start logging
            FileUtils.startLogging();

            // start the thread in the background
            ThreadGroup group = new ThreadGroup("threadGroup");
            qubeThread = new Thread(group, new Runnable() {
                public void run() {
                    startVM();
                }
            }, "QubeThread");
            if (MachineController.getInstance().getMachine().getPrio() == 1)
                qubeThread.setPriority(Thread.MAX_PRIORITY);
            qubeThread.start();
        }

        // Don't restart if killed
        return START_NOT_STICKY;
    }

    private void startVM() {
        //XXX: wait till logging starts capturing
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Log.d(TAG, "Starting VM: " + MachineController.getInstance().getMachine().getName());
        setupLocks();

        // notify we started
        MachineController.getInstance().onServiceStarted();

        //set the exit code before we start
        QubeSettingsManager.setExitCode(service, Config.EXIT_UNKNOWN);

        String res = MachineController.getInstance().start();

        // If the vm exits with a use requested shutdown
        // TODO: create int return codes instead of strings
        if (res != null) {
            if (!res.equals("VM shutdown")) {
                ToastUtils.toastLong(service, res);
                Log.e(TAG, res);
            } else {
                Log.d(TAG, res);
                //set the exit code
                QubeSettingsManager.setExitCode(service, Config.EXIT_SUCCESS);
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            ToastUtils.toastLong(service, res);
        }

        cleanUp();
        stopService();

        Log.d(TAG, "Exiting Qube");
        //XXX: We exit here to force unload the native libs
        System.exit(0);
    }


    public void cleanUp() {
        //XXX flush and close all file descriptors if we haven't already
        FileUtils.close_fds();
    }

    private void setUpAsForeground(String text) {
        if (MachineController.getInstance().getMachine() == null) {
            Log.w(TAG, "No Machine selected");
            return;
        }
        Intent intent = new Intent(service.getApplicationContext(), Config.clientClass);
        int pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntentFlags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pi = PendingIntent.getActivity(service.getApplicationContext(), 0, intent,
                pendingIntentFlags);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel chan = new NotificationChannel(Config.notificationChannelID, Config.notificationChannelName, NotificationManager.IMPORTANCE_NONE);
            NotificationManager notifService = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notifService.createNotificationChannel(chan);
            builder = new NotificationCompat.Builder(service, Config.notificationChannelID);
        } else
            builder = new NotificationCompat.Builder(service, "");
        mNotification = builder.setContentIntent(pi).setContentTitle(getString(R.string.app_name)).setContentText(text)
                .setSmallIcon(R.drawable.qube)
                .setLargeIcon(BitmapFactory.decodeResource(service.getResources(), R.drawable.qube)).build();
        mNotification.tickerText = text;
        mNotification.flags |= Notification.FLAG_ONGOING_EVENT;
        service.startForeground(notifID, mNotification);
    }

    public void updateServiceNotification(String text) {
        if (builder != null) {
            builder.setContentText(text);
            mNotification = builder.build();
            NotificationManager mNotificationManager = (NotificationManager)
                    service.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(notifID, mNotification);
        }
    }

    private void setupLocks() {
        WifiManager wm = (WifiManager) service.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mWifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, Config.wifiLockTag);
        mWifiLock.setReferenceCounted(false);

        PowerManager pm = (PowerManager) service.getApplicationContext().getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, Config.wakeLockTag);
        mWakeLock.setReferenceCounted(false);
    }

    public void onDestroy() {
        Log.d(TAG, "Service destroyed");
        super.onDestroy();
    }

    private void releaseLocks() {
        if (mWifiLock != null && mWifiLock.isHeld()) {
            Log.d(TAG, "Release Wifi lock...");
            mWifiLock.release();
        }

        if (mWakeLock != null && mWakeLock.isHeld()) {
            Log.d(TAG, "Release Wake lock...");
            mWakeLock.release();
        }
    }

    private void stopService() {
        releaseLocks();
        if (service != null) {
            service.stopForeground(true);
            service.stopSelf();
        }
    }

    public void onTaskRemoved(Intent intent) {
        QubeSettingsManager.setExitCode(this, Config.EXIT_SUCCESS);
    }

    public interface OnStatusChangedListener {
        void onStatusChanged(MachineService service, Machine machine, MachineController.MachineStatus status);
    }
}
