/*
Copyright (C) Max Kastanas 2012
Copyright (C) Rhn 2026
 */
package com.max2idea.android.qube.log;

import android.app.Activity;
import androidx.appcompat.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.qube.emu.lib.R;
import com.max2idea.android.qube.files.FileUtils;
import com.max2idea.android.qube.machine.Machine.FileType;
import com.max2idea.android.qube.main.Config;
import com.max2idea.android.qube.main.QubeApplication;
import com.max2idea.android.qube.toast.ToastUtils;
import com.max2idea.android.qube.utils.ClipboardUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Custom logger for android so users can view the log if something went wrong. */
public class Logger {
    public static final String TAG = "Logger";
    private static final int MAX_DISPLAY_CHARS = 50 * 1024;
    private static final int HALF_DISPLAY_CHARS = MAX_DISPLAY_CHARS / 2;

    private static String getLogFilePath() {
        return Config.logFilePath == null ? "" : Config.logFilePath;
    }

    private static String truncateForDisplay(String contents) {
        if (contents.length() <= MAX_DISPLAY_CHARS) {
            return contents;
        }
        return contents.substring(0, HALF_DISPLAY_CHARS)
                + "\n.....\n"
                + contents.substring(contents.length() - HALF_DISPLAY_CHARS);
    }

    public static Spannable formatAndroidLog(String contents) {
        SpannableString formattedString = new SpannableString(contents);
        if (contents.length() == 0) {
            return formattedString;
        }
        try {
            int counter = 0;
            String[] lines = contents.split("\\n", -1);
            for (String line : lines) {
                ForegroundColorSpan colorSpan = null;
                if (line.startsWith("E/") || line.contains(" E ")) {
                    colorSpan = new ForegroundColorSpan(Color.parseColor(Config.LOG_FATAL));
                } else if (line.startsWith("W/") || line.contains(" W ")) {
                    colorSpan = new ForegroundColorSpan(Color.parseColor(Config.LOG_WARN));
                } else if (line.startsWith("D/") || line.contains(" D ")) {
                    colorSpan = new ForegroundColorSpan(Color.parseColor(Config.LOG_DEBUG));
                }
                if (colorSpan != null && counter + line.length() <= formattedString.length()) {
                    formattedString.setSpan(colorSpan, counter, counter + line.length(),
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                counter += line.length() + 1;
            }
        } catch (Exception ex) {
            Log.w(TAG, "Could not format qube log: " + ex.getMessage());
        }
        return formattedString;
    }

    // Autoscroll system: keep the newest log visible until the user scrolls upward.
    public static void UIAlertLog(final Activity activity, String title, Spannable body) {
        final TextView textView = new TextView(activity);
        textView.setPadding(20, 20, 20, 20);
        textView.setText(body);
        textView.setTextSize(12f);
        textView.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        textView.setSingleLine(false);

        final ScrollView scrollView = new ScrollView(activity);
        scrollView.addView(textView);
        final boolean[] userScrolledUp = {false};
        final boolean[] isAutoScrolling = {false};

        scrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
            if (isAutoScrolling[0]) {
                return;
            }
            int maxScroll = textView.getHeight() - scrollView.getHeight();
            boolean nearBottom = maxScroll <= 0 || scrollView.getScrollY() >= maxScroll - 50;
            userScrolledUp[0] = !nearBottom;
        });

        scrollView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        isAutoScrolling[0] = true;
                        scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                        scrollView.post(() -> isAutoScrolling[0] = false);
                        scrollView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                });

        final String logFilePath = getLogFilePath();
        final Runnable updateRunnable = new Runnable() {
            private int lastLength = 0;

            @Override
            public void run() {
                if (!activity.isFinishing()) {
                    String newContents = truncateForDisplay(FileUtils.getFileContents(logFilePath));
                    if (newContents.length() != lastLength) {
                        lastLength = newContents.length();
                        final Spannable formatted = formatAndroidLog(newContents);
                        activity.runOnUiThread(() -> {
                            textView.setText(formatted);
                            if (!userScrolledUp[0]) {
                                textView.post(() -> {
                                    isAutoScrolling[0] = true;
                                    scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                                    scrollView.post(() -> isAutoScrolling[0] = false);
                                });
                            }
                        });
                    }
                    scrollView.postDelayed(this, Config.LOG_DELAY);
                }
            }
        };
        scrollView.post(updateRunnable);

        final androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setTitle(title)
                .setIcon(R.drawable.notes_24px)
                .setPositiveButton(R.string.Ok, null)
                .setView(scrollView)
                .setNeutralButton(R.string.CopyTo, (d, which) -> {
                    ToastUtils.toastShort(activity, activity.getString(R.string.ChooseDirToSaveLogFile));
                    FileUtils.browse(activity, FileType.LOG_DIR, Config.OPEN_LOG_FILE_DIR_REQUEST_CODE);
                })
                .setNegativeButton(R.string.clear_label, (d, which) -> {
                    FileUtils.saveFileContents(logFilePath, "");
                    textView.setText("");
                    userScrolledUp[0] = false;
                })
                .create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnDismissListener(d -> scrollView.removeCallbacks(updateRunnable));
        dialog.show();
        android.view.ViewParent positiveParent = dialog.getButton(AlertDialog.BUTTON_POSITIVE).getParent();
        if (positiveParent instanceof LinearLayout) {
            ((LinearLayout) positiveParent).setGravity(Gravity.CENTER);
        }
    }

    // Shows a crash summary dialog and asks if the user wants to see the log.
    public static void promptShowLog(final Activity activity) {
        String logFilePath = getLogFilePath();
        String contents = FileUtils.getFileContents(logFilePath);
        String errorSummary = extractErrorSummary(contents);
        String message = errorSummary.length() > 0
                ? errorSummary : activity.getString(R.string.UnknownErrorCause);

        TextView stateView = new TextView(activity);
        stateView.setText(message);
        stateView.setPadding(20, 20, 20, 20);
        final String summary = message;
        new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.SomethingWentWrong)
                .setIcon(R.drawable.outline_bug_report_24)
                .setView(stateView)
                .setPositiveButton(R.string.ViewLog, (dialog, which) -> viewQubeLog(activity))
                .setNegativeButton(R.string.Ok, null)
                .setNeutralButton(R.string.CopySummary, (dialog, which) ->
                        ClipboardUtils.copyToClipboard(activity,
                                activity.getString(R.string.CopySummary), summary))
                .create()
                .show();
    }

    public static String extractErrorSummary(String contents) {
        if (contents.length() == 0) {
            return "";
        }
        String[] lines = contents.split("\\n", -1);
        List<String> blockLines = new ArrayList<>();
        int lastBlockStart = -1;
        for (int i = 0; i < lines.length; i++) {
            if (isLogutilsErrorLine(lines[i])
                    && (i == 0 || !isLogutilsErrorLine(lines[i - 1]))) {
                lastBlockStart = i;
            }
        }
        if (lastBlockStart != -1) {
            int i = lastBlockStart;
            while (i < lines.length && isLogutilsErrorLine(lines[i])) {
                String message = lines[i];
                int marker = message.indexOf("qube_logutils.h:");
                if (marker >= 0) {
                    message = message.substring(marker + "qube_logutils.h:".length()).trim();
                }
                if (!message.isEmpty()) {
                    blockLines.add(message);
                }
                i++;
            }
            if (!blockLines.isEmpty()) {
                String binaryName = blockLines.get(0).trim();
                while (binaryName.endsWith(":")) {
                    binaryName = binaryName.substring(0, binaryName.length() - 1).trim();
                }
                int slash = binaryName.lastIndexOf('/');
                if (slash >= 0) {
                    binaryName = binaryName.substring(slash + 1);
                }
                if (binaryName.startsWith("lib")) {
                    binaryName = binaryName.substring(3);
                }
                if (binaryName.endsWith(".so")) {
                    binaryName = binaryName.substring(0, binaryName.length() - 3);
                }
                String errorMessage = null;
                for (int j = blockLines.size() - 1; j >= 1; j--) {
                    String candidate = blockLines.get(j).trim();
                    if (!candidate.isEmpty() && !candidate.equals(":")) {
                        errorMessage = candidate;
                        break;
                    }
                }
                if (binaryName.startsWith("qemu") && errorMessage != null) {
                    return binaryName + ": " + errorMessage;
                }
                String joined = joinLines(blockLines);
                if (!joined.isEmpty()) {
                    return joined;
                }
            }
        }

        for (int i = lines.length - 1; i >= 0; i--) {
            if (lines[i].contains("Fatal signal")) {
                String message = afterMarker(lines[i], "F libc");
                if (!message.isEmpty()) {
                    return message;
                }
                break;
            }
        }
        for (int i = lines.length - 1; i >= 0; i--) {
            if (lines[i].contains("FORTIFY")) {
                String message = afterMarker(lines[i], "libc");
                if (!message.isEmpty()) {
                    return message;
                }
                break;
            }
        }
        return "";
    }

    private static boolean isLogutilsErrorLine(String line) {
        return line.contains("qube_logutils.h") && (line.contains(" E ") || line.contains("E/"));
    }

    private static String afterMarker(String line, String marker) {
        int index = line.indexOf(marker);
        String message = index >= 0 ? line.substring(index + marker.length()) : line;
        return message.trim().replaceFirst("^:+", "").trim();
    }

    private static String joinLines(List<String> lines) {
        StringBuilder result = new StringBuilder();
        for (String line : lines) {
            if (result.length() > 0) {
                result.append('\n');
            }
            result.append(line);
        }
        return result.toString();
    }

    public static void viewQubeLog(final Activity activity) {
        String logFilePath = getLogFilePath();
        String contents = truncateForDisplay(FileUtils.getFileContents(logFilePath));
        final Spannable contentsFormatted = formatAndroidLog(contents);
        activity.runOnUiThread(() -> {
            if (Config.viewLogInternally) {
                UIAlertLog(activity, activity.getString(R.string.QubeLog), contentsFormatted);
            } else {
                try {
                    Intent intent = new Intent(Intent.ACTION_EDIT);
                    File file = new File(logFilePath);
                    Uri uri = Uri.fromFile(file);
                    intent.setDataAndType(uri, "text/plain");
                    activity.startActivity(intent);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    UIAlertLog(activity, activity.getString(R.string.QubeLog), contentsFormatted);
                }
            }
        });
    }

    public static void setupLogFile(String filePath) {
        Config.logFilePath = QubeApplication.getInstance().getCacheDir().toString() + filePath;
    }
}
