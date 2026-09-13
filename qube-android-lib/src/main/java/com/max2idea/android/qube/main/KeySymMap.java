/*
Copyright (C) Rhn 2026
 */
package com.max2idea.android.qube.main;

import android.view.KeyCharacterMap;
import android.view.KeyEvent;

// sends X11 keysyms, not Android keycodes/scancodes, printables via KeyCharacterMap, layout/shift resolved by Android
// rest via fixed table lookup
public class KeySymMap {

    public static long get(KeyEvent event, int keyCode) {
        long fixed = fixedKeysym(keyCode);
        if (fixed != 0)
            return fixed;

        int unicodeChar = event != null ? event.getUnicodeChar() : 0;
        if (unicodeChar == 0) {
            KeyCharacterMap map = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD);
            unicodeChar = map.get(keyCode, 0);
        }
        if (unicodeChar != 0)
            return unicodeToKeysym(unicodeChar);

        return 0;
    }

    // Latin-1 codepoints map 1:1 onto their keysym below 0x100, everything above
    // that is handled via the 0x01000000 Unicode keysym range (RFC/X11 convention).
    private static long unicodeToKeysym(int unicodeChar) {
        if (unicodeChar < 0x100)
            return unicodeChar;
        return 0x01000000L | unicodeChar;
    }

    private static long fixedKeysym(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DEL: return 0xFF08; // Backspace
            case KeyEvent.KEYCODE_FORWARD_DEL: return 0xFFFF; // Delete
            case KeyEvent.KEYCODE_TAB: return 0xFF09;
            case KeyEvent.KEYCODE_ENTER: return 0xFF0D;
            case KeyEvent.KEYCODE_ESCAPE: return 0xFF1B;
            case KeyEvent.KEYCODE_SPACE: return 0x0020;
            case KeyEvent.KEYCODE_MOVE_HOME: return 0xFF50;
            case KeyEvent.KEYCODE_MOVE_END: return 0xFF57;
            case KeyEvent.KEYCODE_PAGE_UP: return 0xFF55;
            case KeyEvent.KEYCODE_PAGE_DOWN: return 0xFF56;
            case KeyEvent.KEYCODE_DPAD_LEFT: return 0xFF51;
            case KeyEvent.KEYCODE_DPAD_UP: return 0xFF52;
            case KeyEvent.KEYCODE_DPAD_RIGHT: return 0xFF53;
            case KeyEvent.KEYCODE_DPAD_DOWN: return 0xFF54;
            case KeyEvent.KEYCODE_INSERT: return 0xFF63;
            case KeyEvent.KEYCODE_SYSRQ: return 0xFF61;
            case KeyEvent.KEYCODE_BREAK: return 0xFF6B;
            case KeyEvent.KEYCODE_CAPS_LOCK: return 0xFFE5;
            case KeyEvent.KEYCODE_NUM_LOCK: return 0xFF7F;
            case KeyEvent.KEYCODE_SCROLL_LOCK: return 0xFF14;
            case KeyEvent.KEYCODE_SHIFT_LEFT: return 0xFFE1;
            case KeyEvent.KEYCODE_SHIFT_RIGHT: return 0xFFE2;
            case KeyEvent.KEYCODE_CTRL_LEFT: return 0xFFE3;
            case KeyEvent.KEYCODE_CTRL_RIGHT: return 0xFFE4;
            case KeyEvent.KEYCODE_ALT_LEFT: return 0xFFE9;
            case KeyEvent.KEYCODE_ALT_RIGHT: return 0xFFEA;
            case KeyEvent.KEYCODE_META_LEFT: return 0xFFEB; // Super_L
            case KeyEvent.KEYCODE_META_RIGHT: return 0xFFEC; // Super_R
            case KeyEvent.KEYCODE_MENU: return 0xFF67;
            case KeyEvent.KEYCODE_F1: return 0xFFBE;
            case KeyEvent.KEYCODE_F2: return 0xFFBF;
            case KeyEvent.KEYCODE_F3: return 0xFFC0;
            case KeyEvent.KEYCODE_F4: return 0xFFC1;
            case KeyEvent.KEYCODE_F5: return 0xFFC2;
            case KeyEvent.KEYCODE_F6: return 0xFFC3;
            case KeyEvent.KEYCODE_F7: return 0xFFC4;
            case KeyEvent.KEYCODE_F8: return 0xFFC5;
            case KeyEvent.KEYCODE_F9: return 0xFFC6;
            case KeyEvent.KEYCODE_F10: return 0xFFC7;
            case KeyEvent.KEYCODE_F11: return 0xFFC8;
            case KeyEvent.KEYCODE_F12: return 0xFFC9;
            default: return 0;
        }
    }
}
