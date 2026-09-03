/*
Copyright (C) Max Kastanas 2012
Copyright (C) Rhn 2026
 */
package com.max2idea.android.qube.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.qube.emu.lib.R;
import com.max2idea.android.qube.files.FileUtils;

import java.util.ArrayList;

/** Custom SpinnerAdapter that shows alternative display values than the ones it was originally
 *  initialized with. The alternative values that are displayed are a compact version of the file
 *  uris that are retrieved by the Android Storage Framework.
 */
public class SpinnerAdapter extends ArrayAdapter<String> {
    private static final String TAG = "SpinnerAdapter";

    int index;
    public SpinnerAdapter(Context context, int layout, ArrayList<String> items, int index) {
        super(context, layout, items);
        this.index = index;
    }

    public static int getItemPosition(Spinner spinner, String value) {
        for(int i =0; i<spinner.getCount(); i++) {
            String item = (String) spinner.getItemAtPosition(i);
            if(item.equals(value))
                return i;
        }
        return -1;
    }

    public static void addItem(Spinner spinner, String value) {
        ((ArrayAdapter<String>) spinner.getAdapter()).add(value);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        View view = super.getDropDownView(position, convertView, parent);
        if (position >= index) {
            TextView textView = (TextView) view.findViewById(R.id.customSpinnerDropDownItem);
            String textStr = FileUtils.convertFilePath(textView.getText() + "", position);
            textView.setText(textStr);
        }
        return view;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        if (position >= index) {
            TextView textView = (TextView) view.findViewById(R.id.customSpinnerItem);
            String textStr = FileUtils.convertFilePath(textView.getText() + "", position);
            textView.setText(textStr);
        }
        return view;
    }

    public static int getPositionFromSpinner(Spinner spinner, String value) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).equals(value))
                return i;
        }
        return -1;
    }

    public static void setDiskAdapterValue(final Spinner spinner, final String value) {
        spinner.post(new Runnable() {
            public void run() {
                if (value != null) {
                    int pos = SpinnerAdapter.getPositionFromSpinner(spinner, value);
                    spinner.setSelection(Math.max(pos, 0));
                } else {
                    spinner.setSelection(0);
                }
            }
        });
    }

}
