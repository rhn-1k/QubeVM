/*
Copyright (C) Max Kastanas 2012
Copyright (C) Rhn 2026
 */
package com.max2idea.android.qube.main;

import android.app.Activity;
import androidx.appcompat.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.qube.emu.lib.R;
import com.max2idea.android.qube.machine.Machine;
import com.max2idea.android.qube.machine.Machine.FileType;
import com.max2idea.android.qube.machine.MachineAction;
import com.max2idea.android.qube.machine.MachineController;
import com.max2idea.android.qube.machine.MachineFilePaths;
import com.max2idea.android.qube.machine.MachineProperty;
import com.max2idea.android.qube.ui.SpinnerAdapter;
import com.max2idea.android.qube.files.FileUtils;

import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

/** A simple custom dialog that serves as a way to change removable drives for Qube.
 * This class communicates passively with the ViewController which observes for changes on the ui.
 */
public class DrivesDialogBox implements Observer {
    private static final String TAG = "DrivesDialogBox";

    private final Machine currMachine;
    private final AlertDialog dialog;
    public Spinner mCD;
    public Spinner mFDA;
    public Spinner mFDB;
    public LinearLayout mCDLayout;
    public LinearLayout mFDALayout;
    public LinearLayout mFDBLayout;
    public FileType fileType;
    private Activity activity;
    private ViewListener viewListener;


    public DrivesDialogBox(Activity activity, int theme, Machine currMachine) {
        this.activity = activity;
        this.currMachine = currMachine;

        dialog = new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.RemovableDrives)
                .setView(R.layout.dev_dialog)
                .create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                setViewListener(null);
                MachineController.getInstance().getMachine().deleteObserver(DrivesDialogBox.this);
            }
        });
        MachineController.getInstance().getMachine().addObserver(this);
    }

    public void show() {
        dialog.show();
        getWidgets();
        initUI();
        setupController();
    }

    public void dismiss() {
        dialog.dismiss();
    }

    public boolean isShowing() {
        return dialog.isShowing();
    }

    private void setupController() {
        setViewListener(QubeApplication.getViewListener());
    }

    public void setViewListener(ViewListener viewListener) {
        this.viewListener = viewListener;
    }

    private void getWidgets() {
        mCD = (Spinner) dialog.findViewById(R.id.cdromimgval);
        mCDLayout = dialog.findViewById(R.id.cdromimgl);

        mFDA = (Spinner) dialog.findViewById(R.id.floppyimgval);
        mFDALayout = dialog.findViewById(R.id.floppyimgl);

        mFDB = (Spinner) dialog.findViewById(R.id.floppybimgval);
        mFDBLayout = dialog.findViewById(R.id.floppybimgl);

    }

    private void setupListeners() {
        setupListener(mCD, MachineProperty.CDROM, FileType.CDROM);
        setupListener(mFDA, MachineProperty.FDA, FileType.FDA);
        setupListener(mFDB, MachineProperty.FDB, FileType.FDB);
    }

    private void setupListener(final Spinner spinner, final MachineProperty machineDrive,
                               final FileType fileType) {
        spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                setDriveValue(spinner, position, machineDrive, fileType);
            }

            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });
    }

    private void setDriveValue(Spinner spinner, int position, MachineProperty driveLabel,
                               FileType filetype) {
        String diskValue = (String) ((ArrayAdapter<?>) spinner.getAdapter()).getItem(position);
        if (position == 0) {
            notifyFieldChange(MachineProperty.REMOVABLE_DRIVE, new Object[]{ driveLabel, ""});
        } else if (position == 1) {
            this.fileType = filetype;
            FileUtils.browse(activity, filetype, Config.OPEN_IMAGE_FILE_REQUEST_CODE);
            spinner.setSelection(0);
        } else if (position > 1) {
            notifyFieldChange(MachineProperty.REMOVABLE_DRIVE, new Object[]{ driveLabel, diskValue});
        }
    }

    public void populateDiskAdapter(final Spinner spinner, final FileType fileType, final boolean createOption,
                                    final String value) {
        Thread t = new Thread(new Runnable() {
            public void run() {
                ArrayList<String> oldHDs = MachineFilePaths.getRecentFilePaths(fileType);
                final ArrayList<String> arraySpinner = new ArrayList<>();
                arraySpinner.add("None");
                if (createOption)
                    arraySpinner.add("New");
                arraySpinner.add(activity.getString(R.string.open));
                final int index = arraySpinner.size();
                for (String file : oldHDs) {
                    if (file != null) {
                        arraySpinner.add(file);
                    }
                }
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    public void run() {
                        SpinnerAdapter adapter = new SpinnerAdapter(activity, R.layout.custom_spinner_item, arraySpinner, index);
                        adapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
                        spinner.setAdapter(adapter);
                        spinner.invalidate();
                        setDiskValue(spinner, value);
                    }
                });
            }
        });
        t.start();
    }

    private void initUI() {
        Thread thread = new Thread(new Runnable() {
            public void run() {
                if (currMachine.isEnableCDROM()) {
                    populateDiskAdapter(mCD, FileType.CDROM, false, currMachine.getCdImagePath());
                } else {
                    mCDLayout.setVisibility(View.GONE);
                }
                if (currMachine.isEnableFDA()) {
                    populateDiskAdapter(mFDA, FileType.FDA, false, currMachine.getFdaImagePath());
                } else {
                    mFDALayout.setVisibility(View.GONE);
                }
                if (currMachine.isEnableFDB()) {
                    populateDiskAdapter(mFDB, FileType.FDB, false, currMachine.getFdbImagePath());
                } else {
                    mFDBLayout.setVisibility(View.GONE);
                }
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    public void run() {
                        setupListeners();
                    }
                }, 500);
            }
        });
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    public void setDriveAttr(FileType fileType, final String file) {
        notifyAction(MachineAction.INSERT_FAV, new Object[]{file, fileType});
        if (fileType == FileType.CDROM && file != null && !file.trim().equals("")) {
            notifyFieldChange(MachineProperty.REMOVABLE_DRIVE, new Object[]{ MachineProperty.CDROM, file});
            setSpinnerValue(mCD, file);
        } else if (file != null && !file.trim().equals("") && fileType == FileType.FDA) {
            notifyFieldChange(MachineProperty.REMOVABLE_DRIVE, new Object[]{ MachineProperty.FDA, file});
            setSpinnerValue(mFDA, file);
        } else if (file != null && !file.trim().equals("") && fileType == FileType.FDB) {
            notifyFieldChange(MachineProperty.REMOVABLE_DRIVE, new Object[]{ MachineProperty.FDB, file});
            setSpinnerValue(mFDB, file);
        }
    }

    private void setSpinnerValue(final Spinner spinner, final String value) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            public void run() {
                if (SpinnerAdapter.getItemPosition(spinner, value) < 0) {
                    SpinnerAdapter.addItem(spinner, value);
                }
                setDiskValue(spinner, value);
                int res = spinner.getSelectedItemPosition();
                if (res == 1) {
                    spinner.setSelection(0);
                }
            }
        });
    }


    private void setDiskValue(final Spinner spinner, final String value) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (spinner != null) {
                    int pos = SpinnerAdapter.getItemPosition(spinner, value);
                    if (pos > 1) {
                        spinner.setSelection(pos);
                    } else {
                        spinner.setSelection(0);
                    }
                }
            }
        });

    }

    public void notifyFieldChange(MachineProperty property, Object value) {
        if(viewListener !=null)
            viewListener.onFieldChange(property, value);
    }

    public void notifyAction(MachineAction action, Object value) {
        if(viewListener !=null)
            viewListener.onAction(action, value);
    }

    @Override
    public void update(Observable observable, Object o) {
        Object[] params = (Object[]) o;
        MachineProperty property = (MachineProperty) params[0];
        Object value = params[1];
        //XXX: if the executor is not able to change the drive then we reset the spinner
        switch(property) {
            case CDROM:
                if(value == null)
                    setDiskValue(mCD, "");
                break;
            case FDA:
                if(value == null)
                    setDiskValue(mFDA, "");
                break;
            case FDB:
                if(value == null)
                    setDiskValue(mFDB, "");
                break;
        }
    }
}
