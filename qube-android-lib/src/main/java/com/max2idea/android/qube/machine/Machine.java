/*
Copyright (C) Max Kastanas 2012
Copyright (C) Rhn 2026
 */
package com.max2idea.android.qube.machine;

import com.max2idea.android.qube.main.Config;
import com.max2idea.android.qube.main.QubeApplication;

import java.util.HashSet;
import java.util.Set;

/**
 * Class holds the machine properties and notifies registered change listeners.
 */
public class Machine {
    private static String TAG = "Machine";
    private final Set<ChangeListener> changeListeners = new HashSet<>();

    private String name;
    private String keyboard = Config.defaultKeyboardLayout;
    private String mouse = "ps2";
    private String dns = Config.defaultDNSServer;
    private int enableVNC;
    private int renderer;
    private String arch;
    private String machineType;
    private String cpu = "Default";
    private int cpuNum = 1;
    private int memory = 64;
    private int tcgBuffer = 256;
    private int enableMTTCG;
    private int enableKVM;
    private int prio;
    private int bootMenu;
    private int disableACPI = 0;
    private int disableHPET = 0;
    private int disableFdBootChk = 0;
    private int disableTSC = 0; //TSC left enabled by default (not recommended to disable on modern guests)
    private int enableVenus = 0;
    // Storage
    private String hdaImagePath;
    private String hdbImagePath;
    private String hdcImagePath;
    private String hddImagePath;
    // HDD interface
    private String hdaInterface = "ide";
    private String hdbInterface = "ide";
    private String hdcInterface = "ide";
    private String hddInterface = "ide";
    private String sharedFolderPath;
    // Shared folder interface
    private String sharedFolderType = "vvfat";
    //Removable devices
    private boolean enableCDROM;
    private boolean enableFDA;
    private boolean enableFDB;
    private boolean enableUSB;
    private String cdImagePath;
    private String fdaImagePath;
    private String fdbImagePath;
    private String usbImagePath;
    private String cdInterface = "ide";
    // Default Settings
    private String bootDevice = "Default";
    private String bios;
    private String biosType = "Default";
    private String biosCode;
    private String biosVars;
    private String kernel;
    private String initRd;
    private String append;
    // net
    public static final String DEFAULT_NETWORK_CARD = "e1000";
    private String network = null;
    private String networkCard = DEFAULT_NETWORK_CARD;
    private String guestFwd;
    private String hostFwd;
    //display
    private String vga = "std";
    //sound
    private String soundCard = null;
    //extra qemu params
    private String extraParams;
    private int sharedFolderMode;

    public Machine(String name, boolean loadDefaults) {
        this.name = name;
        if (loadDefaults)
            setDefaults();
    }

    public String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    public int getEnableVNC() {
        return enableVNC;
    }

    void setEnableVNC(int enableVNC) {
        if (this.enableVNC != enableVNC) {
            this.enableVNC = enableVNC;
        }
    }

    public int getRenderer() {
        return renderer;
    }

    void setRenderer(int renderer) {
        if (this.renderer != renderer) {
            this.renderer = renderer;
            notifyChanged(MachineProperty.UI, renderer);
        }
    }

    public String getArch() {
        return arch;
    }

    void setArch(String arch) {
        if (this.arch == null || !this.arch.equals(arch)) {
            this.arch = arch;
            notifyChanged(MachineProperty.ARCH, arch);
        }
    }

    public String getMachineType() {
        return machineType;
    }

    void setMachineType(String machineType) {
        if (this.machineType == null || !this.machineType.equals(machineType)) {
            this.machineType = machineType;
            notifyChanged(MachineProperty.MACHINETYPE, machineType);
        }
    }

    public String getCpu() {
        return cpu;
    }

    void setCpu(String cpu) {
        if (!this.cpu.equals(cpu)) {
            this.cpu = cpu;
            notifyChanged(MachineProperty.CPU, cpu);
        }
    }

    public int getCpuNum() {
        return cpuNum;
    }

    void setCpuNum(int cpuNum) {
        if (this.cpuNum != cpuNum) {
            this.cpuNum = cpuNum;
            notifyChanged(MachineProperty.CPUNUM, cpuNum);
        }
    }

    public int getMemory() {
        return memory;
    }

    void setMemory(int memory) {
        if (this.memory != memory) {
            this.memory = memory;
            notifyChanged(MachineProperty.MEMORY, memory);
        }
    }

    public int getTcgBuffer() {
        return tcgBuffer;
    }

    void setTcgBuffer(int tcgBuffer) {
        if (this.tcgBuffer != tcgBuffer) {
            this.tcgBuffer = tcgBuffer;
            notifyChanged(MachineProperty.TCG_BUFFER, tcgBuffer);
        }
    }

    public int getEnableMTTCG() {
        return enableMTTCG;
    }

    void setEnableMTTCG(int enableMTTCG) {
        if (this.enableMTTCG != enableMTTCG) {
            this.enableMTTCG = enableMTTCG;
            notifyChanged(MachineProperty.ENABLE_MTTCG, enableMTTCG);
        }
    }

    public int getEnableKVM() {
        return enableKVM;
    }

    void setEnableKVM(int enableKVM) {
        if (this.enableKVM != enableKVM) {
            this.enableKVM = enableKVM;
            notifyChanged(MachineProperty.ENABLE_KVM, enableKVM);
        }
    }

    public int getPrio() {
        return prio;
    }

    void setPrio(int prio) {
        if (this.prio != prio) {
            this.prio = prio;
            notifyChanged(MachineProperty.PRIO, prio);
        }
    }

    public int getBootMenu() {
        return bootMenu;
    }

    void setBootMenu(int bootMenu) {
        if (this.bootMenu != bootMenu) {
            this.bootMenu = bootMenu;
            notifyChanged(MachineProperty.BOOT_MENU, bootMenu);
        }
    }

    public int getDisableACPI() {
        return disableACPI;
    }

    void setDisableACPI(int disableACPI) {
        if (this.disableACPI != disableACPI) {
            this.disableACPI = disableACPI;
            notifyChanged(MachineProperty.DISABLE_ACPI, disableACPI);
        }

    }

    public int getDisableFdBootChk() {
        return disableFdBootChk;
    }

    void setDisableFdBootChk(int disableFdBootChk) {
        if (this.disableFdBootChk != disableFdBootChk) {
            this.disableFdBootChk = disableFdBootChk;
            notifyChanged(MachineProperty.DISABLE_FD_BOOT_CHK, disableFdBootChk);
        }

    }

    public int getDisableTSC() {
        return disableTSC;
    }

    void setDisableTSC(int disableTSC) {
        if (this.disableTSC != disableTSC) {
            this.disableTSC = disableTSC;
            notifyChanged(MachineProperty.DISABLE_TSC, disableTSC);
        }

    }

    public String getHdaImagePath() {
        return hdaImagePath;
    }

    void setHdaImagePath(String hdaImagePath) {
        if (this.hdaImagePath == null || !this.hdaImagePath.equals(hdaImagePath)) {
            this.hdaImagePath = hdaImagePath;
            notifyChanged(MachineProperty.HDA, hdaImagePath);
        }
    }

    public String getHdbImagePath() {
        return hdbImagePath;
    }

    void setHdbImagePath(String hdbImagePath) {
        if (this.hdbImagePath == null || !this.hdbImagePath.equals(hdbImagePath)) {
            this.hdbImagePath = hdbImagePath;
            notifyChanged(MachineProperty.HDB, hdbImagePath);
        }
    }

    public String getHdcImagePath() {
        return hdcImagePath;
    }

    void setHdcImagePath(String hdcImagePath) {
        if (this.hdcImagePath == null || !this.hdcImagePath.equals(hdcImagePath)) {
            this.hdcImagePath = hdcImagePath;
            notifyChanged(MachineProperty.HDC, hdcImagePath);
        }

    }

    public String getHddImagePath() {
        return hddImagePath;
    }

    void setHddImagePath(String hddImagePath) {
        if (this.hddImagePath == null || !this.hddImagePath.equals(hddImagePath)) {
            this.hddImagePath = hddImagePath;
            notifyChanged(MachineProperty.HDD, hddImagePath);
        }

    }

    public String getHdaInterface() {
        return hdaInterface;
    }

    void setHdaInterface(String hdInterface) {
        if (this.hdaInterface == null || !this.hdaInterface.equals(hdInterface)) {
            this.hdaInterface = hdInterface;
            notifyChanged(MachineProperty.HDA_INTERFACE, hdInterface);
        }
    }

    public String getHdbInterface() {
        return hdbInterface;
    }

    void setHdbInterface(String hdInterface) {
        if (this.hdbInterface == null || !this.hdbInterface.equals(hdInterface)) {
            this.hdbInterface = hdInterface;
            notifyChanged(MachineProperty.HDB_INTERFACE, hdInterface);
        }
    }

    public String getHdcInterface() {
        return hdcInterface;
    }

    void setHdcInterface(String hdInterface) {
        if (this.hdcInterface == null || !this.hdcInterface.equals(hdInterface)) {
            this.hdcInterface = hdInterface;
            notifyChanged(MachineProperty.HDC_INTERFACE, hdInterface);
        }
    }

    public String getHddInterface() {
        return hddInterface;
    }

    void setHddInterface(String hdInterface) {
        if (this.hddInterface == null || !this.hddInterface.equals(hdInterface)) {
            this.hddInterface = hdInterface;
            notifyChanged(MachineProperty.HDD_INTERFACE, hdInterface);
        }
    }

    public String getSharedFolderPath() {
        return sharedFolderPath;
    }

    public String getSharedFolderType() {
        return sharedFolderType;
    }

    void setSharedFolderType(String sharedFolderType) {
        if (sharedFolderType == null || sharedFolderType.trim().isEmpty())
            sharedFolderType = "vvfat";
        if (this.sharedFolderType == null || !this.sharedFolderType.equals(sharedFolderType)) {
            this.sharedFolderType = sharedFolderType;
            notifyChanged(MachineProperty.SHARED_FOLDER_TYPE, sharedFolderType);
        }
    }

    void setSharedFolderPath(String sharedFolderPath) {
        if (this.sharedFolderPath == null || !this.sharedFolderPath.equals(sharedFolderPath)) {
            this.sharedFolderPath = sharedFolderPath;
            notifyChanged(MachineProperty.SHARED_FOLDER, sharedFolderPath);
        }

    }

    public boolean isEnableCDROM() {
        return enableCDROM;
    }

    void setEnableCDROM(boolean enableCDROM) {
        if (this.enableCDROM != enableCDROM) {
            this.enableCDROM = enableCDROM;
            notifyChanged(MachineProperty.OTHER, enableCDROM);
        }
    }

    public boolean isEnableFDA() {
        return enableFDA;
    }

    void setEnableFDA(boolean enableFDA) {
        if (this.enableFDA != enableFDA) {
            this.enableFDA = enableFDA;
            notifyChanged(MachineProperty.OTHER, enableFDA);
        }

    }

    public boolean isEnableFDB() {
        return enableFDB;
    }

    void setEnableFDB(boolean enableFDB) {
        if (this.enableFDB != enableFDB) {
            this.enableFDB = enableFDB;
            notifyChanged(MachineProperty.OTHER, enableFDB);
        }

    }

    public boolean isEnableUSB() {
        return enableUSB;
    }

    void setEnableUSB(boolean enableUSB) {
        if (this.enableUSB != enableUSB) {
            this.enableUSB = enableUSB;
            notifyChanged(MachineProperty.OTHER, enableUSB);
        }
    }

    public String getUsbImagePath() {
        return usbImagePath;
    }

    void setUsbImagePath(String usbImagePath) {
        if (this.usbImagePath == null || !this.usbImagePath.equals(usbImagePath)) {
            this.usbImagePath = usbImagePath;
            notifyChanged(MachineProperty.USB, usbImagePath);
        }
    }

    public String getCdImagePath() {
        return cdImagePath;
    }

    void setCdImagePath(String cdImagePath) {

        if (this.cdImagePath == null || !this.cdImagePath.equals(cdImagePath)) {
            this.cdImagePath = cdImagePath;
            notifyChanged(MachineProperty.CDROM, cdImagePath);
        }
    }

    public String getFdaImagePath() {
        return fdaImagePath;
    }

    void setFdaImagePath(String fdaImagePath) {
        if (this.fdaImagePath == null || !this.fdaImagePath.equals(fdaImagePath)) {
            this.fdaImagePath = fdaImagePath;
            notifyChanged(MachineProperty.FDA, fdaImagePath);
        }

    }

    public String getFdbImagePath() {
        return fdbImagePath;
    }

    void setFdbImagePath(String fdbImagePath) {
        if (this.fdbImagePath == null || !this.fdbImagePath.equals(fdbImagePath)) {
            this.fdbImagePath = fdbImagePath;
            notifyChanged(MachineProperty.FDB, fdbImagePath);
        }
    }

    public String getCDInterface() {
        return cdInterface;
    }

    void setCdInterface(String mediaInterface) {
        if (this.cdInterface == null || !this.cdInterface.equals(mediaInterface)) {
            this.cdInterface = mediaInterface;
            notifyChanged(MachineProperty.CDROM_INTERFACE, mediaInterface);
        }
    }

    public String getBootDevice() {
        return bootDevice;
    }

    void setBootDevice(String bootDevice) {
        if (this.bootDevice == null || !this.bootDevice.equals(bootDevice)) {
            this.bootDevice = bootDevice;
            notifyChanged(MachineProperty.BOOT_CONFIG, bootDevice);
        }
    }

    public String getBios() {
        return bios;
    }

    void setBios(String bios) {
        if (this.bios == null || !this.bios.equals(bios)) {
            this.bios = bios;
            notifyChanged(MachineProperty.BIOS, bios);
        }
    }

    public String getBiosType() {
        return biosType;
    }

    void setBiosType(String biosType) {
        if (this.biosType == null || !this.biosType.equals(biosType)) {
            this.biosType = biosType;
            notifyChanged(MachineProperty.BIOS_TYPE, biosType);
        }
    }

    public String getBiosCode() {
        return biosCode;
    }

    void setBiosCode(String biosCode) {
        if (this.biosCode == null || !this.biosCode.equals(biosCode)) {
            this.biosCode = biosCode;
            notifyChanged(MachineProperty.BIOS_CODE, biosCode);
        }
    }

    public String getBiosVars() {
        return biosVars;
    }

    void setBiosVars(String biosVars) {
        if (this.biosVars == null || !this.biosVars.equals(biosVars)) {
            this.biosVars = biosVars;
            notifyChanged(MachineProperty.BIOS_VARS, biosVars);
        }
    }

    public String getKernel() {
        return kernel;
    }

    void setKernel(String kernel) {
        if (this.kernel == null || !this.kernel.equals(kernel)) {
            this.kernel = kernel;
            notifyChanged(MachineProperty.KERNEL, kernel);
        }

    }

    public String getInitRd() {
        return initRd;
    }

    void setInitRd(String initRd) {
        if (this.initRd == null || !this.initRd.equals(initRd)) {
            this.initRd = initRd;
            notifyChanged(MachineProperty.INITRD, initRd);
        }

    }

    public String getAppend() {
        return append;
    }

    void setAppend(String append) {
        if (this.append == null || !this.append.equals(append)) {
            this.append = append;
            notifyChanged(MachineProperty.APPEND, append);
        }

    }

    public String getNetwork() {
        return network;
    }

    void setNetwork(String network) {
        if (this.network == null || !this.network.equals(network)) {
            this.network = network;
            notifyChanged(MachineProperty.NETCONFIG, network);
        }

    }

    public String getNetworkCard() {
        return networkCard;
    }

    void setNetworkCard(String networkCard) {
        if (this.networkCard == null || !this.networkCard.equals(networkCard)) {
            this.networkCard = networkCard;
            notifyChanged(MachineProperty.NICCONFIG, networkCard);
        }

    }

    public String getGuestFwd() {
        return guestFwd;
    }

    void setGuestFwd(String guestFwd) {
        if (this.guestFwd == null || !this.guestFwd.equals(guestFwd)) {
            this.guestFwd = guestFwd;
            notifyChanged(MachineProperty.GUESTFWD, guestFwd);
        }

    }

    public String getHostFwd() {
        return hostFwd;
    }

    void setHostFwd(String hostFwd) {
        if (this.hostFwd == null || !this.hostFwd.equals(hostFwd)) {
            this.hostFwd = hostFwd;
            notifyChanged(MachineProperty.HOSTFWD, hostFwd);
        }

    }

    public String getVga() {
        return vga;
    }

    void setVga(String vga) {
        if (this.vga == null || !this.vga.equals(vga)) {
            this.vga = vga;
            notifyChanged(MachineProperty.VGA, vga);
        }

    }

    public String getExtraParams() {
        return extraParams;
    }

    void setExtraParams(String extraParams) {
        if (this.extraParams == null || !this.extraParams.equals(extraParams)) {
            this.extraParams = extraParams;
            notifyChanged(MachineProperty.EXTRA_PARAMS, extraParams);
        }

    }

    public int getShared_folder_mode() {
        return sharedFolderMode;
    }

    void setShared_folder_mode(int shared_folder_mode) {
        if (this.sharedFolderMode != shared_folder_mode) {
            this.sharedFolderMode = shared_folder_mode;
            notifyChanged(MachineProperty.SHARED_FOLDER_MODE, shared_folder_mode);
        }
    }

    public boolean hasRemovableDevices() {
        return enableCDROM || enableFDA || enableFDB || enableUSB;
    }

    // nographic means headless, so no display, no QGE and no VNC
    // m68k ignores the vga option so we ignore it
    public static boolean isNoGraphic(String vga) {
        return "nographic".equals(vga) && QubeApplication.arch != Config.Arch.m68k;
    }

    void setDefaults() {
        if (QubeApplication.arch == Config.Arch.x86 || QubeApplication.arch == Config.Arch.x86_64) {
            arch = "x86";
            cpu = "Default";
            machineType = "Default";
        } else if (QubeApplication.arch == Config.Arch.arm || QubeApplication.arch == Config.Arch.arm64) {
            arch = "ARM";
            machineType = "virt";
            cpu = "Default";
            vga = "ramfb";
            biosType = "UEFI";
            mouse = "usb-mouse";
        } else if (QubeApplication.arch == Config.Arch.ppc || QubeApplication.arch == Config.Arch.ppc64) {
            arch = "PPC";
            machineType = "Default";
        } else if (QubeApplication.arch == Config.Arch.sparc) {
            arch = "SPARC";
            vga = "cg3";
            machineType = "Default";
        } else if (QubeApplication.arch == Config.Arch.m68k) {
            arch = "m68k";
            vga = "macfb";
            machineType = "Default";
        }
    }

    public String getSoundCard() {
        return soundCard;
    }

    void setSoundCard(String soundCard) {
        if (this.soundCard == null || !this.soundCard.equals(soundCard)) {
            this.soundCard = soundCard;
            notifyChanged(MachineProperty.SOUNDCARD, soundCard);
        }
    }

    public String getKeyboard() {
        return keyboard;
    }

    void setKeyboard(String keyboard) {
        if (this.keyboard == null || !this.keyboard.equals(keyboard)) {
            this.keyboard = keyboard;
            notifyChanged(MachineProperty.KEYBOARD, keyboard);
        }
    }

    public String getMouse() {
        return mouse;
    }

    void setMouse(String mouse) {
        if (this.keyboard == null || this.mouse != mouse) {
            this.mouse = mouse;
            notifyChanged(MachineProperty.MOUSE, mouse);
        }
    }

    public String getDns() {
        return dns;
    }

    void setDns(String dns) {
        if (this.dns == null || !this.dns.equals(dns)) {
            this.dns = dns;
            notifyChanged(MachineProperty.DNS, dns);
        }
    }

    public int getDisableAcpi() {
        return disableACPI;
    }

    public int getDisableHPET() {
        return disableHPET;
    }

    void setDisableHPET(int disableHPET) {
        if (this.disableHPET != disableHPET) {
            this.disableHPET = disableHPET;
            notifyChanged(MachineProperty.DISABLE_HPET, disableHPET);
        }

    }

    public int getEnableVenus() {
        return enableVenus;
    }

    void setEnableVenus(int enableVenus) {
        if (this.enableVenus != enableVenus) {
            this.enableVenus = enableVenus;
            notifyChanged(MachineProperty.ENABLE_VENUS, enableVenus);
        }
    }

    public void addChangeListener(ChangeListener listener) {
        changeListeners.add(listener);
    }

    public void removeChangeListener(ChangeListener listener) {
        changeListeners.remove(listener);
    }

    private void notifyChanged(MachineProperty property, Object value) {
        for (ChangeListener listener : new HashSet<>(changeListeners)) {
            listener.onMachineChanged(this, property, value);
        }
    }

    // Notifies the UI of a failed live drive change (e.g. VM shutting down / QMP
    // unavailable) without touching the persisted path, so the user's selection
    // isn't lost.
    void notifyDriveChangeFailed(MachineProperty property) {
        notifyChanged(property, null);
    }

    public interface ChangeListener {
        void onMachineChanged(Machine machine, MachineProperty property, Object value);
    }

    public enum FileType {
        CDROM, FDA, FDB, USB,
        HDA, HDB, HDC, HDD, SHARED_DIR,
        KERNEL, INITRD, BIOS, BIOS_CODE, BIOS_VARS,
        IMAGE_DIR, LOG_DIR
    }

}
