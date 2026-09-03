/*
Copyright (C) Max Kastanas 2012
Copyright (C) Rhn 2026
 */
package com.max2idea.android.qube.main;

import com.max2idea.android.qube.machine.MachineAction;
import com.max2idea.android.qube.machine.MachineProperty;

public interface ViewListener {
    void onFieldChange(MachineProperty property, Object value);
    void onAction(MachineAction stopVm, Object value);
}
