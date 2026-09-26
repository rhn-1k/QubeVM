/*
Copyright (C) Max Kastanas 2012
Copyright (C) Rhn 2026
 */
package com.max2idea.android.qube.machine;

import com.max2idea.android.qube.main.ViewListener;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Routes UI changes through the backend controller without allowing direct model writes. */
public final class Dispatcher implements ViewListener {
    private static final Dispatcher INSTANCE = new Dispatcher();
    private final ExecutorService dispatcher = Executors.newFixedThreadPool(1);

    private Dispatcher() {
    }

    public static synchronized Dispatcher getInstance() {
        return INSTANCE;
    }

    @Override
    public void onFieldChange(final MachineProperty property, final Object value) {
        dispatcher.submit(() -> requestFieldChange(property, value));
    }

    private void requestFieldChange(MachineProperty property, Object value) {
        Machine machine = getMachine();
        if (machine == null) {
            return;
        }
        switch (property) {
            case DRIVE_ENABLED:
                setDriveEnabled(value);
                break;
            case NON_REMOVABLE_DRIVE:
            case REMOVABLE_DRIVE:
                setDrive(value);
                break;
            case MEDIA_INTERFACE:
                setDriveMediaInterface(value);
                break;
            case SOUNDCARD:
                machine.setSoundCard(convertString(property, value));
                break;
            case ENABLE_VENUS:
                machine.setEnableVenus(convertBoolean(property, value) ? 1 : 0);
                break;
            case CPU:
                machine.setCpu(convertString(property, value));
                break;
            case MEMORY:
                machine.setMemory(convertInt(property, value));
                break;
            case TCG_BUFFER:
                machine.setTcgBuffer(convertInt(property, value));
                break;
            case CPUNUM:
                machine.setCpuNum(convertInt(property, value));
                break;
            case KERNEL:
                machine.setKernel(convertString(property, value));
                break;
            case INITRD:
                machine.setInitRd(convertString(property, value));
                break;
            case BIOS:
                machine.setBios(convertString(property, value));
                break;
            case BIOS_TYPE:
                machine.setBiosType(convertString(property, value));
                break;
            case BIOS_CODE:
                machine.setBiosCode(convertString(property, value));
                break;
            case BIOS_VARS:
                machine.setBiosVars(convertString(property, value));
                break;
            case APPEND:
                machine.setAppend(convertString(property, value));
                break;
            case BOOT_CONFIG:
                machine.setBootDevice(convertString(property, value));
                break;
            case NETCONFIG:
                machine.setNetwork(convertString(property, value));
                break;
            case NICCONFIG:
                machine.setNetworkCard(convertString(property, value));
                break;
            case DISABLE_HPET:
                machine.setDisableHPET(convertBoolean(property, value) ? 1 : 0);
                break;
            case DISABLE_TSC:
                machine.setDisableTSC(convertBoolean(property, value) ? 1 : 0);
                break;
            case VGA:
                machine.setVga(convertString(property, value));
                break;
            case DISABLE_ACPI:
                machine.setDisableACPI(convertBoolean(property, value) ? 1 : 0);
                break;
            case DISABLE_FD_BOOT_CHK:
                machine.setDisableFdBootChk(convertBoolean(property, value) ? 1 : 0);
                break;
            case ENABLE_KVM:
                machine.setEnableKVM(convertBoolean(property, value) ? 1 : 0);
                break;
            case ENABLE_MTTCG:
                machine.setEnableMTTCG(convertBoolean(property, value) ? 1 : 0);
                break;
            case PRIO:
                machine.setPrio(convertBoolean(property, value) ? 1 : 0);
                break;
            case BOOT_MENU:
                machine.setBootMenu(convertBoolean(property, value) ? 1 : 0);
                break;
            case HOSTFWD:
                machine.setHostFwd(convertString(property, value));
                break;
            case DNS:
                machine.setDns(convertString(property, value));
                break;
            case MOUSE:
                changeMouse(convertString(property, value));
                break;
            case UI:
                changeUi(convertString(property, value));
                break;
            case KEYBOARD:
                machine.setKeyboard(convertString(property, value));
                break;
            case MACHINETYPE:
                machine.setMachineType(convertString(property, value));
                break;
            case EXTRA_PARAMS:
                machine.setExtraParams(convertString(property, value));
                break;
            default:
                throw new RuntimeException("Unmapped UI field: " + property);
        }
    }

    private void setDriveValue(MachineProperty diskFileType, String drivePath) {
        Machine machine = getMachine();
        if (machine == null) {
            return;
        }
        switch (diskFileType) {
            case HDA:
                machine.setHdaImagePath(drivePath);
                MachineFilePaths.insertRecentFilePath(Machine.FileType.HDA, drivePath);
                break;
            case HDB:
                machine.setHdbImagePath(drivePath);
                MachineFilePaths.insertRecentFilePath(Machine.FileType.HDB, drivePath);
                break;
            case HDC:
                machine.setHdcImagePath(drivePath);
                MachineFilePaths.insertRecentFilePath(Machine.FileType.HDC, drivePath);
                break;
            case HDD:
                machine.setHddImagePath(drivePath);
                MachineFilePaths.insertRecentFilePath(Machine.FileType.HDD, drivePath);
                break;
            case SHARED_FOLDER:
                machine.setSharedFolderPath(drivePath);
                MachineFilePaths.insertRecentFilePath(Machine.FileType.SHARED_DIR, drivePath);
                break;
            case CDROM:
                MachineController.getInstance().changeRemovableDevice(diskFileType, drivePath);
                MachineFilePaths.insertRecentFilePath(Machine.FileType.CDROM, drivePath);
                break;
            case FDA:
                MachineFilePaths.insertRecentFilePath(Machine.FileType.FDA, drivePath);
                MachineController.getInstance().changeRemovableDevice(diskFileType, drivePath);
                break;
            case FDB:
                MachineController.getInstance().changeRemovableDevice(diskFileType, drivePath);
                MachineFilePaths.insertRecentFilePath(Machine.FileType.FDB, drivePath);
                break;
            case SD:
                MachineController.getInstance().changeRemovableDevice(diskFileType, drivePath);
                MachineFilePaths.insertRecentFilePath(Machine.FileType.SD, drivePath);
                break;
            case BIOS:
                machine.setBios(drivePath);
                MachineFilePaths.insertRecentFilePath(Machine.FileType.BIOS, drivePath);
                break;
            case BIOS_CODE:
                machine.setBiosCode(drivePath);
                MachineFilePaths.insertRecentFilePath(Machine.FileType.BIOS_CODE, drivePath);
                break;
            case BIOS_VARS:
                machine.setBiosVars(drivePath);
                MachineFilePaths.insertRecentFilePath(Machine.FileType.BIOS_VARS, drivePath);
                break;
            default:
                break;
        }
    }

    private void changeMouse(String mouseCfg) {
        String[] values = mouseCfg.split(" ");
        getMachine().setMouse(values[0]);
    }

    private void setMachineEnableDevice(MachineProperty machineProperty, boolean isChecked) {
        Machine machine = getMachine();
        if (machine == null) {
            return;
        }
        switch (machineProperty) {
            case CDROM:
                machine.setEnableCDROM(isChecked);
                break;
            case FDA:
                machine.setEnableFDA(isChecked);
                break;
            case FDB:
                machine.setEnableFDB(isChecked);
                break;
            case SD:
                machine.setEnableSD(isChecked);
                break;
            default:
                break;
        }
    }

    private void changeUi(String ui) {
        Machine machine = getMachine();
        if (machine == null) {
            return;
        }
        if ("VNC".equals(ui)) {
            machine.setRenderer(1);
            machine.setEnableVNC(1);
        } else if ("QGE".equals(ui)) {
            machine.setRenderer(0);
            machine.setEnableVNC(0);
        }
    }

    private void setDriveMediaInterface(Object value) {
        Object[] params = (Object[]) value;
        MachineProperty driveName = (MachineProperty) params[0];
        String driveInterface = (String) params[1];
        Machine machine = getMachine();
        if (machine == null) {
            return;
        }
        switch (driveName) {
            case HDA:
                machine.setHdaInterface(driveInterface);
                break;
            case HDB:
                machine.setHdbInterface(driveInterface);
                break;
            case HDC:
                machine.setHdcInterface(driveInterface);
                break;
            case HDD:
                machine.setHddInterface(driveInterface);
                break;
            case CDROM:
                machine.setCdInterface(driveInterface);
                break;
            case SHARED_FOLDER:
                machine.setSharedFolderType(driveInterface);
                break;
            default:
                break;
        }
    }

    private void setDriveEnabled(Object value) {
        Object[] params = (Object[]) value;
        MachineProperty driveName = (MachineProperty) params[0];
        boolean checked = (Boolean) params[1];
        setMachineEnableDevice(driveName, checked);
        setDriveValue(driveName, checked ? "" : null);
    }

    private void setDrive(Object value) {
        Object[] params = (Object[]) value;
        MachineProperty driveName = (MachineProperty) params[0];
        String diskFileValue = (String) params[1];
        if ("None".equals(diskFileValue) && isDriveEnabled(driveName)) {
            setDriveValue(driveName, "");
        } else if ("None".equals(diskFileValue) || !isDriveEnabled(driveName)) {
            setDriveValue(driveName, null);
        } else if (isDriveEnabled(driveName)) {
            setDriveValue(driveName, diskFileValue);
        }
    }

    private boolean isDriveEnabled(MachineProperty property) {
        Machine machine = getMachine();
        if (machine == null) {
            return false;
        }
        switch (property) {
            case CDROM:
                return machine.isEnableCDROM();
            case FDA:
                return machine.isEnableFDA();
            case FDB:
                return machine.isEnableFDB();
            case SD:
                return machine.isEnableSD();
            default:
                return true;
        }
    }

    @Override
    public void onAction(final MachineAction action, final Object value) {
        dispatcher.submit(() -> requestAction(action, value));
    }

    private void requestAction(MachineAction action, Object value) {
        switch (action) {
            case DELETE_VM:
                deleteVM((Machine) value);
                break;
            case CREATE_VM:
                createVM(convertString(action, value));
                break;
            case STOP_VM:
                MachineController.getInstance().stopvm();
                break;
            case START_VM:
                MachineController.getInstance().startvm();
                break;
            case RESET_VM:
                MachineController.getInstance().restartvm();
                break;
            case LOAD_VM:
                MachineController.getInstance().setStoredMachine((String) value);
                break;
            case INSERT_FAV:
                addDriveToList(value);
                break;
            case UPDATE_NOTIFICATION:
                Machine machine = MachineController.getInstance().getMachine();
                MachineService service = MachineService.getService();
                if (machine != null && service != null) {
                    service.updateServiceNotification(machine.getName() + ": " + (String) value);
                }
                break;
            case FULLSCREEN:
                MachineController.getInstance().setFullscreen();
                break;
            default:
                break;
        }
    }

    private void addDriveToList(Object value) {
        Object[] params = (Object[]) value;
        Machine.FileType fileType = (Machine.FileType) params[0];
        String filePath = (String) params[1];
        MachineFilePaths.insertRecentFilePath(fileType, filePath);
    }

    private boolean deleteVM(Machine machine) {
        return MachineController.getInstance().deleteMachine(machine);
    }

    private boolean createVM(String machineName) {
        return MachineController.getInstance().createVM(machineName);
    }

    private Machine getMachine() {
        return MachineController.getInstance().getMachine();
    }

    private String convertString(MachineProperty property, Object value) {
        if (value instanceof String) {
            return (String) value;
        }
        throw new RuntimeException("Unknown property value: " + value + " for: " + property);
    }

    private String convertString(MachineAction action, Object value) {
        if (value == null || value instanceof String) {
            return (String) value;
        }
        throw new RuntimeException("Unknown action value: " + value + " for: " + action);
    }

    private int convertInt(MachineProperty property, Object value) {
        if (value instanceof String) {
            return Integer.parseInt((String) value);
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        throw new RuntimeException("Unknown property value: " + value + " for: " + property);
    }

    private boolean convertBoolean(MachineProperty property, Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        throw new RuntimeException("Unknown property value: " + value + " for: " + property);
    }
}
