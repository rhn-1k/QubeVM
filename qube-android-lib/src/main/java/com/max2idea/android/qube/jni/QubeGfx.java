package com.max2idea.android.qube.jni;

import android.graphics.Bitmap;

// QGE framebuffer bridge to qemu DisplaySurface

public class QubeGfx {

    // Returns current guest display width, or 0 if no frame yet
    public static native int nativeGetWidth();

    // Returns current guest display height, or 0 if no frame yet
    public static native int nativeGetHeight();

    // Copies guest frame into ARGB_8888 bitmap, returns generation counter (0 if unavailable)
    public static native int nativeCopyFrame(Bitmap bitmap);

    // Pushes refresh rate (Hz) to qemu's frame pump, takes effect on next tick
    public static native void nativeSetRefreshRate(int hz);
}
