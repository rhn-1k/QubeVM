/*
Copyright (C) Max Kastanas 2012
Copyright (C) Rhn 2026
 */
package com.max2idea.android.qube.main;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.view.Choreographer;
import android.window.OnBackInvokedDispatcher;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.view.Display;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.qube.emu.lib.R;
import com.max2idea.android.qube.files.FileUtils;
import com.max2idea.android.qube.keyboard.KeyboardUtils;
import com.max2idea.android.qube.log.Logger;
import com.max2idea.android.qube.machine.Machine;
import com.max2idea.android.qube.machine.MachineAction;
import com.max2idea.android.qube.machine.MachineController;
import com.max2idea.android.qube.machine.MachineProperty;
import com.max2idea.android.qube.screen.ScreenUtils;
import com.max2idea.android.qube.toast.ToastUtils;
import com.max2idea.android.qube.qmp.QmpClient;
import com.max2idea.android.qube.server.SharedFolderServer;
import com.max2idea.android.qube.utils.ClipboardUtils;
import com.max2idea.android.qube.jni.QubeGfx;
import com.max2idea.android.qube.jni.QubeAudio;
import com.max2idea.android.qube.jni.QubeInput;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QubeQGEActivity extends AppCompatActivity
        implements VirtualKeys.OnSendKeyEventListener,
        MachineController.OnMachineStatusChangeListener,
        MachineController.OnEventListener {
    public static final int KEYBOARD = 10000;
    private static final String TAG = "QubeQGEActivity";

    public static boolean toggleKeyboardFlag = true;
    public static boolean isResizing = false;
    public static boolean pendingStop;
    private static boolean machineRunning;

    public static MouseMode mouseMode = MouseMode.Trackpad;
    private final ExecutorService mouseEventsExecutor = Executors.newFixedThreadPool(1);
    private final ExecutorService keyEventsExecutor = Executors.newFixedThreadPool(1);
    private final ExecutorService screenshotExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService shareFolderExecutor = Executors.newSingleThreadExecutor();
    public DrivesDialogBox drives = null;
    public AudioManager am;
    protected int maxVolume;
    private VirtualKeys mVirtualKeys;
    private ViewListener viewListener;
    private boolean quit = false;
    private View mGap;
    private boolean resettingLayout;
    private View mTopLayout;
    private HorizontalScrollView mVirtualKeysContainer;

    QubeQGESurface mSurface;
    private static QubeQGEActivity mSingleton;
    private Bitmap frameBitmap;
    // Qube: frame pull is straight from qemu, see startGfxLoop()
    private int lastGfxGeneration = -1;
    private boolean gfxLoopRunning = false;
    // nativeSetRefreshRate() becomes no-op if called before QEMU loads
    // Handle is guaranteed set once pullGfxFrame() returns real dimensions
    private boolean nativeRefreshRatePushed = false;
    private final Choreographer.FrameCallback gfxFrameCallback = new Choreographer.FrameCallback() {
        @Override
        public void doFrame(long frameTimeNanos) {
            if (!gfxLoopRunning)
                return;
            pullGfxFrame();
            Choreographer.getInstance().postFrameCallback(this);
        }
    };
    // Virtual cursor position in framebuffer coordinates, since we only send absolute
    // positions, trackpad mode is just us moving this ourselves and resending it,
    // touchscreen mode sets it straight from the touch point
    private float cursorX, cursorY;
    private int pointerButtonMask = 0;

    public void showHints() {
        ToastUtils.toastShortTop(this, getString(R.string.PressVolumeDownForRightClick));
    }

    public void setupToolBar() {
        Toolbar tb = findViewById(R.id.toolbar);
        setSupportActionBar(tb);

        // Get the ActionBar here to configure the way it behaves.
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayShowHomeEnabled(false); // hide app icon
            ab.setDisplayHomeAsUpEnabled(false);
            ab.setDisplayShowCustomEnabled(true); // enable overriding the
            ab.setDisplayShowTitleEnabled(true); // disable the default title
            ab.setTitle(R.string.app_name);
            if (!QubeSettingsManager.getAlwaysShowMenuToolbar(this)) {
                ab.hide();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Config.OPEN_IMAGE_FILE_REQUEST_CODE || requestCode == Config.OPEN_IMAGE_FILE_ASF_REQUEST_CODE) {
            String file;
            if (requestCode == Config.OPEN_IMAGE_FILE_ASF_REQUEST_CODE) {
                file = FileUtils.getFileUriFromIntent(this, data, true);
            } else {
                drives.fileType = FileUtils.getFileTypeFromIntent(this, data);
                file = FileUtils.getFilePathFromIntent(this, data);
            }
            if (drives != null && file != null)
                drives.setDriveAttr(drives.fileType, file);
        } else if (requestCode == Config.OPEN_LOG_FILE_DIR_REQUEST_CODE || requestCode == Config.OPEN_LOG_FILE_DIR_ASF_REQUEST_CODE) {
            String file;
            if (requestCode == Config.OPEN_LOG_FILE_DIR_ASF_REQUEST_CODE) {
                file = FileUtils.getFileUriFromIntent(this, data, true);
            } else {
                file = FileUtils.getDirPathFromIntent(this, data);
            }
            if (file != null) {
                FileUtils.saveLogToFile(this, file);
            }
        }
    }




    @Override
    protected void onPause() {
        stopGfxLoop();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (machineRunning)
            startGfxLoop();
    }

    @Override
    protected void onDestroy() {
        quit = true;
        stopGfxLoop();
        removeListeners();
        super.onDestroy();
    }

    private void removeListeners() {
        MachineController.getInstance().removeOnStatusChangeListener(this);
        MachineController.getInstance().removeOnEventListener(this);
        setViewListener(null);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        super.onOptionsItemSelected(item);
        if (item.getItemId() == R.id.itemDrives) {
            // Show up removable devices dialog
            if (MachineController.getInstance().getMachine().hasRemovableDevices()) {
                drives = new DrivesDialogBox(QubeQGEActivity.this, R.style.AppMaterialAlertDialogTheme, MachineController.getInstance().getMachine());
                drives.show();
            } else {
                ToastUtils.toastShort(this, getString(R.string.NoRemovableDevicesAttached));
            }
        } else if (item.getItemId() == R.id.itemReset) {
            QubeActivityCommon.promptResetVM(this, viewListener);
        } else if (item.getItemId() == R.id.itemShutdown) {
            KeyboardUtils.hideKeyboard(this, mSurface);
            QubeActivityCommon.promptStopVM(this, viewListener);
        } else if (item.getItemId() == R.id.itemDisconnet) {
            finish();
        } else if (item.getItemId() == R.id.itemMouse) {
            promptMouseMode();
        } else if (item.getItemId() == KEYBOARD || item.getItemId() == R.id.itemKeyboard) {
            //XXX: need to delay to work properly
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    toggleKeyboardFlag = KeyboardUtils.showKeyboard(QubeQGEActivity.this, toggleKeyboardFlag, mSurface);
                    // Virtual keys toggle based on IME visibility, not guessing the delays
                    // This prevents missed toggles from timing issues
                }
            }, 500);
        } else if (item.getItemId() == R.id.itemVolume) {
            promptVolume();
        } else if (item.getItemId() == R.id.itemCtrlAltDel) {
            sendCtrlAltDel();
        } else if (item.getItemId() == R.id.itemHideToolbar) {
            hideToolbar();
        } else if (item.getItemId() == R.id.itemViewLog) {
            Logger.viewQubeLog(this);
        } else if (item.getItemId() == R.id.itemVirtualKeys) {
            keyboardInsetHandler.removeCallbacks(closeVirtualKeysRunnable);
            mVirtualKeys.toggle();
            virtualKeysAutoOpened = false;
        } else if (item.getItemId() == R.id.itemSendText) {
            promptSendText();
        } else if (item.getItemId() == R.id.itemScreenshot) {
            takeScreenshotQMP();
        } else if (item.getItemId() == R.id.itemShareFolder) {
            showShareFolderDialog();
        }

        invalidateOptionsMenu();
        return true;
    }

    private void showShareFolderDialog() {
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

            builder.setPositiveButton(R.string.share_server_stop, (dialog, which) -> {
                SharedFolderServer.stopServer();
            });
        } else {
            builder.setTitle(R.string.share_folder_stopped_title);
            tvDescription.setText(R.string.share_folder_stopped_desc);
            layoutRunningInfo.setVisibility(View.GONE);

            builder.setPositiveButton(R.string.share_server_start, (dialog, which) -> startShareFolderServer());
        }

        builder.show();
    }

    private void startShareFolderServer() {
        shareFolderExecutor.execute(() -> {
            boolean started = SharedFolderServer.startServer();
            runOnUiThread(() -> {
                if (started) {
                    // Re open the dialog in its "running" state so the user
                    // immediately sees the link and copy actions
                    showShareFolderDialog();
                } else {
                    ToastUtils.toastShort(this, getString(R.string.share_folder_start_error));
                }
            });
        });
    }

    // Screenshot using QMP
    private void takeScreenshotQMP() {
        final String baseDir = Config.storagedir + "/" + Config.screenshotDir;
        final File dir = new File(baseDir);
        if (!dir.exists() && !dir.mkdirs()) {
            ToastUtils.toastShort(this, getString(R.string.screenshot_failed));
            return;
        }

        String machineName = MachineController.getInstance().getMachineName();
        if (machineName == null || machineName.isEmpty()) machineName = "vm";
        // Strip characters that aren't safe in a filename (machine names are free text)
        machineName = machineName.replaceAll("[^a-zA-Z0-9._-]", "_");

        final String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        final String filename = machineName + "_" + timestamp + ".png";
        final String fullPath = baseDir + "/" + filename;

        // QmpClient.screendump() does blocking socket I/O so it
        // must never run on the UI thread or it can ANR the app
        screenshotExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final String result = QmpClient.screendump(fullPath);

                if (result != null) {
                    // Save the picture as media so it can be shown on phone gallery
                    MediaScannerConnection.scanFile(
                            QubeQGEActivity.this,
                            new String[]{fullPath},
                            new String[]{"image/png"},
                            null);
                }

                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        if (result != null) {
                            ToastUtils.toastShort(QubeQGEActivity.this,
                                    getString(R.string.screenshot_saved) + " " + fullPath);
                        } else {
                            ToastUtils.toastShort(QubeQGEActivity.this, getString(R.string.screenshot_failed));
                        }
                    }
                });
            }
        });
    }

    private void promptSendText() {
        final EditText text = new EditText(this);
        final AlertDialog alertDialog;
        alertDialog = new MaterialAlertDialogBuilder(this).create();
        alertDialog.setTitle(getString(R.string.SendText));
        alertDialog.setIcon(R.drawable.edit_24px);
        text.setText("");
        text.setEnabled(true);
        text.setVisibility(View.VISIBLE);
        text.setSingleLine();
        alertDialog.setView(text);

        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clipData = clipboardManager.getPrimaryClip();
        if (clipData != null) {
            if (clipData.getItemCount() > 0)
                text.setText(clipData.getItemAt(0).getText());
        }

        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.Send), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                String textStr = text.getText().toString();
                sendText(textStr);
            }
        });
        alertDialog.show();
    }

    public void hideToolbar() {
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.hide();
        }
    }

    private void promptMouseMode() {
        String[] items = {
                getString(R.string.TrackpadDescr),
                getString(R.string.TouchScreen),
                getString(R.string.ExternalMouseDescr)
        };
        final AlertDialog.Builder mBuilder = new MaterialAlertDialogBuilder(this);
        mBuilder.setTitle(R.string.Mouse);
        mBuilder.setIcon(R.drawable.mouse_24px);
        mBuilder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                switch (i) {
                    case 0:
                        setTrackpadMode();
                        break;
                    case 1:
                    case 2:
                        promptAbsoluteDevice(i == 2);
                        break;
                    default:
                        break;
                }
                dialog.dismiss();
            }
        });
        final AlertDialog alertDialog = mBuilder.create();
        alertDialog.show();
    }

    protected void setTrackpadMode() {
        try {
            ScreenUtils.updateOrientation(this, -1);
            mouseMode = MouseMode.Trackpad;
            invalidateOptionsMenu();
            mSurface.refreshSurfaceView();
        } catch (Exception ex) {
            if (Config.debug)
                ex.printStackTrace();
        }
    }

    private void promptAbsoluteDevice(final boolean externalMouse) {
        final AlertDialog alertDialog;
        alertDialog = new MaterialAlertDialogBuilder(this).create();
        alertDialog.setTitle(getString(R.string.desktopMode));
        alertDialog.setIcon(R.drawable.mouse_24px);
        final LinearLayout mLayout = new LinearLayout(this);
        mLayout.setPadding(20, 20, 20, 20);
        mLayout.setOrientation(LinearLayout.VERTICAL);

        TextView textView = new TextView(this);
        textView.setVisibility(View.VISIBLE);
        String instructions = getString(R.string.absolutePointerInstructions);
        if (externalMouse)
            instructions += "\n" + getString(R.string.externalMouseInstructions);
        textView.setText(instructions);
        mLayout.addView(textView);
        alertDialog.setView(mLayout);
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.Ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // we handle external mouse at all times
                if (!externalMouse)
                    setTouchScreenMode();
                alertDialog.dismiss();
            }
        });
        alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.Cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                alertDialog.dismiss();
            }
        });
        alertDialog.show();
    }

    protected void setTouchScreenMode() {
        try {
            mouseMode = MouseMode.TOUCHSCREEN;
            invalidateOptionsMenu();
            mSurface.refreshSurfaceView();
        } catch (Exception ex) {
            if (Config.debug)
                ex.printStackTrace();
        }
    }

    private void sendCtrlAltDel() {
        onSendKeyEvent(KeyEvent.KEYCODE_CTRL_LEFT, true);
        onSendKeyEvent(KeyEvent.KEYCODE_ALT_LEFT, true);
        onSendKeyEvent(KeyEvent.KEYCODE_FORWARD_DEL, true);
        onSendKeyEvent(KeyEvent.KEYCODE_FORWARD_DEL, false);
        onSendKeyEvent(KeyEvent.KEYCODE_ALT_LEFT, false);
        onSendKeyEvent(KeyEvent.KEYCODE_CTRL_LEFT, false);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        getMenuInflater().inflate(R.menu.qgeactivitymenu, menu);

        int maxMenuItemsShown = 4;
        int actionShow = MenuItem.SHOW_AS_ACTION_IF_ROOM;
        if (ScreenUtils.isLandscapeOrientation(this)) {
            maxMenuItemsShown = 6;
            actionShow = MenuItem.SHOW_AS_ACTION_ALWAYS;
        }

        // Remove scaling for now
        menu.removeItem(menu.findItem(R.id.itemScaling).getItemId());
        // Remove external mouse for now
        menu.removeItem(menu.findItem(R.id.itemExternalMouse).getItemId());
        menu.removeItem(menu.findItem(R.id.itemCtrlAltDel).getItemId());
        menu.removeItem(menu.findItem(R.id.itemCtrlC).getItemId());

        if (MachineController.getInstance().getMachine().getSoundCard() == null) {
            menu.removeItem(menu.findItem(R.id.itemVolume).getItemId());
            maxMenuItemsShown--;
        }

        for (int i = 0; i < menu.size() && i < maxMenuItemsShown; i++) {
            menu.getItem(i).setShowAsAction(actionShow);
        }
        return true;
    }

    // FIXME: We need this to able to catch complex characters strings like
    // grave and send it as text
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_MULTIPLE && event.getKeyCode() == KeyEvent.KEYCODE_UNKNOWN) {
            sendText(event.getCharacters());
            return true;
        } else if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            onBackPressed();
            return true;
        }
        if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN) {
            // We emulate right click with volume down
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                sendRightClick();
            }
            return true;
        } else if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP) {
            // We emulate middle click with volume up
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                sendMiddleClick();
            }
            return true;
        } else {
            return super.dispatchKeyEvent(event);
        }
    }

    private void sendText(String string) {
        KeyCharacterMap keyCharacterMap = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD);
        KeyEvent[] keyEvents = keyCharacterMap.getEvents(string.toCharArray());
        if (keyEvents == null)
            return;
        for (KeyEvent keyEvent : keyEvents) {
            if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                sendKeyEvent(keyEvent, keyEvent.getKeyCode(), true);
            } else if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
                sendKeyEvent(keyEvent, keyEvent.getKeyCode(), false);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Stop the system from also auto-resizing/panning for the IME, since we
        // position virtual_keys_container ourselves in applyKeyboardInset().
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setupScreen();
        super.onCreate(savedInstanceState);
        mSingleton = this;
        setupWidgets();
        setupListeners();
        setupToolBar();
        showHints();
        ScreenUtils.updateOrientation(this, -1);
        checkPendingActions();
        setupUserInterface();
        setupAudio();
        startMachineAndConnect();
        notifyAction(MachineAction.UPDATE_NOTIFICATION, getString(R.string.VMRunning));

        // Android 13+ needs Predictive Back bypassing onBackPressed()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                    () -> onBackPressed()
            );
        }
    }

    private void startMachineAndConnect() {
        notifyAction(MachineAction.START_VM, null);
        onGfxReady(QubeGfx.nativeGetWidth(), QubeGfx.nativeGetHeight());
    }

    private void onGfxReady(final int width, final int height) {
        Log.d(TAG, "onGfxReady: width=" + width + " height=" + height);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (width <= 0 || height <= 0) {
                    Log.e(TAG, "Guest never produced a display surface");
                    return;
                }
                frameBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                cursorX = width / 2f;
                cursorY = height / 2f;
                mSurface.setFrameBitmap(frameBitmap);
                machineRunning = true;
                startGfxLoop();
            }
        });
    }

    // Starts pulling frames directly from qemu's DisplaySurface, in step with
    // this display's vsync, via QubeGfx, see native source files
    private void startGfxLoop() {
        if (gfxLoopRunning) {
            Log.d(TAG, "startGfxLoop: already running, skipped");
            return;
        }
        nativeRefreshRatePushed = false;
        Log.d(TAG, "startGfxLoop: starting, requesting " + QubeSettingsManager.getRefreshRate(this) + "hz");
        applyRefreshRate(QubeSettingsManager.getRefreshRate(this));
        if (Config.enableQGESound && MachineController.getInstance().getMachine().getSoundCard() != null
                && !MachineController.getInstance().getMachine().getSoundCard().toLowerCase().equals("none")) {
            QubeAudio.nativeStartAudio();
        }
        gfxLoopRunning = true;
        lastGfxGeneration = -1;
        Choreographer.getInstance().postFrameCallback(gfxFrameCallback);
    }

    // Push Refresh Rate to QEMU and switch to closest matching display mode
    // so Choreographer ticks at that speed
    private void applyRefreshRate(int hz) {
        QubeGfx.nativeSetRefreshRate(hz);

        Window window = getWindow();
        Display display = window.getDecorView().getDisplay();
        if (display == null) {
            display = getWindowManager().getDefaultDisplay();
        }
        if (display == null) {
            Log.e(TAG, "applyRefreshRate: no display available, bailing");
            return;
        }

        Display.Mode current = display.getMode();
        Display.Mode best = current;
        for (Display.Mode mode : display.getSupportedModes()) {
            if (mode.getPhysicalWidth() != current.getPhysicalWidth()
                    || mode.getPhysicalHeight() != current.getPhysicalHeight()) {
                continue; // keep resolution stable, only vary refresh rate
            }
            // Prefer lowest mode covering requested rate to avoid needlessly higher refresh
            if (mode.getRefreshRate() >= hz - 0.5f
                    && (best == current || mode.getRefreshRate() < best.getRefreshRate())) {
                best = mode;
            }
        }
        if (best == current && current.getRefreshRate() < hz) {
            // Nothing covers the request, fall back to the highest the screen offers
            for (Display.Mode mode : display.getSupportedModes()) {
                if (mode.getPhysicalWidth() == current.getPhysicalWidth()
                        && mode.getPhysicalHeight() == current.getPhysicalHeight()
                        && mode.getRefreshRate() > best.getRefreshRate()) {
                    best = mode;
                }
            }
        }

        WindowManager.LayoutParams params = window.getAttributes();
        params.preferredDisplayModeId = best.getModeId();
        window.setAttributes(params);
        Log.d(TAG, "applyRefreshRate: requested=" + hz + "hz current=" + current.getRefreshRate()
                + "hz chosen=" + best.getRefreshRate() + "hz modeId=" + best.getModeId()
                + " hasFocus=" + window.getDecorView().hasWindowFocus());
    }

    private void stopGfxLoop() {
        gfxLoopRunning = false;
        QubeAudio.nativeStopAudio();
        Choreographer.getInstance().removeFrameCallback(gfxFrameCallback);
    }

    // Resize if surface changed, copy from QEMU memory,
    // and redraw SurfaceView only when generation counter advances
    private void pullGfxFrame() {
        int w = QubeGfx.nativeGetWidth();
        int h = QubeGfx.nativeGetHeight();
        if (w <= 0 || h <= 0)
            return;

        if (!nativeRefreshRatePushed) {
            // real dimensions mean QEMU's .so is loaded now, safe to retry the push
            // that likely became no-op earlier if it ran before the lib finished loading
            int hz = QubeSettingsManager.getRefreshRate(this);
            QubeGfx.nativeSetRefreshRate(hz);
            nativeRefreshRatePushed = true;
            Log.d(TAG, "pullGfxFrame: retried nativeSetRefreshRate(" + hz + ") now that guest is alive");
        }

        if (frameBitmap == null || w != frameBitmap.getWidth() || h != frameBitmap.getHeight()) {
            frameBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            mSurface.setFrameBitmap(frameBitmap);
        }

        int generation = QubeGfx.nativeCopyFrame(frameBitmap);
        if (generation == 0 || generation == lastGfxGeneration)
            return;
        lastGfxGeneration = generation;

        // Plain repaint, View.postInvalidate() is the lightweight redraw we want
        // mSurface.refreshSurfaceView() spawns a thread and changes resolution, wrong for 60x/sec
        mSurface.postInvalidate();
    }

    private void setupUserInterface() {
        Config.keyDelay = QubeSettingsManager.getKeyPressDelay(this);
        Config.mouseButtonDelay = QubeSettingsManager.getMouseButtonDelay(this);
    }

    private void setupScreen() {
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        // if Fullscreen enabled, hide navbar and status bar, if not, hide status bar only
        if (QubeSettingsManager.getFullscreen(this)) {
            controller.hide(WindowInsetsCompat.Type.systemBars());
        } else {
            controller.hide(WindowInsetsCompat.Type.statusBars());
            controller.show(WindowInsetsCompat.Type.navigationBars());
        }
        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        if (pm.isSustainedPerformanceModeSupported())
            getWindow().setSustainedPerformanceMode(true);
    }

    private void setupListeners() {
        MachineController.getInstance().addOnStatusChangeListener(this);
        MachineController.getInstance().addOnEventListener(this);
        setViewListener(QubeApplication.getViewListener());
    }

    public void setViewListener(ViewListener viewListener) {
        this.viewListener = viewListener;
    }

    private void setupWidgets() {
        mSurface = new QubeQGESurface(this, this);

        setContentView(R.layout.qube_qge);
        RelativeLayout mLayout = findViewById(R.id.vnc_layout);

        RelativeLayout mVncContainer = findViewById(R.id.vnc);
        mVncContainer.addView(mSurface);

        mGap = findViewById(R.id.gap);
        mTopLayout = findViewById(R.id.top_layout);
        mVirtualKeysContainer = findViewById(R.id.virtual_keys_container);

        mVirtualKeys = new VirtualKeys(this, mSurface, mVirtualKeysContainer, this);

        initKeyboardInsets();

        updateLayout(getResources().getConfiguration().orientation);
    }

    // Pad mTopLayout bottom to lift virtual_keys_container above keyboard, with listener on main_layout to catch insets early
    private int imeBottomInset = 0;

    private void initKeyboardInsets() {
        View root = findViewById(R.id.main_layout);
        if (root == null || mTopLayout == null)
            return;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R)
            hookInsetsListener(root);
        else
            hookLegacyInsetsListener(root);
    }

    private void hookInsetsListener(View root) {
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());

            v.setPadding(navBars.left, navBars.top, navBars.right, navBars.bottom);
            imeBottomInset = Math.max(0, imeInsets.bottom - navBars.bottom);
            applyKeyboardInset(imeBottomInset);
            updateToolbarForKeyboard(insets.isVisible(WindowInsetsCompat.Type.ime()));
            return insets;
        });
    }

    // IME WindowInsets aren't reliably dispatched below API 30, so estimate keyboard
    // height from the decor view's visible display frame instead
    private void hookLegacyInsetsListener(View root) {
        final View decorView = getWindow().getDecorView();
        decorView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            android.graphics.Rect visibleFrame = new android.graphics.Rect();
            decorView.getWindowVisibleDisplayFrame(visibleFrame);
            int screenHeight = getRealScreenHeight();
            int navBarHeight = getLegacyNavigationBarHeight();
            int keypadHeight = screenHeight - visibleFrame.bottom - navBarHeight;
            imeBottomInset = keypadHeight > screenHeight * 0.15 ? keypadHeight : 0;
            applyKeyboardInset(imeBottomInset);
            updateToolbarForKeyboard(imeBottomInset > 0);
        });
    }

    // On pre-R getWindowVisibleDisplayFrame() includes nav bar in shrinkage, subtract it to get keyboard-only inset
    private int getLegacyNavigationBarHeight() {
        int resId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        if (resId <= 0)
            return 0;
        android.view.WindowManager wm = (android.view.WindowManager)
                getSystemService(Context.WINDOW_SERVICE);
        android.graphics.Point realSize = new android.graphics.Point();
        wm.getDefaultDisplay().getRealSize(realSize);
        android.graphics.Point size = new android.graphics.Point();
        wm.getDefaultDisplay().getSize(size);
        // No real/app size difference means no reserved nav bar (eg gesture nav) to account for
        if (realSize.y == size.y)
            return 0;
        return getResources().getDimensionPixelSize(resId);
    }

    private int getRealScreenHeight() {
        android.view.WindowManager wm = (android.view.WindowManager)
                getSystemService(Context.WINDOW_SERVICE);
        android.graphics.Point realSize = new android.graphics.Point();
        wm.getDefaultDisplay().getRealSize(realSize);
        return realSize.y;
    }

    private void applyKeyboardInset(int bottom) {
        if (mVirtualKeysContainer != null)
            mVirtualKeysContainer.setTranslationY(-bottom);
    }

    // Hide toolbar when keyboard + virtual keys are up, restore only if it was showing before
    private boolean toolbarHiddenForKeyboard = false;
    // Track if virtual keys auto-opened with keyboard, so we only auto-close them, not manual panels
    private boolean virtualKeysAutoOpened = false;
    private final Handler keyboardInsetHandler = new Handler(Looper.getMainLooper());
    private final Runnable closeVirtualKeysRunnable = new Runnable() {
        @Override
        public void run() {
            virtualKeysAutoOpened = false;
            if (mVirtualKeys != null && mVirtualKeys.isShown()) {
                mVirtualKeys.toggle();
            }
        }
    };

    private void updateToolbarForKeyboard(boolean imeVisible) {
        keyboardInsetHandler.removeCallbacks(closeVirtualKeysRunnable);
        if (!imeVisible && virtualKeysAutoOpened) {
            // Ignore transient imeVisible=false blips from toolbar or layout passes during animation
            keyboardInsetHandler.postDelayed(closeVirtualKeysRunnable, 150);
        } else if (imeVisible && QubeSettingsManager.getVkOpenWithKeyboard(this)
                && mVirtualKeys != null && !mVirtualKeys.isShown()) {
            // Open virtual keys exactly when IME becomes visible
            mVirtualKeys.toggle();
            virtualKeysAutoOpened = true;
        }

        ActionBar bar = getSupportActionBar();
        if (bar == null)
            return;

        if (!ScreenUtils.isLandscapeOrientation(this)) {
            if (toolbarHiddenForKeyboard) {
                toolbarHiddenForKeyboard = false;
                bar.show();
            }
            return;
        }

        if (imeVisible && mVirtualKeys != null && mVirtualKeys.isShown()) {
            if (bar.isShowing()) {
                toolbarHiddenForKeyboard = true;
                bar.hide();
            }
        } else if (toolbarHiddenForKeyboard) {
            toolbarHiddenForKeyboard = false;
            bar.show();
        }
    }

    public void promptVolume() {
        final AlertDialog alertDialog;
        alertDialog = new MaterialAlertDialogBuilder(this).create();
        alertDialog.setTitle(getString(R.string.Volume));
        alertDialog.setIcon(R.drawable.volume_up_24px);
        LinearLayout.LayoutParams volParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        LinearLayout t = createVolumePanel();
        t.setLayoutParams(volParams);

        ScrollView s = new ScrollView(this);
        s.addView(t);
        alertDialog.setView(s);
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(android.R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                alertDialog.cancel();
            }
        });
        alertDialog.show();
    }

    public LinearLayout createVolumePanel() {
        LinearLayout layout = new LinearLayout(this);
        layout.setPadding(20, 20, 20, 20);
        LinearLayout.LayoutParams volparams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        SeekBar vol = new SeekBar(this);
        int volume;
        vol.setMax(maxVolume);
        volume = getCurrentVolume();
        vol.setProgress(volume);
        vol.setLayoutParams(volparams);
        vol.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int progress, boolean touch) {
                setVolume(progress);
            }

            public void onStartTrackingTouch(SeekBar arg0) {
            }

            public void onStopTrackingTouch(SeekBar arg0) {
            }
        });
        layout.addView(vol);
        return layout;
    }

    @Override
    public void onSendKeyEvent(int keyCode, boolean down) {
        sendKeyEvent(null, keyCode, down);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        processTrackPadEvents(event);
        return true;
    }

    /**
     * For Virtual Trackpad we need relative coordinates so we capture the events from the
     * activity since we want to use the whole area for touch gestures therefore this should be
     * called from within the activity onTouchEvent callbacks
     *
     * @param event MotionEvent to be processed
     */
    public void processTrackPadEvents(MotionEvent event) {
        if (mouseMode == MouseMode.TOUCHSCREEN)
            return;
        mSurface.onTouchProcess(mSurface, event);
    }

    private void checkPendingActions() {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if (pendingStop) {
                    pendingStop = false;
                    QubeActivityCommon.promptStopVM(QubeQGEActivity.this, viewListener);
                }
            }
        }, 1000);

        machineRunning = true;
    }

    public void onBackPressed() {
        if (!QubeSettingsManager.getAlwaysShowMenuToolbar(this)) {
            ActionBar bar = getSupportActionBar();
            if (bar != null) {
                if (bar.isShowing())
                    bar.hide();
                else {
                    bar.show();
                }
            }
        } else {
            KeyboardUtils.hideKeyboard(this, mSurface);
            finish();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        invalidateOptionsMenu();
        updateLayout(newConfig.orientation);
    }

    public void updateLayout(int orientation) {
        if (orientation == Configuration.ORIENTATION_PORTRAIT)
            mGap.setVisibility(View.VISIBLE);
        else
            mGap.setVisibility(View.GONE);
    }

    protected void setupAudio() {
        if (am == null) {
            am = (AudioManager) mSingleton.getSystemService(Context.AUDIO_SERVICE);
            maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        }
    }

    public void setVolume(int volume) {
        if (am != null)
            am.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
    }

    protected int getCurrentVolume() {
        int volumeTmp = 0;
        if (am != null)
            volumeTmp = am.getStreamVolume(AudioManager.STREAM_MUSIC);
        return volumeTmp;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        Log.d(TAG, "onWindowFocusChanged: hasFocus=" + hasFocus + " gfxLoopRunning=" + gfxLoopRunning);
        // re-assert: mode switch is dropped if requested before the window has focus
        if (hasFocus && gfxLoopRunning) {
            applyRefreshRate(QubeSettingsManager.getRefreshRate(this));
        }
    }

    public void sendRightClick() {
        Thread t = new Thread(new Runnable() {
            public void run() {
                // we use a finger tool type to add the delay
                sendMouseEvent(Config.MOUSE_BUTTON_RIGHT, MotionEvent.ACTION_DOWN, MotionEvent.TOOL_TYPE_FINGER, 0, 0);
                sendMouseEvent(Config.MOUSE_BUTTON_RIGHT, MotionEvent.ACTION_UP, MotionEvent.TOOL_TYPE_FINGER, 0, 0);
            }
        });
        t.start();
    }

    public void sendMiddleClick() {
        Thread t = new Thread(new Runnable() {
            public void run() {
                // we use a finger tool type to add the delay
                sendMouseEvent(Config.MOUSE_BUTTON_MIDDLE, MotionEvent.ACTION_DOWN, MotionEvent.TOOL_TYPE_FINGER, 0, 0);
                sendMouseEvent(Config.MOUSE_BUTTON_MIDDLE, MotionEvent.ACTION_UP, MotionEvent.TOOL_TYPE_FINGER, 0, 0);
            }
        });
        t.start();
    }

    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        return false;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (!processKey(keyCode, event))
            return super.onKeyDown(keyCode, event);
        return true;
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (!processKey(keyCode, event))
            return super.onKeyUp(keyCode, event);
        return true;
    }

    public boolean processKey(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) || (keyCode == KeyEvent.KEYCODE_FORWARD)) {
            // dismiss android back and forward keys
            return true;
        } else if (event.getKeyCode() == KeyEvent.KEYCODE_MENU) {
            return false;
        } else if (event.getAction() == KeyEvent.ACTION_DOWN) {
            sendKey(event, keyCode, event.getAction(), true);
            // if user has a toggled key in VirtualKeys and clicked on keyboard letter, release the toggled key
            // same thing happens to physical keyboard
            if (mVirtualKeys != null && !KeyEvent.isModifierKey(keyCode))
                mVirtualKeys.releaseUnlockedMetaKeys();
            return true;
        } else if (event.getAction() == KeyEvent.ACTION_UP) {
            sendKey(event, keyCode, event.getAction(), false);
            return true;
        } else {
            return false;
        }
    }

    private synchronized void sendKey(final KeyEvent event, final int keyCode, final int action, final boolean down) {
        sendKeyEvent(event, keyCode, down);
    }

    private void delay(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * We treat as relative mode only events with TOOL_TYPE_FINGER as long as the user has not
     * selected to emulate a touch screen.
     *
     * @param toolType Event Tool type
     * @return True if the device will be expected as relative mode by the emulator
     */
    public boolean isRelativeMode(int toolType) {
        return toolType == MotionEvent.TOOL_TYPE_FINGER
                && QubeQGEActivity.mouseMode != QubeQGEActivity.MouseMode.TOUCHSCREEN;
    }

    protected void sendMouseEvent(int button, int action, int toolType, float x, float y) {
        //HACK: we generate an artificial delay since the qemu main event loop
        // is probably not able to process them if the timestamps are too close together?
        sendMouseEvent(button, action, toolType, x, y, action == MotionEvent.ACTION_UP ? Config.mouseButtonDelay : 0);
    }

    private void sendMouseEvent(final int button, final int action, final int toolType,
    final float x, final float y, final long delayMs) {
        if (resettingLayout)
            return;
        mouseEventsExecutor.submit(new Runnable() {
            @Override
            public void run() {
                if (delayMs > 0 && toolType != MotionEvent.TOOL_TYPE_MOUSE)
                    delay(delayMs);
                sendButton(button, action == MotionEvent.ACTION_DOWN);
                if (delayMs > 0 && toolType != MotionEvent.TOOL_TYPE_MOUSE)
                    delay(delayMs);
            }
        });
    }

    // pointer plumbing used by QubeQGESurface, trackpad goes out PS/2-relative,
    // touchscreen stays usb-tablet-absolute, relativePointerMode tracks which was used last
    private boolean relativePointerMode = (mouseMode == MouseMode.Trackpad);

    public synchronized void sendButton(int button, boolean down) {
        int bit = button == Config.MOUSE_BUTTON_LEFT ? 1
                : button == Config.MOUSE_BUTTON_MIDDLE ? 1 << 1
                : button == Config.MOUSE_BUTTON_RIGHT ? 1 << 2 : 0;
        if (down)
            pointerButtonMask |= bit;
        else
            pointerButtonMask &= ~bit;
        sendPointerEvent();
    }

    private void sendPointerEvent() {
        if (!machineRunning)
            return;
        if (relativePointerMode) {
            QubeInput.nativeSendPointerEventRel(0, 0, pointerButtonMask);
        } else {
            QubeInput.nativeSendPointerEvent((int) cursorX, (int) cursorY, pointerButtonMask,
                    QubeGfx.nativeGetWidth(), QubeGfx.nativeGetHeight());
        }
    }

    public synchronized void sendRelativeMove(float dx, float dy) {
        if (!machineRunning)
            return;
        relativePointerMode = true;
        int idx = Math.round(dx);
        int idy = Math.round(dy);
        if (idx == 0 && idy == 0)
            return;
        QubeInput.nativeSendPointerEventRel(idx, idy, pointerButtonMask);
    }

    public synchronized void sendAbsoluteMove(float x, float y) {
        if (!machineRunning)
            return;
        relativePointerMode = false;
        cursorX = clamp(x, 0, QubeGfx.nativeGetWidth() - 1);
        cursorY = clamp(y, 0, QubeGfx.nativeGetHeight() - 1);
        sendPointerEvent();
    }

    public synchronized void sendScroll(float amount) {
        if (!machineRunning || amount == 0)
            return;
        int wheelBit = amount > 0 ? 1 << 3 : 1 << 4;
        pointerButtonMask |= wheelBit;
        sendPointerEvent();
        pointerButtonMask &= ~wheelBit;
        sendPointerEvent();
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    protected void sendKeyEvent(KeyEvent event, int keycode, boolean down) {
        // HACK: generate an artificial delay since the qemu main event loop
        // is probably not able to process key events if the timestamps are too close together.
        sendKeyEvent(event, keycode, down, !down ? Config.keyDelay : 0);
    }

    private void sendKeyEvent(final KeyEvent event, final int keycode, final boolean down, final long delayMs) {
        keyEventsExecutor.submit(new Runnable() {
            @Override
            public void run() {
                if (delayMs > 0)
                    delay(delayMs);
                if (machineRunning) {
                    long keysym = KeySymMap.get(event, keycode);
                    if (keysym != 0)
                        QubeInput.nativeSendKeyEvent(keysym, down);
                }
            }
        });
    }

    @Override
    public void onMachineStatusChanged(Machine machine, MachineController.MachineStatus status, Object o) {
        if (status == MachineController.MachineStatus.Stopped) {
            SharedFolderServer.stopServer();
        }
    }

    public void notifyFieldChange(MachineProperty property, Object value) {
        if (viewListener != null)
            viewListener.onFieldChange(property, value);
    }

    public void notifyAction(MachineAction action, Object value) {
        if (viewListener != null)
            viewListener.onAction(action, value);
    }

    @Override
    public void onEvent(Machine machine, MachineController.Event event, Object o) {
        switch (event) {
        }
    }

    public synchronized void setFullscreen() {
        if (!machineRunning) {
            Log.w(TAG, "Machine not running not reset layout");
            return;
        }
        resettingLayout = true;
        try {
            Log.d(TAG, "Requesting fullscreen");
            viewListener.onAction(MachineAction.FULLSCREEN, null);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        resettingLayout = false;
    }

    public enum MouseMode {
        Trackpad, TOUCHSCREEN
    }
}
