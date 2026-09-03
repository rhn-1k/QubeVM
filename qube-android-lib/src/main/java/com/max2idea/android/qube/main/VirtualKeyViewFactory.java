/*
This is a ported code from the AVNC project
Copyright (C) Gaurav Ujjwal 2020
Copyright (C) Rhn 2026
*/
package com.max2idea.android.qube.main;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ToggleButton;

import com.qube.emu.lib.R;

public class VirtualKeyViewFactory {

    // ToggleButton if key is a toggle
    // ImageButton if key has an icon (label will be ignored)
    // Button in all other cases
    public static View create(Context context, VirtualKey key) {
        View view = key.isToggle ? createToggle(context, key) : createSimple(context, key);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = GridLayout.LayoutParams.WRAP_CONTENT;
        params.height = GridLayout.LayoutParams.WRAP_CONTENT;
        params.setGravity(Gravity.CENTER);
        view.setLayoutParams(params);
        return view;
    }

    private static View createSimple(Context context, VirtualKey key) {
        if (key.icon != 0) {
            ImageButton view = new ImageButton(context, null, 0, selectStyle(key));
            view.setImageResource(key.icon);
            view.setContentDescription(key.getDescription());
            return view;
        } else {
            Button view = new Button(context, null, 0, selectStyle(key));
            view.setText(key.getLabel());
            return view;
        }
    }

    private static View createToggle(Context context, VirtualKey key) {
        ToggleButton view = new ToggleButton(context, null, 0, selectStyle(key));
        view.setClickable(true);

        if (key.icon != 0) {
            view.setCompoundDrawablesRelativeWithIntrinsicBounds(key.icon, 0, 0, 0);
            view.setContentDescription(key.getDescription());
        } else {
            String label = key.getLabel();
            view.setText(label);
            view.setTextOff(label);
            view.setTextOn(label);
        }
        return view;
    }

    private static int selectStyle(VirtualKey key) {
        if (key == VirtualKey.CloseKeys || key == VirtualKey.ToggleKeyboard)
            return R.style.VirtualKey_Special;

        if (key.isToggle)
            return key.icon != 0 ? R.style.VirtualKey_Toggle_Image : R.style.VirtualKey_Toggle;

        return R.style.VirtualKey;
    }
}
