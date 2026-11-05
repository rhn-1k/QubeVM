/*
Copyright (C) Max Kastanas 2012
Copyright (C) Rhn 2026
 */
package com.max2idea.android.qube.main;

/**
 * Configuration
 */
public class Config {

    // Constants
    public static final int MOUSE_BUTTON_LEFT = 1;
    public static final int MOUSE_BUTTON_MIDDLE = 2;
    public static final int MOUSE_BUTTON_RIGHT = 3;
    public static final int SETTINGS_RETURN_CODE = 1000;
    public static final int FILEMAN_RETURN_CODE = 1002;

    // Defining UEFI BIOS naming
    public static final String UEFI_X86_CODE = "QUBE_x86_EFI.fd";
    public static final String UEFI_X86_VARS = "QUBE_x86_VARS.fd";
    public static final String UEFI_ARM_CODE = "QUBE_ARM_EFI.fd";
    public static final String UEFI_ARM_VARS = "QUBE_ARM_VARS.fd";

    public static final int QGE_REQUEST_CODE = 1007;
    public static final int QGE_QUIT_RESULT_CODE = 1009;

    public static final int OPEN_IMAGE_FILE_REQUEST_CODE = 2001;
    public static final int OPEN_IMAGE_FILE_ASF_REQUEST_CODE = 2002;

    public static final int OPEN_IMAGE_DIR_REQUEST_CODE = 2003;
    public static final int OPEN_IMAGE_DIR_ASF_REQUEST_CODE = 2004;

    public static final int OPEN_SHARED_DIR_REQUEST_CODE = 2005;
    public static final int OPEN_SHARED_DIR_ASF_REQUEST_CODE = 2006;

    public static final int OPEN_LOG_FILE_DIR_REQUEST_CODE = 2011;
    public static final int OPEN_LOG_FILE_DIR_ASF_REQUEST_CODE = 2012;

    public static final int STATUS_CREATED = 1000;
    public static final String ACTION_START = "com.max2idea.android.qube.action.STARTVM";

    // GUI Options
    public static final int MAX_CPU_NUM = 8;
    public static final int TCG_BUFFER_STEP = 256;
    public static final int TCG_BUFFER_MAX = 2048;
    public static final int RAM_STEP_MB = 256;
    public static final int RAM_MAX_MB = 12288;
    public static final int VIDEO_MEMORY_STEP = 16;
    public static final int VIDEO_MEMORY_MAX = 512;
    public static final int VENUS_HOST_MEMORY_STEP = 32;
    public static final int VENUS_HOST_MEMORY_MAX = 2048;

    // delay
    public static int mouseButtonDelay = 100;

    // Animation timings (ms)
    // Section collapse speed duration
    public static final long SECTION_TOGGLE_MS = 250;
    // Fade duration for enable/disable state of spinners/switches
    public static final long FADE_IN_MS = 150;
    public static final long FADE_OUT_MS = 180;

    // App config
    public static final String APP_NAME = "QubeVM";

    public static final String defaultDNSServer = "1.1.1.1";
    // App Config
    public static final String githubLink = "https://github.com/rhn-1k/QubeVM/releases";
    public static final String guidesLink = "https://qubevm.vercel.app/docs/index.html";
    public static final String CpuLink = "https://qubevm.vercel.app/docs/board.html";
    public static final String toolsLink = "https://qubevm.vercel.app/docs/advanced.html";
    public static final String NetworkLink = "https://qubevm.vercel.app/docs/video-audio-network.html";
    public static final String versionLink = "https://raw.githubusercontent.com/rhn-1k/QubeVM/refs/heads/main/VERSION";
    public static final String newVersionLink = "https://github.com/rhn-1k/QubeVM/releases/tag/";
    public static final String changelogLink = "https://raw.githubusercontent.com/rhn-1k/QubeVM/refs/heads/main/CHANGELOG";
    public static final String discordLink = "https://discord.gg/MrbJ9QNXNJ";

    // ???
    public static final int SOMETHING_TAPS = 5;
    public static final long SOMETHING_WINDOW_MS = 1000;
    public static final String SOMETHING_URL = "https://youtu.be/dQw4w9WgXcQ?si=wj7g8HBsPx6dlkiM";

    public static final boolean enableMouseOption = true;

    // Debug
    public static final boolean debug = false;
    public static final boolean debugQmp = false;
    public static final boolean debugStrictMode = false;

    public static final int EXIT_SUCCESS = 1;
    public static final int EXIT_UNKNOWN = 2;

    // Enabling or disabling sound in the QGE build
    public static boolean enableQGESound = true;

    // if you don't want to enable software updates set to false
    public static boolean enableSoftwareUpdates = true;

    //TODO: add in settings
    public static boolean syncFilesOnClose = true;

    public enum Arch {
        x86, x86_64, arm, arm64, ppc, ppc64, sparc, m68k, hppa
    }

    //Enable if you build with KVM support, needes android-21 platform
    public static boolean enableKVM = false;
    public static String storagedir = null;
    //Some OSes don't like emulated multi cores for QEMU 2.9.1 you can disable here
    // thought there is also the Disable TSC feature so you don't have to do it here
    public static boolean enableSMPOnlyOnKVM = false;
    //set to true if you need to debug native library loading
    public static boolean loadNativeLibsEarly = false;
    //XXX: QEMU 3.1.0+ needs the libraries to be loaded from the main thread
    public static boolean loadNativeLibsMainThread = true;
    public static String wakeLockTag = "qube:wakelock";
    public static String wifiLockTag = "qube:wifilock";

    public static boolean viewLogInternally = true;
    //XXX some archs don't support floppy or sd card
    public static boolean enableEmulatedFloppy = true;
    public static boolean enableEmulatedSDCard;
    public static String destLogFilename = "qubelog.txt";
    public static String notificationChannelID = "qube";
    public static String notificationChannelName = "qube";
    public static boolean showToast = false;
    public static boolean closeFileDescriptors = true;
    //XXX: qemu vvfat was buggy but we fixed it later
    public static boolean enableSharedFolder = true;

    public static String machineFolder = "machines/";
    public static String logFilePath = null;

    // Screenshot output directory relative to storage, for example Pictures or DCIM
    public static String screenshotDir = "Pictures";

    //QMP
    public static String QMPServer = "127.0.0.1";
    public static int QMPPort = 4444;

    // VNC Defaults
    public static String defaultVNCHost = "127.0.0.1";
    //It seems that new versions of qemu expect a relative number
    // so we stop using absolute port numbers
    public static final int defaultVNCPort = 1;

    //Keyboard Layout
    public static String defaultKeyboardLayout = "en-us";

    //a little nicer ui
    public static boolean collapseSections = true;

    public static boolean enableToggleKeyboard = false;

    //override this at the app level it dependes on the host arch
    public static boolean enableMTTCG = true;

    //Change to true in prod if you want to be notified by default for new versions
    public static boolean defaultCheckNewVersion = true;

    //enable tracing
    // make sure you have access to the dir/files below
    public static boolean enableTracingLog = false;
    public static final String traceDir = "/sdcard/qube/tmp/trace";
    public static final String traceEventsFile = "/sdcard/qube/tmp/events";
    public static final String traceLogFile = "/sdcard/qube/log.txt";

    // Logger
    public static Class<?> clientClass = null;
    public static final long LOG_DELAY = 1500L;
    // Log colors
    public static final String LOG_FATAL = "#F78181";
    public static final String LOG_WARN = "#81A3F7";
    public static final String LOG_DEBUG = "#B1FFD7";

}
