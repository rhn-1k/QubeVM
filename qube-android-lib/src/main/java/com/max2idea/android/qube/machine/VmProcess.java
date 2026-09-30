/*
Copyright (C) Rhn 2026
 */
package com.max2idea.android.qube.machine;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import com.max2idea.android.qube.main.Config;
import com.max2idea.android.qube.main.QubeApplication;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** A bridge between the UI process and the vm process that runs qemu */
public final class VmProcess {
    static final int MSG_REGISTER = 1;
    static final int MSG_STATE = 2;

    // Must match android:process in the manifest
    private static final String PROCESS_SUFFIX = ":vm";
    private static final String KEY_ARCH = "vm.arch";
    private static final String KEY_MACHINE = "vm.machine";

    private static final class Entry {
        final Consumer<Bundle> save;
        final Consumer<Bundle> load;

        Entry(Consumer<Bundle> save, Consumer<Bundle> load) {
            this.save = save;
            this.load = load;
        }
    }

    // save runs in the UI process, load in vn
    private static final Entry[] ENTRIES = {
            text(KEY_ARCH, () -> QubeApplication.arch.name(),
                    v -> QubeApplication.arch = Config.Arch.valueOf(v)),
            text("vm.client", () -> Config.clientClass == null ? null : Config.clientClass.getName(),
                    VmProcess::setClient),
            flag("vm.kvm", () -> Config.enableKVM, v -> Config.enableKVM = v),
            flag("vm.floppy", () -> Config.enableEmulatedFloppy, v -> Config.enableEmulatedFloppy = v),
            flag("vm.mttcg", () -> Config.enableMTTCG, v -> Config.enableMTTCG = v),
            text("vm.machineDir", () -> Config.machineFolder, v -> Config.machineFolder = v),
            text("vm.log", () -> Config.logFilePath, v -> Config.logFilePath = v),
    };

    private static final boolean VM_PROCESS = detectVmProcess();

    private static final Messenger inbox = new Messenger(new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message message) {
            if (message.what == MSG_STATE) {
                MachineController.getInstance().setRemoteRunning(message.arg1 == 1);
            }
        }
    });

    private static final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            Message message = Message.obtain(null, MSG_REGISTER);
            message.replyTo = inbox;
            try {
                new Messenger(binder).send(message);
            } catch (RemoteException e) {
                release();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            release();
        }

        @Override
        public void onBindingDied(ComponentName name) {
            release();
        }
    };

    private static Context appContext;
    private static boolean bound;
    private static boolean configured;
    private static boolean nativeLoaded;

    private VmProcess() {
    }

    private static boolean detectVmProcess() {
        String name = Application.getProcessName();
        return name != null && name.endsWith(PROCESS_SUFFIX);
    }

    public static boolean isVmProcess() {
        return VM_PROCESS;
    }

    // Config is static so vm doesn't share it, we ship it in the intent
    public static Bundle snapshot() {
        Bundle b = new Bundle();
        for (Entry entry : ENTRIES) {
            entry.save.accept(b);
        }
        Machine machine = MachineController.getInstance().getMachine();
        b.putString(KEY_MACHINE, machine == null ? null : machine.getName());
        return b;
    }

    // Every entry point in vm calls this before touching Config or native libs
    public static synchronized void prepare(Bundle extras) {
        if (!VM_PROCESS) {
            return;
        }
        if (!configured && extras != null && extras.containsKey(KEY_ARCH)) {
            applyConfig(extras);
            configured = true;
        }
        if (extras != null && MachineController.getInstance().getMachine() == null) {
            String name = extras.getString(KEY_MACHINE);
            if (name != null) {
                MachineController.getInstance().setStoredMachine(name);
            }
        }
        loadNativeLibs();
    }

    public static synchronized void attach(Context context) {
        if (VM_PROCESS || bound) {
            return;
        }
        appContext = context.getApplicationContext();
        bound = appContext.bindService(new Intent(appContext, MachineService.class), connection,
                Context.BIND_AUTO_CREATE);
    }

    // qemu thread lives in vm so we ask the service to stop it
    public static void requestRemoteStop(Context context) {
        context.startService(new Intent(Config.ACTION_STOP, null, context, MachineService.class));
    }

    public static String qemuLibrary() {
        switch (QubeApplication.arch) {
            case x86:
                return "libqemu-system-i386.so";
            case x86_64:
                return "libqemu-system-x86_64.so";
            case arm:
                return "libqemu-system-arm.so";
            case arm64:
                return "libqemu-system-aarch64.so";
            case ppc:
                return "libqemu-system-ppc.so";
            case ppc64:
                return "libqemu-system-ppc64.so";
            case m68k:
                return "libqemu-system-m68k.so";
            default:
                throw new IllegalStateException("Unexpected value: " + QubeApplication.arch);
        }
    }

    private static synchronized void release() {
        if (bound) {
            bound = false;
            try {
                appContext.unbindService(connection);
            } catch (IllegalArgumentException ignored) {
            }
        }
        // Ensure stopping MachineService if something went wrong with VM
        // or it will stay running and cause memory leak
        appContext.stopService(new Intent(appContext, MachineService.class));
        MachineController.getInstance().remoteGone();
    }

    private static void applyConfig(Bundle extras) {
        for (Entry entry : ENTRIES) {
            entry.load.accept(extras);
        }
    }

    private static Entry text(String key, Supplier<String> get, Consumer<String> set) {
        return new Entry(b -> b.putString(key, get.get()), b -> {
            String value = b.getString(key);
            if (value != null) {
                set.accept(value);
            }
        });
    }

    private static Entry flag(String key, BooleanSupplier get, Consumer<Boolean> set) {
        return new Entry(b -> b.putBoolean(key, get.getAsBoolean()), b -> {
            if (b.containsKey(key)) {
                set.accept(b.getBoolean(key));
            }
        });
    }

    private static void setClient(String name) {
        try {
            Config.clientClass = Class.forName(name);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    //XXX: this needs to be called from the main thread otherwise
    // qemu crashes when it is started later
    private static void loadNativeLibs() {
        if (nativeLoaded) {
            return;
        }
        // Compatibility lib
        System.loadLibrary("compat-qube");

        // Glib deps
        System.loadLibrary("compat-musl");

        // Glib for qemu
        System.loadLibrary("glib-2.0");

        // Pixman for qemu
        System.loadLibrary("pixman-1");

        // VirGL for qemu
        System.loadLibrary("epoxy");
        System.loadLibrary("virglrenderer");

        // Qube needed for vmexecutor
        System.loadLibrary("qube");

        // Qemu arch specific lib
        String qemu = qemuLibrary();
        System.loadLibrary(qemu.substring("lib".length(), qemu.length() - ".so".length()));
        nativeLoaded = true;
    }
}
