 /*
Copyright (C) Rhn 2026
 */

#ifdef __QUBE__

#include "qemu/osdep.h"
#include "qemu/module.h"
#include "qemu/thread.h"
#include "audio.h"

#define AUDIO_CAP "qube"
#include "audio_int.h"

// Format delivered to the Android callback.
#define QUBE_AUDIO_FREQ 48000
#define QUBE_AUDIO_CHANNELS 2
#define QUBE_AUDIO_CALLBACK_FRAMES (QUBE_AUDIO_FREQ / 50) // 20 ms
#define QUBE_AUDIO_BUFFER_COUNT 4

typedef struct QubeVoiceOut {
    HWVoiceOut hw;
    int exit;
    int initialized;
} QubeVoiceOut;

// Guards the emulation buffer shared by QEMU and Android.
static QemuMutex qube_audio_lock;
static int qube_audio_lock_ready;
static QubeVoiceOut *qube_voice;

static void qube_audio_lock_init(void)
{
    if (!qube_audio_lock_ready) {
        qemu_mutex_init(&qube_audio_lock);
        qube_audio_lock_ready = 1;
    }
}

// Discard queued samples and clear the backing buffer.
static void qube_audio_clear_pending_locked(QubeVoiceOut *qube)
{
    HWVoiceOut *hw = &qube->hw;

    if (hw->buf_emul && hw->size_emul) {
        audio_pcm_info_clear_buf(
            &hw->info,
            hw->buf_emul,
            hw->size_emul / hw->info.bytes_per_frame);
    }
    hw->pending_emul = 0;
}

// Drain QEMU output; silence fills any underrun.
static size_t qube_callback_out_locked(
    QubeVoiceOut *qube,
    uint8_t *buf,
    size_t len)
{
    HWVoiceOut *hw = &qube->hw;
    size_t copied = 0;

    if (!qube->exit) {
        while (hw->pending_emul && len) {
            size_t write_len;
            size_t start;

            start = audio_ring_posb(
                hw->pos_emul,
                hw->pending_emul,
                hw->size_emul);
            assert(start < hw->size_emul);

            write_len = MIN(
                MIN(hw->pending_emul, len),
                hw->size_emul - start);
            memcpy(buf, hw->buf_emul + start, write_len);
            hw->pending_emul -= write_len;
            copied += write_len;
            len -= write_len;
            buf += write_len;
        }
    }

    if (len) {
        audio_pcm_info_clear_buf(
            &hw->info,
            buf,
            len / hw->info.bytes_per_frame);
    }
    return copied;
}

static size_t qube_buffer_get_free(HWVoiceOut *hw)
{
    size_t ret;

    qemu_mutex_lock(&qube_audio_lock);
    ret = audio_generic_buffer_get_free(hw);
    qemu_mutex_unlock(&qube_audio_lock);
    return ret;
}

static void *qube_get_buffer_out(HWVoiceOut *hw, size_t *size)
{
    void *ret;

    qemu_mutex_lock(&qube_audio_lock);
    ret = audio_generic_get_buffer_out(hw, size);
    qemu_mutex_unlock(&qube_audio_lock);
    return ret;
}

static size_t qube_put_buffer_out(
    HWVoiceOut *hw,
    void *buf,
    size_t size)
{
    size_t ret;

    qemu_mutex_lock(&qube_audio_lock);
    ret = audio_generic_put_buffer_out(hw, buf, size);
    qemu_mutex_unlock(&qube_audio_lock);
    return ret;
}

static size_t qube_write(HWVoiceOut *hw, void *buf, size_t size)
{
    size_t ret;

    qemu_mutex_lock(&qube_audio_lock);
    ret = audio_generic_write(hw, buf, size);
    qemu_mutex_unlock(&qube_audio_lock);
    return ret;
}

static int qube_init_out(
    HWVoiceOut *hw,
    struct audsettings *as,
    void *drv_opaque)
{
    QubeVoiceOut *qube = (QubeVoiceOut *)hw;
    struct audsettings fixed = {
        .freq = QUBE_AUDIO_FREQ,
        .nchannels = QUBE_AUDIO_CHANNELS,
        .fmt = AUDIO_FORMAT_S16,
        .endianness = AUDIO_HOST_ENDIANNESS,
    };

    (void)as;
    (void)drv_opaque;
    qube_audio_lock_init();

    // Keep QEMU's producer buffer aligned with the callback period.
    audio_pcm_init_info(&hw->info, &fixed);
    hw->samples = QUBE_AUDIO_BUFFER_COUNT * QUBE_AUDIO_CALLBACK_FRAMES;
    qube->exit = 0;
    qube->initialized = 1;
    qube_voice = qube;
    return 0;
}

// Stop reads and release queued output.
static void qube_fini_out(HWVoiceOut *hw)
{
    QubeVoiceOut *qube = (QubeVoiceOut *)hw;

    qemu_mutex_lock(&qube_audio_lock);
    qube->exit = 1;
    qube_audio_clear_pending_locked(qube);
    if (qube_voice == qube) {
        qube_voice = NULL;
    }
    qube->initialized = 0;
    qemu_mutex_unlock(&qube_audio_lock);
}

// Reset stale samples when playback is paused.
static void qube_enable_out(HWVoiceOut *hw, bool enable)
{
    QubeVoiceOut *qube = (QubeVoiceOut *)hw;

    qemu_mutex_lock(&qube_audio_lock);
    qube->exit = !enable;
    if (!enable) {
        qube_audio_clear_pending_locked(qube);
    }
    qemu_mutex_unlock(&qube_audio_lock);
}

static void *qube_audio_init(Audiodev *dev)
{
    assert(dev->driver == AUDIODEV_DRIVER_QUBE);
    return dev;
}

static void qube_audio_fini(void *opaque)
{
    (void)opaque;
}

static struct audio_pcm_ops qube_pcm_ops = {
    .init_out = qube_init_out,
    .fini_out = qube_fini_out,
    .write = qube_write,
    .buffer_get_free = qube_buffer_get_free,
    .get_buffer_out = qube_get_buffer_out,
    .put_buffer_out = qube_put_buffer_out,
    .enable_out = qube_enable_out,
};

static struct audio_driver qube_audio_driver = {
    .name = "qube",
    .descr = "Qube Android audio output",
    .init = qube_audio_init,
    .fini = qube_audio_fini,
    .pcm_ops = &qube_pcm_ops,
    .can_be_default = 0,
    .max_voices_out = 1,
    .max_voices_in = 0,
    .voice_size_out = sizeof(QubeVoiceOut),
    .voice_size_in = 0,
};

static void register_audio_qube(void)
{
    audio_driver_register(&qube_audio_driver);
}
type_init(register_audio_qube);

// JNI-facing callback entry point, resolved with dlsym.
int qube_audio_read(void *dst, int max_bytes)
{
    int copied;

    if (!qube_audio_lock_ready || !qube_voice || max_bytes <= 0) {
        return 0;
    }

    qemu_mutex_lock(&qube_audio_lock);
    copied = (int)qube_callback_out_locked(
        qube_voice,
        (uint8_t *)dst,
        (size_t)max_bytes);
    qemu_mutex_unlock(&qube_audio_lock);
    return copied;
}

// Drop pending output before playback is stopped or reopened.
void qube_audio_flush(void)
{
    if (!qube_audio_lock_ready || !qube_voice) {
        return;
    }

    qemu_mutex_lock(&qube_audio_lock);
    qube_audio_clear_pending_locked(qube_voice);
    qemu_mutex_unlock(&qube_audio_lock);
}

// Fixed format shared with the Android audio stream.
void qube_audio_get_format(int *rate, int *channels, int *bits)
{
    *rate = QUBE_AUDIO_FREQ;
    *channels = QUBE_AUDIO_CHANNELS;
    *bits = 16;
}

#endif // __QUBE__
