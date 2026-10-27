/*
Copyright (C) Rhn 2026
 */
package com.max2idea.android.qube.main;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.max2idea.android.qube.utils.EdgeToEdgeUtils;
import com.max2idea.android.qube.utils.ToolbarUtils;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.qube.emu.lib.R;

public final class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        EdgeToEdgeUtils.apply(this, R.id.about_root, R.id.about_top_app_bar, R.id.about_content);

        ToolbarUtils.setup(this, ToolbarUtils.config()
                .homeAsUp(true)
                .title(this, R.string.About)
                .onNavigationClick(this::onBackPressed));

        TextView version = findViewById(R.id.about_version);
        version.setText(getString(R.string.app_version_format, readVersionName()));

        String qemuVersion = QubeApplication.getQemuVersionString();
        TextView engine = findViewById(R.id.about_engine);
        engine.setText(getString(R.string.about_engine,
                qemuVersion == null ? getString(R.string.version_unknown) : qemuVersion.trim()));

        ShapeAppearanceModel shapeAppearanceModel = ShapeAppearanceModel.builder()
                .setAllCornerSizes(ShapeAppearanceModel.PILL)
                .build();
        MaterialShapeDrawable boxBackground = new MaterialShapeDrawable(shapeAppearanceModel);
        boxBackground.setFillColor(android.content.res.ColorStateList.valueOf(
                com.google.android.material.color.MaterialColors.getColor(engine, com.google.android.material.R.attr.colorSurfaceVariant)
        ));
        engine.setBackground(boxBackground);

        findViewById(R.id.about_github).setOnClickListener(view -> openUrl(Config.downloadLink));
        findViewById(R.id.about_discord).setOnClickListener(view -> openUrl(Config.discordLink));

        // Something?
        setupSomething();
    }

    // ???
    private final long[] somethingTapTimes = new long[Config.SOMETHING_TAPS];
    private int somethingTapCount;

    private void setupSomething() {
        android.view.View icon = findViewById(R.id.about_icon);
        icon.setOnTouchListener((view, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    view.animate()
                            .scaleX(0.90f)
                            .scaleY(0.90f)
                            .setDuration(100)
                            .setInterpolator(new DecelerateInterpolator())
                            .start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    view.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(300)
                            .setInterpolator(new DecelerateInterpolator())
                            .start();
                    break;
                default:
                    break;
            }
            return false;
        });
        icon.setOnClickListener(view -> {
            android.os.Vibrator vibrator = getSystemService(android.os.Vibrator.class);
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(android.os.VibrationEffect.createPredefined(
                        android.os.VibrationEffect.EFFECT_CLICK));
            }
            long now = android.os.SystemClock.elapsedRealtime();
            somethingTapTimes[somethingTapCount % Config.SOMETHING_TAPS] = now;
            somethingTapCount++;
            if (somethingTapCount >= Config.SOMETHING_TAPS) {
                long oldestTap = somethingTapTimes[somethingTapCount % Config.SOMETHING_TAPS];
                if (now - oldestTap < Config.SOMETHING_WINDOW_MS) {
                    somethingTapCount = 0;
                    openUrl(Config.SOMETHING_URL);
                }
            }
        });
    }

    private String readVersionName() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            return info.versionName == null ? getString(R.string.version_unknown) : info.versionName;
        } catch (Exception ignored) {
            return getString(R.string.version_unknown);
        }
    }

    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException error) {
            Toast.makeText(this, R.string.could_not_open_link, Toast.LENGTH_LONG).show();
        }
    }
}
