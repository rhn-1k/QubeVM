 /*
Copyright (C) Rhn 2026
 */

// QGE (Qube Graphics Engine)
// Tracks the QEMU DisplaySurface used for Android frame capture.
#ifdef __QUBE__

#include "qemu/osdep.h"
#include "qemu/atomic.h"
#include "qemu/error-report.h"
#include "qemu/module.h"
#include "qemu/thread.h"
#include "qemu/timer.h"
#include "system/system.h"
#include "ui/console.h"
#if defined(__ARM_NEON) || defined(__ARM_NEON__)
#include <arm_neon.h>
#endif

// virgl/venus scanout path using Android's system EGL
#ifdef CONFIG_QUBE_VIRGL
#include "ui/egl-context.h"
#include "ui/egl-helpers.h"
#include "ui/shader.h"
#include <virglrenderer.h>
#include <android/log.h>
static uint8_t *qube_gfx_gl_scratch;
static size_t qube_gfx_gl_scratch_size;
#endif

static QemuConsole *qube_gfx_con;
static DisplaySurface *qube_gfx_surface;
static QemuMutex qube_gfx_lock;
static QEMUTimer *qube_gfx_timer;
static int qube_gfx_lock_ready;
static int qube_gfx_generation;

// Guest refresh interval, in ms. controlled by the app via
// QubeGfx.nativeSetRefreshRate, qemu keeps no default opinion
// 16ms is just the boot time value until the app sets one
static int qube_gfx_interval_ms = 16;
#define QUBE_GFX_MIN_INTERVAL_MS 6 // ~165Hz max allowed
#define QUBE_GFX_MAX_INTERVAL_MS 1000 // 1Hz floor, sanity clamp

// defined further down, the -display qube-qge init hook needs it before
// vl.c's own unconditional call, the ready flag guard makes calling twice fine
void qube_gfx_init(QemuConsole *con);

static void qube_gfx_update(DisplayChangeListener *dcl, int x, int y, int w, int h)
{
    // Surface pointer is stable, only pixel contents change
    qatomic_inc(&qube_gfx_generation);
}

static void qube_gfx_switch(DisplayChangeListener *dcl,
    struct DisplaySurface *new_surface)
{
    qemu_mutex_lock(&qube_gfx_lock);
    qube_gfx_surface = new_surface;
    qemu_mutex_unlock(&qube_gfx_lock);
    qatomic_inc(&qube_gfx_generation);
}

#ifdef CONFIG_QUBE_VIRGL
// Read guest scanout into a blit target, then copy it to the DisplaySurface.
static egl_fb qube_gfx_guest_fb;
static egl_fb qube_gfx_blit_fb;
static bool qube_gfx_y0_top;

// virtio-gpu sends the cursor as plain ARGB pixels over its own cursor
// virtqueue, not as part of the GL scanout, so it needs compositing here by
// hand. Guarded by qube_gfx_lock like qube_gfx_surface
static QEMUCursor *qube_gfx_cursor;
static int qube_gfx_cursor_x;
static int qube_gfx_cursor_y;
static bool qube_gfx_cursor_on;

// qube_gfx_gl_scratch holds the last GL readback, cursor-free, in RGBA.
// Tracked so a cursor-only move (no new virgl frame) can recomposite it
// without touching GL from a thread that may not have the context current
static bool qube_gfx_scratch_valid;
static int qube_gfx_scratch_w;
static int qube_gfx_scratch_h;

static void qube_gfx_gl_scanout_disable(DisplayChangeListener *dcl)
{
    qube_gfx_scratch_valid = false;
    egl_fb_destroy(&qube_gfx_guest_fb);
    egl_fb_destroy(&qube_gfx_blit_fb);
}

// GLES requires glTexImage2D's format to match internalformat exactly, the
// stock RGBA/BGRA pairing is invalid on GLES and leaves the FBO incomplete
static void qube_gfx_fb_setup_new_tex(egl_fb *fb, int width, int height)
{
    GLuint texture;
    glGenTextures(1, &texture);
    glBindTexture(GL_TEXTURE_2D, texture);
    glTexImage2D(GL_TEXTURE_2D,
        0,
        GL_RGBA,
        width,
        height,
        0,
        GL_RGBA,
        GL_UNSIGNED_BYTE,
        0);
    egl_fb_setup_for_tex(fb, width, height, texture, true);
}

static void qube_gfx_gl_scanout_texture(DisplayChangeListener *dcl,
    uint32_t backing_id,
    bool backing_y_0_top,
    uint32_t backing_width,
    uint32_t backing_height,
    uint32_t x,
    uint32_t y,
    uint32_t w,
    uint32_t h,
    void *d3d_tex2d)
{
    // d3d_tex2d is a Windows only param, unused here, kept for signature match
    qube_gfx_y0_top = backing_y_0_top;
    egl_fb_setup_for_tex(&qube_gfx_guest_fb,
        backing_width,
        backing_height,
        backing_id,
        false);
    if (qube_gfx_blit_fb.width != backing_width ||
        qube_gfx_blit_fb.height != backing_height) {
        egl_fb_destroy(&qube_gfx_blit_fb);
        qube_gfx_fb_setup_new_tex(&qube_gfx_blit_fb, backing_width, backing_height);
    }
    // dpy_gfx_replace_surface(con, NULL) leaves qube_gfx_surface stuck at a
    // stale-sized placeholder once GL scanout takes over, resize it here
    if (!qube_gfx_surface || surface_width(qube_gfx_surface) != (int)backing_width ||
        surface_height(qube_gfx_surface) != (int)backing_height) {
        qemu_console_set_surface(dcl->con,
            qemu_create_displaysurface(backing_width, backing_height));
    }
}

// Blends qube_gfx_cursor onto the BGRX surface at its current position,
// call with qube_gfx_lock held. cursor->data is RGBA byte order, straight
// (non-premultiplied) alpha, per ui/cursor.c
static void qube_gfx_cursor_blend(void)
{
    QEMUCursor *c = qube_gfx_cursor;
    int sw, sh, stride, row, col, dst_x, dst_y;
    uint8_t *dst;

    if (!c || !qube_gfx_cursor_on || !qube_gfx_surface) {
        return;
    }

    sw = surface_width(qube_gfx_surface);
    sh = surface_height(qube_gfx_surface);
    stride = surface_stride(qube_gfx_surface);
    dst = (uint8_t *)surface_data(qube_gfx_surface);
    dst_x = qube_gfx_cursor_x - c->hot_x;
    dst_y = qube_gfx_cursor_y - c->hot_y;

    for (row = 0; row < c->height; row++) {
        int py = dst_y + row;
        if (py < 0 || py >= sh) {
            continue;
        }
        for (col = 0; col < c->width; col++) {
            int px = dst_x + col;
            uint32_t pixel;
            uint8_t r, g, b, a;
            uint8_t *out;

            if (px < 0 || px >= sw) {
                continue;
            }
            pixel = c->data[row * c->width + col];
            r = pixel & 0xff;
            g = (pixel >> 8) & 0xff;
            b = (pixel >> 16) & 0xff;
            a = (pixel >> 24) & 0xff;
            if (a == 0) {
                continue;
            }
            out = dst + py * stride + px * 4;
            out[0] = (b * a + out[0] * (255 - a)) / 255; // B
            out[1] = (g * a + out[1] * (255 - a)) / 255; // G
            out[2] = (r * a + out[2] * (255 - a)) / 255; // R
        }
    }
}

// Repacks the last clean GL readback (qube_gfx_gl_scratch, RGBA, no cursor)
// into the BGRX surface, then blends the cursor on top. Pure CPU, no GL
// calls, so it's safe to run from any thread on a cursor-only move, not just
// from qube_gfx_gl_update. Call with qube_gfx_lock held. Returns false if
// there's no clean frame yet matching the current surface size
static bool qube_gfx_repack_and_blend(void)
{
    int sw, sh, row, col;
    uint8_t *src, *dst;

    if (!qube_gfx_scratch_valid || !qube_gfx_surface) {
        return false;
    }
    sw = surface_width(qube_gfx_surface);
    sh = surface_height(qube_gfx_surface);
    if (qube_gfx_scratch_w != sw || qube_gfx_scratch_h != sh) {
        return false;
    }

    src = qube_gfx_gl_scratch;
    dst = (uint8_t *)surface_data(qube_gfx_surface);
    for (row = 0; row < sh; row++) {
        uint8_t *src_row = src + row * sw * 4;
        uint8_t *dst_row = dst + row * surface_stride(qube_gfx_surface);
        for (col = 0; col < sw; col++) {
            dst_row[col * 4 + 0] = src_row[col * 4 + 2]; // B
            dst_row[col * 4 + 1] = src_row[col * 4 + 1]; // G
            dst_row[col * 4 + 2] = src_row[col * 4 + 0]; // R
            dst_row[col * 4 + 3] = src_row[col * 4 + 3]; // X
        }
    }
    qube_gfx_cursor_blend();
    return true;
}

// virtio-gpu's cursor virtqueue calls this with fresh pixel data whenever
// the guest's cursor shape changes (theme, app cursor, etc.). Recomposites
// right away so a shape change alone doesn't wait for a new virgl frame
static void qube_gfx_cursor_define(DisplayChangeListener *dcl, QEMUCursor *cursor)
{
    qemu_mutex_lock(&qube_gfx_lock);
    if (qube_gfx_cursor) {
        cursor_unref(qube_gfx_cursor);
    }
    qube_gfx_cursor = cursor ? cursor_ref(cursor) : NULL;
    if (qube_gfx_repack_and_blend()) {
        qatomic_inc(&qube_gfx_generation);
    }
    qemu_mutex_unlock(&qube_gfx_lock);
}

// Called on every guest cursor move, x/y is the on-screen position of the
// cursor's hotspot, not its top-left corner. Recomposites right away using
// the last clean GL frame, don't wait for virgl to push a new one
static void qube_gfx_mouse_set(DisplayChangeListener *dcl, int x, int y, bool on)
{
    qemu_mutex_lock(&qube_gfx_lock);
    qube_gfx_cursor_x = x;
    qube_gfx_cursor_y = y;
    qube_gfx_cursor_on = on;
    if (qube_gfx_repack_and_blend()) {
        qatomic_inc(&qube_gfx_generation);
    }
    qemu_mutex_unlock(&qube_gfx_lock);
}

// Called after virgl finishes a frame, blits guest_fb into the same
// DisplaySurface the 2D path reads from, so nativeCopyFrame stays GL-agnostic
static void qube_gfx_gl_update(DisplayChangeListener *dcl,
    uint32_t x,
    uint32_t y,
    uint32_t w,
    uint32_t h)
{
    qemu_mutex_lock(&qube_gfx_lock);
    if (!qube_gfx_guest_fb.texture || !qube_gfx_surface) {
        qemu_mutex_unlock(&qube_gfx_lock);
        return;
    }
    egl_fb_blit(&qube_gfx_blit_fb, &qube_gfx_guest_fb, qube_gfx_y0_top);
    // GL_BGRA needs an extension not guaranteed on GLES/Adreno, read the
    // spec-guaranteed GL_RGBA into the scratch buffer, qube_gfx_repack_and_blend
    // repacks it into the BGRX layout copy_frame expects and adds the cursor
    {
        int sw = surface_width(qube_gfx_surface);
        int sh = surface_height(qube_gfx_surface);
        size_t need = (size_t)sw * sh * 4;

        if (qube_gfx_gl_scratch_size < need) {
            g_free(qube_gfx_gl_scratch);
            qube_gfx_gl_scratch = g_malloc(need);
            qube_gfx_gl_scratch_size = need;
        }
        glBindFramebuffer(GL_READ_FRAMEBUFFER, qube_gfx_blit_fb.framebuffer);
        glReadBuffer(GL_COLOR_ATTACHMENT0_EXT);
        glReadPixels(0, 0, sw, sh, GL_RGBA, GL_UNSIGNED_BYTE, qube_gfx_gl_scratch);
        qube_gfx_scratch_valid = true;
        qube_gfx_scratch_w = sw;
        qube_gfx_scratch_h = sh;
    }
    qube_gfx_repack_and_blend();
    qatomic_inc(&qube_gfx_generation);
    qemu_mutex_unlock(&qube_gfx_lock);
    qemu_console_update(dcl->con, x, y, w, h);
}
#endif // CONFIG_QUBE_VIRGL

static const DisplayChangeListenerOps qube_gfx_ops = {
    .dpy_name = "qube-qge",
    .dpy_gfx_update = qube_gfx_update,
    .dpy_gfx_switch = qube_gfx_switch,
#ifdef CONFIG_QUBE_VIRGL
    .dpy_cursor_define = qube_gfx_cursor_define,
    .dpy_mouse_set = qube_gfx_mouse_set,
    .dpy_gl_scanout_disable = qube_gfx_gl_scanout_disable,
    .dpy_gl_scanout_texture = qube_gfx_gl_scanout_texture,
    .dpy_gl_update = qube_gfx_gl_update,
#endif
};

static DisplayChangeListener qube_gfx_dcl;

#ifdef CONFIG_QUBE_VIRGL
// Android always has system EGL, no DRM render node needed like
// egl-headless wants, eglGetDisplay(EGL_DEFAULT_DISPLAY) just works
static bool qube_gfx_egl_ready;

// Shared with every per-context virgl GL context creates, so buffers and
// FBOs made in one stay visible in the others. Without this, Xorg's second
// GL context can't see objects the first context made, and you get map,
// framebuffer, and readback failures the moment the desktop starts drawing
static EGLContext qube_gfx_root_ctx = EGL_NO_CONTEXT;

static bool qube_gfx_egl_init(void)
{
    static const EGLint conf_att[] = {
        EGL_SURFACE_TYPE,
        EGL_PBUFFER_BIT,
        EGL_RENDERABLE_TYPE,
        EGL_OPENGL_ES2_BIT | EGL_OPENGL_ES3_BIT_KHR,
        EGL_RED_SIZE,
        8,
        EGL_GREEN_SIZE,
        8,
        EGL_BLUE_SIZE,
        8,
        EGL_ALPHA_SIZE,
        8,
        EGL_NONE,
    };
    EGLint major, minor, n;

    if (qube_gfx_egl_ready) {
        return true;
    }

    qemu_egl_display = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    if (qemu_egl_display == EGL_NO_DISPLAY ||
        !eglInitialize(qemu_egl_display, &major, &minor) ||
        !eglBindAPI(EGL_OPENGL_ES_API) ||
        !eglChooseConfig(qemu_egl_display, conf_att, &qemu_egl_config, 1, &n) ||
        n != 1) {
        error_report("qube-qge: egl init failed");
        return false;
    }

    qemu_egl_mode = DISPLAY_GL_MODE_ES;

    // Created once here, shared by every context virgl creates below
    {
        QEMUGLParams root_params = { .major_ver = 3, .minor_ver = 2 };
        qube_gfx_root_ctx = qemu_egl_create_context(NULL, &root_params, EGL_NO_CONTEXT);
        if (qube_gfx_root_ctx == EGL_NO_CONTEXT) {
            error_report("qube-qge: failed to create root GL context for sharing");
            return false;
        }
    }

    qube_gfx_egl_ready = true;
    return true;
}

// Only one console/listener here, so compatibility is trivially true,
// same call virgl makes through dpy_gl_ctx_create/make_current below
static bool qube_gfx_gl_ctx_is_compatible_dcl(DisplayGLCtx *dgc,
    DisplayChangeListener *dcl)
{
    return dcl->ops == &qube_gfx_ops;
}

// qemu_egl_create_context shares with eglGetCurrentContext(). If the old
// context is destroyed while still current, Adreno rejects sharing with it,
// and the shared root context should never get torn down here either
static void qube_gfx_gl_ctx_destroy(DisplayGLCtx *dgc, QEMUGLContext ctx)
{
    if (ctx == (QEMUGLContext)qube_gfx_root_ctx) {
        return;
    }
    eglMakeCurrent(qemu_egl_display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
    qemu_egl_destroy_context(dgc, ctx);
}

// dpy_gl_ctx_create is 2-arg inside qemu, share with qube_gfx_root_ctx so
// GL objects stay valid across all of virgl's per-context contexts
static QEMUGLContext qube_gfx_gl_ctx_create(DisplayGLCtx *dgc,
    QEMUGLParams *params)
{
    return qemu_egl_create_context(dgc, params, qube_gfx_root_ctx);
}

static const DisplayGLCtxOps qube_gfx_glctx_ops = {
    .dpy_gl_ctx_is_compatible_dcl = qube_gfx_gl_ctx_is_compatible_dcl,
    .dpy_gl_ctx_create = qube_gfx_gl_ctx_create,
    .dpy_gl_ctx_destroy = qube_gfx_gl_ctx_destroy,
    .dpy_gl_ctx_make_current = qemu_egl_make_context_current,
};

static DisplayGLCtx qube_gfx_glctx = {
    .ops = &qube_gfx_glctx_ops,
};

// virgl_error()/virgl_info() go to stderr by default, which never reaches
// logcat, route them through __android_log_print instead.
static void qube_gfx_virgl_log(enum virgl_log_level_flags level,
    const char *message, void *user_data)
{
    int prio = ANDROID_LOG_INFO;
    if (level == VIRGL_LOG_LEVEL_ERROR) prio = ANDROID_LOG_ERROR;
    else if (level == VIRGL_LOG_LEVEL_WARNING) prio = ANDROID_LOG_WARN;
    __android_log_print(prio, TAG, "%s", message);
}

static void early_qube_qge_init(DisplayOptions *opts)
{
    if (opts->has_gl && opts->gl) {
        display_opengl = 1;
        virgl_set_log_callback(qube_gfx_virgl_log, NULL, NULL);
    }
}

// -display qube-qge[,gl=on] uses no native window; Android copies the frame.
static void qube_qge_display_init(DisplayState *ds, DisplayOptions *opts)
{
    QemuConsole *con = qemu_console_lookup_by_index(0);

    if (opts->has_gl && opts->gl) {
        if (!qube_gfx_egl_init()) {
            error_report("qube-qge: falling back to 2D, no GL context");
        } else {
            qemu_console_set_display_gl_ctx(con, &qube_gfx_glctx);
        }
    }

    qube_gfx_init(con);
}

static QemuDisplay qemu_display_qube_qge = {
    .type = DISPLAY_TYPE_QUBE_QGE,
    .early_init = early_qube_qge_init,
    .init = qube_qge_display_init,
};

static void register_qube_qge(void) { qemu_display_register(&qemu_display_qube_qge); }

type_init(register_qube_qge);
#endif // CONFIG_QUBE_VIRGL

// Nothing redraws the surface without this, VNC/GTK poke the guest device the same way
// from their own timers
static void qube_gfx_timer_cb(void *opaque)
{
    qemu_console_hw_update(qube_gfx_con);
    timer_mod(qube_gfx_timer,
        qemu_clock_get_ms(QEMU_CLOCK_REALTIME) + qatomic_read(&qube_gfx_interval_ms));
}

// Called from Android when the Refresh Rate setting changes, even mid-session.
// Takes effect on the next timer tick, no restart needed
void qube_gfx_set_interval_ms(int ms)
{
    if (ms < QUBE_GFX_MIN_INTERVAL_MS) {
        ms = QUBE_GFX_MIN_INTERVAL_MS;
    } else if (ms > QUBE_GFX_MAX_INTERVAL_MS) {
        ms = QUBE_GFX_MAX_INTERVAL_MS;
    }
    qatomic_set(&qube_gfx_interval_ms, ms);
}

// Called from vl.c always, and from qube_qge_display_init when -display qube-qge is
// picked idempotent, only the first call actually registers anything
void qube_gfx_init(QemuConsole *con)
{
    if (qube_gfx_lock_ready) {
        return;
    }
    qemu_mutex_init(&qube_gfx_lock);
    qube_gfx_lock_ready = 1;

    qube_gfx_con = con ? con : qemu_console_lookup_by_index(0);
    qube_gfx_dcl.con = qube_gfx_con;
    qemu_console_register_listener(qube_gfx_con, &qube_gfx_dcl, &qube_gfx_ops);

    qube_gfx_timer = timer_new_ms(QEMU_CLOCK_REALTIME, qube_gfx_timer_cb, NULL);
    timer_mod(qube_gfx_timer,
        qemu_clock_get_ms(QEMU_CLOCK_REALTIME) + qube_gfx_interval_ms);
}

int qube_gfx_get_width(void)
{
    int w = 0;
    if (!qube_gfx_lock_ready) {
        return 0;
    }
    qemu_mutex_lock(&qube_gfx_lock);
    if (qube_gfx_surface) {
        w = surface_width(qube_gfx_surface);
    }
    qemu_mutex_unlock(&qube_gfx_lock);
    return w;
}

int qube_gfx_get_height(void)
{
    int h = 0;
    if (!qube_gfx_lock_ready) {
        return 0;
    }
    qemu_mutex_lock(&qube_gfx_lock);
    if (qube_gfx_surface) {
        h = surface_height(qube_gfx_surface);
    }
    qemu_mutex_unlock(&qube_gfx_lock);
    return h;
}

// Returns gen counter at copy time (0 if no surface). Compare to skip unchanged frames.
// dst_stride in bytes, dst must fit max_h*dst_stride. 32bpp only
int qube_gfx_copy_frame(void *dst, int dst_stride, int max_w, int max_h)
{
    int generation;

    if (!qube_gfx_lock_ready) {
        return 0;
    }

    qemu_mutex_lock(&qube_gfx_lock);
    if (!qube_gfx_surface || surface_bytes_per_pixel(qube_gfx_surface) != 4) {
        qemu_mutex_unlock(&qube_gfx_lock);
        return 0;
    }

    {
        int w = surface_width(qube_gfx_surface);
        int h = surface_height(qube_gfx_surface);
        int src_stride = surface_stride(qube_gfx_surface);
        uint8_t *src = (uint8_t *)surface_data(qube_gfx_surface);
        uint8_t *out = (uint8_t *)dst;
        int copy_w = w < max_w ? w : max_w;
        int copy_h = h < max_h ? h : max_h;
        int row, col;

        // Convert the guest BGRX surface to the RGBA layout expected by Android.
        // Reading while the BQL thread writes may tear a frame
        for (row = 0; row < copy_h; row++) {
            uint8_t *src_row = src + row * src_stride;
            uint8_t *out_row = out + row * dst_stride;
            for (col = 0; col < copy_w; col++) {
                out_row[col * 4 + 0] = src_row[col * 4 + 2];
                out_row[col * 4 + 1] = src_row[col * 4 + 1];
                out_row[col * 4 + 2] = src_row[col * 4 + 0];
                out_row[col * 4 + 3] = 0xff;
            }
        }
    }

    generation = qatomic_read(&qube_gfx_generation);
    qemu_mutex_unlock(&qube_gfx_lock);
    return generation;
}

#endif // __QUBE__
