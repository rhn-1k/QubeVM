/*
This is a ported code from the AVNC project
Copyright (C) Gaurav Ujjwal 2020
Copyright (C) Rhn 2026
*/
package com.max2idea.android.qube.main;

import android.animation.LayoutTransition;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.button.MaterialButton;
import com.qube.emu.lib.R;
import com.max2idea.android.qube.toast.ToastUtils;

import java.util.ArrayList;
import java.util.List;

public class VirtualKeysEditorActivity extends AppCompatActivity {

    // Wraps a VirtualKey & corresponding View
    private static class KeyWrapper {
        final VirtualKey vk;
        final View view;

        KeyWrapper(VirtualKey vk, View view) {
            this.vk = vk;
            this.view = view;
        }
    }

    private GridLayout keyGrid;
    private TextView focusedKeyLabel;
    private MaterialButton addKeyBtn, moveUpBtn, moveDownBtn, deleteBtn;
    private Button saveBtn, cancelBtn;
    private Drawable focusOverlay;

    private final List<KeyWrapper> keyList = new ArrayList<>();
    private KeyWrapper focusedKey = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.virtual_keys_editor);
        setupEdgeToEdgeToolbar();

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.pref_customize_virtual_keys);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        focusOverlay = AppCompatResources.getDrawable(this, R.drawable.focus_overlay);


        keyGrid = findViewById(R.id.key_grid);
        focusedKeyLabel = findViewById(R.id.focused_key_label);
        addKeyBtn = findViewById(R.id.add_key_btn);
        moveUpBtn = findViewById(R.id.move_up_btn);
        moveDownBtn = findViewById(R.id.move_down_btn);
        deleteBtn = findViewById(R.id.delete_btn);
        saveBtn = findViewById(R.id.save_btn);
        cancelBtn = findViewById(R.id.cancel_btn);

        keyGrid.setRowCount(QubeSettingsManager.getVkRowCount(this));
        keyGrid.setOrientation(GridLayout.VERTICAL);
        LayoutTransition transition = new LayoutTransition();
        transition.setDuration(150);
        transition.setStartDelay(LayoutTransition.CHANGE_DISAPPEARING, 150);
        transition.addTransitionListener(new LayoutTransition.TransitionListener() {
            @Override
            public void startTransition(LayoutTransition t, ViewGroup c, View view, int tt) {}

            @Override
            public void endTransition(LayoutTransition t, ViewGroup c, View view, int tt) {
                if (focusedKey != null && view == focusedKey.view)
                    refreshFocus();
            }
        });
        keyGrid.setLayoutTransition(transition);

        for (VirtualKey vk : VirtualKeyLayoutConfig.getLayout(this))
            addNewKey(vk);

        addKeyBtn.setOnClickListener(v -> showNewKeyPopup());
        moveUpBtn.setOnClickListener(v -> { if (focusedKey != null) moveKey(focusedKey, -1); });
        moveDownBtn.setOnClickListener(v -> { if (focusedKey != null) moveKey(focusedKey, 1); });
        deleteBtn.setOnClickListener(v -> { if (focusedKey != null) removeKey(focusedKey); });
        updateActionButtons();

        saveBtn.setOnClickListener(v -> saveKeys());
        cancelBtn.setOnClickListener(v -> finish());
    }

    private void setupEdgeToEdgeToolbar() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        View screen = findViewById(R.id.virtual_keys_screen);
        View appBar = findViewById(R.id.virtual_keys_top_app_bar);
        View content = findViewById(R.id.editor_root);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), screen);
        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setNavigationBarContrastEnforced(false);
        }
        final int appBarLeft = appBar.getPaddingLeft();
        final int appBarTop = appBar.getPaddingTop();
        final int appBarRight = appBar.getPaddingRight();
        final int appBarBottom = appBar.getPaddingBottom();
        android.view.ViewGroup.MarginLayoutParams appBarParams =
                (android.view.ViewGroup.MarginLayoutParams) appBar.getLayoutParams();
        final int appBarLeftMargin = appBarParams.leftMargin;
        final int appBarRightMargin = appBarParams.rightMargin;
        final int contentLeft = content.getPaddingLeft();
        final int contentTop = content.getPaddingTop();
        final int contentRight = content.getPaddingRight();
        final int contentBottom = content.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(appBar, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            android.view.ViewGroup.MarginLayoutParams params =
                    (android.view.ViewGroup.MarginLayoutParams) view.getLayoutParams();
            params.leftMargin = appBarLeftMargin + bars.left;
            params.rightMargin = appBarRightMargin + bars.right;
            view.setLayoutParams(params);
            view.setPadding(appBarLeft, appBarTop + bars.top,
                    appBarRight, appBarBottom);
            return insets;
        });
        ViewCompat.setOnApplyWindowInsetsListener(content, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(contentLeft + bars.left, contentTop,
                    contentRight + bars.right, contentBottom + bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(screen);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(R.string.title_load_defaults);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getTitle() != null && item.getTitle().equals(getString(R.string.title_load_defaults))) {
            loadDefaults();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveKeys() {
        List<VirtualKey> enabledKeys = new ArrayList<>();
        for (KeyWrapper k : keyList) enabledKeys.add(k.vk);
        VirtualKeyLayoutConfig.setLayout(this, enabledKeys);
        ToastUtils.toastShort(this, getString(R.string.msg_saved));
        finish();
    }

    private void loadDefaults() {
        while (!keyList.isEmpty())
            removeKey(keyList.get(keyList.size() - 1));
        for (VirtualKey vk : VirtualKeyLayoutConfig.getDefaultLayout(this))
            addNewKey(vk);
    }

    private void updateActionButtons() {
        int index = focusedKey != null ? keyList.indexOf(focusedKey) : -1;
        boolean canMoveUp = index > 0;
        boolean canMoveDown = index >= 0 && index < keyList.size() - 1;
        boolean canDelete = index >= 0 && keyList.size() > 1;

        moveUpBtn.setEnabled(canMoveUp);
        moveUpBtn.setAlpha(canMoveUp ? 1f : 0.35f);
        moveDownBtn.setEnabled(canMoveDown);
        moveDownBtn.setAlpha(canMoveDown ? 1f : 0.35f);
        deleteBtn.setEnabled(canDelete);
        deleteBtn.setAlpha(canDelete ? 1f : 0.35f);
    }

    private KeyWrapper addNewKey(VirtualKey vk) {
        for (KeyWrapper k : keyList) {
            if (k.vk == vk)
                throw new IllegalStateException("Duplicate key detected");
        }

        View view = VirtualKeyViewFactory.create(this, vk);
        keyGrid.addView(view);
        view.setContentDescription(vk.getDescription());
        view.setOnClickListener(v -> {
            if (focusedKey != null && focusedKey.view == view)
                setFocusedKey(null); // Clear focus on 2nd click
            else
                setFocusedKey(view);
            if (view instanceof ToggleButton)
                ((ToggleButton) view).setChecked(false); // Keep toggle keys unchecked
        });

        KeyWrapper wrapper = new KeyWrapper(vk, view);
        keyList.add(wrapper);

        updateActionButtons();
        return wrapper;
    }

    private void removeKey(KeyWrapper key) {
        keyGrid.removeView(key.view);
        keyList.remove(key);

        if (focusedKey == key)
            setFocusedKey(null);

        updateActionButtons();
    }

    private void moveKey(KeyWrapper key, int delta) {
        int currentIndex = keyList.indexOf(key);
        int newIndex = currentIndex + delta;
        if (currentIndex != keyGrid.indexOfChild(key.view))
            throw new IllegalStateException("View index mismatch");

        keyList.remove(currentIndex);
        keyList.add(newIndex, key);

        // Instead of removing current view, we remove the target view.
        // This gives a better layout animation where the view at new
        // position disappears and current view moves in its place.
        View viewAtNew = keyGrid.getChildAt(newIndex);
        keyGrid.removeViewAt(newIndex);
        keyGrid.addView(viewAtNew, currentIndex);

        updateActionButtons();
    }

    private void setFocusedKey(View keyView) {
        if (focusedKey != null)
            focusedKey.view.getOverlay().clear();

        focusedKey = null;
        for (KeyWrapper k : keyList) {
            if (k.view == keyView) {
                focusedKey = k;
                break;
            }
        }
        if (focusedKey != null)
            focusedKey.view.getOverlay().add(focusOverlay);
        refreshFocus();

        focusedKeyLabel.setText(focusedKey != null ? focusedKey.view.getContentDescription() : "");
        updateActionButtons();
    }

    private void refreshFocus() {
        if (focusedKey == null)
            return;
        View v = focusedKey.view;
        if (v.getWidth() > 0 || v.getHeight() > 0) {
            focusOverlay.setBounds(0, 0, v.getWidth(), v.getHeight());
            Rect r = new Rect();
            v.getDrawingRect(r);
            v.requestRectangleOnScreen(r);
        }
    }

    // For now, only one instance of a particular VirtualKey is added in key list.
    private void selectOrAddKey(VirtualKey virtualKey) {
        KeyWrapper key = null;
        for (KeyWrapper k : keyList) {
            if (k.vk == virtualKey) { key = k; break; }
        }
        if (key == null)
            key = addNewKey(virtualKey);
        setFocusedKey(key.view);
    }

    private void showNewKeyPopup() {
        final PopupWindow[] popupRef = new PopupWindow[1];
        ScrollView root = new ScrollView(this);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        root.setPadding(padding, padding, padding, padding);

        GridLayout picker = new GridLayout(this);
        picker.setColumnCount(6);
        for (VirtualKey virtualKey : VirtualKey.values()) {
            View keyView = VirtualKeyViewFactory.create(this, virtualKey);
            keyView.setOnClickListener(v -> {
                selectOrAddKey(virtualKey);
                if (popupRef[0] != null)
                    popupRef[0].dismiss();
            });
            picker.addView(keyView);
        }
        root.addView(picker);

        PopupWindow popup = new PopupWindow(root, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        popupRef[0] = popup;
        popup.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.bg_round_rect));
        popup.setOutsideTouchable(true);
        popup.setFocusable(true);
        popup.setElevation(30f);
        popup.showAsDropDown(addKeyBtn);
    }
}
