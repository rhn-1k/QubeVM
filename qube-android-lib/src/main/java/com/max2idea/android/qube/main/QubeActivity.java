/*
Copyright (C) Max Kastanas 2012
Copyright (C) Rhn 2026
 */
package com.max2idea.android.qube.main;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import com.google.android.material.button.MaterialButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.net.Uri;
import android.os.Environment;
import android.provider.Settings;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.qube.emu.lib.BuildConfig;
import com.qube.emu.lib.R;
import com.max2idea.android.qube.dialog.DialogUtils;
import com.max2idea.android.qube.files.FileInstaller;
import com.max2idea.android.qube.files.FileUtils;
import com.max2idea.android.qube.help.Help;
import com.max2idea.android.qube.install.Installer;
import com.max2idea.android.qube.keyboard.KeyboardUtils;
import com.max2idea.android.qube.log.Logger;
import com.max2idea.android.qube.machine.ArchDefinitions;
import com.max2idea.android.qube.machine.GraphicsCapabilities;
import com.max2idea.android.qube.machine.Machine;
import com.max2idea.android.qube.machine.Machine.FileType;
import com.max2idea.android.qube.machine.MachineAction;
import com.max2idea.android.qube.machine.MachineController;
import com.max2idea.android.qube.machine.MachineController.MachineStatus;
import com.max2idea.android.qube.machine.MachineFilePaths;
import com.max2idea.android.qube.machine.MachineProperty;
import com.max2idea.android.qube.server.SharedFolderServer;
import com.max2idea.android.qube.utils.ClipboardUtils;
import com.max2idea.android.qube.network.NetworkUtils;
import com.max2idea.android.qube.toast.ToastUtils;
import com.max2idea.android.qube.ui.SpinnerAdapter;
import com.max2idea.android.qube.updates.UpdateChecker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QubeActivity extends AppCompatActivity
        implements MachineController.OnMachineStatusChangeListener,
        MachineController.OnEventListener, Observer {

    private static final String TAG = "QubeActivity";

    private static final int HELP = 0;
    private static final int QUIT = 1;
    private static final int INSTALL = 2;
    private static final int DELETE = 3;
    private static final int RENAME = 4;

    private static final int CHANGELOG = 6;
    private static final int LICENSE = 7;
    private static final int VIEWLOG = 8;
    private static final int CREATE = 9;
    private static final int SETTINGS = 13;
    private static final int TOOLS = 14;
    private static final int POST_NOTIFICATIONS_REQUEST = 16;

    // TCG Buffer
    private static final int TCG_MIN = 256;
    private static final int TCG_MAX = 2048;

    // disk mapping
    private static final Hashtable<FileType, DiskInfo> diskMapping = new Hashtable<>();

    private static boolean libLoaded;
    public View parent;
    private boolean machineLoaded;
    private FileType browseFileType = null;

    //Widgets
    private ImageView mStatus;
    private EditText mDNS;
    private EditText mHOSTFWD;
    private EditText mAppend;
    private EditText mExtraParams;
    private TextView mStatusText;
    private Spinner mMachine;
    private Spinner mCPU;
    private Spinner mMachineType;
    private Spinner mCPUNum;
    private Spinner mKernel;
    private Spinner mInitrd;
    private Spinner mBios;
    private Spinner mBiosType;
    private Spinner mBiosCode;
    private Spinner mBiosVars;

    // HDD
    private ImageView mHDAOptions;
    private Spinner mHDA;
    private ImageView mHDBOptions;
    private Spinner mHDB;
    private ImageView mHDCOptions;
    private Spinner mHDC;
    private ImageView mHDDOptions;
    private Spinner mHDD;
    private Spinner mSharedFolder;
    private ImageView mSharedFolderOptions;

    //removable
    private Spinner mCD;
    private Spinner mFDA;
    private Spinner mFDB;
    private MaterialSwitch mCDenable;
    private MaterialSwitch mFDAenable;
    private MaterialSwitch mFDBenable;
    private ImageView mCDOptions;
    private TextView mCDStr;
    private TextView mFDAStr;
    private TextView mFDBStr;

    // misc
    private Spinner mRamSize;
    private Spinner mBootDevices;
    private Spinner mNetworkCard;
    private Spinner mNetConfig;
    private Spinner mVGAConfig;
    private ImageView mVGAConfigInfo;
    private ImageView mUIInfo;
    private MaterialSwitch mEnableVenus;
    private ImageView mEnableVenusInfo;
    private MaterialSwitch mHostShareServer;
    private ImageView mHostShareServerInfo;
    private LinearLayout mHostShareServerRow;
    private final OnCheckedChangeListener mHostShareServerCheckedChangeListener = new OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                hostShareServerExecutor.execute(() -> {
                    boolean started = SharedFolderServer.startServer();
                    runOnUiThread(() -> {
                        if (!started) {
                            mHostShareServer.setOnCheckedChangeListener(null);
                            mHostShareServer.setChecked(false);
                            mHostShareServer.setOnCheckedChangeListener(mHostShareServerCheckedChangeListener);
                            ToastUtils.toastShort(QubeActivity.this, getString(R.string.share_folder_start_error));
                        }
                    });
                });
            } else {
                SharedFolderServer.stopServer();
            }
        }
    };
    private final ExecutorService hostShareServerExecutor = Executors.newSingleThreadExecutor();
    private Spinner mSoundCard;
    private MaterialSwitch mHighPrio;
    private Spinner mUI;
    private MaterialSwitch mDisableACPI;
    private MaterialSwitch mDisableHPET;
    private MaterialSwitch mDisableTSC;
    private MaterialSwitch mEnableKVM;
    private MaterialSwitch mEnableMTTCG;
    private com.google.android.material.slider.Slider mTcgBuffer;
    private TextView mTcgBufferDisplay;
    private Spinner mKeyboard;
    private Spinner mMouse;

    // buttons
    private MaterialButton mStart;
    private MaterialButton mStop;
    private MaterialButton mRestart;

    //sections
    private LinearLayout mCPUSectionDetails;
    private LinearLayout mStorageSectionDetails;
    private LinearLayout mUserInterfaceSectionDetails;
    private LinearLayout mAdvancedSectionDetails;
    private LinearLayout mBootSectionDetails;
    private LinearLayout mGraphicsSectionDetails;
    private LinearLayout mRemovableStorageSectionDetails;
    private LinearLayout mBiosSectionDetails;
    private LinearLayout mNetworkSectionDetails;
    private LinearLayout mAudioSectionDetails;

    //summary
    private TextView mUISectionSummary;
    private TextView mCPUSectionSummary;
    private TextView mStorageSectionSummary;
    private TextView mRemovableStorageSectionSummary;
    private TextView mBiosSectionSummary;
    private TextView mGraphicsSectionSummary;
    private TextView mAudioSectionSummary;
    private TextView mNetworkSectionSummary;
    private TextView mBootSectionSummary;
    private TextView mAdvancedSectionSummary;

    //layouts
    private NestedScrollView mScrollView;
    private boolean firstMTTCGCheck;
    private ViewListener viewListener;

    public void changeStatus(final MachineStatus status_changed) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (MachineController.getInstance().isRunning() || status_changed == MachineStatus.Running) {
                    mStatus.setImageResource(R.drawable.power_settings_new_24px);
                    mStatusText.setText(R.string.Running);
                    //XXX: we block the user from changing the drives from this activity
                    // while running, since the VM thread will block on it
                    enableRemovableDiskValues(true);
                    unlockRemovableDevices(false);
                    enableNonRemovableDeviceOptions(false);
                    mMachine.setEnabled(false);
                    } else if (status_changed == MachineStatus.Ready || status_changed == MachineStatus.Stopped) {
                        mStatus.setImageResource(R.drawable.power_settings_new_24px);
                        mStatusText.setText(R.string.Stopped);
                        if (getMachine() != null) {
                            unlockRemovableDevices(true);
                            enableRemovableDiskValues(true);
                            enableNonRemovableDeviceOptions(true);
                        } else {
                            unlockRemovableDevices(false);
                            enableRemovableDiskValues(false);
                            enableNonRemovableDeviceOptions(false);
                        }
                    }
                }
            });
        }

    private void onTap() {
        String userid = QubeApplication.getUserId(this);
        if (!(new File("/dev/net/tun")).exists()) {
            QubeActivityCommon.tapNotSupported(this, userid);
            return;
        }
        QubeActivityCommon.promptTap(this, userid);
    }

    public void setUserPressed(boolean pressed) {
        if (pressed) {
            setupMiscOptions();
            setupNonRemovableDiskListeners();
            enableRemovableDiskListeners();
        } else {
            disableListeners();
            disableRemovableDiskListeners();
        }
    }

    private void disableRemovableDiskListeners() {
        disableRemovableDiskListener(mCDenable, mCD);
        disableRemovableDiskListener(mFDAenable, mFDA);
        disableRemovableDiskListener(mFDBenable, mFDB);
    }

    private void disableRemovableDiskListener(MaterialSwitch enableDrive, Spinner spinner) {
        enableDrive.setOnCheckedChangeListener(null);
        spinner.setOnItemSelectedListener(null);
    }

    private void enableRemovableDiskListeners() {
        enableRemovableDiskListener(mCD, mCDenable, mCDOptions, MachineProperty.CDROM, FileType.CDROM);
        enableRemovableDiskListener(mFDA, mFDAenable, null, MachineProperty.FDA, FileType.FDA);
        enableRemovableDiskListener(mFDB, mFDBenable, null, MachineProperty.FDB, FileType.FDB);
    }

    private void enableRemovableDiskListener(final Spinner spinner, final MaterialSwitch driveEnable,
    final ImageView driveOptions,
    final MachineProperty driveName,
    final FileType fileType) {
        spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if (getMachine() == null)
                    return;
                String value = (String) ((ArrayAdapter<?>) spinner.getAdapter()).getItem(position);
                if (position == 1 && driveEnable.isChecked()) {
                    browseFileType = fileType;
                    FileUtils.browse(QubeActivity.this, browseFileType, Config.OPEN_IMAGE_FILE_REQUEST_CODE);
                    spinner.setSelection(0);
                } else {
                    notifyFieldChange(MachineProperty.REMOVABLE_DRIVE, new Object[]{driveName, value});
                }
            }

            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });
        driveEnable.setOnCheckedChangeListener(
                new OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton viewButton, boolean isChecked) {
                        // Determine which label TextView belongs to this spinner
                        TextView label = null;
                        if (spinner == mCD) label = mCDStr;
                        else if (spinner == mFDA) label = mFDAStr;
                        else if (spinner == mFDB) label = mFDBStr;
                        setRemovableDriveRowEnabled(label, spinner, isChecked, true);
                        notifyFieldChange(MachineProperty.DRIVE_ENABLED, new Object[]{driveName, isChecked});
                        triggerUpdateSpinner(spinner);
                    }
                }
        );
        if(driveOptions!=null) {
            driveOptions.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(driveEnable.isChecked())
                        promptDriveInterface(driveName);
                }
            });
        }
    }


    private void setupMiscOptions() {

        mCPU.setOnItemSelectedListener(new OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if (getMachine() == null)
                    return;
                String cpu = (String) ((ArrayAdapter<?>) mCPU.getAdapter()).getItem(position);
                notifyFieldChange(MachineProperty.CPU, cpu);
            }

            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        mMachineType.setOnItemSelectedListener(new OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if (getMachine() == null)
                    return;
                String machineType = (String) ((ArrayAdapter<?>) mMachineType.getAdapter()).getItem(position);
                notifyFieldChange(MachineProperty.MACHINETYPE, machineType);
            }

            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        mUI.setOnItemSelectedListener(new OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if (getMachine() == null)
                    return;
                String ui = (String) ((ArrayAdapter<?>) mUI.getAdapter()).getItem(position);
                if ("VNC".equals(ui) && GraphicsCapabilities.isVirglGpu(getSelectedVga())) {
                    enforceQGEForVirgl();
                    return;
                }
                notifyFieldChange(MachineProperty.UI, ui);
                boolean isVNC = ui.equals("VNC");
                updateHostShareServerVisibility(isVNC);
                if (!isVNC) {
                    stopHostShareServerIfRunning();
                }
                if (ui.equals("VNC")) {
                    SpinnerAdapter.setDiskAdapterValue(mSoundCard, "None");
                    notifyFieldChange(MachineProperty.SOUNDCARD, "None");
                    updateSoundCardEnabledState(false);
                } else if (Config.enableQGESound) {
                    updateSoundCardEnabledState(true);
                }
            }

            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        mCPUNum.setOnItemSelectedListener(new OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if (getMachine() == null)
                    return;
                final String cpuNum = (String) ((ArrayAdapter<?>) mCPUNum.getAdapter()).getItem(position);
                if (position > 0 && getMachine().getEnableMTTCG() != 1 && getMachine().getEnableKVM() != 1 && !firstMTTCGCheck) {
                    firstMTTCGCheck = true;
                    promptMultiCPU(cpuNum);
                } else {
                    notifyFieldChange(MachineProperty.CPUNUM, cpuNum);
                }
            }

            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        mRamSize.setOnItemSelectedListener(new OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if (getMachine() == null)
                    return;
                String ram = (String) ((ArrayAdapter<?>) mRamSize.getAdapter()).getItem(position);
                notifyFieldChange(MachineProperty.MEMORY, ram);
            }

            public void onNothingSelected(AdapterView<?> parentView) {

            }
        });

        mKernel.setOnItemSelectedListener(new OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if (getMachine() == null)
                    return;
                String kernel = (String) ((ArrayAdapter<?>) mKernel.getAdapter()).getItem(position);
                if (position == 0) {
                    notifyFieldChange(MachineProperty.KERNEL, "");
                } else if (position == 1) {
                    browseFileType = FileType.KERNEL;
                    FileUtils.browse(QubeActivity.this, browseFileType, Config.OPEN_IMAGE_FILE_REQUEST_CODE);
                    mKernel.setSelection(0);
                } else if (position > 1) {
                    notifyFieldChange(MachineProperty.KERNEL, kernel);
                }
            }

            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        mBiosType.setOnItemSelectedListener(new OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if (getMachine() == null)
                    return;
                String biosType = (String) ((ArrayAdapter<?>) mBiosType.getAdapter()).getItem(position);
                notifyFieldChange(MachineProperty.BIOS_TYPE, biosType);
                updateBiosTypeVisibility(biosType);
                if ("UEFI".equalsIgnoreCase(biosType))
                    ensureDefaultUefiFirmware();
            }

            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        mBios.setOnItemSelectedListener(new OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if (getMachine() == null)
                    return;
                String bios = (String) ((ArrayAdapter<?>) mBios.getAdapter()).getItem(position);
                if (position == 0) {
                    notifyFieldChange(MachineProperty.BIOS, "");
                } else if (position == 1) {
                    browseFileType = FileType.BIOS;
                    FileUtils.browse(QubeActivity.this, browseFileType, Config.OPEN_IMAGE_FILE_REQUEST_CODE);
                    mBios.setSelection(0);
                } else if (position > 1) {
                    notifyFieldChange(MachineProperty.BIOS, bios);
                }
            }

            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        setupUefiFileListener(mBiosCode, MachineProperty.BIOS_CODE, FileType.BIOS_CODE);
        setupUefiFileListener(mBiosVars, MachineProperty.BIOS_VARS, FileType.BIOS_VARS);

        mInitrd.setOnItemSelectedListener(new OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if (getMachine() == null)
                    return;
                String initrd = (String) ((ArrayAdapter<?>) mInitrd.getAdapter()).getItem(position);
                if (position == 0) {
                    notifyFieldChange(MachineProperty.INITRD, "");
                } else if (position == 1) {
                    browseFileType = FileType.INITRD;
                    FileUtils.browse(QubeActivity.this, browseFileType, Config.OPEN_IMAGE_FILE_REQUEST_CODE);
                    mInitrd.setSelection(0);
                } else if (position > 1) {
                    notifyFieldChange(MachineProperty.INITRD, initrd);
                }
            }

            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        mBootDevices.setOnItemSelectedListener(new OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if (getMachine() == null)
                    return;

                String bootDev = (String) ((ArrayAdapter<?>) mBootDevices.getAdapter()).getItem(position);
                notifyFieldChange(MachineProperty.BOOT_CONFIG, bootDev);
            }

            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        mNetConfig.setOnItemSelectedListener(new OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if (getMachine() == null)
                    return;

                String netcfg = (String) ((ArrayAdapter<?>) mNetConfig.getAdapter()).getItem(position);
                notifyFieldChange(MachineProperty.NETCONFIG, netcfg);
                if (position > 0
                        && MachineController.getInstance().getCurrStatus() != MachineStatus.Running) {
                    mNetworkCard.setEnabled(true);
                    mDNS.setEnabled(true);
                    mHOSTFWD.setEnabled(true);
                } else {
                    mNetworkCard.setEnabled(false);
                    mDNS.setEnabled(false);
                    mHOSTFWD.setEnabled(false);
                }

                if (netcfg.equals("TAP")) {
                    onTap();
                } else if (netcfg.equals("User")) {
                    QubeActivityCommon.onNetworkUser(QubeActivity.this);
                }
            }

            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        mNetworkCard.setOnItemSelectedListener(new OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if (getMachine() == null)
                    return;
                if (position < 0 || position >= mNetworkCard.getCount()) {
                    mNetworkCard.setSelection(0);
                    return;
                }
                String niccfg = (String) ((ArrayAdapter<?>) mNetworkCard.getAdapter()).getItem(position);
                notifyFieldChange(MachineProperty.NICCONFIG, niccfg);
            }

            public void onNothingSelected(final AdapterView<?> parentView) {
            }
        });

        mVGAConfig.setOnItemSelectedListener(new OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if (getMachine() == null)
                    return;
                String vgacfg = (String) ((ArrayAdapter<?>) mVGAConfig.getAdapter()).getItem(position);
                notifyFieldChange(MachineProperty.VGA, vgacfg);
                if (GraphicsCapabilities.isVirglGpu(vgacfg)) {
                    enforceQGEForVirgl();
                }
                updateVenusEnabledState();
            }

            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        if (mVGAConfigInfo != null) {
            mVGAConfigInfo.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    CharSequence message = Html.fromHtml(getString(R.string.videoDisplayInfoMessage), Html.FROM_HTML_MODE_LEGACY);
                    new MaterialAlertDialogBuilder(QubeActivity.this)
                            .setTitle(R.string.videoDisplayInfoTitle)
                            .setIcon(R.drawable.desktop_windows_24px)
                            .setMessage(message)
                            .setCancelable(false)
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                }
            });
        }

        if (mUIInfo != null) {
            mUIInfo.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    CharSequence message = Html.fromHtml(getString(R.string.displayInfoMessage), Html.FROM_HTML_MODE_LEGACY);
                    new MaterialAlertDialogBuilder(QubeActivity.this)
                            .setTitle(R.string.displayInfoTitle)
                            .setIcon(R.drawable.cast_24px)
                            .setMessage(message)
                            .setCancelable(false)
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                }
            });
        }


        mSoundCard.setOnItemSelectedListener(new OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if (getMachine() == null)
                    return;
                String sndcfg = (String) ((ArrayAdapter<?>) mSoundCard.getAdapter()).getItem(position);
                notifyFieldChange(MachineProperty.SOUNDCARD, sndcfg);
            }

            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        mDisableACPI.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton viewButton, boolean isChecked) {
                if (getMachine() == null)
                    return;
                notifyFieldChange(MachineProperty.DISABLE_ACPI, isChecked);
            }
        });

        mDisableHPET.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton viewButton, boolean isChecked) {
                if (getMachine() == null)
                    return;
                notifyFieldChange(MachineProperty.DISABLE_HPET, isChecked);
            }
        });

        mDisableTSC.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton viewButton, boolean isChecked) {
                if (getMachine() == null)
                    return;
                notifyFieldChange(MachineProperty.DISABLE_TSC, isChecked);
            }
        });

        mDNS.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (getMachine() == null)
                    return;
                if (!hasFocus) {
                    setDNSServer(mDNS.getText().toString());
                    notifyFieldChange(MachineProperty.DNS, mDNS.getText().toString());
                    updateNetworkSummary(false);
                }
            }
        });

        mHOSTFWD.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (getMachine() == null)
                    return;
                if (!hasFocus) {
                    notifyFieldChange(MachineProperty.HOSTFWD, mHOSTFWD.getText().toString());
                }
            }
        });

        mAppend.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (getMachine() == null)
                    return;
                if (!hasFocus) {
                    notifyFieldChange(MachineProperty.APPEND, mAppend.getText().toString());
                }
            }
        });

        mExtraParams.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (getMachine() == null)
                    return;
                if (!hasFocus) {
                    notifyFieldChange(MachineProperty.EXTRA_PARAMS, mExtraParams.getText().toString());
                }
            }
        });

        OnClickListener resetClickListener = new OnClickListener() {
            @Override
            public void onClick(View view) {
                view.setFocusableInTouchMode(true);
                view.setFocusable(true);
            }
        };

        mDNS.setOnClickListener(resetClickListener);
        mAppend.setOnClickListener(resetClickListener);
        mHOSTFWD.setOnClickListener(resetClickListener);
        mExtraParams.setOnClickListener(resetClickListener);
        mEnableKVM.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton viewButton, boolean isChecked) {
                if (getMachine() == null)
                    return;
                if (isChecked) {
                    promptKVM();
                } else {
                    notifyFieldChange(MachineProperty.ENABLE_KVM, isChecked);
                }

            }

        });

        mEnableMTTCG.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton viewButton, boolean isChecked) {
                if (getMachine() == null)
                    return;
                if (isChecked) {
                    promptEnableMTTCG();
                } else {
                    notifyFieldChange(MachineProperty.ENABLE_MTTCG, isChecked);
                }
            }
        });

        mTcgBuffer.addOnChangeListener(new com.google.android.material.slider.Slider.OnChangeListener() {
            public void onValueChange(com.google.android.material.slider.Slider slider, float value, boolean fromUser) {
                if (getMachine() == null || !fromUser)
                    return;
                int intValue = (int) value;
                mTcgBufferDisplay.setText(intValue + "M");
                notifyFieldChange(MachineProperty.TCG_BUFFER, (int) value);
            }
        });

        mHighPrio.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (getMachine() == null)
                    return;
                if (isChecked) {
                    new MaterialAlertDialogBuilder(QubeActivity.this)
                            .setTitle(R.string.HighPriority)
                            .setMessage(R.string.highPriorityWarning)
                            .setIcon(R.drawable.info_24px)
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    notifyFieldChange(MachineProperty.PRIO, true);
                                }
                            })
                            .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    mHighPrio.setChecked(false);
                                }
                            })
                            .setCancelable(false)
                            .show();
                } else {
                    notifyFieldChange(MachineProperty.PRIO, false);
                }
            }
        });

        mKeyboard.setOnItemSelectedListener(new OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if (getMachine() == null)
                    return;
                String keyboardCfg = (String) ((ArrayAdapter<?>) mKeyboard.getAdapter()).getItem(position);
                notifyFieldChange(MachineProperty.KEYBOARD, keyboardCfg);
            }

            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        mMouse.setOnItemSelectedListener(new OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                String mouseCfg = (String) ((ArrayAdapter<?>) mMouse.getAdapter()).getItem(position);
                notifyFieldChange(MachineProperty.MOUSE, mouseCfg);
            }

            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });
    }

    private void setupUefiFileListener(final Spinner spinner, final MachineProperty property,
                                        final FileType fileType) {
        spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if (getMachine() == null)
                    return;
                String value = (String) ((ArrayAdapter<?>) spinner.getAdapter()).getItem(position);
                if (position == 0) {
                    notifyFieldChange(property, "Default");
                } else if (position == 1) {
                    browseFileType = fileType;
                    FileUtils.browse(QubeActivity.this, browseFileType, Config.OPEN_IMAGE_FILE_REQUEST_CODE);
                    spinner.setSelection(0);
                } else {
                    notifyFieldChange(property, value);
                }
            }

            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });
    }

    private void ensureDefaultUefiFirmware() {
        if (getMachine() == null)
            return;

        if (isUnsetUefiFirmware(getMachine().getBiosCode())) {
            notifyFieldChange(MachineProperty.BIOS_CODE, "Default");
            seMachineDriveValue(FileType.BIOS_CODE, "Default");
        }
        if (isUnsetUefiFirmware(getMachine().getBiosVars())) {
            notifyFieldChange(MachineProperty.BIOS_VARS, "Default");
            seMachineDriveValue(FileType.BIOS_VARS, "Default");
        }
    }

    private boolean isUnsetUefiFirmware(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void updateBiosTypeVisibility(String biosType) {
        boolean uefi = "UEFI".equalsIgnoreCase(biosType);
        View legacyBios = findViewById(R.id.biosl);
        View code = findViewById(R.id.bioscodel);
        View vars = findViewById(R.id.biosvarsl);
        if (legacyBios != null) legacyBios.setVisibility(uefi ? View.GONE : View.VISIBLE);
        if (code != null) code.setVisibility(uefi ? View.VISIBLE : View.GONE);
        if (vars != null) vars.setVisibility(uefi ? View.VISIBLE : View.GONE);
    }

    private void setCPUOptions() {
        if (MachineController.getInstance().getCurrStatus() != MachineStatus.Running &&
                (QubeApplication.arch == Config.Arch.x86 || QubeApplication.arch == Config.Arch.x86_64)) {
            mDisableACPI.setEnabled(true);
            mDisableHPET.setEnabled(true);
            mDisableTSC.setEnabled(true);
        } else {
            mDisableACPI.setEnabled(false);
            mDisableHPET.setEnabled(false);
            mDisableTSC.setEnabled(false);
        }
    }

    private void setArchOptions() {
        if (!machineLoaded) {
            populateMachineType(getMachine().getMachineType());
            populateCPUs(getMachine().getCpu());
            populateNetDevices(getMachine().getNetworkCard());
        }
    }

    private void promptKVM() {
        DialogInterface.OnClickListener okListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                notifyFieldChange(MachineProperty.ENABLE_KVM, true);
                mEnableMTTCG.setChecked(false);
            }
        };

        DialogInterface.OnClickListener cancelListener =
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mEnableKVM.setChecked(false);
                        notifyFieldChange(MachineProperty.ENABLE_KVM, false);
                    }
                };

        DialogInterface.OnClickListener helpListener =
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mEnableKVM.setChecked(false);
                        notifyFieldChange(MachineProperty.ENABLE_KVM, false);
                        QubeActivityCommon.goToURL(QubeActivity.this, Config.CpuLink);
                    }
                };

        DialogUtils.UIAlert(QubeActivity.this, getString(R.string.EnableKVM),
                getString(R.string.EnableKVMWarning),
                16, false, true, R.drawable.info_24px,
                getString(android.R.string.ok),
                okListener, getString(android.R.string.cancel),
                cancelListener, getString(R.string.KVMHelp), helpListener);
    }

    private void promptEnableMTTCG() {
        DialogInterface.OnClickListener okListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                notifyFieldChange(MachineProperty.ENABLE_MTTCG, true);
                mEnableKVM.setChecked(false);
            }
        };
        DialogInterface.OnClickListener cancelListener =
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        notifyFieldChange(MachineProperty.ENABLE_MTTCG, false);
                        mEnableMTTCG.setChecked(false);
                    }
                };
        DialogInterface.OnClickListener helpListener =
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mEnableMTTCG.setChecked(false);
                        notifyFieldChange(MachineProperty.ENABLE_MTTCG, false);
                        QubeActivityCommon.goToURL(QubeActivity.this, Config.CpuLink);
                    }
                };
        DialogUtils.UIAlert(QubeActivity.this, getString(R.string.enableMTTCG),
                getString(R.string.enableMTTCGWarning),
                16, false, true, R.drawable.info_24px,
                getString(android.R.string.ok), okListener,
                getString(android.R.string.cancel)
                , cancelListener, getString(R.string.mttcgHelp), helpListener);
    }

    private void promptMultiCPU(final String cpuNum) {
        DialogInterface.OnClickListener okListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                notifyFieldChange(MachineProperty.CPUNUM, cpuNum);
            }
        };
        DialogInterface.OnClickListener cancelListener =
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mCPUNum.setSelection(0);
                    }
                };
        DialogInterface.OnClickListener helpListener =
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mCPUNum.setSelection(0);
                        QubeActivityCommon.goToURL(QubeActivity.this, Config.CpuLink);
                    }
                };
        DialogUtils.UIAlert(QubeActivity.this, getString(R.string.multipleVCPU),
                getString(R.string.multipleVCPUWarning)
                        + ((QubeApplication.arch == Config.Arch.x86_64) ?
                        getString(R.string.disableTSCInstructions) : "")
                        + " " + getString(R.string.DoYouWantToContinue),
                16, false, true, R.drawable.info_24px,
                getString(android.R.string.ok), okListener,
                getString(android.R.string.cancel), cancelListener, getString(R.string.vCPUHelp), helpListener);
    }

    private void setupNonRemovableDiskListeners() {
        setupNonRemovableDiskListener(mHDA, mHDAOptions, MachineProperty.HDA, FileType.HDA);
        setupNonRemovableDiskListener(mHDB, mHDBOptions, MachineProperty.HDB, FileType.HDB);
        setupNonRemovableDiskListener(mHDC, mHDCOptions, MachineProperty.HDC, FileType.HDC);
        setupNonRemovableDiskListener(mHDD, mHDDOptions, MachineProperty.HDD, FileType.HDD);
        setupSharedFolderDisk();
    }

    private void setupNonRemovableDiskListener(final Spinner diskSpinner, final ImageView diskImage,
                                               final MachineProperty machineDriveName,
                                               final FileType diskFileType) {
        diskSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if (getMachine() == null)
                    return;

                String hdName = (String) ((ArrayAdapter<?>) diskSpinner.getAdapter()).getItem(position);
                if (position == 1) {
                    promptImageName(QubeActivity.this, diskFileType);
                    diskSpinner.setSelection(0);
                } else if (position == 2) {
                    browseFileType = diskFileType;
                    FileUtils.browse(QubeActivity.this, browseFileType, Config.OPEN_IMAGE_FILE_REQUEST_CODE);
                    diskSpinner.setSelection(0);
                } else {
                    notifyFieldChange(MachineProperty.NON_REMOVABLE_DRIVE, new Object[]{machineDriveName, hdName});
                }
            }

            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        diskImage.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                promptDriveInterface(machineDriveName);
            }
        });
    }

    private void promptDriveInterface(final MachineProperty machineDriveName) {
        if(getMachine() == null)
            return;

        final String[] items = machineDriveName == MachineProperty.SHARED_FOLDER
                ? new String[] { "vvfat", "virtio9p" }
                : new String[] { "ide", "scsi", "virtio" };
        final AlertDialog.Builder mBuilder = new MaterialAlertDialogBuilder(this);
        String driveTitle = machineDriveName == MachineProperty.SHARED_FOLDER
                ? getString(R.string.SharedFolder)
                : machineDriveName.toString();
        mBuilder.setTitle(driveTitle + " " + getString(R.string.Interface));
        mBuilder.setIcon(machineDriveName == MachineProperty.SHARED_FOLDER
                ? R.drawable.folder_24px
                : R.drawable.hard_drive_24px);
        int driveInterface = getMachineInterface(machineDriveName, items);
        mBuilder.setSingleChoiceItems(items, driveInterface, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                notifyFieldChange(MachineProperty.MEDIA_INTERFACE, new Object[] {machineDriveName, items[i]});
                dialog.dismiss();
            }
        });
        final AlertDialog alertDialog = mBuilder.create();
        alertDialog.show();
    }

    private int getMachineInterface(MachineProperty machineDriveName, String[] items) {
        String hdInterfaceStr = null;
        switch(machineDriveName) {
            case HDA:
                hdInterfaceStr = getMachine().getHdaInterface();
                break;
            case HDB:
                hdInterfaceStr = getMachine().getHdbInterface();
                break;
            case HDC:
                hdInterfaceStr = getMachine().getHdcInterface();
                break;
            case HDD:
                hdInterfaceStr = getMachine().getHddInterface();
                break;
            case CDROM:
                hdInterfaceStr = getMachine().getCDInterface();
                break;
            case SHARED_FOLDER:
                hdInterfaceStr = getMachine().getSharedFolderType();
                break;
        }
        for(int i=0; i<items.length; i++) {
            if(items[i].equals(hdInterfaceStr))
                return i;
        }
        return 0;
    }

    public void setupSharedFolderDisk() {
        mSharedFolder.setOnItemSelectedListener(new OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {

                if (getMachine() == null)
                    return;

                String shared_folder = (String) ((ArrayAdapter<?>) mSharedFolder.getAdapter()).getItem(position);
                if (position == 0) {
                    notifyFieldChange(MachineProperty.NON_REMOVABLE_DRIVE, new Object[]{MachineProperty.SHARED_FOLDER, shared_folder});
                } else if (position == 1) {
                    browseFileType = FileType.SHARED_DIR;
                    FileUtils.browse(QubeActivity.this, browseFileType, Config.OPEN_SHARED_DIR_REQUEST_CODE);
                  // Reset spinner to "Open" without re-triggering listener to avoid overwriting saved value
                    mSharedFolder.setOnItemSelectedListener(null);
                    mSharedFolder.setSelection(0);
                    mSharedFolder.setOnItemSelectedListener(this);
                } else if (position > 1) {
                    notifyFieldChange(MachineProperty.NON_REMOVABLE_DRIVE, new Object[]{MachineProperty.SHARED_FOLDER, shared_folder});
                }
            }

            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        mSharedFolderOptions.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                promptDriveInterface(MachineProperty.SHARED_FOLDER);
            }
        });
    }

    protected synchronized void setDNSServer(String string) {

        File resolvConf = new File(QubeApplication.getBasefileDir() + "/etc/resolv.conf");
        FileOutputStream fileStream = null;
        try {
            fileStream = new FileOutputStream(resolvConf);
            String str = "nameserver " + string + "\n\n";
            byte[] data = str.getBytes();
            fileStream.write(data);
        } catch (Exception ex) {
            Log.e(TAG, "Could not write DNS to file: " + ex);
        } finally {
            if (fileStream != null)
                try {
                    fileStream.close();
                } catch (IOException e) {

                    e.printStackTrace();
                }
        }
    }

    private void disableListeners() {
        if (mMachine == null)
            return;
        mUI.setOnItemSelectedListener(null);
        mKeyboard.setOnItemSelectedListener(null);
        mMouse.setOnItemSelectedListener(null);
        mMachineType.setOnItemSelectedListener(null);
        mCPU.setOnItemSelectedListener(null);
        mCPUNum.setOnItemSelectedListener(null);
        mRamSize.setOnItemSelectedListener(null);
        mDisableACPI.setOnCheckedChangeListener(null);
        mDisableHPET.setOnCheckedChangeListener(null);
        mDisableTSC.setOnCheckedChangeListener(null);
        mEnableKVM.setOnCheckedChangeListener(null);
        mEnableMTTCG.setOnCheckedChangeListener(null);
        mHighPrio.setOnCheckedChangeListener(null);
        mHDA.setOnItemSelectedListener(null);
        mHDB.setOnItemSelectedListener(null);
        mHDC.setOnItemSelectedListener(null);
        mHDD.setOnItemSelectedListener(null);
        mSharedFolder.setOnItemSelectedListener(null);
        mBootDevices.setOnItemSelectedListener(null);
        mKernel.setOnItemSelectedListener(null);
        mInitrd.setOnItemSelectedListener(null);
        mBiosType.setOnItemSelectedListener(null);
        mBios.setOnItemSelectedListener(null);
        mBiosCode.setOnItemSelectedListener(null);
        mBiosVars.setOnItemSelectedListener(null);
        mAppend.setOnFocusChangeListener(null);
        mVGAConfig.setOnItemSelectedListener(null);
        mSoundCard.setOnItemSelectedListener(null);
        mNetConfig.setOnItemSelectedListener(null);
        mNetworkCard.setOnItemSelectedListener(null);
        mDNS.setOnFocusChangeListener(null);
        mHOSTFWD.setOnFocusChangeListener(null);
        mExtraParams.setOnFocusChangeListener(null);
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupAppEnvironment();
        clearNotifications();
        setupStrictMode();
        setContentView(R.layout.qube_main);
        setupEdgeToEdge();
        setupWidgets();
        setupController();
        setupDiskMapping();
        createListeners();
        populateAttributesUI();
        checkFirstLaunch();
        setupToolbar();
        requestNotificationPermissionIfNeeded();
        checkUpdate();
        checkLog();
        checkAndLoadLibs();
        restore();
        setupListeners();
    }

    private void setupEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        View root = findViewById(R.id.main_layout);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), root);
        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(false);
        getWindow().setNavigationBarContrastEnforced(false);
        View appBar = findViewById(R.id.top_app_bar);
        final int appBarLeftPadding = appBar.getPaddingLeft();
        final int appBarTopPadding = appBar.getPaddingTop();
        final int appBarRightPadding = appBar.getPaddingRight();
        final int appBarBottomPadding = appBar.getPaddingBottom();
        android.view.ViewGroup.MarginLayoutParams appBarParams =
                (android.view.ViewGroup.MarginLayoutParams) appBar.getLayoutParams();
        final int appBarLeftMargin = appBarParams.leftMargin;
        final int appBarRightMargin = appBarParams.rightMargin;
        ViewCompat.setOnApplyWindowInsetsListener(appBar, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            android.view.ViewGroup.MarginLayoutParams params =
                    (android.view.ViewGroup.MarginLayoutParams) view.getLayoutParams();
            params.leftMargin = appBarLeftMargin + bars.left;
            params.rightMargin = appBarRightMargin + bars.right;
            view.setLayoutParams(params);
            view.setPadding(
                    appBarLeftPadding,
                    appBarTopPadding + bars.top,
                    appBarRightPadding,
                    appBarBottomPadding
            );
            return insets;
        });
        NestedScrollView scrollView = findViewById(R.id.scroll_view);
        final int leftPadding = scrollView.getPaddingLeft();
        final int topPadding = scrollView.getPaddingTop();
        final int rightPadding = scrollView.getPaddingRight();
        final int bottomPadding = scrollView.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(scrollView, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(
                    leftPadding + bars.left,
                    topPadding,
                    rightPadding + bars.right,
                    bottomPadding + bars.bottom
            );
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    POST_NOTIFICATIONS_REQUEST
            );
        }
    }

    private void setupAppEnvironment() {
        QubeApplication.setupEnv(this);
    }

    private void setupController() {
        setViewListener(QubeApplication.getViewListener());
    }

    public void setViewListener(ViewListener viewListener) {
        this.viewListener = viewListener;
    }

    private void setupListeners() {
        MachineController.getInstance().addOnStatusChangeListener(this);
        MachineController.getInstance().addOnEventListener(this);
    }

    private void restoreUI(final String machine) {
        int position = SpinnerAdapter.getItemPosition(mMachine, machine);
        mMachine.setSelection(position);
    }

    private void restore() {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if (MachineController.getInstance().isRunning()) {
                    restoreUI(MachineController.getInstance().getMachineName());
                }
            }
        }, 1000);
    }

    private void checkAndLoadLibs() {
        if (Config.loadNativeLibsEarly)
            if (Config.loadNativeLibsMainThread)
                setupNativeLibs();
            else
                setupNativeLibsAsync();
    }

    private void clearNotifications() {
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }

    private void setupDiskMapping() {
        diskMapping.clear();
        addDiskMapping(FileType.HDA, mHDA, null, MachineProperty.HDA);
        addDiskMapping(FileType.HDB, mHDB, null, MachineProperty.HDB);
        addDiskMapping(FileType.HDC, mHDC, null, MachineProperty.HDC);
        addDiskMapping(FileType.HDD, mHDD, null, MachineProperty.HDD);
        addDiskMapping(FileType.SHARED_DIR, mSharedFolder, null, MachineProperty.SHARED_FOLDER);

        addDiskMapping(FileType.CDROM, mCD, mCDenable, MachineProperty.CDROM);
        addDiskMapping(FileType.FDA, mFDA, mFDAenable, MachineProperty.FDA);
        addDiskMapping(FileType.FDB, mFDB, mFDBenable, MachineProperty.FDB);

        addDiskMapping(FileType.KERNEL, mKernel, null, MachineProperty.KERNEL);
        addDiskMapping(FileType.INITRD, mInitrd, null, MachineProperty.INITRD);
        addDiskMapping(FileType.BIOS, mBios, null, MachineProperty.BIOS);
        addDiskMapping(FileType.BIOS_CODE, mBiosCode, null, MachineProperty.BIOS_CODE);
        addDiskMapping(FileType.BIOS_VARS, mBiosVars, null, MachineProperty.BIOS_VARS);
    }

    private void addDiskMapping(FileType fileType, Spinner spinner,
                                MaterialSwitch enableCheckBox, MachineProperty dbColName) {
        spinner.setTag(fileType);

        diskMapping.put(fileType, new DiskInfo(spinner, enableCheckBox, dbColName));
    }

    private void setupNativeLibsAsync() {

        Thread thread = new Thread(new Runnable() {
            public void run() {
                setupNativeLibs();
            }
        });
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();

    }

    private void createListeners() {

        mMachine.setOnItemSelectedListener(new OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {

                if (position == 0) {
                    enableNonRemovableDeviceOptions(false);
                    enableRemovableDeviceOptions(false);
                    updateSummary();
                    if (!MachineController.getInstance().isRunning())
                        notifyAction(MachineAction.LOAD_VM, null);
                } else if (position == 1) {
                    mMachine.setSelection(0);
                    promptMachineName(QubeActivity.this);
                } else {
                    final String machine = (String) ((ArrayAdapter<?>) mMachine.getAdapter()).getItem(position);
                    setUserPressed(false);
                    machineLoaded = true;
                    notifyAction(MachineAction.LOAD_VM, machine);
                }
            }

            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        mScrollView.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                savePendingEditText();
            }
        });

        mStart.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                if (!Config.loadNativeLibsEarly && Config.loadNativeLibsMainThread) {
                    setupNativeLibs();
                }
                Thread thread = new Thread(new Runnable() {
                    public void run() {
                        if (!Config.loadNativeLibsEarly && !Config.loadNativeLibsMainThread) {
                            setupNativeLibs();
                        }
                        onStartButton();
                    }
                });
                thread.setPriority(Thread.MIN_PRIORITY);
                thread.start();
            }
        });
        mStop.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                onStopButton(false);
            }
        });
        mRestart.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                onRestartButton();
            }
        });
    }

    private void savePendingEditText() {
        View currentView = getCurrentFocus();
        if (currentView instanceof EditText) {
            currentView.setFocusable(false);
        }
    }

    private void checkFirstLaunch() {
        Thread t = new Thread(new Runnable() {
            public void run() {
                if (QubeSettingsManager.isFirstLaunch(QubeActivity.this)) {
                    onFirstLaunch();
                } else {
                    checkStoragePermission();
                }
            }
        });
        t.start();
    }

    private void checkLog() {
        Thread t = new Thread(new Runnable() {
            public void run() {
                if (QubeSettingsManager.getExitCode(QubeActivity.this) != Config.EXIT_SUCCESS) {
                    if (MachineController.getInstance().isRunning())
                        QubeSettingsManager.setExitCode(QubeActivity.this, Config.EXIT_UNKNOWN);
                    else
                        QubeSettingsManager.setExitCode(QubeActivity.this, Config.EXIT_SUCCESS);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            com.max2idea.android.qube.log.Logger.promptShowLog(QubeActivity.this);
                        }
                    });
                }
            }
        });
        t.start();
    }

    //XXX: this needs to be called from the main thread otherwise
    //  qemu crashes when it is started later
    public void setupNativeLibs() {
        if (libLoaded)
            return;
        // Compatibility lib
        System.loadLibrary("compat-qube");

        // Glib deps
        System.loadLibrary("compat-musl");

        // Glib for qemu
        System.loadLibrary("glib-2.0");

        // Pixman for qemu
        System.loadLibrary("pixman-1");

        // VirGL for qemu
        try {
            System.loadLibrary("epoxy");
            System.loadLibrary("virglrenderer");
        // If not found (disabled) skip
        } catch (UnsatisfiedLinkError e) {
        }

        //Qube needed for vmexecutor
        System.loadLibrary("qube");

        // qemu arch specific lib
        loadQEMULib();

        libLoaded = true;
    }

    protected void loadQEMULib() {

    }

    public void setupToolbar() {
        Toolbar tb = findViewById(R.id.toolbar);
        setSupportActionBar(tb);

        final ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayShowHomeEnabled(false);
            ab.setDisplayHomeAsUpEnabled(false);
            ab.setDisplayShowCustomEnabled(true);
            ab.setDisplayShowTitleEnabled(true);
            ab.setTitle(R.string.app_name);
        }
    }

    public void checkUpdate() {
        Thread t = new Thread(new Runnable() {
            public void run() {
                UpdateChecker.checkNewVersion(QubeActivity.this);
            }
        });
        t.start();
    }

    private void setupStrictMode() {
        if (Config.debugStrictMode) {
            StrictMode.setThreadPolicy(
                    new StrictMode.ThreadPolicy.Builder().detectDiskReads().detectDiskWrites().detectNetwork()
                            .penaltyLog().build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects().penaltyLog()
                    .build());
        }
    }

    private void populateAttributesUI() {
        populateMachines(null);
        populateMachineType(null);
        populateCPUs(null);
        populateCPUNum();
        populateRAM();
        populateTcgBuffer();
        populateDisks();
        populateBiosType();
        populateBootDevices();
        populateNet();
        populateNetDevices(null);
        populateVGA();
        populateSoundcardConfig();
        populateUI();
        populateKeyboardLayout();
        populateMouse();
    }

    private void populateDisks() {

        //disks
        populateDiskAdapter(mHDA, FileType.HDA, true);
        populateDiskAdapter(mHDB, FileType.HDB, true);
        populateDiskAdapter(mHDC, FileType.HDC, true);
        populateDiskAdapter(mHDD, FileType.HDD, true);
        populateDiskAdapter(mSharedFolder, FileType.SHARED_DIR, false);

        //removables drives
        populateDiskAdapter(mCD, FileType.CDROM, false);
        populateDiskAdapter(mFDA, FileType.FDA, false);
        populateDiskAdapter(mFDB, FileType.FDB, false);

        //bios
        populateDiskAdapter(mBios, FileType.BIOS, false);
        populateDiskAdapter(mBiosCode, FileType.BIOS_CODE, false);
        populateDiskAdapter(mBiosVars, FileType.BIOS_VARS, false);

        //boot
        populateDiskAdapter(mKernel, FileType.KERNEL, false);
        populateDiskAdapter(mInitrd, FileType.INITRD, false);

    }

    public void onFirstLaunch() {
        autoDetectAndSaveRefreshRate();
        promptLicense(true);
    }

    // Detects the screen's own max refresh rate and saves it to settings
    // on first launch only
    private void autoDetectAndSaveRefreshRate() {
        if (QubeSettingsManager.hasRefreshRatePref(this)) {
            return; // already set (e.g. restored from backup), don't override
        }
        final int[] supportedHz = {30, 60, 75, 90, 120, 144, 165};
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                float maxRefreshRate = 60f;
                Display display = getDisplay();
                if (display != null) {
                    for (Display.Mode mode : display.getSupportedModes()) {
                        if (mode.getRefreshRate() > maxRefreshRate) {
                            maxRefreshRate = mode.getRefreshRate();
                        }
                    }
                }
                int chosen = supportedHz[0];
                for (int hz : supportedHz) {
                    if (hz <= maxRefreshRate + 0.5f) {
                        chosen = hz;
                    }
                }
                QubeSettingsManager.setRefreshRate(QubeActivity.this, chosen);
            }
        });
    }

    private void createMachine(String machineName) {
        notifyAction(MachineAction.CREATE_VM, machineName);
    }

    private void machineCreated() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                populateMachines(getMachine().getName());
                enableNonRemovableDeviceOptions(true);
                enableRemovableDeviceOptions(true);
                setArchOptions();
            }
        });
    }

    private void onDeleteMachine() {
        if (getMachine() == null) {
            ToastUtils.toastShort(this, getString(R.string.SelectAMachineFirst));
            return;
        }
        Thread t = new Thread(new Runnable() {
            public void run() {
                final String name = getMachine().getName();
                notifyAction(MachineAction.DELETE_VM, getMachine());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        disableListeners();
                        disableRemovableDiskListeners();
                        mMachine.setSelection(0);
                        notifyAction(MachineAction.LOAD_VM, null);
                        populateAttributesUI();
                        ToastUtils.toastShort(QubeActivity.this, getString(R.string.MachineDeleted) + ": " + name);
                        setupMiscOptions();
                        setupNonRemovableDiskListeners();
                        enableRemovableDiskListeners();
                    }
                });
            }
        });
        t.start();

    }

    private void promptLicense() {
        promptLicense(false);
    }

    private void promptLicense(boolean isFirstLaunch) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    QubeActivityCommon.promptLicense(QubeActivity.this,
                            Config.APP_NAME + " " + QubeApplication.getQubeVersionString()
                            + " " + "QEMU" + " " + QubeApplication.getQemuVersionString() ,
                            FileUtils.LoadFile(QubeActivity.this, "LICENSE", false),
                            isFirstLaunch ? QubeActivity.this::checkStoragePermission : null);
                } catch (IOException e) {

                    e.printStackTrace();
                }
            }
        });

    }

    public void exit() {
        if (MachineController.getInstance().isRunning()) {
            if (getMachine() != null && getMachine().getRenderer() == 1)
                QubeActivityCommon.promptStopVM(this, viewListener);
            else
                onStopButton(true);
        } else
            System.exit(0);
    }

    private void unlockRemovableDevices(boolean flag) {
        mCDenable.setEnabled(flag);
        mFDAenable.setEnabled(flag);
        mFDBenable.setEnabled(flag);
        mCDOptions.setEnabled(flag);
    }

    private void enableRemovableDeviceOptions(boolean flag) {
        unlockRemovableDevices(flag);
        enableRemovableDiskValues(flag);
    }

    // Fade helper for enable/disable animations
    private void fadeViewEnabledState(final View view, final boolean enabled, boolean animate,
            final boolean toggleEnabled, long fadeInMs, long fadeOutMs) {
        if (view == null) return;
        final float targetAlpha = enabled ? 1.0f : 0.4f;
        boolean alreadyCorrect = (!toggleEnabled || view.isEnabled() == enabled)
                && Math.abs(view.getAlpha() - targetAlpha) < 0.01f;
        if (!animate) {
            view.setAlpha(targetAlpha);
            if (toggleEnabled) view.setEnabled(enabled);
            return;
        }
        if (alreadyCorrect) return;
        if (!enabled) {
            // Fade out then disable
            view.animate().alpha(0.4f).setDuration(fadeOutMs)
                    .withEndAction(() -> {
                        if (toggleEnabled) view.setEnabled(false);
                    })
                    .start();
        } else {
            // Enable immediately, then fade in
            if (toggleEnabled) view.setEnabled(true);
            view.animate().alpha(1.0f).setDuration(fadeInMs).start();
        }
    }

    private void updateSoundCardEnabledState(boolean enabled) {
        fadeViewEnabledState(mSoundCard, enabled, true, true, 200, 250);
    }

    // Only enabled if a supported virtio device is selected
    private void updateVenusEnabledState() {
        if (!BuildConfig.USE_VENUS)
            return;
        if (mEnableVenus == null || mVGAConfig == null)
            return;
        boolean venusSupported = GraphicsCapabilities.supportsVenus(getSelectedVga());

        if (!venusSupported && mEnableVenus.isEnabled()) {
            // Turn off the switch first, then fade then disable it
            if (mEnableVenus.isChecked()) {
                mEnableVenus.setChecked(false);
            }
            mEnableVenus.postDelayed(() -> fadeViewEnabledState(mEnableVenus, false, true, true, 200, 250), 150);
        } else {
            fadeViewEnabledState(mEnableVenus, venusSupported, true, true, 200, 250);
        }
    }

    private void enableRemovableDiskValues(boolean flag) {
        setRemovableDriveRowEnabled(mCDStr, mCD, flag && mCDenable.isChecked(), false);
        setRemovableDriveRowEnabled(mFDAStr, mFDA, flag && mFDAenable.isChecked(), false);
        setRemovableDriveRowEnabled(mFDBStr, mFDB, flag && mFDBenable.isChecked(), false);
    }

    // Fades label and spinner
    private void setRemovableDriveRowEnabled(TextView label, Spinner spinner, boolean enabled, boolean animate) {
        if (label == null || spinner == null) {
            if (spinner != null) spinner.setEnabled(enabled);
            return;
        }

        // Label keeps its enabled flag untouched, spinner doesn't
        fadeViewEnabledState(label, enabled, animate, false, 180, 220);
        fadeViewEnabledState(spinner, enabled, animate, true, 180, 220);
    }

    private void enableNonRemovableDeviceOptions(boolean flag) {
        if (MachineController.getInstance().isRunning())
            flag = false;

        //ui
        mUI.setEnabled(flag);
        mKeyboard.setEnabled(Config.enableKeyboardLayoutOption && flag);
        mMouse.setEnabled(Config.enableMouseOption && flag);

        // Enable everything except removable devices
        mMachineType.setEnabled(flag);
        mCPU.setEnabled(flag);
        mCPUNum.setEnabled(flag);
        mRamSize.setEnabled(flag);
        mEnableKVM.setEnabled(flag && Config.enableKVM);
        mEnableMTTCG.setEnabled(flag && Config.enableMTTCG);
        mTcgBuffer.setEnabled(flag);

        //drives
        mHDA.setEnabled(flag);
        mHDAOptions.setEnabled(flag);
        mHDB.setEnabled(flag);
        mHDBOptions.setEnabled(flag);
        mHDC.setEnabled(flag);
        mHDCOptions.setEnabled(flag);
        mHDD.setEnabled(flag);
        mHDDOptions.setEnabled(flag);
        mSharedFolder.setEnabled(flag);

        //bios
        mBiosType.setEnabled(flag);
        mBios.setEnabled(flag);
        mBiosCode.setEnabled(flag);
        mBiosVars.setEnabled(flag);

        //boot
        mBootDevices.setEnabled(flag);
        mKernel.setEnabled(flag);
        mInitrd.setEnabled(flag);
        mAppend.setEnabled(flag);

        //graphics
        mVGAConfig.setEnabled(flag);

        //audio
        if (Config.enableQGESound && getMachine() != null
                && getMachine().getRenderer() != 1)
            mSoundCard.setEnabled(flag);
        else
            mSoundCard.setEnabled(false);

        //net
        mNetConfig.setEnabled(flag);
        mNetworkCard.setEnabled(flag && mNetConfig.getSelectedItemPosition() > 0);
        mDNS.setEnabled(flag && mNetConfig.getSelectedItemPosition() > 0);
        mHOSTFWD.setEnabled(flag && mNetConfig.getSelectedItemPosition() > 0);

        //advanced
        mDisableACPI.setEnabled(flag);
        mDisableHPET.setEnabled(flag);
        mDisableTSC.setEnabled(flag);
        mExtraParams.setEnabled(flag);
        mHighPrio.setEnabled(flag);

    }

    // Main event function
    // Retrives values from saved preferences
    private void onStartButton() {

        if (mMachine.getSelectedItemPosition() == 0 || getMachine() == null) {
            ToastUtils.toastShort(QubeActivity.this, getString(R.string.SelectOrCreateVirtualMachineFirst));
            return;
        }
        // focus out of edit texts to make sure they are applied to the db
        mStart.requestFocus();

        if (!validateFiles()) {
            return;
        }

        try {
            createMachineDir(MachineController.getInstance().getMachineSaveDir());
        } catch (Exception ex) {
            ToastUtils.toastLong(QubeActivity.this, getString(R.string.Error) + ": " + ex);
            return;
        }

        //XXX: make sure that bios files are installed in case we ran out of space in the last run
        FileInstaller.installFiles(QubeActivity.this, false);

        startVNC();
    }

    private void createMachineDir(String dir) throws Exception {
        File destDir = new File(dir);
        if (!destDir.exists()) {
            if (!destDir.mkdirs())
                throw new Exception(getString(R.string.failToCreateMachineDirError));
        }
    }

    public void startVNC() {
        if (getMachine().getRenderer() == 0) {
            startQGE();
        } else {
            startExternalVNC();
        }
    }

    // Start QGE display wrapper if selected
    public void startQGE() {
        Intent intent = new Intent(QubeActivity.this, QubeQGEActivity.class);
        startActivityForResult(intent, Config.QGE_REQUEST_CODE);
    }

    // Start VNC host if selected
    public void startExternalVNC() {
        if (QubeSettingsManager.getEnableExternalVNC(this)) {
            // VNC external connections
            QubeActivityCommon.promptVNCServer(this,
                    getString(R.string.ExternalVNCEnabledWarning), viewListener);
        } else {
            notifyAction(MachineAction.START_VM, null);
        }
    }

    private boolean validateFiles() {
        return FileUtils.fileValid(getMachine().getHdaImagePath())
                && FileUtils.fileValid(getMachine().getHdbImagePath())
                && FileUtils.fileValid(getMachine().getHdcImagePath())
                && FileUtils.fileValid(getMachine().getHddImagePath())
                && FileUtils.fileValid(getMachine().getFdaImagePath())
                && FileUtils.fileValid(getMachine().getFdbImagePath())
                && FileUtils.fileValid(getMachine().getCdImagePath())
                && FileUtils.fileValid(getMachine().getKernel())
                && FileUtils.fileValid(getMachine().getInitRd());
    }

    private void onStopButton(boolean exitApp) {
        KeyboardUtils.hideKeyboard(this, mScrollView);
        if (MachineController.getInstance().isRunning()) {
            if (getMachine() != null && getMachine().getRenderer() == 1)
                QubeActivityCommon.promptStopVM(this, viewListener);
            else {
                QubeQGEActivity.pendingStop = true;
                startQGE();
            }
        } else {
            ToastUtils.toastShort(QubeActivity.this, getString(R.string.vmNotRunning));
        }
    }

    private void onRestartButton() {
        if (!MachineController.getInstance().isRunning()) {
            ToastUtils.toastShort(QubeActivity.this, getString(R.string.VMNotRunning));
            return;
        }
        QubeActivityCommon.promptResetVM(this, viewListener);
    }

    public void toggleSectionVisibility(View view) {
        if (view.getVisibility() == View.VISIBLE) {
            view.setVisibility(View.GONE);
        } else if (view.getVisibility() == View.GONE || view.getVisibility() == View.INVISIBLE) {
            view.setVisibility(View.VISIBLE);
        }
    }

    public void setupWidgets() {
        setupSections();
        mScrollView = findViewById(R.id.scroll_view);
        mStatus = findViewById(R.id.statusVal);
        mStatus.setImageResource(R.drawable.power_settings_new_24px);
        mStatusText = findViewById(R.id.statusStr);

        mStart = findViewById(R.id.startvm);
        mStop = findViewById(R.id.stopvm);
        mRestart = findViewById(R.id.restartvm);

        //Machine
        mMachine = findViewById(R.id.machineval);
        if (MachineController.getInstance().isRunning())
            mMachine.setEnabled(false);

        //UI
        mKeyboard = findViewById(R.id.keyboardval);
        mMouse = findViewById(R.id.mouseval);

        //cpu/board
        mCPU = findViewById(R.id.cpuval);
        mMachineType = findViewById(R.id.machinetypeval);
        mCPUNum = findViewById(R.id.cpunumval);
        mUI = findViewById(R.id.uival);
        mRamSize = findViewById(R.id.rammemval);
        mEnableKVM = findViewById(R.id.enablekvmval);
        mEnableMTTCG = findViewById(R.id.enablemttcgval);
        mTcgBuffer = findViewById(R.id.tcgbufferval);
        mTcgBufferDisplay = findViewById(R.id.tcgBufferValueDisplay);
        mDisableACPI = findViewById(R.id.acpival);
        mDisableHPET = findViewById(R.id.hpetval);
        mDisableTSC = findViewById(R.id.tscval);

        //disks
        mHDA = findViewById(R.id.hdaimgval);
        mHDAOptions = findViewById(R.id.hdaoptions);
        mHDB = findViewById(R.id.hdbimgval);
        mHDBOptions = findViewById(R.id.hdboptions);
        mHDC = findViewById(R.id.hdcimgval);
        mHDCOptions = findViewById(R.id.hdcoptions);
        mHDD = findViewById(R.id.hddimgval);
        mHDDOptions = findViewById(R.id.hddoptions);

        LinearLayout sharedFolderLayout = findViewById(R.id.sharedfolderl);
        if (!Config.enableSharedFolder)
            sharedFolderLayout.setVisibility(View.GONE);
        mSharedFolder = findViewById(R.id.sharedfolderval);
        mSharedFolderOptions = findViewById(R.id.sharedfolderoptions);

        //Removable storage
        mCD = findViewById(R.id.cdromimgval);
        mFDA = findViewById(R.id.floppyimgval);
        mFDB = findViewById(R.id.floppybimgval);
        mCDOptions = findViewById(R.id.cdromoptions);
        if (!Config.enableEmulatedFloppy) {
            LinearLayout mFDALayout = findViewById(R.id.floppyimgl);
            mFDALayout.setVisibility(View.GONE);
            LinearLayout mFDBLayout = findViewById(R.id.floppybimgl);
            mFDBLayout.setVisibility(View.GONE);
        }
        mCDenable = findViewById(R.id.cdromimgcheck);
        mFDAenable = findViewById(R.id.floppyimgcheck);
        mFDBenable = findViewById(R.id.floppybimgcheck);
        mCDStr = findViewById(R.id.cdromimgstr);
        mFDAStr = findViewById(R.id.floppyimgstr);
        mFDBStr = findViewById(R.id.floppybimgstr);

        //bios
        mBiosType = findViewById(R.id.biostypeval);
        mBios = findViewById(R.id.biosval);
        mBiosCode = findViewById(R.id.bioscodeval);
        mBiosVars = findViewById(R.id.biosvarsval);

        //boot
        mBootDevices = findViewById(R.id.bootfromval);
        mKernel = findViewById(R.id.kernelval);
        mInitrd = findViewById(R.id.initrdval);
        mAppend = findViewById(R.id.appendval);

        //display
        mVGAConfig = findViewById(R.id.vgacfgval);
        mVGAConfigInfo = findViewById(R.id.vgacfgInfo);
        mUIInfo = findViewById(R.id.uiInfo);

        mEnableVenus = findViewById(R.id.venusaccelval);
        mEnableVenusInfo = findViewById(R.id.venusaccelInfo);
        View mVenusRow = findViewById(R.id.venusaccell);

        // if the Venus support was not compiled into this build, hide the whole option
        if (!BuildConfig.USE_VENUS) {
            if (mVenusRow != null)
                mVenusRow.setVisibility(View.GONE);
        } else {
        // Starts disabled, called once a virtio GL device is selected
        mEnableVenus.setEnabled(false);
        mEnableVenus.setAlpha(0.4f);
        mEnableVenus.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (getMachine() == null)
                    return;
                notifyFieldChange(MachineProperty.ENABLE_VENUS, isChecked);
            }
        });

        if (mEnableVenusInfo != null) {
            mEnableVenusInfo.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    CharSequence message = Html.fromHtml(getString(R.string.venusInfoMessage), Html.FROM_HTML_MODE_LEGACY);
                    new MaterialAlertDialogBuilder(QubeActivity.this)
                            .setTitle(R.string.venusInfoTitle)
                            .setIcon(R.drawable.desktop_windows_24px)
                            .setMessage(message)
                            .setCancelable(false)
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                }
            });
        }
        }

        //share server
        mHostShareServer = findViewById(R.id.hostsharefolderval);
        mHostShareServerInfo = findViewById(R.id.hostsharefolderInfo);
        mHostShareServerRow = findViewById(R.id.hostsharefolderl);

        if (mHostShareServer != null) {
            mHostShareServer.setChecked(SharedFolderServer.isRunning());
            mHostShareServer.setOnCheckedChangeListener(mHostShareServerCheckedChangeListener);
        }

        if (mHostShareServerInfo != null) {
            mHostShareServerInfo.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    showHostShareServerInfoDialog();
                }
            });
        }

        //sound
        mSoundCard = findViewById(R.id.soundcfgval);

        //network
        mNetConfig = findViewById(R.id.netcfgval);
        mNetworkCard = findViewById(R.id.netDevicesVal);
        mDNS = findViewById(R.id.dnsval);
        mHOSTFWD = findViewById(R.id.hostfwdval);

        // advanced
        mExtraParams = findViewById(R.id.extraparamsval);

        mHighPrio = findViewById(R.id.highprioval);

        disableFeatures();
        enableRemovableDeviceOptions(false);
        enableNonRemovableDeviceOptions(false);
    }

    private void updateHostShareServerVisibility(boolean isVNC) {
        if (mHostShareServerRow != null) {
            mHostShareServerRow.setVisibility(isVNC ? View.VISIBLE : View.GONE);
        }
    }

     // The shared folder server is only meant to run alongside VNC
     // If the user switches to QGE while it's running, stop it and reset the switch
     // This doesn't affect QubeQGEActivity because it has its own mechanism
    private void stopHostShareServerIfRunning() {
        if (SharedFolderServer.isRunning()) {
            SharedFolderServer.stopServer();
        }
        if (mHostShareServer != null && mHostShareServer.isChecked()) {
            mHostShareServer.setOnCheckedChangeListener(null);
            mHostShareServer.setChecked(false);
            mHostShareServer.setOnCheckedChangeListener(mHostShareServerCheckedChangeListener);
        }
    }

    private void showHostShareServerInfoDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_share_folder, null);
        TextView tvDescription = view.findViewById(R.id.tvShareFolderDescription);
        View layoutRunningInfo = view.findViewById(R.id.layoutShareFolderRunningInfo);
        TextView tvLink = view.findViewById(R.id.tvShareFolderLink);
        ImageButton btnCopyLink = view.findViewById(R.id.btnCopyShareFolderLink);

        boolean running = SharedFolderServer.isRunning();

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setView(view)
                .setCancelable(false)
                .setNegativeButton(R.string.close, null);

        if (running) {
            builder.setTitle(R.string.share_folder_running_title);
            tvDescription.setText(R.string.share_folder_running_desc);
            layoutRunningInfo.setVisibility(View.VISIBLE);
            tvLink.setText(SharedFolderServer.getHostUrl());
            btnCopyLink.setOnClickListener(v -> ClipboardUtils.copyToClipboard(
                    this, getString(R.string.copy_link), SharedFolderServer.getHostUrl()));
        } else {
            builder.setTitle(R.string.share_folder_stopped_title);
            tvDescription.setText(R.string.share_folder_stopped_desc);
            layoutRunningInfo.setVisibility(View.GONE);
        }

        builder.show();
    }

    private void disableFeatures() {
        LinearLayout mAudioSectionLayout = findViewById(R.id.audiosectionl);
        if (!Config.enableQGESound) {
            mAudioSectionLayout.setVisibility(View.GONE);
        }

        LinearLayout mDisableTSCLayout = findViewById(R.id.tscl);
        LinearLayout mDisableACPILayout = findViewById(R.id.acpil);
        LinearLayout mDisableHPETLayout = findViewById(R.id.hpetl);
        LinearLayout mEnableKVMLayout = findViewById(R.id.kvml);

        if (QubeApplication.arch != Config.Arch.x86 && QubeApplication.arch != Config.Arch.x86_64) {
            mDisableTSCLayout.setVisibility(View.GONE);
            mDisableACPILayout.setVisibility(View.GONE);
            mDisableHPETLayout.setVisibility(View.GONE);
        }
        if (QubeApplication.arch != Config.Arch.x86 && QubeApplication.arch != Config.Arch.x86_64
                && QubeApplication.arch != Config.Arch.arm && QubeApplication.arch != Config.Arch.arm64) {
            mEnableKVMLayout.setVisibility(View.GONE);
        }
    }

    private void setupSections() {

        if (Config.collapseSections) {
            mCPUSectionDetails = findViewById(R.id.cpusectionDetails);
            mCPUSectionDetails.setVisibility(View.GONE);
            mCPUSectionSummary = findViewById(R.id.cpusectionsummaryStr);
            LinearLayout mCPUSectionHeader = findViewById(R.id.cpusectionheaderl);
            mCPUSectionHeader.setOnClickListener(new OnClickListener() {
                public void onClick(View view) {
                    disableListeners();
                    disableRemovableDiskListeners();
                    toggleSectionVisibility(mCPUSectionDetails);
                    enableListenersDelayed();
                }
            });

            mStorageSectionDetails = findViewById(R.id.storagesectionDetails);
            mStorageSectionDetails.setVisibility(View.GONE);
            mStorageSectionSummary = findViewById(R.id.storagesectionsummaryStr);
            LinearLayout mStorageSectionHeader = findViewById(R.id.storageheaderl);
            mStorageSectionHeader.setOnClickListener(new OnClickListener() {
                public void onClick(View view) {
                    disableListeners();
                    disableRemovableDiskListeners();
                    toggleSectionVisibility(mStorageSectionDetails);
                    enableListenersDelayed();
                }
            });

            mUserInterfaceSectionDetails = findViewById(R.id.userInterfaceDetails);
            mUserInterfaceSectionDetails.setVisibility(View.GONE);
            mUISectionSummary = findViewById(R.id.uisectionsummaryStr);
            LinearLayout mUserInterfaceSectionHeader = findViewById(R.id.userinterfaceheaderl);
            mUserInterfaceSectionHeader.setOnClickListener(new OnClickListener() {
                public void onClick(View view) {
                    disableListeners();
                    disableRemovableDiskListeners();
                    toggleSectionVisibility(mUserInterfaceSectionDetails);
                    enableListenersDelayed();
                }
            });


            mRemovableStorageSectionDetails = findViewById(R.id.removableStoragesectionDetails);
            mRemovableStorageSectionDetails.setVisibility(View.GONE);
            mRemovableStorageSectionSummary = findViewById(R.id.removablesectionsummaryStr);
            LinearLayout mRemovableStorageSectionHeader = findViewById(R.id.removablestorageheaderl);
            mRemovableStorageSectionHeader.setOnClickListener(new OnClickListener() {
                public void onClick(View view) {
                    disableListeners();
                    disableRemovableDiskListeners();
                    toggleSectionVisibility(mRemovableStorageSectionDetails);
                    enableListenersDelayed();
                }
            });

            mBiosSectionDetails = findViewById(R.id.biossectionDetails);
            View biosSection = findViewById(R.id.biosSectionl);
            // We hide UEFI/Bios on PowerPC because it's limited to handle them
            if (QubeApplication.arch == Config.Arch.ppc || QubeApplication.arch == Config.Arch.ppc64)
                biosSection.setVisibility(View.GONE);
            mBiosSectionDetails.setVisibility(View.GONE);
            mBiosSectionSummary = findViewById(R.id.biossectionsummaryStr);
            View mBiosSectionHeader = findViewById(R.id.biosheaderl);
            mBiosSectionHeader.setOnClickListener(new OnClickListener() {
                public void onClick(View view) {
                    disableListeners();
                    disableRemovableDiskListeners();
                    toggleSectionVisibility(mBiosSectionDetails);
                    enableListenersDelayed();
                }
            });

            mGraphicsSectionDetails = findViewById(R.id.graphicssectionDetails);
            mGraphicsSectionDetails.setVisibility(View.GONE);
            mGraphicsSectionSummary = findViewById(R.id.graphicssectionsummaryStr);
            LinearLayout mGraphicsSectionHeader = findViewById(R.id.graphicsheaderl);
            mGraphicsSectionHeader.setOnClickListener(new OnClickListener() {
                public void onClick(View view) {
                    disableListeners();
                    disableRemovableDiskListeners();
                    toggleSectionVisibility(mGraphicsSectionDetails);
                    enableListenersDelayed();
                }
            });
            mAudioSectionDetails = findViewById(R.id.audiosectionDetails);
            mAudioSectionDetails.setVisibility(View.GONE);
            mAudioSectionSummary = findViewById(R.id.audiosectionsummaryStr);
            LinearLayout mAudioSectionHeader = findViewById(R.id.audioheaderl);
            mAudioSectionHeader.setOnClickListener(new OnClickListener() {
                public void onClick(View view) {
                    disableListeners();
                    disableRemovableDiskListeners();
                    toggleSectionVisibility(mAudioSectionDetails);
                    enableListenersDelayed();
                }
            });

            mNetworkSectionDetails = findViewById(R.id.networksectionDetails);
            mNetworkSectionDetails.setVisibility(View.GONE);
            mNetworkSectionSummary = findViewById(R.id.networksectionsummaryStr);
            View mNetworkSectionHeader = findViewById(R.id.networkheaderl);
            mNetworkSectionHeader.setOnClickListener(new OnClickListener() {
                public void onClick(View view) {
                    disableListeners();
                    disableRemovableDiskListeners();
                    toggleSectionVisibility(mNetworkSectionDetails);
                    enableListenersDelayed();
                }
            });

            mBootSectionDetails = findViewById(R.id.bootsectionDetails);
            mBootSectionDetails.setVisibility(View.GONE);
            mBootSectionSummary = findViewById(R.id.bootsectionsummaryStr);
            View mBootSectionHeader = findViewById(R.id.bootheaderl);
            mBootSectionHeader.setOnClickListener(new OnClickListener() {
                public void onClick(View view) {
                    disableListeners();
                    disableRemovableDiskListeners();
                    toggleSectionVisibility(mBootSectionDetails);
                    enableListenersDelayed();
                }
            });

            mAdvancedSectionDetails = findViewById(R.id.advancedSectionDetails);
            mAdvancedSectionDetails.setVisibility(View.GONE);
            mAdvancedSectionSummary = findViewById(R.id.advancedsectionsummaryStr);
            LinearLayout mAdvancedSectionHeader = findViewById(R.id.advancedheaderl);
            mAdvancedSectionHeader.setOnClickListener(new OnClickListener() {
                public void onClick(View view) {
                    disableListeners();
                    disableRemovableDiskListeners();
                    toggleSectionVisibility(mAdvancedSectionDetails);
                    enableListenersDelayed();
                }
            });
        }
    }

    private void enableListenersDelayed() {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                setupMiscOptions();
                setupNonRemovableDiskListeners();
                enableRemovableDiskListeners();
            }
        }, 500);
    }

    public void updateUISummary(boolean clear) {
        if (clear || getMachine() == null || mMachine.getSelectedItemPosition() < 2)
            mUISectionSummary.setText("");
        else {
            boolean isVNC = getMachine().getRenderer() == 1;
            String text = getString(R.string.display) + ": " + (isVNC ? "VNC" : "QGE");
            if (isVNC) {
                text += ", " + getString(R.string.server);
                text += ": " + NetworkUtils.getVNCAddress(this) + ":" + Config.defaultVNCPort;
            }
            if (getMachine().getKeyboard() != null) {
                text += ", " + getString(R.string.keyboard) + ": " + getMachine().getKeyboard();
            }
            if (getMachine().getMouse() != null) {
                text += ", " + getString(R.string.mouse) + ": " + getMachine().getMouse();
            }
            mUISectionSummary.setText(text);
        }
    }

    private Machine getMachine() {
        return MachineController.getInstance().getMachine();
    }

    public void updateCPUSummary(boolean clear) {
        if (clear || getMachine() == null || mMachine.getSelectedItemPosition() < 2)
            mCPUSectionSummary.setText("");
        else {
            String text = getString(R.string.summary_machine_type) + ": " + getMachine().getMachineType()
                    + ", " + getString(R.string.summary_cpu) + ": " + getMachine().getCpu()
                    + ", " + getMachine().getCpuNum() + " " + (getMachine().getCpuNum() > 1 ? getString(R.string.summary_cpu_plural) : getString(R.string.summary_cpu))
                    + ", " + getMachine().getMemory() + " " + getString(R.string.summary_mb);
            if (mEnableMTTCG.isChecked())
                text = appendOption(getString(R.string.enableMTTCG), text);
            if (mEnableKVM.isChecked())
                text = appendOption(getString(R.string.EnableKVM), text);
            if (mDisableACPI.isChecked())
                text = appendOption(getString(R.string.disable_acpi_label), text);
            if (mDisableHPET.isChecked())
                text = appendOption(getString(R.string.disable_hpet_label), text);
            if (mDisableTSC.isChecked())
                text = appendOption(getString(R.string.disable_tsc_label), text);
            mCPUSectionSummary.setText(text);
        }
    }

    public void updateStorageSummary(boolean clear) {
        if (clear || getMachine() == null || mMachine.getSelectedItemPosition() < 2)
            mStorageSectionSummary.setText("");
        else {
            String text = null;
            text = appendDriveFilename(getMachine().getHdaImagePath(), text, getString(R.string.summary_hda), false);
            text = appendDriveFilename(getMachine().getHdbImagePath(), text, getString(R.string.summary_hdb), false);
            text = appendDriveFilename(getMachine().getHdcImagePath(), text, getString(R.string.summary_hdc), false);
            text = appendDriveFilename(getMachine().getHddImagePath(), text, getString(R.string.summary_hdd), false);

            if (Config.enableSharedFolder)
                text = appendDriveFilename(getMachine().getSharedFolderPath(), text,
                        getString(R.string.SharedFolder), false);

            if (text == null || text.equals("'"))
                text = getString(R.string.summary_none);
            mStorageSectionSummary.setText(text);
        }
    }

    public void updateRemovableStorageSummary(boolean clear) {
        if (clear || getMachine() == null || mMachine.getSelectedItemPosition() < 2)
            mRemovableStorageSectionSummary.setText("");
        else {
            String text = null;

            text = appendDriveFilename(getMachine().getCdImagePath(), text, getString(R.string.summary_cdrom), true);
            text = appendDriveFilename(getMachine().getFdaImagePath(), text, getString(R.string.summary_fda), true);
            text = appendDriveFilename(getMachine().getFdbImagePath(), text, getString(R.string.summary_fdb), true);


            if (text == null || text.equals(""))
                text = getString(R.string.summary_none);

            mRemovableStorageSectionSummary.setText(text);
        }
    }

    public void updateBootSummary(boolean clear) {
        if (clear || getMachine() == null || mMachine.getSelectedItemPosition() < 2)
            mBootSectionSummary.setText("");
        else {
            String text = getString(R.string.summary_boot_from) + ": " + getMachine().getBootDevice();
            text = appendDriveFilename(getMachine().getKernel(), text, getString(R.string.summary_kernel), false);
            text = appendDriveFilename(getMachine().getInitRd(), text, getString(R.string.summary_initrd), false);
            text = appendDriveFilename(getMachine().getAppend(), text, getString(R.string.summary_append), false);
            mBootSectionSummary.setText(text);
        }
    }

    public void updateBiosSummary(boolean clear) {
        if (clear || getMachine() == null || mMachine.getSelectedItemPosition() < 2) {
            mBiosSectionSummary.setText("");
            return;
        }

        String biosType = getMachine().getBiosType();
        if (biosType == null || biosType.trim().isEmpty())
            biosType = "Default";
        String text = getString(R.string.bios_type_label) + ": " + biosType;
        if ("UEFI".equalsIgnoreCase(biosType)) {
            text = appendDriveFilename(getMachine().getBiosCode(), text,
                    getString(R.string.bios_code_label), false);
            text = appendDriveFilename(getMachine().getBiosVars(), text,
                    getString(R.string.bios_vars_label), false);
        } else {
            text = appendDriveFilename(getMachine().getBios(), text,
                    getString(R.string.bios_label), false);
        }
        mBiosSectionSummary.setText(text);
    }

    private String appendDriveFilename(String driveFile, String text, String drive, boolean allowEmptyDrive) {

        String file = null;
        if (driveFile != null) {
            if ((driveFile.equals("") || driveFile.equals("None")) && allowEmptyDrive) {
                file = drive + ": Empty";
            } else if (!driveFile.equals("") && !driveFile.equals("None"))
                file = drive + ": " + FileUtils.getFilenameFromPath(driveFile);
        }
        if (text == null && file != null)
            text = file;
        else if (file != null)
            text += (", " + file);
        return text;
    }

    public void updateGraphicsSummary(boolean clear) {
        if (clear || getMachine() == null || mMachine.getSelectedItemPosition() < 2)
            mGraphicsSectionSummary.setText("");
        else {
            String text = getString(R.string.summary_video_card) + ": " + getMachine().getVga();
            mGraphicsSectionSummary.setText(text);
        }
    }

    public void updateAudioSummary(boolean clear) {
        if (clear || getMachine() == null
                || mMachine.getSelectedItemPosition() < 2)
            mAudioSectionSummary.setText("");
        else {
            String soundCard = getMachine().getSoundCard();
            String text = getString(R.string.AudioCard) + ": " + (soundCard != null ? soundCard : getString(R.string.summary_none));
            mAudioSectionSummary.setText(text);
        }
    }

    public void updateNetworkSummary(boolean clear) {
        if (clear || getMachine() == null
                || mMachine.getSelectedItemPosition() < 2)
            mNetworkSectionSummary.setText("");
        else {
            String netCfg = getMachine().getNetwork();
            String text = getString(R.string.Network) + ": " + (netCfg != null ? netCfg : getString(R.string.summary_none));
            if (netCfg != null && !netCfg.equals("None")) {
                String nicCard = getMachine().getNetworkCard();
                text += ", " + getString(R.string.NicCard) + ": " + (nicCard != null ? nicCard : getString(R.string.summary_none));
                text += ", " + getString(R.string.DNSServer) + ": " + mDNS.getText();
                String hostFWD = getMachine().getHostFwd();
                if (hostFWD != null && !hostFWD.equals(""))
                    text += ", " + getString(R.string.HostForward) + ": " + hostFWD;
            }
            mNetworkSectionSummary.setText(text);
        }
    }

    public void updateAdvancedSummary(boolean clear) {
        if (clear || getMachine() == null || mMachine.getSelectedItemPosition() < 2)
            mAdvancedSectionSummary.setText("");
        else {
            String text = null;
            if (getMachine().getExtraParams() != null
                    && !getMachine().getExtraParams().equals(""))
                text = getString(R.string.ExtraParams) + ": " + getMachine().getExtraParams();
            if (mHighPrio.isChecked())
                text = appendOption(getString(R.string.HighPriority), text);
            mAdvancedSectionSummary.setText(text != null ? text : "");
        }
    }

    private String appendOption(String option, String text) {

        if (text == null && option != null)
            text = option;
        else if (option != null)
            text += (", " + option);
        return text;
    }

    private void triggerUpdateSpinner(final Spinner spinner) {

        final int position = (int) spinner.getSelectedItemId();
        spinner.setSelection(0);

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                spinner.setSelection(position);
            }
        }, 100);
    }

    private void loadMachine() {

        setUserPressed(false);
        if (getMachine() == null) {
            return;
        }
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            public void run() {
                loadMachineUI();
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        postLoadMachineUI();
                    }
                }, 1000);
                setCPUOptions();
                getMachine().addObserver(QubeActivity.this);
            }
        });
    }

    private void postLoadMachineUI() {

        mFDAenable.setChecked(getMachine().getFdaImagePath() != null);
        mFDBenable.setChecked(getMachine().getFdbImagePath() != null);
        mCDenable.setChecked(getMachine().getCdImagePath() != null);

        changeStatus(MachineController.getInstance().getCurrStatus());
        enableNonRemovableDeviceOptions(true);
        enableRemovableDeviceOptions(true);
        setUserPressed(true);
        machineLoaded = false;
        mMachine.setEnabled(!MachineController.getInstance().isRunning());
    }

    private void loadMachineUI() {
        populateMachineType(getMachine().getMachineType());
        populateCPUs(getMachine().getCpu());
        populateNetDevices(getMachine().getNetworkCard());
        SpinnerAdapter.setDiskAdapterValue(mCPUNum, getMachine().getCpuNum() + "");
        SpinnerAdapter.setDiskAdapterValue(mRamSize, getMachine().getMemory() + "");
        seMachineDriveValue(FileType.KERNEL, getMachine().getKernel());
        seMachineDriveValue(FileType.INITRD, getMachine().getInitRd());
        String biosType = getMachine().getBiosType();
        if (biosType == null || biosType.trim().isEmpty())
            biosType = "Default";
        SpinnerAdapter.setDiskAdapterValue(mBiosType, biosType);
        populateDiskAdapter(mBios, FileType.BIOS, false, getMachine().getBios());
        if ("UEFI".equalsIgnoreCase(biosType))
            ensureDefaultUefiFirmware();
        populateDiskAdapter(mBiosCode, FileType.BIOS_CODE, false, getMachine().getBiosCode());
        populateDiskAdapter(mBiosVars, FileType.BIOS_VARS, false, getMachine().getBiosVars());
        updateBiosTypeVisibility(biosType);
        if (getMachine().getAppend() != null)
            mAppend.setText(getMachine().getAppend());
        else
            mAppend.setText("");

        if (getMachine().getHostFwd() != null)
            mHOSTFWD.setText(getMachine().getHostFwd());
        else
            mHOSTFWD.setText("");

        if (getMachine().getExtraParams() != null)
            mExtraParams.setText(getMachine().getExtraParams());
        else
            mExtraParams.setText("");

        if (getMachine().getDns() != null)
            mDNS.setText(getMachine().getDns());
        else
            mDNS.setText(Config.defaultDNSServer);

        // CDROM
        seMachineDriveValue(FileType.CDROM, getMachine().getCdImagePath());

        // Floppy
        seMachineDriveValue(FileType.FDA, getMachine().getFdaImagePath());
        seMachineDriveValue(FileType.FDB, getMachine().getFdbImagePath());

        // HDD
        seMachineDriveValue(FileType.HDA, getMachine().getHdaImagePath());
        seMachineDriveValue(FileType.HDB, getMachine().getHdbImagePath());
        seMachineDriveValue(FileType.HDC, getMachine().getHdcImagePath());
        seMachineDriveValue(FileType.HDD, getMachine().getHddImagePath());

        //sharedfolder
        seMachineDriveValue(FileType.SHARED_DIR, getMachine().getSharedFolderPath());

        // Advanced
        SpinnerAdapter.setDiskAdapterValue(mBootDevices, getMachine().getBootDevice());
        SpinnerAdapter.setDiskAdapterValue(mNetConfig, getMachine().getNetwork());
        SpinnerAdapter.setDiskAdapterValue(mVGAConfig, getMachine().getVga());
        SpinnerAdapter.setDiskAdapterValue(mSoundCard, getMachine().getSoundCard());
        SpinnerAdapter.setDiskAdapterValue(mUI, getMachine().getRenderer() == 1 ? "VNC" : "QGE");
        updateHostShareServerVisibility(getMachine().getRenderer() == 1);
        SpinnerAdapter.setDiskAdapterValue(mMouse, getMachine().getMouse());
        SpinnerAdapter.setDiskAdapterValue(mKeyboard, getMachine().getKeyboard());

        // motherboard settings
        mDisableACPI.setChecked(getMachine().getDisableAcpi() == 1);
        mDisableHPET.setChecked(getMachine().getDisableHPET() == 1);
        if (QubeApplication.arch == Config.Arch.x86 || QubeApplication.arch == Config.Arch.x86_64)
            mDisableTSC.setChecked(getMachine().getDisableTSC() == 1);
        mEnableKVM.setChecked(getMachine().getEnableKVM() == 1);
        mEnableMTTCG.setChecked(getMachine().getEnableMTTCG() == 1);
        int tcgBuffer = getMachine().getTcgBuffer();
        if (tcgBuffer < TCG_MIN) tcgBuffer = TCG_MIN;
        if (tcgBuffer > TCG_MAX) tcgBuffer = TCG_MAX;
        mTcgBuffer.setValue(tcgBuffer);
        mTcgBufferDisplay.setText(tcgBuffer + "M");
        mHighPrio.setChecked(getMachine().getPrio() == 1);
        if (BuildConfig.USE_VENUS)
            mEnableVenus.setChecked(getMachine().getEnableVenus() == 1);

        enableNonRemovableDeviceOptions(true);
        enableRemovableDeviceOptions(!MachineController.getInstance().isRunning());

        if (Config.enableQGESound) {
            updateSoundCardEnabledState(getMachine().getRenderer() != 1);
        } else
            mSoundCard.setEnabled(false);

        // updateVenusEnabledState must run AFTER the spinner's post{} completes
        mVGAConfig.post(() -> updateVenusEnabledState());

        mMachine.setEnabled(false);
    }

    private synchronized void updateSummary() {
        updateUISummary(false);
        updateCPUSummary(false);
        updateStorageSummary(false);
        updateRemovableStorageSummary(false);
        updateBiosSummary(false);
        updateGraphicsSummary(false);
        updateAudioSummary(false);
        updateNetworkSummary(false);
        updateBootSummary(false);
        updateAdvancedSummary(false);
    }

    public void promptMachineName(final Activity activity) {
        final AlertDialog alertDialog;
        alertDialog = new MaterialAlertDialogBuilder(activity).create();
        alertDialog.setTitle(getString(R.string.NewMachineName));
        alertDialog.setIcon(R.drawable.developer_board_24px);
        final EditText vmNameTextView = new EditText(activity);
        vmNameTextView.setPadding(20, 20, 20, 20);
        vmNameTextView.setEnabled(true);
        vmNameTextView.setVisibility(View.VISIBLE);
        vmNameTextView.setSingleLine();
        alertDialog.setView(vmNameTextView);
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.Create), (DialogInterface.OnClickListener) null);

        alertDialog.show();

        Button button = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (vmNameTextView.getText().toString().trim().equals(""))
                    ToastUtils.toastShort(activity, getString(R.string.MachineNameCannotBeEmpty));
                else {
                    createMachine(vmNameTextView.getText().toString());
                    alertDialog.dismiss();
                }
            }
        });
        alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(vmNameTextView.getWindowToken(), 0);
            }
        });
    }

    public void promptImageName(final Activity activity, final FileType fileType) {

        final AlertDialog alertDialog;
        alertDialog = new MaterialAlertDialogBuilder(activity).create();
        alertDialog.setTitle(getString(R.string.ImageName));
        alertDialog.setIcon(R.drawable.hard_drive_24px);

        LinearLayout mLayout = new LinearLayout(this);
        mLayout.setPadding(20, 20, 20, 20);
        mLayout.setOrientation(LinearLayout.VERTICAL);

        final EditText imageNameView = new EditText(activity);
        imageNameView.setEnabled(true);
        imageNameView.setVisibility(View.VISIBLE);
        imageNameView.setSingleLine();
        LinearLayout.LayoutParams imageNameViewParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        mLayout.addView(imageNameView, imageNameViewParams);

        final Spinner size = new Spinner(this);
        LinearLayout.LayoutParams spinnerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        String[] arraySpinner = getResources().getStringArray(R.array.diskSizeOptions);

        ArrayAdapter<?> sizeAdapter = new ArrayAdapter<Object>(this, R.layout.custom_spinner_item, arraySpinner);
        sizeAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        size.setAdapter(sizeAdapter);
        mLayout.addView(size, spinnerParams);

        alertDialog.setView(mLayout);

        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.Create), (DialogInterface.OnClickListener) null);
        alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.ChangeDirectory), (DialogInterface.OnClickListener) null);

        alertDialog.show();

        Button positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        positiveButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if (QubeSettingsManager.getImagesDir(QubeActivity.this) == null) {
                    changeImagesDir();
                    return;
                }

                int sizeSel = size.getSelectedItemPosition();
                String templateImage = "hd1g.qcow2";
                if (sizeSel == 0) {
                    templateImage = "hd1g.qcow2";
                } else if (sizeSel == 1) {
                    templateImage = "hd2g.qcow2";
                } else if (sizeSel == 2) {
                    templateImage = "hd4g.qcow2";
                } else if (sizeSel == 3) {
                    templateImage = "hd10g.qcow2";
                } else if (sizeSel == 4) {
                    templateImage = "hd16g.qcow2";
                } else if (sizeSel == 5) {
                    templateImage = "hd32g.qcow2";
                } else if (sizeSel == 6) {
                    templateImage = "hd48g.qcow2";
                } else if (sizeSel == 7) {
                    templateImage = "hd64g.qcow2";
                } else if (sizeSel == 8) {
                    templateImage = "hd128g.qcow2";
                } else if (sizeSel == 9) {
                    templateImage = "hd256g.qcow2";
                }

                String image = imageNameView.getText().toString();
                if (image.trim().equals(""))
                    ToastUtils.toastShort(activity, getString(R.string.ImageFilenameCannotBeEmpty));
                else {
                    if (!image.endsWith(".qcow2")) {
                        image += ".qcow2";
                    }
                    String filePath = FileUtils.createImgFromTemplate(QubeActivity.this, templateImage, image, fileType);
                    if (filePath!=null) {
                        updateDrive(fileType, filePath);
                        alertDialog.dismiss();
                    }

                }
            }
        });

        Button negativeButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        negativeButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                changeImagesDir();

            }
        });
    }

    public void changeImagesDir() {
        ToastUtils.toastLong(QubeActivity.this, getString(R.string.chooseDirToCreateImage));
        FileUtils.browse(QubeActivity.this, FileType.IMAGE_DIR, Config.OPEN_IMAGE_DIR_REQUEST_CODE);
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(true);
            return true; // return
        }

        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Config.QGE_QUIT_RESULT_CODE) {
            if (getParent() != null) {
                getParent().finish();
            }
            finish();
            if (MachineController.getInstance().isRunning()) {
                notifyAction(MachineAction.STOP_VM, null);
            }
        } else if (requestCode == Config.OPEN_IMAGE_FILE_REQUEST_CODE || requestCode == Config.OPEN_IMAGE_FILE_ASF_REQUEST_CODE) {
            String file;
            if (requestCode == Config.OPEN_IMAGE_FILE_ASF_REQUEST_CODE) {
                file = FileUtils.getFileUriFromIntent(this, data, true);
            } else {
                browseFileType = FileUtils.getFileTypeFromIntent(this, data);
                file = FileUtils.getFilePathFromIntent(this, data);
            }
            if (file != null)
                updateDrive(browseFileType, file);
        } else if (requestCode == Config.OPEN_IMAGE_DIR_REQUEST_CODE || requestCode == Config.OPEN_IMAGE_DIR_ASF_REQUEST_CODE) {
            String imageDir;
            if (requestCode == Config.OPEN_IMAGE_DIR_ASF_REQUEST_CODE) {
                imageDir = FileUtils.getFileUriFromIntent(this, data, true);
            } else {
                imageDir = FileUtils.getDirPathFromIntent(this, data);
            }
            if (imageDir != null)
                QubeSettingsManager.setImagesDir(this, imageDir);

        } else if (requestCode == Config.OPEN_SHARED_DIR_REQUEST_CODE || requestCode == Config.OPEN_SHARED_DIR_ASF_REQUEST_CODE) {
            String file;
            if (requestCode == Config.OPEN_SHARED_DIR_ASF_REQUEST_CODE) {
                file = FileUtils.getFileUriFromIntent(this, data, true);
            } else {
                browseFileType = FileUtils.getFileTypeFromIntent(this, data);
                file = FileUtils.getDirPathFromIntent(this, data);
            }
            if (file != null) {
                updateDrive(browseFileType, file);
                QubeSettingsManager.setSharedDir(this, file);
            }
        } else if (requestCode == Config.OPEN_LOG_FILE_DIR_REQUEST_CODE || requestCode == Config.OPEN_LOG_FILE_DIR_ASF_REQUEST_CODE) {
            String file;
            if (requestCode == Config.OPEN_LOG_FILE_DIR_ASF_REQUEST_CODE) {
                file = FileUtils.getFileUriFromIntent(this, data, true);
            } else {
                file = FileUtils.getDirPathFromIntent(this, data);
            }
            if (file != null) {
                FileUtils.saveLogToFile(QubeActivity.this, file);
            }
        }
    }

    private void updateDrive(FileType fileType, String diskValue) {
        //FIXME: sometimes the array adapters try to set invalid values
        if (fileType == null || diskValue == null) {
            return;
        }
        Spinner spinner = getSpinner(fileType);
        if (!diskValue.trim().isEmpty()) {
            if (SpinnerAdapter.getPositionFromSpinner(spinner, diskValue) < 0) {
                android.widget.SpinnerAdapter adapter = spinner.getAdapter();
                if (adapter instanceof ArrayAdapter) {
                    SpinnerAdapter.addItem(spinner, diskValue);
                }
            }
            notifyAction(MachineAction.INSERT_FAV, new Object[]{diskValue, fileType});
            seMachineDriveValue(fileType, diskValue);
        }
        int res = spinner.getSelectedItemPosition();
        if (res == 1) {
            spinner.setSelection(0);
        }
    }

    private ArrayAdapter getAdapter(FileType fileType) {
        Spinner spinner = getSpinner(fileType);
        return (ArrayAdapter) spinner.getAdapter();
    }

    private Spinner getSpinner(FileType fileType) {
        if (diskMapping.containsKey(fileType))
            return diskMapping.get(fileType).spinner;
        return null;
    }

    private MachineProperty getProperty(FileType fileType) {
        if (diskMapping.containsKey(fileType))
            return diskMapping.get(fileType).colName;
        return null;
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        savePendingEditText();
        MachineController.getInstance().removeOnStatusChangeListener(this);
        Machine machine = getMachine();
        if (machine != null) {
            machine.deleteObserver(QubeActivity.this);
        }
        setViewListener(null);
        super.onDestroy();
    }

    private void populateRAM() {
        ArrayList<String> arraySpinner = new ArrayList<>();
        // We add these because some OSes need lower ram size to boot
        arraySpinner.add("64");
        arraySpinner.add("128");
        for (int i = 0; i < 48; i++) {
            arraySpinner.add(((i + 1) * 256) + "");
        }
        ArrayAdapter<String> ramAdapter = new ArrayAdapter<>(this, R.layout.custom_spinner_item, arraySpinner);
        ramAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        mRamSize.setAdapter(ramAdapter);
        mRamSize.invalidate();
    }

    private void populateCPUNum() {
        String[] arraySpinner = new String[Config.MAX_CPU_NUM];
        for (int i = 0; i < arraySpinner.length; i++) {
            arraySpinner[i] = (i + 1) + "";
        }
        ArrayAdapter<String> cpuNumAdapter = new ArrayAdapter<>(this, R.layout.custom_spinner_item, arraySpinner);
        cpuNumAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        mCPUNum.setAdapter(cpuNumAdapter);
        mCPUNum.invalidate();
    }

    private void populateTcgBuffer() {
        mTcgBuffer.setValueFrom(TCG_MIN);
        mTcgBuffer.setValueTo(TCG_MAX);
        mTcgBuffer.setStepSize(TCG_MIN);
        mTcgBuffer.setValue(TCG_MIN);
        mTcgBufferDisplay.setText(TCG_MIN + "M");
    }

    private void populateBiosType() {
        String[] biosTypes = {"Default", "UEFI"};
        ArrayAdapter<String> biosTypeAdapter = new ArrayAdapter<>(this, R.layout.custom_spinner_item, biosTypes);
        biosTypeAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        mBiosType.setAdapter(biosTypeAdapter);
        mBiosType.invalidate();
        updateBiosTypeVisibility("Default");
    }

    private void populateBootDevices() {
        ArrayList<String> bootDevicesList = new ArrayList<>();
        bootDevicesList.add("Default");
        bootDevicesList.add("CDROM");
        bootDevicesList.add("Hard Disk");
        if (Config.enableEmulatedFloppy)
            bootDevicesList.add("Floppy");

        String[] arraySpinner = bootDevicesList.toArray(new String[0]);

        ArrayAdapter<String> bootDevAdapter = new ArrayAdapter<>(this, R.layout.custom_spinner_item, arraySpinner);
        bootDevAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        mBootDevices.setAdapter(bootDevAdapter);
        mBootDevices.invalidate();
    }

    private void populateNet() {
        String[] arraySpinner = {"None", "User", "TAP"};
        ArrayAdapter<String> netAdapter = new ArrayAdapter<>(this, R.layout.custom_spinner_item, arraySpinner);
        netAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        mNetConfig.setAdapter(netAdapter);
        mNetConfig.invalidate();
    }

    private void populateVGA() {
        ArrayList<String> arrList = ArchDefinitions.getVGAValues(this);
        ArrayAdapter<String> vgaAdapter = new ArrayAdapter<>(this, R.layout.custom_spinner_item, arrList);
        vgaAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        mVGAConfig.setAdapter(vgaAdapter);
        mVGAConfig.invalidate();
    }

    private String getSelectedVga() {
        if (mVGAConfig == null || mVGAConfig.getSelectedItem() == null) {
            return null;
        }
        return (String) mVGAConfig.getSelectedItem();
    }

    private void enforceQGEForVirgl() {
        if (mUI != null && mUI.getSelectedItem() != null && !"QGE".equals(mUI.getSelectedItem())) {
            SpinnerAdapter.setDiskAdapterValue(mUI, "QGE");
            notifyFieldChange(MachineProperty.UI, "QGE");
            ToastUtils.toastShort(this, getString(R.string.virgl_requires_qge));
        }
    }

    private void populateKeyboardLayout() {
        ArrayList<String> arrList = ArchDefinitions.getKeyboardValues(this);
        ArrayAdapter<String> keyboardAdapter = new ArrayAdapter<>(this, R.layout.custom_spinner_item, arrList);
        keyboardAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        mKeyboard.setAdapter(keyboardAdapter);
        mKeyboard.invalidate();
        //TODO: for now we use only English keyboard, add more layouts
        mKeyboard.setSelection(0);
    }

    private void populateMouse() {
        ArrayList<String> arrList = ArchDefinitions.getMouseValues(this);
        ArrayAdapter<String> mouseAdapter = new ArrayAdapter<>(this, R.layout.custom_spinner_item, arrList);
        mouseAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        mMouse.setAdapter(mouseAdapter);
        mMouse.invalidate();
    }

    private void populateSoundcardConfig() {
        ArrayList<String> soundCards = new ArrayList<>();
        soundCards.add("None");
        soundCards.addAll(ArchDefinitions.getSoundcards(this));
        ArrayAdapter<String> sndAdapter = new ArrayAdapter<>(this, R.layout.custom_spinner_item, soundCards);
        sndAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        mSoundCard.setAdapter(sndAdapter);
        mSoundCard.invalidate();
    }

    private void populateNetDevices(String nic) {
        ArrayList<String> networkCards = ArchDefinitions.getNetworkDevices(this);
        ArrayAdapter<String> nicCfgAdapter = new ArrayAdapter<>(this, R.layout.custom_spinner_item, networkCards);
        nicCfgAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        mNetworkCard.setAdapter(nicCfgAdapter);
        mNetworkCard.invalidate();

        int pos = nicCfgAdapter.getPosition(nic);
        if (pos >= 0) {
            mNetworkCard.setSelection(pos);
        }
    }

    private void populateMachines(final String machineValue) {
        Thread thread = new Thread(new Runnable() {
            public void run() {
                final ArrayList<String> machinesList = ArchDefinitions.getMachineValues(QubeActivity.this);
                ArrayList<String> machinesDB = MachineController.getInstance().getStoredMachines();
                machinesList.addAll(machinesDB);
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    public void run() {
                        ArrayAdapter<String> machineAdapter = new ArrayAdapter<>(QubeActivity.this, R.layout.custom_spinner_item, machinesList);
                        machineAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
                        mMachine.setAdapter(machineAdapter);
                        mMachine.invalidate();
                        if (machineValue != null)
                            SpinnerAdapter.setDiskAdapterValue(mMachine, machineValue);
                    }
                });
            }
        });
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    private void seMachineDriveValue(FileType fileType, final String diskValue) {
        Spinner spinner = getSpinner(fileType);
        if (spinner != null)
            SpinnerAdapter.setDiskAdapterValue(spinner, diskValue);
    }

    private void populateCPUs(String cpu) {
        ArrayList<String> arrList = ArchDefinitions.getCpuValues(this);
        ArrayAdapter<String> cpuAdapter = new ArrayAdapter<>(this, R.layout.custom_spinner_item, arrList);
        cpuAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        mCPU.setAdapter(cpuAdapter);
        mCPU.invalidate();
        int pos = cpuAdapter.getPosition(cpu);
        if (pos >= 0) {
            mCPU.setSelection(pos);
        }
    }

    private void populateMachineType(String machineType) {
        ArrayList<String> arrList = ArchDefinitions.getMachineTypeValues(this);

        ArrayAdapter<String> machineTypeAdapter = new ArrayAdapter<>(this, R.layout.custom_spinner_item, arrList);
        machineTypeAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        mMachineType.setAdapter(machineTypeAdapter);

        mMachineType.invalidate();
        int pos = machineTypeAdapter.getPosition(machineType);
        mMachineType.setSelection(Math.max(pos, 0));

    }

    private void populateUI() {
        ArrayList<String> arrList = ArchDefinitions.getUIValues();
        ArrayAdapter<String> uiAdapter = new ArrayAdapter<>(this, R.layout.custom_spinner_item, arrList);
        uiAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        mUI.setAdapter(uiAdapter);
        mUI.invalidate();
    }

    public void populateDiskAdapter(final Spinner spinner, final FileType fileType, final boolean createOption) {
        populateDiskAdapter(spinner, fileType, createOption, null);
    }

    public void populateDiskAdapter(final Spinner spinner, final FileType fileType, final boolean createOption, final String selectedValue) {
        Thread t = new Thread(new Runnable() {
            public void run() {
                ArrayList<String> oldHDs = MachineFilePaths.getRecentFilePaths(fileType);
                final ArrayList<String> arraySpinner = new ArrayList<>();
                final boolean uefiFirmware = fileType == FileType.BIOS_CODE || fileType == FileType.BIOS_VARS;
                arraySpinner.add(uefiFirmware ? "Default" : "None");
                if (createOption)
                    arraySpinner.add("New");
                arraySpinner.add(getString(R.string.open));
                final int index = arraySpinner.size();
                if (oldHDs != null) {
                    for (String file : oldHDs) {
                        if (file != null && !(uefiFirmware && ("None".equalsIgnoreCase(file)
                                || "Default".equalsIgnoreCase(file)))) {
                            arraySpinner.add(file);
                        }
                    }
                }
                // Ensure current configured value is always present,
                // even if not in recent files, so the selection can find it
                if (selectedValue != null && !selectedValue.trim().isEmpty()
                        && !arraySpinner.contains(selectedValue)) {
                    arraySpinner.add(selectedValue);
                }
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    public void run() {
                        SpinnerAdapter adapter = new SpinnerAdapter(QubeActivity.this, R.layout.custom_spinner_item, arraySpinner, index);
                        adapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
                        spinner.setAdapter(adapter);
                        spinner.invalidate();
                        if (selectedValue != null) {
                            int pos = SpinnerAdapter.getPositionFromSpinner(spinner, selectedValue);
                            spinner.setSelection(Math.max(pos, 0));
                        }
                    }
                });
            }
        });
        t.start();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        invalidateOptionsMenu();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        menu.add(0, HELP, 0, R.string.help).setIcon(R.drawable.help_24px);
        menu.add(0, INSTALL, 0, R.string.InstallRoms).setIcon(R.drawable.archive_24px);
        if(!MachineController.getInstance().isRunning()) {
            menu.add(0, CREATE, 0, R.string.CreateMachine).setIcon(R.drawable.developer_board_24px);
            menu.add(0, RENAME, 0, R.string.renameMachine).setIcon(R.drawable.edit_note_24px);
            menu.add(0, DELETE, 0, R.string.DeleteMachine).setIcon(R.drawable.delete_24px);
        }
        menu.add(0, SETTINGS, 0, R.string.Settings).setIcon(R.drawable.settings_24px);
        menu.add(0, TOOLS, 0, R.string.advancedTools).setIcon(R.drawable.build_24px);
        menu.add(0, VIEWLOG, 0, R.string.ViewLog).setIcon(R.drawable.notes_24px);
        menu.add(0, HELP, 0, R.string.help).setIcon(R.drawable.help_24px);
        menu.add(0, CHANGELOG, 0, R.string.Changelog).setIcon(R.drawable.notes_24px);
        menu.add(0, LICENSE, 0, R.string.License).setIcon(R.drawable.copyright_24px);
        menu.add(0, QUIT, 0, R.string.Exit).setIcon(R.drawable.power_settings_new_24px);

        for (int i = 0; i < 2; i++) {
            menu.getItem(i).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {

        super.onOptionsItemSelected(item);
        if (item.getItemId() == INSTALL) {
            Installer.installFiles(this, true);
        } else if (item.getItemId() == DELETE) {
            promptDeleteMachine();
        } else if (item.getItemId() == CREATE) {
            promptMachineName(this);
        } else if (item.getItemId() == RENAME) {
            promptRenameMachine();
        } else if (item.getItemId() == SETTINGS) {
            showSettings();
        } else if (item.getItemId() == TOOLS) {
            QubeActivityCommon.goToURL(this, Config.toolsLink);
        } else if (item.getItemId() == HELP) {
            Help.showHelp(this);
        } else if (item.getItemId() == VIEWLOG) {
            Logger.viewQubeLog(QubeActivity.this);
        } else if (item.getItemId() == CHANGELOG) {
            QubeActivityCommon.showChangelog(QubeActivity.this);
        } else if (item.getItemId() == LICENSE) {
            promptLicense();
        } else if (item.getItemId() == QUIT) {
            exit();
        }
        return true;
    }

    private void showSettings() {
        Intent i = new Intent(this, QubeSettingsManager.class);
        startActivity(i);
    }

    private void promptRenameMachine() {
        Machine machine = getMachine();
        if (machine == null) {
            ToastUtils.toastShort(this, getString(R.string.NoMachineSelected));
            return;
        }
        EditText nameInput = new EditText(this);
        nameInput.setSingleLine(true);
        nameInput.setText(machine.getName());
        nameInput.setSelectAllOnFocus(true);
        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        nameInput.setPadding(padding, padding / 2, padding, padding / 2);
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.renameMachine)
                .setIcon(R.drawable.edit_note_24px)
                .setView(nameInput)
                .setPositiveButton(R.string.Save, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            String newName = nameInput.getText().toString().trim();
            if (newName.isEmpty()) {
                ToastUtils.toastShort(this, getString(R.string.MachineNameCannotBeEmpty));
                return;
            }
            if (!MachineController.getInstance().renameMachine(machine, newName)) {
                ToastUtils.toastShort(this, getString(R.string.VMNameExistsChooseAnother));
                return;
            }
            dialog.dismiss();
            populateMachines(newName);
        }));
        dialog.show();
    }

    public void promptDeleteMachine() {
        if (getMachine() == null) {
            ToastUtils.toastShort(this, getString(R.string.NoMachineSelected));
            return;
        }
        new MaterialAlertDialogBuilder(this).setTitle(getString(R.string.DeleteVM) + ": " + getMachine().getName())
                .setIcon(R.drawable.delete_24px)
                .setMessage(R.string.deleteVMWarning)
                .setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        onDeleteMachine();
                    }
                }).setNegativeButton(getString(android.R.string.no), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            }
        }).show();
    }

    public void onPause() {
        View currentView = getCurrentFocus();
        if (currentView instanceof EditText) {
            currentView.setFocusable(false);
        }
        super.onPause();
    }

    public void onResume() {
        super.onResume();
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                updateValues();
            }
        }, 1000);

    }

    private void updateValues() {
        Thread t = new Thread(new Runnable() {
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        changeStatus(MachineController.getInstance().getCurrStatus());
                        updateRemovableDiskValues();
                        updateSummary();
                    }
                });
            }
        });
        t.start();
    }

    private void updateRemovableDiskValues() {
        if (getMachine() != null) {
            disableRemovableDiskListeners();
            updateDrive(FileType.CDROM, getMachine().getCdImagePath());
            updateDrive(FileType.FDA, getMachine().getFdaImagePath());
            updateDrive(FileType.FDB, getMachine().getFdbImagePath());
            enableRemovableDiskListeners();
        }
    }

    public boolean isLandscapeOrientation(Activity activity) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        Point screenSize = new Point();
        display.getSize(screenSize);
        return screenSize.x >= screenSize.y;
    }

    @Override
    public void onMachineStatusChanged(Machine machine, MachineStatus status, Object o) {
        changeStatus(status);
    }

    @Override
    public void onEvent(Machine machine, MachineController.Event event, Object o) {
        switch (event) {
            case MachineCreateFailed:
                if (o instanceof Integer) {
                    ToastUtils.toastShort(QubeActivity.this, getString((int) o));
                } else if (o instanceof String) {
                    ToastUtils.toastShort(QubeActivity.this, (String) o);
                }
                break;
            case MachineCreated:
                machineCreated();
                break;
            case MachineLoaded:
                loadMachine();
                break;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateSummary();
            }
        });
    }


    private void updateFavAdapters() {
        mHDA.getAdapter().getCount();
        mHDB.getAdapter().getCount();
        mHDC.getAdapter().getCount();
        mHDD.getAdapter().getCount();
        mCD.getAdapter().getCount();
        mFDA.getAdapter().getCount();
        mFDB.getAdapter().getCount();
        mKernel.getAdapter().getCount();
        mInitrd.getAdapter().getCount();
    }

    @Override
    public void update(Observable observable, final Object o) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Object [] params = (Object[] ) o;
                if (params[0] instanceof MachineProperty) {
                    MachineProperty property = (MachineProperty) params[0];
                    if (property == MachineProperty.UI) {

                        mSoundCard.setEnabled(getMachine().getRenderer() != 1);
                    }
                }
                updateSummary();
            }
        });

    }

    public void notifyFieldChange(MachineProperty property, Object value) {
        if (viewListener != null)
            viewListener.onFieldChange(property, value);
    }

    public void notifyAction(MachineAction action, Object value) {
        if (viewListener != null)
            viewListener.onAction(action, value);
    }

    //check permissions before start
    private void checkStoragePermission() {
        runOnUiThread(() -> {
            if (!Environment.isExternalStorageManager()) {
                new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.WriteAccess)
                    .setIcon(R.drawable.folder_24px)
                    .setMessage(R.string.FullAccessWarning)
                    .setCancelable(false)
                    .setPositiveButton(R.string.OkIUnderstand, (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    })
                    .show();
            }
        });
    }

    static class DiskInfo {
        public MaterialSwitch enableCheckBox;
        public Spinner spinner;
        public MachineProperty colName;

        public DiskInfo(Spinner spinner, MaterialSwitch enableCheckbox, MachineProperty dbColName) {
            this.spinner = spinner;
            this.enableCheckBox = enableCheckbox;
            this.colName = dbColName;
        }
    }

}
