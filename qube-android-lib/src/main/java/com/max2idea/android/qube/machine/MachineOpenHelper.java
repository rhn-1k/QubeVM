/*
Copyright (C) Max Kastanas 2012
Copyright (C) Rhn 2026
 */
package com.max2idea.android.qube.machine;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.max2idea.android.qube.main.Config;
import com.max2idea.android.qube.main.QubeApplication;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;

/**
 * DAO implementation for storing our machines into an SQLite database
 */
public class MachineOpenHelper extends SQLiteOpenHelper implements IMachineDatabase, Observer {
    private static final String TAG = "MachineOpenHelper";

    private static final int DATABASE_VERSION = 25;
    private static final String DATABASE_NAME = "QUBE";
    private static final String MACHINE_TABLE_NAME = "machines";

    private static final String MACHINE_TABLE_CREATE = "CREATE TABLE IF NOT EXISTS " + MACHINE_TABLE_NAME + " ("
            + MachineProperty.MACHINE_NAME.name() + " TEXT , " + MachineProperty.SNAPSHOT_NAME.name() + " TEXT , " + MachineProperty.CPU.name() + " TEXT, " + MachineProperty.ARCH.name() + " TEXT, " + MachineProperty.MEMORY.name()
            + " TEXT, " + MachineProperty.FDA.name() + " TEXT, " + MachineProperty.FDB.name() + " TEXT, " + MachineProperty.CDROM.name() + " TEXT, " + MachineProperty.HDA.name() + " TEXT, " + MachineProperty.HDB.name() + " TEXT, "
            + MachineProperty.HDC.name() + " TEXT, " + MachineProperty.HDD.name() + " TEXT, " + MachineProperty.BOOT_CONFIG.name() + " TEXT, " + MachineProperty.NETCONFIG.name() + " TEXT, " + MachineProperty.NICCONFIG.name()
            + " TEXT, " + MachineProperty.VGA.name() + " TEXT, " + MachineProperty.SOUNDCARD.name() + " TEXT, " + MachineProperty.HDCONFIG.name() + " TEXT, " + MachineProperty.DISABLE_ACPI.name()
            + " INTEGER, " + MachineProperty.DISABLE_HPET.name() + " INTEGER, " + MachineProperty.ENABLE_USBMOUSE.name() + " INTEGER, " + MachineProperty.STATUS.name() + " TEXT, "
            + MachineProperty.LAST_UPDATED.name() + " DATE, " + MachineProperty.KERNEL.name() + " INTEGER, " + MachineProperty.INITRD.name() + " TEXT, " + MachineProperty.APPEND.name() + " TEXT, " + MachineProperty.CPUNUM.name()
            + " INTEGER, " + MachineProperty.MACHINETYPE.name() + " TEXT, " + MachineProperty.DISABLE_FD_BOOT_CHK.name() + " INTEGER, " + MachineProperty.SD.name() + " TEXT, " + MachineProperty.SHARED_FOLDER.name() + " TEXT, " + MachineProperty.SHARED_FOLDER_MODE.name() + " INTEGER, " + MachineProperty.EXTRA_PARAMS.name() + " TEXT, "
            + MachineProperty.HOSTFWD.name() + " TEXT, " + MachineProperty.GUESTFWD.name() + " TEXT, " + MachineProperty.UI.name() + " TEXT, " + MachineProperty.DISABLE_TSC.name() + " INTEGER, "
            + MachineProperty.MOUSE.name() + " TEXT, " + MachineProperty.KEYBOARD.name() + " TEXT, " + MachineProperty.ENABLE_MTTCG.name() + " INTEGER, " + MachineProperty.ENABLE_KVM.name() + " INTEGER , "
            + MachineProperty.HDA_INTERFACE.name() + " TEXT, " + MachineProperty.HDB_INTERFACE.name() + " TEXT, " + MachineProperty.HDC_INTERFACE.name() + " TEXT, " + MachineProperty.HDD_INTERFACE.name() + " TEXT , "
            + MachineProperty.CDROM_INTERFACE.name() + " TEXT, " + MachineProperty.PRIO.name() + " INTEGER, "
            + MachineProperty.ENABLE_VENUS.name() + " INTEGER, "
            + MachineProperty.BIOS.name() + " TEXT, " + MachineProperty.BIOS_TYPE.name() + " TEXT, "
            + MachineProperty.BIOS_CODE.name() + " TEXT, " + MachineProperty.BIOS_VARS.name() + " TEXT, "
            + MachineProperty.TCG_BUFFER.name() + " INTEGER, "
            + MachineProperty.DNS.name() + " TEXT, "
            + MachineProperty.SHARED_FOLDER_TYPE.name() + " TEXT "
            + ");";

    private static MachineOpenHelper sInstance;
    private SQLiteDatabase db;

    private MachineOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        getDB();
    }

    static MachineOpenHelper getInstance() {
        return sInstance;
    }

    public static synchronized void initialize(Context context) {
        if (sInstance == null) {
            sInstance = new MachineOpenHelper(context.getApplicationContext());
            sInstance.setWriteAheadLoggingEnabled(true);
        }
    }

    private synchronized void getDB() {
        if (db == null)
            db = getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(MACHINE_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w("machineOpenHelper", "Upgrading database from version " + oldVersion + " to " + newVersion);
        if (newVersion >= 3 && oldVersion <= 2) {
            db.execSQL("ALTER TABLE " + MACHINE_TABLE_NAME + " ADD COLUMN " + MachineProperty.KERNEL + " TEXT;");
            db.execSQL("ALTER TABLE " + MACHINE_TABLE_NAME + " ADD COLUMN " + MachineProperty.INITRD + " TEXT;");
        }

        if (newVersion >= 4 && oldVersion <= 3) {
            db.execSQL("ALTER TABLE " + MACHINE_TABLE_NAME + " ADD COLUMN " + MachineProperty.CPUNUM + " TEXT;");
            db.execSQL("ALTER TABLE " + MACHINE_TABLE_NAME + " ADD COLUMN " + MachineProperty.MACHINETYPE + " TEXT;");
        }

        if (newVersion >= 5 && oldVersion <= 4) {
            db.execSQL("ALTER TABLE " + MACHINE_TABLE_NAME + " ADD COLUMN " + MachineProperty.HDC + " TEXT;");
            db.execSQL("ALTER TABLE " + MACHINE_TABLE_NAME + " ADD COLUMN " + MachineProperty.HDD + " TEXT;");
        }

        if (newVersion >= 6 && oldVersion <= 5) {
            db.execSQL("ALTER TABLE " + MACHINE_TABLE_NAME + " ADD COLUMN " + MachineProperty.APPEND + " TEXT;");
        }

        if (newVersion >= 7 && oldVersion <= 6) {
            db.execSQL("ALTER TABLE " + MACHINE_TABLE_NAME + " ADD COLUMN " + MachineProperty.DISABLE_FD_BOOT_CHK + " INTEGER;");
        }

        if (newVersion >= 8 && oldVersion <= 7) {
            db.execSQL("ALTER TABLE " + MACHINE_TABLE_NAME + " ADD COLUMN " + MachineProperty.ARCH + " TEXT;");
        }

        if (newVersion >= 9 && oldVersion <= 8) {
            db.execSQL("ALTER TABLE " + MACHINE_TABLE_NAME + " ADD COLUMN " + MachineProperty.SD + " TEXT;");
        }


        if (newVersion >= 11 && oldVersion <= 10) {
            db.execSQL("ALTER TABLE " + MACHINE_TABLE_NAME + " ADD COLUMN " + MachineProperty.SHARED_FOLDER + " TEXT;");
            db.execSQL("ALTER TABLE " + MACHINE_TABLE_NAME + " ADD COLUMN " + MachineProperty.SHARED_FOLDER_MODE + " INTEGER;");
        }

        if (newVersion >= 12 && oldVersion <= 11) {
            db.execSQL("ALTER TABLE " + MACHINE_TABLE_NAME + " ADD COLUMN " + MachineProperty.EXTRA_PARAMS + " TEXT;");
        }

        if (newVersion >= 13 && oldVersion <= 12) {
            db.execSQL("ALTER TABLE " + MACHINE_TABLE_NAME + " ADD COLUMN " + MachineProperty.HOSTFWD + " TEXT;");
            db.execSQL("ALTER TABLE " + MACHINE_TABLE_NAME + " ADD COLUMN " + MachineProperty.GUESTFWD + " TEXT;");
        }

        if (newVersion >= 14 && oldVersion <= 13) {
            db.execSQL("ALTER TABLE " + MACHINE_TABLE_NAME + " ADD COLUMN " + MachineProperty.UI + " TEXT;");
        }

        if (newVersion >= 15 && oldVersion <= 14) {
            db.execSQL("ALTER TABLE " + MACHINE_TABLE_NAME + " ADD COLUMN " + MachineProperty.DISABLE_TSC + " INTEGER;");
            db.execSQL("ALTER TABLE " + MACHINE_TABLE_NAME + " ADD COLUMN " + MachineProperty.MOUSE + " TEXT;");
            db.execSQL("ALTER TABLE " + MACHINE_TABLE_NAME + " ADD COLUMN " + MachineProperty.KEYBOARD + " TEXT;");
            db.execSQL("ALTER TABLE " + MACHINE_TABLE_NAME + " ADD COLUMN " + MachineProperty.ENABLE_MTTCG + " INTEGER;");
            db.execSQL("ALTER TABLE " + MACHINE_TABLE_NAME + " ADD COLUMN " + MachineProperty.ENABLE_KVM + " INTEGER;");
        }

        if (newVersion >= 16 && oldVersion <= 15) {
            db.execSQL("ALTER TABLE " + MACHINE_TABLE_NAME + " ADD COLUMN " + MachineProperty.HDA_INTERFACE + " TEXT;");
            db.execSQL("ALTER TABLE " + MACHINE_TABLE_NAME + " ADD COLUMN " + MachineProperty.HDB_INTERFACE + " TEXT;");
            db.execSQL("ALTER TABLE " + MACHINE_TABLE_NAME + " ADD COLUMN " + MachineProperty.HDC_INTERFACE + " TEXT;");
            db.execSQL("ALTER TABLE " + MACHINE_TABLE_NAME + " ADD COLUMN " + MachineProperty.HDD_INTERFACE + " TEXT;");
            db.execSQL("ALTER TABLE " + MACHINE_TABLE_NAME + " ADD COLUMN " + MachineProperty.CDROM_INTERFACE + " TEXT;");
        }

        if (newVersion >= 17 && oldVersion <= 16) {
            db.execSQL("ALTER TABLE " + MACHINE_TABLE_NAME + " ADD COLUMN " + MachineProperty.PRIO + " INTEGER;");
        }

        if (newVersion >= 19 && oldVersion <= 18) {
            db.execSQL("ALTER TABLE " + MACHINE_TABLE_NAME + " ADD COLUMN " + MachineProperty.ENABLE_VENUS + " INTEGER;");
        }

        if (newVersion >= 21 && oldVersion <= 20) {
            db.execSQL("ALTER TABLE " + MACHINE_TABLE_NAME + " ADD COLUMN " + MachineProperty.BIOS + " TEXT;");
        }
        if (newVersion >= 22 && oldVersion <= 21) {
            db.execSQL("ALTER TABLE " + MACHINE_TABLE_NAME + " ADD COLUMN " + MachineProperty.TCG_BUFFER + " INTEGER;");
        }
        if (newVersion >= 23 && oldVersion <= 22) {
            db.execSQL("ALTER TABLE " + MACHINE_TABLE_NAME + " ADD COLUMN " + MachineProperty.BIOS_TYPE + " TEXT;");
            db.execSQL("ALTER TABLE " + MACHINE_TABLE_NAME + " ADD COLUMN " + MachineProperty.BIOS_CODE + " TEXT;");
            db.execSQL("ALTER TABLE " + MACHINE_TABLE_NAME + " ADD COLUMN " + MachineProperty.BIOS_VARS + " TEXT;");
        }
        if (newVersion >= 24 && oldVersion <= 23) {
            db.execSQL("ALTER TABLE " + MACHINE_TABLE_NAME + " ADD COLUMN " + MachineProperty.DNS + " TEXT;");
        }
        if (newVersion >= 25 && oldVersion <= 24) {
            db.execSQL("ALTER TABLE " + MACHINE_TABLE_NAME + " ADD COLUMN " + MachineProperty.SHARED_FOLDER_TYPE + " TEXT;");
        }
    }

    public synchronized int insertMachine(Machine machine) {
        int seqnum = -1;
        SQLiteDatabase db = getWritableDatabase();

        Log.d(TAG, "inserting machine: " + machine.getName());
        ContentValues stateValues = new ContentValues();
        stateValues.put(MachineProperty.MACHINE_NAME.name(), machine.getName()); //Legacy
        stateValues.put(MachineProperty.CPU.name(), machine.getCpu());
        stateValues.put(MachineProperty.CPUNUM.name(), machine.getCpuNum());
        stateValues.put(MachineProperty.MEMORY.name(), machine.getMemory());
        stateValues.put(MachineProperty.HDA.name(), machine.getHdaImagePath());
        stateValues.put(MachineProperty.HDA_INTERFACE.name(), machine.getHdaInterface());
        stateValues.put(MachineProperty.HDB.name(), machine.getHdbImagePath());
        stateValues.put(MachineProperty.HDB_INTERFACE.name(), machine.getHdbInterface());
        stateValues.put(MachineProperty.HDC.name(), machine.getHdcImagePath());
        stateValues.put(MachineProperty.HDC_INTERFACE.name(), machine.getHdcInterface());
        stateValues.put(MachineProperty.HDD.name(), machine.getHddImagePath());
        stateValues.put(MachineProperty.HDD_INTERFACE.name(), machine.getHddInterface());
        stateValues.put(MachineProperty.CDROM.name(), machine.getCdImagePath());
        stateValues.put(MachineProperty.CDROM_INTERFACE.name(), machine.getCDInterface());
        stateValues.put(MachineProperty.FDA.name(), machine.getFdaImagePath());
        stateValues.put(MachineProperty.FDB.name(), machine.getFdbImagePath());
        stateValues.put(MachineProperty.SHARED_FOLDER.name(), machine.getSharedFolderPath());
        stateValues.put(MachineProperty.SHARED_FOLDER_MODE.name(), machine.getShared_folder_mode());
        stateValues.put(MachineProperty.SHARED_FOLDER_TYPE.name(), machine.getSharedFolderType());
        stateValues.put(MachineProperty.BOOT_CONFIG.name(), machine.getBootDevice());
        stateValues.put(MachineProperty.NETCONFIG.name(), machine.getNetwork());
        stateValues.put(MachineProperty.NICCONFIG.name(), machine.getNetworkCard());
        stateValues.put(MachineProperty.VGA.name(), machine.getVga());
        stateValues.put(MachineProperty.DISABLE_ACPI.name(), machine.getDisableAcpi());
        stateValues.put(MachineProperty.DISABLE_HPET.name(), machine.getDisableHPET());
        stateValues.put(MachineProperty.DISABLE_TSC.name(), machine.getDisableTSC());
        stateValues.put(MachineProperty.DISABLE_FD_BOOT_CHK.name(), machine.getDisableFdBootChk());
        stateValues.put(MachineProperty.SOUNDCARD.name(), machine.getSoundCard());
        stateValues.put(MachineProperty.KERNEL.name(), machine.getKernel());
        stateValues.put(MachineProperty.INITRD.name(), machine.getInitRd());
        stateValues.put(MachineProperty.APPEND.name(), machine.getAppend());
        stateValues.put(MachineProperty.MACHINETYPE.name(), machine.getMachineType());
        stateValues.put(MachineProperty.ARCH.name(), machine.getArch());
        stateValues.put(MachineProperty.EXTRA_PARAMS.name(), machine.getExtraParams());
        stateValues.put(MachineProperty.HOSTFWD.name(), machine.getHostFwd());
        stateValues.put(MachineProperty.GUESTFWD.name(), machine.getGuestFwd());
        stateValues.put(MachineProperty.UI.name(), machine.getRenderer() == 1 ? "VNC" : "QGE");
        stateValues.put(MachineProperty.MOUSE.name(), machine.getMouse());
        stateValues.put(MachineProperty.KEYBOARD.name(), machine.getKeyboard());
        stateValues.put(MachineProperty.ENABLE_MTTCG.name(), machine.getEnableMTTCG());
        stateValues.put(MachineProperty.ENABLE_KVM.name(), machine.getEnableKVM());
        stateValues.put(MachineProperty.PRIO.name(), machine.getPrio());
        stateValues.put(MachineProperty.ENABLE_VENUS.name(), machine.getEnableVenus());
        stateValues.put(MachineProperty.BIOS.name(), machine.getBios());
        stateValues.put(MachineProperty.BIOS_TYPE.name(), machine.getBiosType());
        stateValues.put(MachineProperty.BIOS_CODE.name(), machine.getBiosCode());
        stateValues.put(MachineProperty.BIOS_VARS.name(), machine.getBiosVars());
        stateValues.put(MachineProperty.TCG_BUFFER.name(), machine.getTcgBuffer());
        stateValues.put(MachineProperty.DNS.name(), machine.getDns());

        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        Date date = new Date();
        stateValues.put(MachineProperty.LAST_UPDATED.name(), dateFormat.format(date));
        stateValues.put(MachineProperty.STATUS.name(), Config.STATUS_CREATED);

        try {
            seqnum = (int) db.insertOrThrow(MACHINE_TABLE_NAME, null, stateValues);
        } catch (Exception e) {
            Log.w(TAG, "Error while Insert machine: " + e.getMessage());
            e.printStackTrace();
        }
        return seqnum;
    }

    public void updateMachineFieldAsync(final Machine machine, final MachineProperty property,
                                 final String value) {
        Thread t = new Thread(new Runnable() {
            public void run() {
                updateMachineField(machine, property, value);
            }
        });
        t.start();
    }

    public void updateMachineField(Machine machine, MachineProperty property, String value) {
        if (machine == null)
            return;
        ContentValues stateValues = new ContentValues();
        stateValues.put(property.name(), value);
        try {
            db.beginTransaction();
            db.update(MACHINE_TABLE_NAME, stateValues,
                    MachineProperty.MACHINE_NAME.name() + "=\"" + machine.getName() + "\" ",
                    null);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.w(TAG, "Error while Updating value: " + e.getMessage());
            if (Config.debug)
                e.printStackTrace();
        } finally {
            db.endTransaction();
        }
    }

    public Machine getMachine(String machine) {
        String qry = "select "
                + MachineProperty.MACHINE_NAME + " , " + MachineProperty.CPU + " , " + MachineProperty.MEMORY + " , " + MachineProperty.CDROM + " , " + MachineProperty.FDA
                + " , " + MachineProperty.FDB + " , " + MachineProperty.HDA + " , " + MachineProperty.HDB + " , " + MachineProperty.HDC + " , " + MachineProperty.HDD + " , "
                + MachineProperty.NETCONFIG + " , " + MachineProperty.NICCONFIG + " , " + MachineProperty.VGA + " , " + MachineProperty.SOUNDCARD + " , "
                + MachineProperty.HDCONFIG + " , " + MachineProperty.DISABLE_ACPI + " , " + MachineProperty.DISABLE_HPET + " , "
                + MachineProperty.ENABLE_USBMOUSE + " , " + MachineProperty.SNAPSHOT_NAME + " , " + MachineProperty.BOOT_CONFIG + " , " + MachineProperty.KERNEL
                + " , " + MachineProperty.INITRD + " , " + MachineProperty.APPEND + " , " + MachineProperty.CPUNUM + " , " + MachineProperty.MACHINETYPE + " , "
                + MachineProperty.DISABLE_FD_BOOT_CHK + " , " + MachineProperty.ARCH + " , " + MachineProperty.SD + " , "
                + MachineProperty.SHARED_FOLDER + " , " + MachineProperty.SHARED_FOLDER_MODE + " , " + MachineProperty.EXTRA_PARAMS + " , "
                + MachineProperty.HOSTFWD + " , " + MachineProperty.GUESTFWD + " , " + MachineProperty.UI + ", " + MachineProperty.DISABLE_TSC + ", "
                + MachineProperty.MOUSE + ", " + MachineProperty.KEYBOARD + ", " + MachineProperty.ENABLE_MTTCG + ", " + MachineProperty.ENABLE_KVM + ", "
                + MachineProperty.HDA_INTERFACE + ", " + MachineProperty.HDB_INTERFACE + ", " + MachineProperty.HDC_INTERFACE + ", " + MachineProperty.HDD_INTERFACE + ", "
                + MachineProperty.CDROM_INTERFACE + " , " + MachineProperty.PRIO + " , " +  MachineProperty.ENABLE_VENUS + " , " + MachineProperty.BIOS + " , " + MachineProperty.BIOS_TYPE + " , " + MachineProperty.BIOS_CODE + " , " + MachineProperty.BIOS_VARS + " , " + MachineProperty.TCG_BUFFER + " , " + MachineProperty.DNS + " , " + MachineProperty.SHARED_FOLDER_TYPE + " "
                + " from " + MACHINE_TABLE_NAME
                + " where " + MachineProperty.STATUS + " = " + Config.STATUS_CREATED
                + " and " + MachineProperty.MACHINE_NAME + "=\"" + machine + "\"" + ";";

        Machine myMachine = null;

        Cursor cur = db.rawQuery(qry, null);

        cur.moveToFirst();
        if (!cur.isAfterLast()) {
            String machinename = cur.getString(0);
            myMachine = new Machine(machinename, false);

            myMachine.setCpu(cur.getString(1));
            myMachine.setMemory(cur.getInt(2));
            myMachine.setCdImagePath(cur.getString(3));
            if (myMachine.getCdImagePath() != null)
                myMachine.setEnableCDROM(true);

            myMachine.setFdaImagePath(cur.getString(4));
            if (myMachine.getFdaImagePath() != null)
                myMachine.setEnableFDA(true);
            myMachine.setFdbImagePath(cur.getString(5));
            if (myMachine.getFdbImagePath() != null)
                myMachine.setEnableFDB(true);

            myMachine.setHdaImagePath(cur.getString(6));
            myMachine.setHdbImagePath(cur.getString(7));
            myMachine.setHdcImagePath(cur.getString(8));
            myMachine.setHddImagePath(cur.getString(9));

            myMachine.setNetwork(cur.getString(10));
            myMachine.setNetworkCard(cur.getString(11));
            myMachine.setVga(cur.getString(12));
            myMachine.setSoundCard(cur.getString(13));
            myMachine.setDisableACPI(cur.getInt(15));
            myMachine.setDisableHPET(cur.getInt(16));
            myMachine.setBootDevice(cur.getString(19));
            myMachine.setKernel(cur.getString(20));
            myMachine.setInitRd(cur.getString(21));
            myMachine.setAppend(cur.getString(22));
            myMachine.setCpuNum(cur.getInt(23));
            myMachine.setMachineType(cur.getString(24));
            myMachine.setDisableFdBootChk(cur.getInt(25));
            myMachine.setArch(cur.getString(26));

            myMachine.setSharedFolderPath(cur.getString(28));
            myMachine.setShared_folder_mode(1); //hard drives are always Read/Write
            myMachine.setExtraParams(cur.getString(30));
            myMachine.setHostFwd(cur.getString(31));
            myMachine.setGuestFwd(cur.getString(32));
            myMachine.setEnableVNC(cur.getString(33).equals("VNC") ? 1 : 0);
            myMachine.setRenderer(cur.getString(33).equals("VNC") ? 1 : 0);
            myMachine.setDisableTSC(cur.getInt(34));
            myMachine.setMouse(cur.getString(35));
            myMachine.setKeyboard(cur.getString(36));
            myMachine.setEnableMTTCG(cur.getInt(37));
            myMachine.setEnableKVM(cur.getInt(38));
            myMachine.setHdaInterface(cur.getString(39));
            myMachine.setHdbInterface(cur.getString(40));
            myMachine.setHdcInterface(cur.getString(41));
            myMachine.setHddInterface(cur.getString(42));
            myMachine.setCdInterface(cur.getString(43));
            myMachine.setPrio(cur.getInt(44));
            myMachine.setEnableVenus(cur.getInt(45));
            myMachine.setBios(cur.getString(46));
            myMachine.setBiosType(cur.getString(47));
            myMachine.setBiosCode(cur.getString(48));
            myMachine.setBiosVars(cur.getString(49));
            myMachine.setTcgBuffer(cur.getInt(50));
            myMachine.setDns(cur.getString(51));
            myMachine.setSharedFolderType(cur.getString(52));
        }
        cur.close();

        return myMachine;
    }

    public ArrayList<String> getMachineNames() {
        String qry = "select " + MachineProperty.MACHINE_NAME + " " + " from " + MACHINE_TABLE_NAME
                + " where " + MachineProperty.STATUS + " = " + Config.STATUS_CREATED + " order by 1; ";

        ArrayList<String> arrStr = new ArrayList<>();
        Cursor cur = db.rawQuery(qry, null);
        cur.moveToFirst();
        while (!cur.isAfterLast()) {
            String machinename = cur.getString(0);
            cur.moveToNext();
            arrStr.add(machinename);
        }
        cur.close();

        return arrStr;
    }

    public boolean deleteMachine(Machine machine) {
        int rowsAffected = 0;
        try {
            rowsAffected = db.delete(MACHINE_TABLE_NAME, MachineProperty.MACHINE_NAME + "=\"" + machine.getName() + "\"", null);
        } catch (Exception e) {
            Log.w(TAG, "Error while deleting VM: " + e.getMessage());
            if (Config.debug)
                e.printStackTrace();
        }

        boolean deleted = rowsAffected > 0;
        return deleted;
    }

    public synchronized boolean renameMachine(Machine machine, String newName) {
        if (machine == null || newName == null)
            return false;
        if (getMachine(newName) != null)
            return false;

        int rowsAffected = 0;
        try {
            ContentValues stateValues = new ContentValues();
            stateValues.put(MachineProperty.MACHINE_NAME.name(), newName);
            db.beginTransaction();
            rowsAffected = db.update(MACHINE_TABLE_NAME, stateValues,
                    MachineProperty.MACHINE_NAME.name() + "=\"" + machine.getName() + "\" ",
                    null);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.w(TAG, "Error while renaming VM: " + e.getMessage());
            if (Config.debug)
                e.printStackTrace();
        } finally {
            db.endTransaction();
        }

        boolean renamed = rowsAffected > 0;
        if (renamed)
            machine.setName(newName);
        return renamed;
    }


    @Override
    public void update(Observable observable, Object o) {
        Object[] params = (Object[]) o;
        MachineProperty property = (MachineProperty) params[0];
        Object value = params[1];
        switch(property) {
            case UI:
                updateMachineField((Machine) observable, property, ((int) params[1]) == 1 ? "VNC" : "QGE");
                return;
            case OTHER:
                return;
        }
        if(value instanceof Integer)
            updateMachineField((Machine) observable, property, ((int) params[1])+"");
        else
            updateMachineField((Machine) observable, property, (String) params[1]);

    }
}
