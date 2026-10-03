package com.qubevm.arm;

import android.os.Bundle;

import com.max2idea.android.qube.log.Logger;
import com.max2idea.android.qube.main.Config;
import com.max2idea.android.qube.main.QubeActivity;
import com.max2idea.android.qube.main.QubeApplication;

public class QubeEmuActivity extends QubeActivity {

    @Override
    public void onCreate(Bundle bundle) {
        QubeApplication.arch = Config.Arch.arm64;
        Config.clientClass = this.getClass();
        Config.enableKVM = true;
        Config.enableEmulatedFloppy = false;
        // Enable MTTCG only on 64-bit hosts when it is enabled globally.
        if (QubeApplication.isHost64Bit() && Config.enableMTTCG) {
            Config.enableMTTCG = true;
        } else {
            Config.enableMTTCG = false;
        }
        Config.machineFolder = Config.machineFolder + "other/arm_machines/";
        super.onCreate(bundle);
        Logger.setupLogFile("/qube/qube-arm-log.txt");
    }

    @Override
    protected void loadQEMULib() {
        try {
            System.loadLibrary("qemu-system-arm");
        } catch (Error ex) {
            System.loadLibrary("qemu-system-aarch64");
        }
    }
}
