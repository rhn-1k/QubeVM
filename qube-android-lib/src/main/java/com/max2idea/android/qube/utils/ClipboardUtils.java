package com.max2idea.android.qube.utils;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.widget.Toast;

public class ClipboardUtils {

    public static void copyToClipboard(Context context, String label, String text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText(label, text));
        }
        // Android 13+ shows its own copy confirmation UI, so avoid a
        // redundant toast there.
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
        }
    }
}
