package com.qubevm.sparc;

import android.os.Bundle;

import com.max2idea.android.qube.log.Logger;
import com.max2idea.android.qube.main.Config;
import com.max2idea.android.qube.main.QubeActivity;
import com.max2idea.android.qube.main.QubeApplication;

public class QubeEmuActivity extends QubeActivity {

    @Override
    public void onCreate(Bundle bundle) {
        QubeApplication.arch = Config.Arch.sparc;
        Config.clientClass = this.getClass();
        Config.enableKVM = false;
        //XXX: sparc emulators are not mttcg capable (yet)
        Config.enableMTTCG = false;
        //XXX: audio isn't available in sparc arch (yet)
        Config.enableQGESound = false;
        Config.machineFolder = Config.machineFolder + "other/sparc_machines/";
        super.onCreate(bundle);
        Logger.setupLogFile("/qube/qube-sparc-log.txt");
    }

    @Override
    protected void loadQEMULib() {
        System.loadLibrary("qemu-system-sparc");
    }
}
