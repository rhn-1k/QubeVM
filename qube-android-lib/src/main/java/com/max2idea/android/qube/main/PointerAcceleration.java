/*
Copyright (C) Rhn 2026
 */
package com.max2idea.android.qube.main;

import android.content.Context;

// Pointer logic using simple speed scaling
// Scale comes from QubeSettingsManager.getPointerSpeed() (Settings > Pointer Speed)
public class PointerAcceleration {
    private final float scale;
    private float scale_accum_x = 0f;
    private float scale_accum_y = 0f;

    public PointerAcceleration(Context context) {
        scale = QubeSettingsManager.getPointerSpeed(context);
    }

    public void clear() {
        scale_accum_x = 0f;
        scale_accum_y = 0f;
    }

    public float updateDx(float dx) {
        return getScaledDelta(dx, true);
    }

    public float updateDy(float dy) {
        return getScaledDelta(dy, false);
    }

    // Accumulates the scaled fractional delta and only emits whole pixels, carrying the
    // remainder forward so low-scale sub-pixel movement isn't rounded away every call
    private float getScaledDelta(float value, boolean isX) {
        if (isX) {
            scale_accum_x += scale * value;
            float result = scale_accum_x >= 0.0f
                    ? (float) Math.floor(scale_accum_x)
                    : (float) Math.ceil(scale_accum_x);
            scale_accum_x -= result;
            return result;
        } else {
            scale_accum_y += scale * value;
            float result = scale_accum_y >= 0.0f
                    ? (float) Math.floor(scale_accum_y)
                    : (float) Math.ceil(scale_accum_y);
            scale_accum_y -= result;
            return result;
        }
    }
}
