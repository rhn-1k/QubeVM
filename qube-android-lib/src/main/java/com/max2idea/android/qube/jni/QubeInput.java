package com.max2idea.android.qube.jni;

// QGE input bridge to qemu

public class QubeInput {

    // Sends key event using X11 keysym, down true for press
    public static native void nativeSendKeyEvent(long keysym, boolean down);

    // Sends absolute pointer event in guest coordinates, buttonMask bits: left/mid/right/wheel-up/down
    public static native void nativeSendPointerEvent(int x, int y, int buttonMask, int width, int height);

    // Sends relative pointer event (PS/2), dx/dy are deltas
    public static native void nativeSendPointerEventRel(int dx, int dy, int buttonMask);
}