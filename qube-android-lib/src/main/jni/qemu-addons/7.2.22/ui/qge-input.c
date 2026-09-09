 /*
Copyright (C) Rhn 2026
 */

// Qube input bridge
// Calls qemu_input_event_send_key_qcode()/qemu_input_queue_*() directly from JNI.
#ifdef __QUBE__

#include "qemu/osdep.h"
#include "ui/console.h"
#include "ui/input.h"
#include "ui/keymaps.h"
#include "ui/vnc_keysym.h"
#include "sysemu/sysemu.h"
#include "qapi/error.h"
#include "qemu/main-loop.h"

static QemuConsole *qube_input_con;
static kbd_layout_t *qube_input_kbd_layout;
static int qube_input_ready;

void qube_input_init(QemuConsole *con)
{
    if (qube_input_ready) {
        return;
    }
    qube_input_con = con ? con : qemu_console_lookup_by_index(0);
    qube_input_kbd_layout = init_keyboard_layout(name2keysym,
                                                    keyboard_layout ?: "en-us",
                                                    &error_fatal);
    qube_input_ready = 1;
}

// Some Android keyboards send shifted keysyms without the shift event.
// QEMU's X11 keysym table requires explicit shift, so synthesize it here.
static bool qube_input_needs_shift(uint32_t sym)
{
    return (sym >= 123 && sym <= 126)
        || (sym >= 33 && sym <= 38)
        || (sym >= 40 && sym < 44)
        || (sym >= 62 && sym <= 64)
        || (sym >= 94 && sym <= 95)
        || sym == 58 || sym == 60;
}

// sym is an X11 keysym from Java KeySymMap, translated to QKeyCode via the shared keyboard layout
void qube_input_send_key(uint32_t sym, int down)
{
    uint32_t lsym = sym;
    int keycode;

    if (!qube_input_ready) {
        return;
    }
    if (lsym >= 'A' && lsym <= 'Z') {
        lsym = lsym - 'A' + 'a';
    }
    keycode = keysym2scancode(qube_input_kbd_layout, lsym & 0xFFFF,
                               NULL, down) & SCANCODE_KEYMASK;

    // need the BQL ourselves, called from an Android thread not the qemu main loop
    qemu_mutex_lock_iothread();
    if (qube_input_needs_shift(sym)) {
        if (sym == 60) {
            keycode = keysym2scancode(qube_input_kbd_layout, ',', NULL, down)
                      & SCANCODE_KEYMASK;
        }
        if (down) {
            qemu_input_event_send_key_qcode(qube_input_con, Q_KEY_CODE_SHIFT, true);
        }
        qemu_input_event_send_key_qcode(qube_input_con,
            qemu_input_key_number_to_qcode(keycode), down);
        if (!down) {
            qemu_input_event_send_key_qcode(qube_input_con, Q_KEY_CODE_SHIFT, false);
        }
        qemu_mutex_unlock_iothread();
        return;
    }

    qemu_input_event_send_key_qcode(qube_input_con,
        qemu_input_key_number_to_qcode(keycode), down);
    qemu_mutex_unlock_iothread();
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
    qemu_mutex_lock_iothread();
    qemu_input_queue_abs(qube_input_con, INPUT_AXIS_X, x, 0, width - 1);
    qemu_input_queue_abs(qube_input_con, INPUT_AXIS_Y, y, 0, height - 1);
    for (i = 0; i < ARRAY_SIZE(buttons); i++) {
        qemu_input_queue_btn(qube_input_con, buttons[i],
                              (button_mask & (1 << i)) != 0);
    }
    qemu_input_event_sync();
    qemu_mutex_unlock_iothread();
}

// dx/dy are relative deltas from the trackpad, queued as REL so PS/2 handles them,
// usb-tablet stays untouched here since it only listens for ABS
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
    qemu_mutex_lock_iothread();
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
    qemu_mutex_unlock_iothread();
}

#endif /* __QUBE__ */
