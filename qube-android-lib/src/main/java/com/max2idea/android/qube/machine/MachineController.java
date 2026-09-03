/*
Copyright (C) Max Kastanas 2012
Copyright (C) Rhn 2026
 */
package com.max2idea.android.qube.machine;

import android.util.Log;

import com.qube.emu.lib.R;
import com.max2idea.android.qube.files.FileUtils;
import com.max2idea.android.qube.jni.MachineExecutorFactory;
import com.max2idea.android.qube.main.QubeApplication;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Observer;

/**
 * Class communicates to the qemu process via the jni bridge MachineExecutor. It is responsible for
 * starting, stopping, and retrieving he status of the virtual machine.
 */
public class MachineController {
    private static final String TAG = "MachineController";
    private static MachineController mSingleton;
    private final MachineExecutor machineExecutor;
    private final HashSet<OnMachineStatusChangeListener> onMachineStatusChangeListeners = new HashSet<>();
    private final HashSet<OnEventListener> onEventListeners = new HashSet<>();
    private final Class<MachineService> serviceClass;
    private final IMachineDatabase machineDatabase;
    private Machine machine;

    private MachineController() {
        machineExecutor = MachineExecutorFactory.createMachineExecutor(this, MachineExecutorFactory.MachineExecutorType.QEMU);
        machineDatabase = MachineOpenHelper.getInstance();
        serviceClass = MachineService.class;
    }

    public static MachineController getInstance() {
        if (mSingleton == null) {
            mSingleton = new MachineController();
        }
        return mSingleton;
    }


    public MachineStatus getCurrStatus() {
        if(getMachine() ==null)
            return MachineStatus.Stopped;
        else if (MachineService.getService() == null)
            return MachineController.MachineStatus.Ready;
        else if (MachineService.getService().qubeThread != null)
            return MachineController.MachineStatus.Running;
        else
            return MachineController.MachineStatus.Stopped;
    }

    public void addOnStatusChangeListener(OnMachineStatusChangeListener listener) {
        onMachineStatusChangeListeners.add(listener);
    }

    public void removeOnStatusChangeListener(OnMachineStatusChangeListener listener) {
        onMachineStatusChangeListeners.remove(listener);
    }

    void removeOnStatusChangeListeners() {
        onMachineStatusChangeListeners.clear();
    }

    public void addOnEventListener(OnEventListener listener) {
        onEventListeners.add(listener);
    }

    public void removeOnEventListener(OnEventListener listener) {
        onEventListeners.remove(listener);
    }

    void removeOnEventListeners() {
        onEventListeners.clear();
    }

    void stopvm() {
        machineExecutor.stopvm(0);
    }

    private void notifyMachineStatusChangeListeners(Machine machine, MachineStatus status, Object o) {
        for (OnMachineStatusChangeListener listener : onMachineStatusChangeListeners)
            listener.onMachineStatusChanged(machine, status, o);
    }

    private void notifyEventListeners(Event status, Object o) {
        for (OnEventListener listener : onEventListeners)
            listener.onEvent(machine, status, o);
    }

    void startvm() {
        machineExecutor.startService();
    }

    void restartvm() {
        new Thread(new Runnable() {
            public void run() {
                if (machineExecutor != null) {
                    Log.d(TAG, "Restarting the VM...");
                    machineExecutor.stopvm(1);
                }
            }
        }).start();
    }

    private MachineExecutor getMachineExecutor() {
        return machineExecutor;
    }

    public String getMachineSaveDir() {
        return QubeApplication.getMachineDir() + machineExecutor.getMachine().getName();
    }

    void changeRemovableDevice(MachineProperty property, String value) {
        if (isRunning()) {
            boolean res = getMachineExecutor().changeRemovableDevice(property, value);
            if (!res)
                value = null;
        }
        switch (property) {
            case CDROM:
                getMachine().setCdImagePath(value);
                break;
            case FDA:
                getMachine().setFdaImagePath(value);
                break;
            case FDB:
                getMachine().setFdbImagePath(value);
                break;
            case SHARED_FOLDER:
                getMachine().setSharedFolderPath(value);
                break;
        }
    }

    public boolean isRunning() {
        return getCurrStatus() == MachineStatus.Running;
    }

    public String getMachineName() {
        return machineExecutor.getMachine().getName();
    }

    public boolean isVNCEnabled() {
        return getMachineExecutor().getMachine().getEnableVNC() == 1;
    }

    String start() {
        return machineExecutor.start();
    }

    public Machine getMachine() {
        return machine;
    }

    void setMachine(Machine machine) {
        if (this.machine != machine) {
            if (this.machine != null) {
                this.machine.deleteObservers();
            }
            this.machine = machine;
            if (this.machine != null)
                this.machine.addObserver((Observer) machineDatabase);
            notifyEventListeners(Event.MachineLoaded, machine);
        }
    }

    void setStoredMachine(String value) {
        setMachine(machineDatabase.getMachine(value));
    }

    boolean createVM(String machineName) {
        if (machineDatabase.getMachine(machineName) != null) {
            notifyEventListeners(Event.MachineCreateFailed, (Integer) R.string.VMNameExistsChooseAnother);
            return false;
        }
        Machine machine = new Machine(machineName, true);
        // Insert before notifying, avoids a race where the UI repopulates the
        // chooser from the DB before this machine is actually in it
        machineDatabase.insertMachine(machine);
        MachineController.getInstance().setMachine(machine);
        notifyEventListeners(Event.MachineCreated, machineName);
        notifyMachineStatusChangeListeners(machine, MachineStatus.Ready, null);
        return true;
    }

    public ArrayList<String> getStoredMachines() {
        return machineDatabase.getMachineNames();
    }

    protected boolean deleteMachine(Machine machine) {
        return machineDatabase.deleteMachine(machine);
    }

    public Class<?> getServiceClass() {
        return serviceClass;
    }

    protected void onServiceStarted() {
        notifyMachineStatusChangeListeners(machine, getCurrStatus(), null);
    }

    protected void onVMResolutionChanged(MachineExecutor machineExecutor, int vm_width, int vm_height) {
        if(machineExecutor == this.machineExecutor)
            notifyEventListeners(Event.MachineResolutionChanged, new Object[]{vm_width, vm_height});
    }

    public void setFullscreen() {
        notifyEventListeners(Event.MachineFullscreen, null);
    }

    public enum MachineStatus {
        Ready, Stopped, Unknown, Running
    }

    public enum Event {
        MachineCreated, MachineCreateFailed, MachineLoaded, MachineResolutionChanged, MachineFullscreen
    }

    public interface OnMachineStatusChangeListener {
        void onMachineStatusChanged(Machine machine, MachineStatus status, Object o);
    }

    public interface OnEventListener {
        void onEvent(Machine machine, Event event, Object o);
    }

}
