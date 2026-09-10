/*
Copyright (C) Max Kastanas 2012
Copyright (C) Rhn 2026
 */
package com.max2idea.android.qube.jni;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;

import com.qube.emu.lib.BuildConfig;
import com.qube.emu.lib.R;
import com.max2idea.android.qube.files.FileInstaller;
import com.max2idea.android.qube.files.FileUtils;
import com.max2idea.android.qube.machine.GraphicsCapabilities;
import com.max2idea.android.qube.machine.Machine;
import com.max2idea.android.qube.machine.MachineController;
import com.max2idea.android.qube.machine.MachineExecutor;
import com.max2idea.android.qube.machine.MachineProperty;
import com.max2idea.android.qube.main.Config;
import com.max2idea.android.qube.main.QubeApplication;
import com.max2idea.android.qube.main.QubeSettingsManager;
import com.max2idea.android.qube.qmp.QmpClient;
import com.max2idea.android.qube.toast.ToastUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;


/**
 * Class is used to start and stop the qemu process and communicate file descriptions, mouse,
 * and keyboard events.
 */
class VMExecutor extends MachineExecutor {
    private static final String TAG = "VMExecutor";

    private static final String cdDeviceName = "ide1-cd0";
    private static final String fdaDeviceName = "floppy0";
    private static final String fdbDeviceName = "floppy1";
    private static final String sdDeviceName = "sd0";
    private static final String DEFAULT_UEFI = "Default";
    //TODO: make this a proper singleton but the views should not be able to access it
    private static VMExecutor mInstance;

    VMExecutor(MachineController machineController) {
        super(machineController);
        mInstance = this;
    }

    //JNI Methods
    private native String start(String storage_dir, String base_dir,
                                String lib_filename, String lib_path,
                                Object[] params);

    private native String stop(int restart);

    public native void nativeRefreshScreen(int value);

    /**
     * Prints parameters in qemu format
     *
     * @param params Parameters to be printed
     */
    public void printParams(String[] params) {
        Log.d(TAG, "Params:");
        for (int i = 0; i < params.length; i++) {
            Log.d(TAG, i + ": " + params[i]);
        }
    }

    // Translate to QEMU format
    private String getSoundCard() {
        if (Config.enableQGESound && getMachine().getSoundCard() != null
                && !getMachine().getSoundCard().toLowerCase().equals("none"))
            return getMachine().getSoundCard();
        return null;
    }

    private String getQemuLibrary() {
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
            default:
                throw new IllegalStateException("Unexpected value: " + QubeApplication.arch);
        }
    }

    private String[] prepareParams(Context context) throws Exception {
        ArrayList<String> paramsList = new ArrayList<>();
        paramsList.add(getQemuLibrary());
        addUIOptions(context, paramsList);
        addCpuBoardOptions(paramsList);
        addDrives(paramsList);
        addRemovableDrives(paramsList);
        addBootOptions(paramsList);
        addGraphicsOptions(context, paramsList);
        addAudioOptions(paramsList);
        addNetworkOptions(paramsList);
        addGenericOptions(context, paramsList);
        addAdvancedOptions(paramsList);
        addAccelerationOptions(paramsList);
        return paramsList.toArray(new String[0]);
    }

    private void addUIOptions(Context context, ArrayList<String> paramsList) {
        if (getMachine().getRenderer() == 1) {
            paramsList.add("-vnc");
            String vncHost = Config.defaultVNCHost;
            if (QubeSettingsManager.getEnableExternalVNC(context)) {
                // Allow connections from outside localhost
                vncHost = "0.0.0.0";
            }
            paramsList.add(vncHost + ":" + Config.defaultVNCPort);
        } else {
            // Using our own wrapper
            paramsList.add("-display");
            paramsList.add("qube-qge");
        }

        if (getMachine().getKeyboard() != null) {
            paramsList.add("-k");
            paramsList.add(getMachine().getKeyboard());
        }

        if (getMachine().getMouse() != null && !getMachine().getMouse().equals("ps2")) {
            // Arm needs specific usb type to allow usb-tablet
            if (QubeApplication.arch == Config.Arch.arm || QubeApplication.arch == Config.Arch.arm64) {
                paramsList.add("-device");
                paramsList.add("qemu-xhci");
            }

            paramsList.add("-usb");
            paramsList.add("-device");
            paramsList.add(getMachine().getMouse());
        }
    }

    private void addAdvancedOptions(ArrayList<String> paramsList) {

        if (getMachine().getExtraParams() != null && !getMachine().getExtraParams().trim().equals("")) {
            String[] paramsTmp = getMachine().getExtraParams().split(" ");
            paramsList.addAll(Arrays.asList(paramsTmp));
        }
    }

    private void addAudioOptions(ArrayList<String> paramsList) {
        // Native AAudio path via qube-audio
        String soundCard = getSoundCard();
        if (soundCard == null) {
            return;
        }
        paramsList.add("-audiodev");
        paramsList.add("qube,id=qube-audio-out");

        if (soundCard.toLowerCase().equals("hda")) {
            // Use hda-output (not duplex/micro) since our audiodev is output only
            paramsList.add("-device");
            paramsList.add("intel-hda");
            paramsList.add("-device");
            paramsList.add("hda-output,audiodev=qube-audio-out");
        } else {
            paramsList.add("-device");
            paramsList.add(soundCard + ",audiodev=qube-audio-out");
        }
    }

    private void addGenericOptions(Context context, ArrayList<String> paramsList) {
        paramsList.add("-L");
        paramsList.add(QubeApplication.getBasefileDir());
        if (QubeSettingsManager.getEnableQmp(context)) {
            paramsList.add("-qmp");
            if (getQMPAllowExternal()) {
                String qmpParams = "tcp:";
                qmpParams += (":" + Config.QMPPort);
                qmpParams += ",server,nowait";
                paramsList.add(qmpParams);
            } else {
                //Specify a unix local domain as localhost to limit to local connections only
                String qmpParams = "unix:";
                qmpParams += QubeApplication.getLocalQMPSocketPath();
                qmpParams += ",server,nowait";
                paramsList.add(qmpParams);
            }
        }

        //Enable Tracing log
        if (Config.enableTracingLog) {
            paramsList.add("-D");
            paramsList.add(Config.traceLogFile);
            paramsList.add("--trace");
            paramsList.add("events=" + Config.traceEventsFile);
            paramsList.add("--trace");
            paramsList.add("file=" + Config.traceDir);
        }

        // tb-size moved into addAccelerationOptions() single -accel line
        // QEMU only honors the first -accel entry, a second one is ignored entirely

        paramsList.add("-overcommit");
        paramsList.add("mem-lock=off");

        paramsList.add("-rtc");
        paramsList.add("base=localtime");

    }

    private void addCpuBoardOptions(ArrayList<String> paramsList) {

        // XXX: SMP is not working correctly for some guest OSes
        // so we enable multi core only under KVM
        if (getMachine().getCpuNum() > 1) {
            paramsList.add("-smp");
            paramsList.add(getMachine().getCpuNum() + "");
        }

        StringBuilder machineOpt = new StringBuilder();
        if (getMachineType() != null && !getMachineType().equals("Default")) {
            machineOpt.append(getMachineType());
        }
        if (getMachine().getDisableAcpi() != 0) {
            if (machineOpt.length() > 0) machineOpt.append(",");
            machineOpt.append("acpi=off"); //disable ACPI
        }
        if (getMachine().getDisableHPET() != 0) {
            if (machineOpt.length() > 0) machineOpt.append(",");
            machineOpt.append("hpet=off"); //disable HPET
        }
        if (machineOpt.length() > 0) {
            paramsList.add("-M");
            paramsList.add(machineOpt.toString());
        }

        String cpu = getMachine().getCpu();

        //XXX: we disable tsc feature for x86 since some guests are kernel panicking
        // if the cpu has not specified by user we use the internal qemu32/64
        if (getMachine().getDisableTSC() == 1 && (QubeApplication.arch == Config.Arch.x86 || QubeApplication.arch == Config.Arch.x86_64)) {
            if (cpu == null || cpu.equals("Default")) {
                if (QubeApplication.arch == Config.Arch.x86)
                    cpu = "qemu32";
                else if (QubeApplication.arch == Config.Arch.x86_64)
                    cpu = "qemu64";
            }
            cpu += ",-tsc";
        }

        //+svm exposes AMD-V nested virtualization to the guest, useful for running
        //hypervisors/VMs inside the emulated guest OS
        if (getMachine().getEnableSVM() == 1 && (QubeApplication.arch == Config.Arch.x86 || QubeApplication.arch == Config.Arch.x86_64)) {
            if (cpu == null || cpu.equals("Default")) {
                if (QubeApplication.arch == Config.Arch.x86)
                    cpu = "qemu32";
                else if (QubeApplication.arch == Config.Arch.x86_64)
                    cpu = "qemu64";
            }
            cpu += ",+svm";
        }

        if (cpu != null && !cpu.equals("Default")) {
            paramsList.add("-cpu");
            paramsList.add(cpu);
        }

        paramsList.add("-m");
        paramsList.add(getMachine().getMemory() + "");
    }


    private void addAccelerationOptions(ArrayList<String> paramsList) {

        // XXX: we add the acceleration options after the extra params
        // this is due to QEMU applying the first instance of this option
        // so the extra params cannot override it
        if (getMachine().getEnableKVM() != 0) {
            paramsList.add("-accel");
            paramsList.add("kvm");
        } else {
            paramsList.add("-accel");
            String tcgParams = "tcg";
            if (getMachine().getEnableMTTCG() != 0) {
                tcgParams += ",thread=multi";
            } else {
                tcgParams += ",thread=single";
            }
            if (getMachine().getTcgBuffer() > 0) {
                tcgParams += ",tb-size=" + getMachine().getTcgBuffer();
            }
            paramsList.add(tcgParams);
        }
    }

    private String getMachineType() {
        return getMachine().getMachineType();
    }

    private void addNetworkOptions(ArrayList<String> paramsList) throws Exception {

        String network = getNetCfg();
        String networkCard = getNicCard();

        if (network == null || network.equals("none")) {
            paramsList.add("-net");
            paramsList.add("none");
            return;
        }

        // TAP config
        if (network.equals("tap")) {
            paramsList.add("-netdev");
            paramsList.add("tap,id=net0,ifname=tap0,script=no");
        }

        // User config
        if (network.equals("user")) {
            String netdevParams = "user,id=net0";
            String hostFwd = getHostFwd();
            if (hostFwd != null) {

                //hostfwd=[tcp|udp]:[hostaddr]:hostport-[guestaddr]:guestport{,hostfwd=...}
                // example forward ssh from guest port 2222 to guest port 22:
                // hostfwd=tcp::2222-:22
                if (hostFwd.startsWith("hostfwd")) {
                    throw new Exception("Invalid format for Host Forward, should be: tcp:hostport1:guestport1,udp:hostport2:questport2,...");
                }
                String[] hostFwdParams = hostFwd.split(",");
                for (int i = 0; i < hostFwdParams.length; i++) {
                    String[] hostfwdparam = hostFwdParams[i].split(":");
                    netdevParams += (",hostfwd=" + hostfwdparam[0] + "::" + hostfwdparam[1] + "-:" + hostfwdparam[2]);
                }
            }
            paramsList.add("-netdev");
            paramsList.add(netdevParams);
        }

        paramsList.add("-device");
        paramsList.add(networkCard + ",netdev=net0");
    }

    private String getHostFwd() {
        if (getMachine().getNetwork().equals("User")) {
            if (getMachine().getHostFwd() != null && !getMachine().getHostFwd().equals(""))
                return getMachine().getHostFwd();
        }
        return null;
    }

    private String getNicCard() {
        if (getMachine().getNetwork() == null || getMachine().getNetwork().equals("None")) {
            return null;
        } else if (getMachine().getNetwork().equals("User")) {
            return getMachine().getNetworkCard();
        } else if (getMachine().getNetwork().equals("TAP")) {
            return getMachine().getNetworkCard();
        }
        return null;
    }

    private String getNetCfg() {
        if (getMachine().getNetwork() == null || getMachine().getNetwork().equals("None")) {
            return "none";
        } else if (getMachine().getNetwork().equals("User")) {
            return "user";
        } else if (getMachine().getNetwork().equals("TAP")) {
            return "tap";
        }
        return null;
    }

    private void addGraphicsOptions(Context context, ArrayList<String> paramsList) {
        String vga = getMachine().getVga();
        if (vga != null) {
            if (vga.equals("Default")) {
                //do nothing
            } else if (vga.equals("nographic")) {
                paramsList.add("-nographic");
            } else if (GraphicsCapabilities.isDeviceBackedGpu(vga)) {
                // -vga lacks the virtio-gpu-* variants
                // These must use -device instead

                // q35 machine auto-adds default VGA, stealing it from our GL device
                // so we set -vga none to prevent it from choosing
                paramsList.add("-vga");
                paramsList.add("none");

                String deviceStr = vga;

                boolean venus = BuildConfig.USE_VENUS && GraphicsCapabilities.supportsVenus(vga) && getMachine().getEnableVenus() == 1;
                if (venus) {
                    // Venus needs QEMU's blob-resource machinery
                    // a "hostmem" window on the device plus a shared
                    // host-visible memory-backend for the whole VM
                    paramsList.add("-object");
                    paramsList.add("memory-backend-memfd,id=venusmem,size=" + getMachine().getMemory() + "M");
                    paramsList.add("-machine");
                    paramsList.add("memory-backend=venusmem");
                    deviceStr += ",blob=on,hostmem=" + Config.venusHostMem + ",venus=on";
                }

                paramsList.add("-device");
                paramsList.add(deviceStr);

                // gl/virgl/venus devices need a display backend with its own GL context
                // qube-qge is that backend, see qge.c
                String displayBackend = GraphicsCapabilities.getDisplayBackend(vga);
                if (displayBackend != null) {
                    paramsList.add("-display");
                    paramsList.add(displayBackend);
                }
            } else {
                paramsList.add("-vga");
                paramsList.add(vga);
            }
        }
    }

    private void addBootOptions(ArrayList<String> paramsList) {
        if ("UEFI".equalsIgnoreCase(getMachine().getBiosType())) {
            addUefiFirmware(paramsList);
        } else {
            String bios = getDriveFilePath(getMachine().getBios());
            if (bios != null && !bios.equals("None")) {
                paramsList.add("-bios");
                paramsList.add(bios);
            }
        }

        if (getBootDevice() != null) {
            paramsList.add("-boot");
            paramsList.add(getBootDevice());
        }

        String kernel = getKernel();
        if (kernel != null && !kernel.equals("")) {
            paramsList.add("-kernel");
            paramsList.add(kernel);
        }

        String initrd = getInitRd();
        if (initrd != null && !initrd.equals("")) {
            paramsList.add("-initrd");
            paramsList.add(initrd);
        }

        if (getMachine().getAppend() != null && !getMachine().getAppend().equals("")) {
            paramsList.add("-append");
            paramsList.add(getMachine().getAppend());
        }
    }

    private void addUefiFirmware(ArrayList<String> paramsList) {
        String vars = resolveUefiFirmware(getMachine().getBiosVars(), true);
        if (vars == null) {
            throw new IllegalStateException("UEFI vars firmware is unavailable");
        }

        String code = resolveUefiFirmware(getMachine().getBiosCode(), false);
        if (code != null) {
            paramsList.add("-drive");
            paramsList.add("if=pflash,unit=0,format=raw,readonly=on,file=" + code);
        }
        paramsList.add("-drive");
        paramsList.add("if=pflash,unit=1,format=raw,file=" + vars);
    }

    // Resolve Default UEFI settings to the shared bundled Code/Vars files
    private String resolveUefiFirmware(String configuredPath, boolean vars) {
        if (configuredPath == null || configuredPath.trim().isEmpty()
                || DEFAULT_UEFI.equalsIgnoreCase(configuredPath.trim())) {
            String bundledFilename = getBundledUefiFilename(vars);
            return new File(QubeApplication.getBasefileDir(), bundledFilename).getAbsolutePath();
        }
        return getDriveFilePath(configuredPath);
    }

    // Select the bundled UEFI filename for the active architecture
    private String getBundledUefiFilename(boolean vars) {
        switch (QubeApplication.arch) {
            case x86:
            case x86_64:
                return vars ? Config.UEFI_X86_VARS : Config.UEFI_X86_CODE;
            case arm:
            case arm64:
                return vars ? Config.UEFI_ARM_VARS : Config.UEFI_ARM_CODE;
            default:
                throw new IllegalStateException("Bundled UEFI is unavailable for architecture: "
                        + QubeApplication.arch);
        }
    }

    private String getBootDevice() {

        if (QubeApplication.arch == Config.Arch.arm || QubeApplication.arch == Config.Arch.arm64) {
            return null;
        } else if (getMachine().getBootDevice().equals("Default")) {
            return null;
        } else if (getMachine().getBootDevice().equals("CDROM")) {
            return "d";
        } else if (getMachine().getBootDevice().equals("Floppy")) {
            return "a";
        } else if (getMachine().getBootDevice().equals("Hard Disk")) {
            return "c";
        }
        return null;
    }

    private String getInitRd() {
        return getDriveFilePath(getMachine().getInitRd());
    }

    private String getKernel() {
        return getDriveFilePath(getMachine().getKernel());
    }

    public String getDriveFilePath(String driveFilePath) {
        if (driveFilePath == null || driveFilePath.trim().isEmpty()
                || driveFilePath.equals("None"))
            return null;

        String realPath = FileUtils.getRealPathForFile(QubeApplication.getInstance(), driveFilePath);
        if (realPath != null) {
            Log.d(TAG, "getDriveFilePath: resolved to real path: " + realPath);
            return realPath;
        }

        return FileUtils.getQemuFilePath(driveFilePath);
    }

    public void addDrives(ArrayList<String> paramsList) {
        addHardDisk(paramsList, getDriveFilePath(getMachine().getHdaImagePath()),
                0, getMachine().getHdaInterface());
        addHardDisk(paramsList, getDriveFilePath(getMachine().getHdbImagePath()),
                1, getMachine().getHdbInterface());
        addHardDisk(paramsList, getDriveFilePath(getMachine().getHdcImagePath()),
                2, getMachine().getHdcInterface());
        addHardDisk(paramsList, getDriveFilePath(getMachine().getHddImagePath()),
                3, getMachine().getHddInterface());
        addSharedFolder(paramsList, getMachine().getSharedFolderPath());
    }

    public void addHardDisk(ArrayList<String> paramsList, String imagePath, int index, String hdInterface) {
        if (imagePath != null && !imagePath.trim().equals("")) {
            paramsList.add("-drive");
            String param = "index=" + index;
            param += ",if=";
            param += hdInterface;
            param += ",media=disk";
            if (!imagePath.equals("")) {
                param += ",file=" + imagePath;
            }
            String cache = QubeSettingsManager.getDiskCache(QubeApplication.getInstance());
            if(cache != null && !cache.equals("default"))
                param += ",cache=" + cache;
            paramsList.add(param);
        }
    }

    public void addSharedFolder(ArrayList<String> paramsList, String sharedFolderPath) {
        if (!Config.enableSharedFolder || sharedFolderPath == null || sharedFolderPath.trim().isEmpty())
            return;

        // QEMU's "fat:rw:" driver needs a real path, not a SAF content:// URI,
        // or it fails to create its temp file and crashes the whole process.
        String realPath = FileUtils.getRealPathForSharedFolder(QubeApplication.getInstance(), sharedFolderPath);
        if (realPath == null) {
            Log.w(TAG, "Shared folder could not be resolved to a real path, skipping: " + sharedFolderPath);
            ToastUtils.toastLong(QubeApplication.getInstance(),
                    QubeApplication.getInstance().getString(R.string.SharedFolderNotSupported));
            return;
        }

        //XXX; We use hdd to mount any virtual fat drives
        paramsList.add("-drive"); //empty
        String driveParams = "index=3";
        driveParams += ",media=disk";
        driveParams += ",if=ide";
        driveParams += ",format=raw";
        driveParams += ",file=fat:";
        driveParams += "rw:"; //Always Read/Write
        driveParams += realPath;
        paramsList.add(driveParams);
    }

    public void addRemovableDrives(ArrayList<String> paramsList) {
        String cdImagePath = getDriveFilePath(getMachine().getCdImagePath());
        if (cdImagePath != null) {
            paramsList.add("-drive"); //empty
            String param = "index=2";
            param += ",if=";
            param += getMachine().getCDInterface();
            param += ",media=cdrom";
            param += ",id=" + cdDeviceName;
            if (!cdImagePath.equals("")) {
                param += ",file=" + cdImagePath;
            }
            paramsList.add(param);
        }

        String fdaImagePath = getDriveFilePath(getMachine().getFdaImagePath());
        if (Config.enableEmulatedFloppy && fdaImagePath != null) {
            paramsList.add("-drive"); //empty
            String param = "index=0,if=floppy,id=" + fdaDeviceName;
            if (!fdaImagePath.equals("")) {
                param += ",file=" + fdaImagePath;
            }
            paramsList.add(param);
        }

        String fdbImagePath = getDriveFilePath(getMachine().getFdbImagePath());
        if (Config.enableEmulatedFloppy && fdbImagePath != null) {
            paramsList.add("-drive"); //empty
            String param = "index=1,if=floppy,id=" + fdbDeviceName;
            if (!fdbImagePath.equals("")) {
                param += ",file=" + fdbImagePath;
            }
            paramsList.add(param);
        }


    }


    protected String changedev(String dev, String value) {
        String response = QmpClient.sendCommand(QmpClient.getChangeDeviceCommand(dev, value));
        String displayDevValue = FileUtils.getFullPathFromDocumentFilePath(value);
        if (response != null && response.contains("\"error\"")) {
            String errDesc = response;
            try {
                JSONObject resObj = new JSONObject(response);
                if (resObj.has("error")) {
                    errDesc = resObj.getJSONObject("error").optString("desc", response);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Log.e(TAG, "changedev QMP error for " + dev + ": " + errDesc);
            ToastUtils.toastLong(QubeApplication.getInstance(), Gravity.BOTTOM,
                    QubeApplication.getInstance().getString(R.string.CouldNotOpenDocFile) + ": " + errDesc);
            return null;
        }
        if (Config.debug)
            ToastUtils.toastLong(QubeApplication.getInstance(), Gravity.BOTTOM,
                    QubeApplication.getInstance().getString(R.string.ChangedDevice) + ": "
                            + dev + ": " + displayDevValue);
        return response;
    }

    protected String ejectdev(String dev) {
        String response = QmpClient.sendCommand(QmpClient.getEjectDeviceCommand(dev));
        if (Config.debug)
            ToastUtils.toastLong(QubeApplication.getInstance(), Gravity.BOTTOM,
                    QubeApplication.getInstance().getString(R.string.EjectedDevice) + ": " + dev);
        return response;
    }


    /**
     * Starts the service that will later start the qemu process
     */
    public void startService() {
        Intent i = new Intent(Config.ACTION_START, null, QubeApplication.getInstance(),
                MachineController.getInstance().getServiceClass());
        Bundle b = new Bundle();
        i.putExtras(b);
        Log.d(TAG, "Starting VM service");
        QubeApplication.getInstance().startService(i);
    }

    /**
     * Starts the native process. This should be called from a background thread from a
     * foreground service in order to prevent the process from being killed
     *
     * @return String from the native code vm-executor-jni.cpp
     */
    public String start() {
        String res = null;
        try {
            String[] params = prepareParams(QubeApplication.getInstance());
            printParams(params);

            QmpClient.setExternal(QubeSettingsManager.getEnableExternalQMP(QubeApplication.getInstance()));
            String libFilename = getQemuLibrary();
            res = start(Config.storagedir, QubeApplication.getBasefileDir(),
                    libFilename, FileUtils.getNativeLibDir(QubeApplication.getInstance()) + "/" + libFilename,
                    params);
        } catch (Exception ex) {
            ToastUtils.toastLong(QubeApplication.getInstance(), ex.getMessage());
            return res;
        }
        return res;
    }

    public void stopvm(final int restart) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (restart != 0) {
                    QmpClient.sendCommand(QmpClient.getResetCommand());
                } else {
                    //XXX: Qmp command only halts the VM but doesn't exit so we use force close
//            QmpClient.sendCommand(QmpClient.powerDown());
                    stop(restart);
                }
            }
        }).start();
    }

    @Override
    public String getDeviceName(MachineProperty driveProperty) {
        switch (driveProperty) {
            case CDROM:
                return cdDeviceName;
            case FDA:
                return fdaDeviceName;
            case FDB:
                return fdbDeviceName;
            case SD:
                return sdDeviceName;
        }
        return null;
    }

    //TODO: re-enable getting status from the vm
    public String getVmState() {
        String res = QmpClient.sendCommand(QmpClient.getStateCommand());
        String state = "";
        if (res != null && !res.equals("")) {
            try {
                JSONObject resObj = new JSONObject(res);
                String resInfo = resObj.getString("return");
                JSONObject resInfoObj = new JSONObject(resInfo);
                state = resInfoObj.getString("status");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return state;
    }

    /**
     * Function sends a command via qmp to change or eject the removable device
     *
     * @param drive     The device to be changed
     * @param imagePath If its null it ejects the drive otherwise it uses the disk file at that path
     */
    public boolean changeRemovableDevice(final MachineProperty drive, final String imagePath) {
        if (!QubeSettingsManager.getEnableQmp(QubeApplication.getInstance())) {
            ToastUtils.toastShort(QubeApplication.getInstance(), QubeApplication.getInstance().getString(R.string.EnableQMPForChangingDrives));
            return false;
        }
        String dev = getDeviceName(drive);

        //XXX: first we eject any previous media
        String response = VMExecutor.this.ejectdev(dev);

        // if there is no media there is nothing else to do
        if (imagePath == null || imagePath.trim().equals("")) {
            return true;
        }

        // Use our function in FileUtils.java to get the real not encoded path
        String imagePathConverted = getDriveFilePath(imagePath);


        if (!FileUtils.fileValid(imagePathConverted)) {
            String msg = QubeApplication.getInstance().getString(R.string.CouldNotOpenDocFile) + " "
                    + FileUtils.getFullPathFromDocumentFilePath(imagePathConverted)
                    + "\n" + QubeApplication.getInstance().getString(R.string.PleaseReassingYourDiskFiles);
            ToastUtils.toastLong(QubeApplication.getInstance(), msg);
            return false;
        }
        response = VMExecutor.this.changedev(dev, imagePathConverted);
        if (response == null)
            return false;

        return true;
    }

    /**
     * Fuction is a pass thru from the c get_fd() function called from native code
     * This is bridged to the java code because it's the only way to open a file descriptor
     * from the native code
     *
     * @param path File path
     * @return Return value of FileUtils.get_fd()
     */
    public int get_fd(String path) {
        return FileUtils.get_fd(path);
    }

    /**
     * Fuction is a pass thru from the c close_fd() function called from native code
     * This is similar to the above get_fd but perhaps not needed.
     *
     * @param fd File Descriptor to be closed
     * @return Return value of FileUtils.close_fd()
     */
    public int close_fd(int fd) {
        return FileUtils.close_fd(fd);
    }

    public boolean getQMPAllowExternal() {
        return QubeSettingsManager.getEnableExternalQMP(QubeApplication.getInstance());
    }
}
