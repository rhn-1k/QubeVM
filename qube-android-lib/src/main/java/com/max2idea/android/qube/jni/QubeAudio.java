package com.max2idea.android.qube.jni;

// AAudio playback bridge (48kHz/stereo/S16 for QEMU)

public class QubeAudio {

    // Start or recover AAudio stream, callback pulls from QEMU ring and fills underruns with silence
    public static native boolean nativeStartAudio();

    // Stop and close AAudio, flush QEMU audio, allow restart
    public static native void nativeStopAudio();
}
