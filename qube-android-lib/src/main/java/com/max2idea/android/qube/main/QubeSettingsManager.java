/*
Copyright (C) Max Kastanas 2012
Copyright (C) Rhn 2026
 */
package com.max2idea.android.qube.main;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.qube.emu.lib.R;
import com.max2idea.android.qube.toast.ToastUtils;
import com.max2idea.android.qube.machine.MachineController;
import com.max2idea.android.qube.machine.MachineFilePaths;



public class QubeSettingsManager extends AppCompatActivity {
    private static final String TAG = "QubeSettingsManager";
    private SettingsFragment settingsFragment;

    // DNS server
    static String getDNSServer(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString("dnsServer", Config.defaultDNSServer);
    }

    public static void setDNSServer(Context context, String dnsServer) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString("dnsServer", dnsServer);
        edit.apply();
    }

    // Screen
    public static int getOrientationSetting(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int orientation = Integer.parseInt(prefs.getString("orientationPref", "0"));
        return orientation;
    }

    public static boolean getAlwaysShowMenuToolbar(Context activity) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        return prefs.getBoolean("AlwaysShowMenuToolbar", false);
    }

    public static boolean getFullscreen(Context activity) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        return prefs.getBoolean("ShowFullscreen", true);
    }

    // updates
    public static boolean getPromptUpdateVersion(Context context) {
        if(!Config.enableSoftwareUpdates)
            return false;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("updateVersionPrompt", Config.defaultCheckNewVersion);
    }

    public static void setPromptUpdateVersion(Context context, boolean value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean("updateVersionPrompt", value);
        edit.apply();
    }

    // files
    // REMOVED: getEnableLegacyFileManager - no longer needed, we always use SAF

    public static String getLastDir(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString("lastDir", null);
    }

    public static void setLastDir(Context context, String imagesPath) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString("lastDir", imagesPath);
        edit.apply();
    }

    public static String getImagesDir(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString("imagesDir", null);
    }

    public static void setImagesDir(Context context, String imagesPath) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString("imagesDir", imagesPath);
        edit.apply();
    }

    public static String getSharedDir(Context context) {
        String lastDir = Environment.getExternalStorageDirectory().getPath();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString("sharedDir", lastDir);
    }

    public static void setSharedDir(Context context, String lastDir) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString("sharedDir", lastDir);
        edit.apply();
    }

    // exit code
    public static int getExitCode(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getInt("exitCode", Config.EXIT_SUCCESS);
    }

    //XXX: we need to make sure this gets written to preferences right before we call System.exit()
    // so we need to use commit instead of apply
    @SuppressLint("ApplySharedPref")
    public static void setExitCode(Context context, int exitCode) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putInt("exitCode", exitCode);
        edit.commit();
    }

    public static boolean isFirstLaunch(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("firstTime" + QubeApplication.getQubeVersionString(), true);
    }

    public static void setFirstLaunch(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean("firstTime" + QubeApplication.getQubeVersionString(), false);
        edit.apply();
    }

    // VNC
    public static boolean getEnableExternalVNC(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("enableExternalVNC", false);
    }

    // QMP
    public static boolean getEnableQmp(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("enableQMP", true);
    }

    public static boolean getEnableExternalQMP(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("enableExternalQMP", false);
    }

    public static int getKeyPressDelay(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String sizeStr = prefs.getString("keyPressDelay", "100");
        return Integer.parseInt(sizeStr);
    }

    public static int getMouseButtonDelay(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String sizeStr = prefs.getString("mouseButtonDelay", "100");
        return Integer.parseInt(sizeStr);
    }

    // Refresh Rate (Hz), drives both qemu's frame pump (QubeGfx.nativeSetRefreshRate)
    public static int getRefreshRate(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String rateStr = prefs.getString("refreshRatePref", "60");
        return Integer.parseInt(rateStr);
    }

    // Pointer Speed scale for trackpad/relative mouse movement (PointerAcceleration).
    // Stored as a percentage so it works as a plain ListPreference
    public static float getPointerSpeed(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String pctStr = prefs.getString("pointerSpeedPref", "100");
        int pct;
        try {
            pct = Integer.parseInt(pctStr);
        } catch (NumberFormatException e) {
            pct = 100;
        }
        return pct / 100f;
    }

    public static void setRefreshRate(Context context, int hz) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString("refreshRatePref", String.valueOf(hz));
        edit.apply();
    }

    // Whether the user (or the first-launch auto-detect below) has ever set this explicitly,
    // vs. still sitting on the "60" fallback default from getRefreshRate() above.
    public static boolean hasRefreshRatePref(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.contains("refreshRatePref");
    }

    public static String getDiskCache(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString("diskCachePref", context.getString(R.string.Default));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setupEdgeToEdgeToolbar();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.Settings);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        Intent data = new Intent();
        setResult(Config.SETTINGS_RETURN_CODE, data);

        settingsFragment = new SettingsFragment();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_container, settingsFragment)
                .commit();
    }

    private void setupEdgeToEdgeToolbar() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        View root = findViewById(R.id.settings_root);
        View appBar = findViewById(R.id.settings_top_app_bar);
        View content = findViewById(R.id.settings_container);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), root);
        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(false);
        getWindow().setNavigationBarContrastEnforced(false);
        final int appBarLeft = appBar.getPaddingLeft();
        final int appBarTop = appBar.getPaddingTop();
        final int appBarRight = appBar.getPaddingRight();
        final int appBarBottom = appBar.getPaddingBottom();
        android.view.ViewGroup.MarginLayoutParams appBarParams =
                (android.view.ViewGroup.MarginLayoutParams) appBar.getLayoutParams();
        final int appBarLeftMargin = appBarParams.leftMargin;
        final int appBarRightMargin = appBarParams.rightMargin;
        final int contentLeft = content.getPaddingLeft();
        final int contentTop = content.getPaddingTop();
        final int contentRight = content.getPaddingRight();
        final int contentBottom = content.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(appBar, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            android.view.ViewGroup.MarginLayoutParams params =
                    (android.view.ViewGroup.MarginLayoutParams) view.getLayoutParams();
            params.leftMargin = appBarLeftMargin + bars.left;
            params.rightMargin = appBarRightMargin + bars.right;
            view.setLayoutParams(params);
            view.setPadding(appBarLeft, appBarTop + bars.top,
                    appBarRight, appBarBottom);
            return insets;
        });
        ViewCompat.setOnApplyWindowInsetsListener(content, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(contentLeft + bars.left, contentTop,
                    contentRight + bars.right, contentBottom + bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
    }

    public void onPreferencesCreated() {
        Preference clearDirHistory = settingsFragment.findPreference("clearDirHistory");
        if (clearDirHistory != null) {
            clearDirHistory.setOnPreferenceClickListener(preference -> {
                promptClearDirHistory(QubeSettingsManager.this);
                return true;
            });
        }

        // Virtual Keys Customize layout
        Preference customizeVirtualKeys = settingsFragment.findPreference("customizeVirtualKeys");
        if (customizeVirtualKeys != null) {
            customizeVirtualKeys.setOnPreferenceClickListener(preference -> {
                startActivity(new Intent(QubeSettingsManager.this, VirtualKeysEditorActivity.class));
                return true;
            });
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.settings, rootKey);

            if (Config.enableSoftwareUpdates)
                mergePreferencesFromResource(R.xml.software_updates);

            if (getActivity() instanceof QubeSettingsManager) {
                ((QubeSettingsManager) getActivity()).onPreferencesCreated();
            }
        }

        @Override
        public void onDisplayPreferenceDialog(@androidx.annotation.NonNull Preference preference) {
            if (preference instanceof androidx.preference.ListPreference) {
                showListPreferenceDialog((androidx.preference.ListPreference) preference);
            } else {
                super.onDisplayPreferenceDialog(preference);
            }
        }

        private void showListPreferenceDialog(final androidx.preference.ListPreference preference) {
            final CharSequence[] entries = preference.getEntries();
            final CharSequence[] entryValues = preference.getEntryValues();
            if (entries == null || entryValues == null || entries.length != entryValues.length) {
                super.onDisplayPreferenceDialog(preference);
                return;
            }

            int checkedIndex = preference.findIndexOfValue(preference.getValue());
            CharSequence dialogTitle = preference.getDialogTitle() != null
                    ? preference.getDialogTitle()
                    : preference.getTitle();

            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(dialogTitle)
                    .setSingleChoiceItems(entries, checkedIndex, (dialog, which) -> {
                        String newValue = entryValues[which].toString();
                        if (preference.callChangeListener(newValue)) {
                            preference.setValue(newValue);
                        }
                        dialog.dismiss();
                    })
                    .setNegativeButton(getString(R.string.Cancel), null);

            if (preference.getDialogIcon() != null) {
                builder.setIcon(preference.getDialogIcon());
            }

            builder.show();
        }

        // addPreferencesFromResource() replaces the current PreferenceScreen rather than
        // merging into it, so extra XML files are inflated separately and their top-level
        // categories moved into the main screen instead.
        private void mergePreferencesFromResource(int resId) {
            androidx.preference.PreferenceScreen extraScreen =
                    getPreferenceManager().inflateFromResource(requireContext(), resId, null);
            androidx.preference.PreferenceScreen mainScreen = getPreferenceScreen();
            while (extraScreen.getPreferenceCount() > 0) {
                androidx.preference.Preference pref = extraScreen.getPreference(0);
                extraScreen.removePreference(pref);
                mainScreen.addPreference(pref);
            }
        }
    }

    public void promptClearDirHistory(final Activity activity) {
        new MaterialAlertDialogBuilder(activity)
                .setTitle(getString(R.string.ClearDirHistory))
                .setMessage(getString(R.string.ClearDirHistoryConfirm))
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    MachineFilePaths.clearAllRecentFilePaths();
                    ToastUtils.toastShort(activity, getString(R.string.DirHistoryCleared));
                })
                .setNegativeButton(getString(R.string.Cancel), null)
                .show();
    }

    // Virtual Keys
    public static boolean getVkShowAll(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("vkShowAll", false);
    }

    public static boolean getVkOpenWithKeyboard(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("vkOpenWithKeyboard", false);
    }

    public static boolean getVkUseSuperWithSingleTap(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("vkUseSuperWithSingleTap", false);
    }

    public static int getVkRowCount(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return Integer.parseInt(prefs.getString("vkRowCount", "2"));
    }

    // Comma separated list of VirtualKey names, or null if user has not customized the layout
    public static String getVkLayout(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString("vkLayout", null);
    }

    public static void setVkLayout(Context context, String layout) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        if (layout == null)
            edit.remove("vkLayout");
        else
            edit.putString("vkLayout", layout);
        edit.apply();
    }

}
