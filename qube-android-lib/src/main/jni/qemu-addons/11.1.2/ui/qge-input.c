 /*
Copyright (C) Rhn 2026
 */

// Qube input bridge
// Calls qemu_input_event_send_key_number()/qemu_input_queue_*() directly from JNI.
#ifdef __QUBE__

#include "qemu/osdep.h"
#include "ui/console.h"
#include "ui/input.h"
#include "keymaps.h"
#include "ui/vnc_keysym.h"
#include "system/system.h"
#include "qapi/error.h"
#include "qemu/main-loop.h"
#include "qemu/error-report.h"

static QemuConsole *qube_input_con;
static kbd_layout_t *qube_input_kbd_layout;
static int qube_input_ready;

void qube_input_init(QemuConsole *con)
{
    if (qube_input_ready) {
        return;
    }
    qube_input_con = con ? con : qemu_console_lookup_by_index(0);
    qube_input_kbd_layout = kbd_layout_new(name2keysym, keyboard_layout ?: "en-us", &error_fatal);
    qube_input_ready = 1;
}

// switches the layout of the running vm, false when the keymap can't be loaded
bool qube_input_set_layout(const char *name)
{
    Error *err = NULL;
    kbd_layout_t *layout;

    if (!qube_input_ready) {
        return false;
    }
    layout = kbd_layout_new(name2keysym, name, &err);
    if (!layout) {
        error_report_err(err);
        return false;
    }
    bql_lock();
    kbd_layout_free(qube_input_kbd_layout);
    qube_input_kbd_layout = layout;
    bql_unlock();
    return true;
}

// keycode is the keymap scancode with the modifier flags already masked off
static void qube_input_send_number(int keycode, bool down)
{
    qemu_input_event_send_key_number(qube_input_con, keycode, down);
}

// send_key_qcode is gone in qemu 11.x, only send_key_number remains
static void qube_input_send_qcode(int qcode, bool down)
{
    unsigned int lnx = qcode < (int)qemu_input_map_qcode_to_linux_len
                        ? qemu_input_map_qcode_to_linux[qcode] : 0;
    unsigned int num = lnx < qemu_input_map_linux_to_qnum_len
                        ? qemu_input_map_linux_to_qnum[lnx] : 0;
    qube_input_send_number(num, down);
}

// shift as Android left it, so a layout entry can override it
static bool qube_input_shift_held;

// sym is an X11 keysym from Java KeySymMap, translated to QKeyCode via the shared keyboard layout
void qube_input_send_key(uint32_t sym, int down)
{
    int code;
    int keycode;
    bool shift;

    if (!qube_input_ready) {
        return;
    }

    // need the BQL ourselves, called from an Android thread not the qemu main loop
    // the layout is only used under it since it can be swapped from another thread
    bql_lock();
    code = keysym2scancode(qube_input_kbd_layout, sym, NULL, down);
    keycode = code & SCANCODE_KEYMASK;
    // keysym not in the active layout, nothing to send
    if (!keycode) {
        bql_unlock();
        return;
    }

    // Shift_L and Shift_R are only tracked and passed through
    if (sym == 0xFFE1 || sym == 0xFFE2) {
        qube_input_shift_held = down;
        qube_input_send_number(keycode, down);
        bql_unlock();
        return;
    }

    // press what the layout entry needs before the key
    shift = code & SCANCODE_SHIFT;
    if (down) {
        if (shift != qube_input_shift_held) {
            qube_input_send_qcode(Q_KEY_CODE_SHIFT, shift);
        }
        if (code & SCANCODE_ALTGR) {
            qube_input_send_qcode(Q_KEY_CODE_ALT_R, true);
        }
        if (code & SCANCODE_CTRL) {
            qube_input_send_qcode(Q_KEY_CODE_CTRL, true);
        }
    }
    qube_input_send_number(keycode, down);
    // release in reverse order and give shift back to what Android had
    if (!down) {
        if (code & SCANCODE_CTRL) {
            qube_input_send_qcode(Q_KEY_CODE_CTRL, false);
        }
        if (code & SCANCODE_ALTGR) {
            qube_input_send_qcode(Q_KEY_CODE_ALT_R, false);
        }
        if (shift != qube_input_shift_held) {
            qube_input_send_qcode(Q_KEY_CODE_SHIFT, qube_input_shift_held);
        }
    }
    bql_unlock();
}

// x/y are absolute guest coordinates (0..width-1 / 0..height-1) from QubeGfx.
void qube_input_send_pointer(int x, int y, int button_mask, int width, int height)
{
    static const int buttons[] = {
        INPUT_BUTTON_LEFT, INPUT_BUTTON_MIDDLE, INPUT_BUTTON_RIGHT,
        INPUT_BUTTON_WHEEL_UP, INPUT_BUTTON_WHEEL_DOWN,
    };
    int i;

    if (!qube_input_ready || width <= 0 || height <= 0) {
        return;
    }
    // Same BQL requirement as qube_input_send_key above.
    bql_lock();
    qemu_input_queue_abs(qube_input_con, INPUT_AXIS_X, x, 0, width - 1);
    qemu_input_queue_abs(qube_input_con, INPUT_AXIS_Y, y, 0, height - 1);
    for (i = 0; i < ARRAY_SIZE(buttons); i++) {
        qemu_input_queue_btn(qube_input_con, buttons[i],
                              (button_mask & (1 << i)) != 0);
    }
    qemu_input_event_sync();
    bql_unlock();
}

// dx/dy are relative deltas from the trackpad, queued as REL so PS/2 handles them
void qube_input_send_pointer_rel(int dx, int dy, int button_mask)
{
    static const int buttons[] = {
        INPUT_BUTTON_LEFT, INPUT_BUTTON_MIDDLE, INPUT_BUTTON_RIGHT,
        INPUT_BUTTON_WHEEL_UP, INPUT_BUTTON_WHEEL_DOWN,
    };
    int i;

    if (!qube_input_ready) {
        return;
    }
    // same BQL requirement as qube_input_send_pointer above
    bql_lock();
    if (dx != 0) {
        qemu_input_queue_rel(qube_input_con, INPUT_AXIS_X, dx);
    }
    if (dy != 0) {
        qemu_input_queue_rel(qube_input_con, INPUT_AXIS_Y, dy);
    }
    for (i = 0; i < ARRAY_SIZE(buttons); i++) {
        qemu_input_queue_btn(qube_input_con, buttons[i],
                              (button_mask & (1 << i)) != 0);
    }
    qemu_input_event_sync();
    bql_unlock();
}

#endif /* __QUBE__ */
