package com.max2idea.android.qube.utils;

import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.max2idea.android.qube.main.QubeSettingsManager;
import com.qube.emu.lib.R;

/**
 * Small, reusable helper for configuring an Activity's Toolbar / ActionBar.
 * <p>
 * Replaces the previously duplicated setupToolbar()/setupToolBar() methods
 * found in QubeActivity, QubeQGEActivity and the inline toolbar setup in
 * VirtualKeysEditorActivity / QubeSettingsManager.
 */
public class ToolbarUtils {

    private ToolbarUtils() {
    }

    public static Config config() {
        return new Config();
    }

    /**
     * Sets up the Activity's toolbar/action bar according to the given config
     * and returns the resolved Toolbar view (useful if the caller needs to do
     * anything else with it).
     */
    public static Toolbar setup(AppCompatActivity activity, Config cfg) {
        Toolbar tb = activity.findViewById(R.id.toolbar);
        activity.setSupportActionBar(tb);

        ActionBar ab = activity.getSupportActionBar();
        if (ab != null) {
            ab.setDisplayShowHomeEnabled(cfg.showHome);
            ab.setDisplayHomeAsUpEnabled(cfg.homeAsUp);
            ab.setDisplayShowCustomEnabled(cfg.showCustom);
            ab.setDisplayShowTitleEnabled(cfg.showTitle);
            ab.setTitle(cfg.title != null
                    ? cfg.title
                    : activity.getApplicationInfo().loadLabel(activity.getPackageManager()));

            if (cfg.hideIfMenuToolbarSettingOff
                    && !QubeSettingsManager.getAlwaysShowMenuToolbar(activity)) {
                ab.hide();
            }
        }

        if (tb != null && cfg.navigationClickListener != null) {
            tb.setNavigationOnClickListener(v -> cfg.navigationClickListener.run());
        }

        return tb;
    }

    /**
     * Fluent config object for {@link #setup(AppCompatActivity, Config)}.
     * Defaults match the plain "main screen" toolbar (no home/up icon, app
     * label as title, always shown).
     */
    public static class Config {
        boolean showHome = false;
        boolean homeAsUp = false;
        boolean showCustom = true;
        boolean showTitle = true;
        CharSequence title;
        boolean hideIfMenuToolbarSettingOff = false;
        Runnable navigationClickListener;

        public Config showHome(boolean value) {
            showHome = value;
            return this;
        }

        public Config homeAsUp(boolean value) {
            homeAsUp = value;
            return this;
        }

        public Config showCustom(boolean value) {
            showCustom = value;
            return this;
        }

        public Config showTitle(boolean value) {
            showTitle = value;
            return this;
        }

        public Config title(CharSequence title) {
            this.title = title;
            return this;
        }

        public Config title(AppCompatActivity activity, @StringRes int titleResId) {
            this.title = activity.getString(titleResId);
            return this;
        }

        /**
         * When true, the ActionBar is hidden unless
         * QubeSettingsManager.getAlwaysShowMenuToolbar() is true.
         * Used by QubeQGEActivity.
         */
        public Config hideIfMenuToolbarSettingOff(boolean value) {
            hideIfMenuToolbarSettingOff = value;
            return this;
        }

        /**
         * Wires the toolbar's navigation (back arrow) click. Typically
         * activity::onBackPressed.
         */
        public Config onNavigationClick(Runnable listener) {
            navigationClickListener = listener;
            return this;
        }
    }
}
