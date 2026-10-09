/*
Copyright (C) Rhn 2026
 */
package com.max2idea.android.qube.keyboard;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.appcompat.app.AlertDialog;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.max2idea.android.qube.machine.ArchDefinitions;
import com.qube.emu.lib.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// Searchable single choice list of the keyboard layouts
public class KeyboardLayoutDialog {

    public interface OnLayoutSelectedListener {
        void onLayoutSelected(String layout);
    }

    private final Activity activity;
    private final String selected;
    private final OnLayoutSelectedListener listener;

    public KeyboardLayoutDialog(Activity activity, String selected, OnLayoutSelectedListener listener) {
        this.activity = activity;
        this.selected = selected;
        this.listener = listener;
    }

    public void show() {
        View view = activity.getLayoutInflater().inflate(R.layout.dialog_keyboard_layout, null);
        TextInputEditText search = view.findViewById(R.id.keyboardLayoutSearch);
        RecyclerView list = view.findViewById(R.id.keyboardLayoutList);
        View empty = view.findViewById(R.id.keyboardLayoutEmpty);

        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.keyboard_layout)
                .setIcon(R.drawable.language_24px)
                .setView(view)
                .setNegativeButton(R.string.Cancel, null)
                .create();
        // the list shrinks above the soft keyboard instead of being covered
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        // ignore taps during the close delay, so the layout is only applied once
        boolean[] done = {false};
        LayoutAdapter adapter = new LayoutAdapter(ArchDefinitions.getKeyboardValues(activity), selected, layout -> {
            if (done[0])
                return;
            done[0] = true;
            // let the ripple animation play before closing
            list.postDelayed(() -> {
                dialog.dismiss();
                if (!layout.equals(selected))
                    listener.onLayoutSelected(layout);
            }, 200);
        });
        LinearLayoutManager layoutManager = new LinearLayoutManager(activity);
        list.setLayoutManager(layoutManager);
        list.setAdapter(adapter);
        layoutManager.scrollToPositionWithOffset(adapter.positionOf(selected), 0);

        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
                empty.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        search.setOnEditorActionListener((v, actionId, event) -> {
            KeyboardUtils.hideKeyboard(activity, v);
            return true;
        });
        // the search field gives up focus once the soft keyboard closes
        boolean[] imeShown = {false};
        ViewCompat.setOnApplyWindowInsetsListener(dialog.getWindow().getDecorView(), (v, insets) -> {
            boolean visible = insets.isVisible(WindowInsetsCompat.Type.ime());
            if (imeShown[0] && !visible)
                view.requestFocus();
            imeShown[0] = visible;
            return ViewCompat.onApplyWindowInsets(v, insets);
        });
        dialog.show();
    }

    private static class LayoutAdapter extends RecyclerView.Adapter<LayoutAdapter.Holder> {
        private final List<String> all;
        private final List<String> shown;
        private final String selected;
        private final OnLayoutSelectedListener listener;

        LayoutAdapter(List<String> layouts, String selected, OnLayoutSelectedListener listener) {
            this.all = layouts;
            this.shown = new ArrayList<>(layouts);
            this.selected = selected;
            this.listener = listener;
        }

        @SuppressLint("NotifyDataSetChanged")
        void filter(String query) {
            String text = query.trim().toLowerCase(Locale.ROOT);
            shown.clear();
            for (String layout : all) {
                if (layout.contains(text))
                    shown.add(layout);
            }
            notifyDataSetChanged();
        }

        int positionOf(String layout) {
            return Math.max(shown.indexOf(layout), 0);
        }

        @Override
        public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_keyboard_layout, parent, false));
        }

        @Override
        public void onBindViewHolder(Holder holder, int position) {
            String layout = shown.get(position);
            holder.button.setText(layout);
            holder.button.setChecked(layout.equals(selected));
            holder.button.setOnClickListener(v -> listener.onLayoutSelected(layout));
        }

        @Override
        public int getItemCount() {
            return shown.size();
        }

        static class Holder extends RecyclerView.ViewHolder {
            final MaterialButton button;

            Holder(View itemView) {
                super(itemView);
                button = (MaterialButton) itemView;
            }
        }
    }
}
