/*
Copyright (C) Max Kastanas 2012
Copyright (C) Rhn 2026
 */
package com.max2idea.android.qube.main;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import com.google.android.material.color.DynamicColors;
import com.max2idea.android.qube.files.FileUtils;
import com.max2idea.android.qube.machine.Dispatcher;
import com.max2idea.android.qube.machine.FavOpenHelper;
import com.max2idea.android.qube.machine.MachineOpenHelper;
import com.max2idea.android.qube.toast.ToastUtils;
import java.io.File;

public class QubeApplication extends Application {
    private static final String TAG = "QubeApplication";
    public static Config.Arch arch = Config.Arch.x86;
    private static Context instance;
    private static String qemuVersionString;
    private static int qemuVersion;
    private static String qubeVersionString;
    private static int qubeVersion;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        DynamicColors.applyToActivitiesIfAvailable(this);
        try {
            Class.forName("android.os.AsyncTask");
        } catch (Throwable ignored) {
            // ignored
        }
        MachineOpenHelper.initialize(this);
        FavOpenHelper.initialize(this);
        setupFolders();
    }

    private void setupFolders() {
        Config.storagedir = Environment.getExternalStorageDirectory().toString();
        File folder = new File(getTmpFolder());
        if (!folder.exists() && !folder.mkdirs()) {
            Log.e(TAG, "Could not create temp folder: " + folder.getPath());
        }
    }

    public static Context getInstance() {
        if (instance == null) {
            throw new IllegalStateException("QubeApplication has not been created yet");
        }
        return instance;
    }

    public static void setupEnv(Context context) {
        try {
            Package packageObject = context.getClass().getPackage();
            String packageName = packageObject != null && packageObject.getName() != null
                    ? packageObject.getName() : context.getPackageName();
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(
                    packageName, PackageManager.GET_META_DATA);
            qubeVersion = getPackageVersionCode(packageInfo);
            qubeVersionString = packageInfo.versionName;
            Log.d(TAG, "QubeVM Version: " + qubeVersion);
            Log.d(TAG, "QubeVM Version Code: " + qubeVersionString);

            qemuVersionString = FileUtils.LoadFile(context, "QEMU_VERSION", false);
            if (qemuVersionString == null) {
                throw new IllegalStateException("QEMU_VERSION is missing");
            }
            String[] parts = qemuVersionString.trim().split("\\.");
            qemuVersion = Integer.parseInt(parts[0]) * 10000
                    + Integer.parseInt(parts[1]) * 100
                    + Integer.parseInt(parts[2]);
            Log.d(TAG, "Qemu Version: " + qemuVersionString);
            Log.d(TAG, "Qemu Version Number: " + qemuVersion);
            HostCapabilities.logHostCapabilities();
        } catch (Exception ex) {
            ex.printStackTrace();
            ToastUtils.toastShort(context, "Could not load version information: " + ex);
        }
    }

    public static String getUserId(Context context) {
        String userId = "None";
        try {
            Package packageObject = context.getClass().getPackage();
            String packageName = packageObject != null && packageObject.getName() != null
                    ? packageObject.getName() : context.getPackageName();
            ApplicationInfo applicationInfo = context.getPackageManager().getApplicationInfo(
                    packageName, PackageManager.GET_META_DATA);
            userId = Integer.toString(applicationInfo.uid);
        } catch (PackageManager.NameNotFoundException ex) {
            ex.printStackTrace();
        }
        return userId;
    }

    public static boolean isHost64Bit() {
        return Build.SUPPORTED_64_BIT_ABIS != null && Build.SUPPORTED_64_BIT_ABIS.length > 0;
    }

    public static boolean isHostX86_64() {
        return contains(Build.SUPPORTED_64_BIT_ABIS, "x86_64");
    }

    public static boolean isHostX86() {
        return contains(Build.SUPPORTED_32_BIT_ABIS, "x86");
    }

    public static boolean isHostArm() {
        return contains(Build.SUPPORTED_32_BIT_ABIS, "armeabi-v7a");
    }

    public static boolean isHostArmv8() {
        return contains(Build.SUPPORTED_64_BIT_ABIS, "arm64-v8a");
    }

    private static boolean contains(String[] values, String target) {
        if (values == null) {
            return false;
        }
        for (String value : values) {
            if (target.equals(value)) {
                return true;
            }
        }
        return false;
    }

    public static ViewListener getViewListener() {
        return Dispatcher.getInstance();
    }

    public static String getBasefileDir() {
        return getInstance().getCacheDir() + "/qube/";
    }

    public static String getTmpFolder() {
        return getBasefileDir() + "var/tmp";
    }

    public static String getMachineDir() {
        return getBasefileDir() + Config.machineFolder;
    }

    public static String getLocalQMPSocketPath() {
        return getInstance().getCacheDir() + "/qmpsocket";
    }

    public static String getQemuVersionString() {
        return qemuVersionString;
    }

    public static int getQemuVersion() {
        return qemuVersion;
    }

    public static String getQubeVersionString() {
        return qubeVersionString;
    }

    public static int getQubeVersion() {
        return qubeVersion;
    }

    @SuppressWarnings("deprecation")
    private static int getPackageVersionCode(PackageInfo packageInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return (int) packageInfo.getLongVersionCode();
        }
        return packageInfo.versionCode;
    }
}
