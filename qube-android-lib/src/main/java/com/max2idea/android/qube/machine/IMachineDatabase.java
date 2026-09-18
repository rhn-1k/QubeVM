/*
Copyright (C) Max Kastanas 2012
Copyright (C) Rhn 2026
 */
package com.max2idea.android.qube.machine;

import java.util.ArrayList;

/** A DAO interface for saving user defined machines. This could be anything but for Android we
 * prefer an SQLite Helpers
  */
public interface IMachineDatabase {
    Machine getMachine(String value);
    void updateMachineFieldAsync(Machine machine, MachineProperty property, String value);
    void updateMachineField(Machine machine, MachineProperty property, String value);
    int insertMachine(Machine machine);
    ArrayList<String> getMachineNames();
    boolean deleteMachine(Machine machine);
    boolean renameMachine(Machine machine, String newName);
}
