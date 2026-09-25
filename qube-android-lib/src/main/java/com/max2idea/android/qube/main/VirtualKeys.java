/*
This is a ported code from the AVNC project
Copyright (C) Gaurav Ujjwal 2020
Copyright (C) Rhn 2026
*/
package com.max2idea.android.qube.main;

import android.app.Activity;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ToggleButton;

import com.max2idea.android.qube.keyboard.KeyboardUtils;
import com.qube.emu.lib.R;

import java.util.HashSet;
import java.util.Set;

public class VirtualKeys {

    // Callback used to forward on-screen virtual key presses to whoever owns the emulator input.
    public interface OnSendKeyEventListener {
        void onSendKeyEvent(int keyCode, boolean down);
    }

    private final Activity activity;
    private final View mSurface;
    private final OnSendKeyEventListener listener;
    private final HorizontalScrollView container;
    private final GridLayout keysGrid;
    private final Set<ToggleButton> toggleKeys = new HashSet<>();
    private final Set<ToggleButton> lockedToggleKeys = new HashSet<>();

    public VirtualKeys(Activity activity, View mSurface, HorizontalScrollView container,
                        OnSendKeyEventListener listener) {
        this.activity = activity;
        this.mSurface = mSurface;
        this.container = container;
        this.listener = listener;
        this.keysGrid = container.findViewById(R.id.virtual_keys);
        initKeys();
    }

    public boolean isShown() {
        return container.getVisibility() == View.VISIBLE;
    }

    public void show() {
        container.setVisibility(View.VISIBLE);
    }

    public void hide() {
        container.setVisibility(View.GONE);
    }

    public boolean toggle() {
        if (isShown()) {
            hide();
            return false;
        } else {
            show();
            return true;
        }
    }

    // Releases all modifier keys, e.g. after a key combo has been sent
    public void releaseMetaKeys() {
        for (ToggleButton key : toggleKeys) {
            if (key.isChecked())
                key.setChecked(false);
        }
    }

    public void releaseUnlockedMetaKeys() {
        for (ToggleButton key : toggleKeys) {
            if (key.isChecked() && !lockedToggleKeys.contains(key))
                key.setChecked(false);
        }
    }

    // Rebuilds the key row from the current user layout/row-count preference.
    // Call this after returning from VirtualKeysEditorActivity.
    public void reloadLayout() {
        keysGrid.removeAllViews();
        toggleKeys.clear();
        lockedToggleKeys.clear();
        initKeys();
    }

    private void initKeys() {
        keysGrid.setRowCount(QubeSettingsManager.getVkRowCount(activity));
        for (VirtualKey vk : VirtualKeyLayoutConfig.getLayout(activity)) {
            View view = VirtualKeyViewFactory.create(activity, vk);
            keysGrid.addView(view);

            if (vk == VirtualKey.ToggleKeyboard) {
                view.setOnClickListener(v ->
                        QubeQGEActivity.toggleKeyboardFlag =
                                KeyboardUtils.showKeyboard(activity, QubeQGEActivity.toggleKeyboardFlag, mSurface));
            } else if (vk == VirtualKey.CloseKeys) {
                view.setOnClickListener(v -> hide());
            } else if (vk.keyCode != null) {
                if (view instanceof ToggleButton)
                    initToggleKey((ToggleButton) view, vk.keyCode);
                else
                    initNormalKey(view, vk.keyCode);
            }
        }
    }

    private void initToggleKey(final ToggleButton key, final int keyCode) {
        key.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sendKey(keyCode, isChecked);
            if (!isChecked) lockedToggleKeys.remove(key);
        });
        key.setOnLongClickListener(v -> {
            key.setChecked(!key.isChecked());
            if (key.isChecked()) {
                lockedToggleKeys.add(key);
                v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            }
            return true;
        });

        if ((keyCode == KeyEvent.KEYCODE_META_LEFT || keyCode == KeyEvent.KEYCODE_META_RIGHT)
                && QubeSettingsManager.getVkUseSuperWithSingleTap(activity)) {
            key.setOnClickListener(v -> {
                key.setChecked(true);
                key.setChecked(false);
            });
        }

        toggleKeys.add(key);
    }

    private void initNormalKey(final View key, final int keyCode) {
        key.setOnClickListener(v -> sendKey(keyCode));
        makeKeyRepeatable(key);
    }

    // As long as finger stays on the key, we keep repeating clicks, like a physical keyboard
    private void makeKeyRepeatable(final View keyView) {
        keyView.setOnTouchListener(new View.OnTouchListener() {
            private boolean doRepeat = false;

            private void repeat(View v) {
                if (doRepeat) {
                    v.performClick();
                    v.postDelayed(() -> repeat(v), ViewConfiguration.getKeyRepeatDelay());
                }
            }

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        doRepeat = true;
                        v.postDelayed(() -> repeat(v), ViewConfiguration.getKeyRepeatTimeout());
                        break;
                    case MotionEvent.ACTION_POINTER_DOWN:
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        doRepeat = false;
                        break;
                }
                return false;
            }
        });
    }

    private void sendKey(int keyCode) {
        sendKey(keyCode, true);
        sendKey(keyCode, false);
        if (!KeyEvent.isModifierKey(keyCode))
            releaseUnlockedMetaKeys();
    }

    private void sendKey(int keyCode, boolean down) {
        if (listener != null)
            listener.onSendKeyEvent(keyCode, down);
    }
}
