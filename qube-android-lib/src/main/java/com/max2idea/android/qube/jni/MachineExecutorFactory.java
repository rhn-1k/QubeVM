/*
Copyright (C) Max Kastanas 2012
Copyright (C) Rhn 2026
 */
package com.max2idea.android.qube.jni;

import com.max2idea.android.qube.machine.MachineController;
import com.max2idea.android.qube.machine.MachineExecutor;

public class MachineExecutorFactory {
    private static final String TAG = "MachineExecutorFactory";

    public static MachineExecutor createMachineExecutor(MachineController machineController, MachineExecutorType type) {
        switch (type) {
            case QEMU:
                return new VMExecutor(machineController);
        }
        return null;
    }

    public enum MachineExecutorType {
        QEMU
    }
}
