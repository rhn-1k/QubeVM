package com.max2idea.android.qube.jni;

public class QubeAudio {

    public static native boolean nativeStartAudio();

    // Stop and close AAudio stream
    public static native void nativeStopAudio();
}
