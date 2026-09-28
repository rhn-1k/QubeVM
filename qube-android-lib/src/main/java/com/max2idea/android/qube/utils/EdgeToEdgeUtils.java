package com.max2idea.android.qube.utils;

import android.view.View;
import android.view.Window;
import android.view.ViewGroup.MarginLayoutParams;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/**
 * Small, reusable helper for applying edge-to-edge window insets to an
 * Activity's root layout, app bar, and (optionally) its main content view.
 * <p>
 * Replaces the previously duplicated setupEdgeToEdge()/setupEdgeToEdgeToolbar()
 * methods found in QubeActivity, QubeSettingsManager and
 * VirtualKeysEditorActivity.
 */
public class EdgeToEdgeUtils {

    private EdgeToEdgeUtils() {
    }

    /** Edge-to-edge for a screen that only needs the app bar inset (no separate content view). */
    public static void apply(AppCompatActivity activity, int rootId, int appBarId) {
        apply(activity, rootId, appBarId, 0);
    }

    /**
     * Edge-to-edge for a screen with a root layout, an app bar, and a main
     * content view that both need inset-aware padding/margins.
     *
     * @param contentId pass 0 to skip content-view handling.
     */
    public static void apply(AppCompatActivity activity, int rootId, int appBarId, int contentId) {
        Window window = activity.getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);

        View root = activity.findViewById(rootId);
        View appBar = activity.findViewById(appBarId);

        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, root);
        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(false);
        window.setNavigationBarContrastEnforced(false);

        final int appBarLeft = appBar.getPaddingLeft();
        final int appBarTop = appBar.getPaddingTop();
        final int appBarRight = appBar.getPaddingRight();
        final int appBarBottom = appBar.getPaddingBottom();
        MarginLayoutParams appBarParams = (MarginLayoutParams) appBar.getLayoutParams();
        final int appBarLeftMargin = appBarParams.leftMargin;
        final int appBarRightMargin = appBarParams.rightMargin;

        ViewCompat.setOnApplyWindowInsetsListener(appBar, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            MarginLayoutParams params = (MarginLayoutParams) view.getLayoutParams();
            params.leftMargin = appBarLeftMargin + bars.left;
            params.rightMargin = appBarRightMargin + bars.right;
            view.setLayoutParams(params);
            view.setPadding(appBarLeft, appBarTop + bars.top, appBarRight, appBarBottom);
            return insets;
        });

        if (contentId != 0) {
            View content = activity.findViewById(contentId);
            final int contentLeft = content.getPaddingLeft();
            final int contentTop = content.getPaddingTop();
            final int contentRight = content.getPaddingRight();
            final int contentBottom = content.getPaddingBottom();

            ViewCompat.setOnApplyWindowInsetsListener(content, (view, insets) -> {
                Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                        | WindowInsetsCompat.Type.displayCutout());
                view.setPadding(contentLeft + bars.left, contentTop,
                        contentRight + bars.right, contentBottom + bars.bottom);
                return insets;
            });
        }

        ViewCompat.requestApplyInsets(root);
    }
}
