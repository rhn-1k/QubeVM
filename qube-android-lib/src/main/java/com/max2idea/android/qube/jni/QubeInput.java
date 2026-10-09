package com.max2idea.android.qube.jni;

// QGE input bridge to qemu

public class QubeInput {

    // Sends key event using X11 keysym, down true for press
    public static native void nativeSendKeyEvent(long keysym, boolean down);

    // Sends absolute pointer event in guest coordinates, buttonMask bits: left/mid/right/wheel-up/down
    public static native void nativeSendPointerEvent(int x, int y, int buttonMask, int width, int height);

    // Switches the keyboard layout of the running vm, false when the keymap can't be loaded
    public static native boolean nativeSetKeyboardLayout(String layout);

    // Sends relative pointer event (PS/2), dx/dy are deltas
    public static native void nativeSendPointerEventRel(int dx, int dy, int buttonMask);
}