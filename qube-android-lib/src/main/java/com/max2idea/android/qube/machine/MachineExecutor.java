/*
Copyright (C) Max Kastanas 2012
Copyright (C) Rhn 2026
 */
package com.max2idea.android.qube.machine;

/** Our emulation abstract bridge. It can be extended to implement bridges with other native
 * emulators.
 */
public abstract class MachineExecutor {
    private static String TAG = "MachineExecutor";

    private final MachineController machineController;

    public MachineExecutor(MachineController machineController) {
        this.machineController = machineController;
    }

    protected Machine getMachine() {
        return machineController.getMachine();
    }

    abstract public void startService();

    // TODO: create int success code instead of string
    abstract public String start();

    abstract protected void stopvm(final int restart);


    public abstract boolean changeRemovableDevice(MachineProperty drive, String diskValue);

    public abstract String getDeviceName(MachineProperty driveProperty);



}
