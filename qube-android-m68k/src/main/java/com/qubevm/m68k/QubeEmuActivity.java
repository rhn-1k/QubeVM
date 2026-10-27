package com.qubevm.m68k;

import android.os.Bundle;

import com.max2idea.android.qube.log.Logger;
import com.max2idea.android.qube.main.Config;
import com.max2idea.android.qube.main.QubeActivity;
import com.max2idea.android.qube.main.QubeApplication;

public class QubeEmuActivity extends QubeActivity {

    @Override
    public void onCreate(Bundle bundle) {
        QubeApplication.arch = Config.Arch.m68k;
        Config.clientClass = this.getClass();
        Config.enableKVM = false;
        Config.enableMTTCG = false;
        Config.enableQGESound = true;
        Config.machineFolder = Config.machineFolder + "other/m68k_machines/";
        super.onCreate(bundle);
        Logger.setupLogFile("/qube/qube-m68k-log.txt");
    }

    @Override
    protected void loadQEMULib() {
        System.loadLibrary("qemu-system-m68k");
    }
}
