/*
This is a ported code from the AVNC project
Copyright (C) Gaurav Ujjwal 2020
Copyright (C) Rhn 2026
*/
package com.max2idea.android.qube.main;

import android.view.KeyEvent;
import com.qube.emu.lib.R;

public enum VirtualKey {

    // Special actions
    ToggleKeyboard(null, null, R.drawable.keyboard_24px, "Toggle keyboard", false),
    CloseKeys(null, null, R.drawable.close_24px, "Close virtual keys", false),

    // Meta keys
    LeftShift(KeyEvent.KEYCODE_SHIFT_LEFT, "Shift", 0, null, true),
    LeftCtrl(KeyEvent.KEYCODE_CTRL_LEFT, "Ctrl", 0, null, true),
    LeftAlt(KeyEvent.KEYCODE_ALT_LEFT, "Alt", 0, null, true),
    LeftSuper(KeyEvent.KEYCODE_META_LEFT, "Super", R.drawable.super_key_24px, null, true),

    Esc(KeyEvent.KEYCODE_ESCAPE, null, 0, null, false),
    Tab(KeyEvent.KEYCODE_TAB, null, 0, null, false),
    Home(KeyEvent.KEYCODE_MOVE_HOME, null, 0, null, false),
    End(KeyEvent.KEYCODE_MOVE_END, null, 0, null, false),
    PgUp(KeyEvent.KEYCODE_PAGE_UP, "PgUp", 0, null, false),
    PgDn(KeyEvent.KEYCODE_PAGE_DOWN, "PgDn", 0, null, false),
    Insert(KeyEvent.KEYCODE_INSERT, "Ins", 0, null, false),
    Delete(KeyEvent.KEYCODE_FORWARD_DEL, "Del", 0, null, false),

    // Arrow keys
    Left(KeyEvent.KEYCODE_DPAD_LEFT, null, R.drawable.keyboard_arrow_left_24px, null, false),
    Right(KeyEvent.KEYCODE_DPAD_RIGHT, null, R.drawable.keyboard_arrow_right_24px, null, false),
    Up(KeyEvent.KEYCODE_DPAD_UP, null, R.drawable.keyboard_arrow_up_24px, null, false),
    Down(KeyEvent.KEYCODE_DPAD_DOWN, null, R.drawable.keyboard_arrow_down_24px, null, false),

    F1(KeyEvent.KEYCODE_F1, null, 0, null, false),
    F2(KeyEvent.KEYCODE_F2, null, 0, null, false),
    F3(KeyEvent.KEYCODE_F3, null, 0, null, false),
    F4(KeyEvent.KEYCODE_F4, null, 0, null, false),
    F5(KeyEvent.KEYCODE_F5, null, 0, null, false),
    F6(KeyEvent.KEYCODE_F6, null, 0, null, false),
    F7(KeyEvent.KEYCODE_F7, null, 0, null, false),
    F8(KeyEvent.KEYCODE_F8, null, 0, null, false),
    F9(KeyEvent.KEYCODE_F9, null, 0, null, false),
    F10(KeyEvent.KEYCODE_F10, null, 0, null, false),
    F11(KeyEvent.KEYCODE_F11, null, 0, null, false),
    F12(KeyEvent.KEYCODE_F12, null, 0, null, false);

    // KeyEvent keycode to be generated when this key is pressed. Null for special actions.
    public final Integer keyCode;
    // If key name is not appropriate for UI, use this to set the label.
    public final String label;
    // If icon is set (non-zero), this key will be rendered as an ImageButton.
    public final int icon;
    // Short description of the key, if the label itself isn't sufficient.
    public final String description;
    public final boolean isToggle;

    VirtualKey(Integer keyCode, String label, int icon, String description, boolean isToggle) {
        this.keyCode = keyCode;
        this.label = label;
        this.icon = icon;
        this.description = description;
        this.isToggle = isToggle;
    }

    public String getLabel() {
        return label != null ? label : name();
    }

    public String getDescription() {
        return description != null ? description : getLabel();
    }
}
