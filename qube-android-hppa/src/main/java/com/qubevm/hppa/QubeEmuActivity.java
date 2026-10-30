package com.qubevm.hppa;

import android.os.Bundle;

import com.max2idea.android.qube.log.Logger;
import com.max2idea.android.qube.main.Config;
import com.max2idea.android.qube.main.QubeActivity;
import com.max2idea.android.qube.main.QubeApplication;

public class QubeEmuActivity extends QubeActivity {

    @Override
    public void onCreate(Bundle bundle) {
        QubeApplication.arch = Config.Arch.hppa;
        Config.clientClass = this.getClass();
        Config.enableKVM = false;
        Config.enableMTTCG = false;
        //XXX: audio isn't available in sparc arch (yet)
        Config.enableQGESound = true;
        Config.machineFolder = Config.machineFolder + "other/hppa_machines/";
        super.onCreate(bundle);
        // TODO: change location to something that the user will have access outside of qube
        // like internal storage
        Logger.setupLogFile("/qube/qube-hppa-log.txt");
    }

    @Override
    protected void loadQEMULib() {
        System.loadLibrary("qemu-system-hppa");
    }
}
