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

// send_key_qcode is gone in qemu 11.x, only send_key_number remains
static void qube_input_send_qcode(QemuConsole *con, int qcode, int down)
{
    unsigned int lnx = qcode < (int)qemu_input_map_qcode_to_linux_len
                        ? qemu_input_map_qcode_to_linux[qcode] : 0;
    unsigned int num = lnx < qemu_input_map_linux_to_qnum_len
                        ? qemu_input_map_linux_to_qnum[lnx] : 0;
    qemu_input_event_send_key_number(con, num, down);
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
// Same lookup used by QEMU's VNC, GTK, and SDL.
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
    bql_lock();
    if (qube_input_needs_shift(sym)) {
        if (sym == 60) {
            keycode = keysym2scancode(qube_input_kbd_layout, ',', NULL, down)
                      & SCANCODE_KEYMASK;
        }
        if (down) {
            qube_input_send_qcode(qube_input_con, Q_KEY_CODE_SHIFT, true);
        }
        qemu_input_event_send_key_number(qube_input_con, keycode, down);
        if (!down) {
            qube_input_send_qcode(qube_input_con, Q_KEY_CODE_SHIFT, false);
        }
        bql_unlock();
        return;
    }

    qemu_input_event_send_key_number(qube_input_con, keycode, down);
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
