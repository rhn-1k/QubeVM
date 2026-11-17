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

#define QUBE_AUDIO_FREQ 48000
#define QUBE_AUDIO_CHANNELS 2
#define QUBE_AUDIO_RING_BYTES (16 * 1024) // ~85ms at 48kHz/stereo/S16

typedef struct QubeVoiceOut {
    HWVoiceOut hw;
} QubeVoiceOut;

static QemuMutex qube_audio_lock;
static int qube_audio_lock_ready;
static uint8_t qube_audio_ring[QUBE_AUDIO_RING_BYTES];
static int qube_audio_head; // next byte to write
static int qube_audio_tail; // next byte to read
static int qube_audio_fill; // bytes currently buffered

static void qube_audio_ring_init(void)
{
    if (!qube_audio_lock_ready) {
        qemu_mutex_init(&qube_audio_lock);
        qube_audio_lock_ready = 1;
    }
}

// Called from the audio thread. Always accepts the whole write, if the ring
// is full we drop the oldest bytes rather than block and stall the guest.
static size_t qube_write(HWVoiceOut *hw, void *buf, size_t len)
{
    uint8_t *src = (uint8_t *) buf;

    qemu_mutex_lock(&qube_audio_lock);
    if (len > QUBE_AUDIO_RING_BYTES) {
        // Bigger than the whole ring, only the tail of it can ever matter.
        src += len - QUBE_AUDIO_RING_BYTES;
        len = QUBE_AUDIO_RING_BYTES;
    }
    if (qube_audio_fill + (int) len > QUBE_AUDIO_RING_BYTES) {
        int drop = qube_audio_fill + (int) len - QUBE_AUDIO_RING_BYTES;
        qube_audio_tail = (qube_audio_tail + drop) % QUBE_AUDIO_RING_BYTES;
        qube_audio_fill -= drop;
    }
    for (size_t i = 0; i < len; i++) {
        qube_audio_ring[qube_audio_head] = src[i];
        qube_audio_head = (qube_audio_head + 1) % QUBE_AUDIO_RING_BYTES;
    }
    qube_audio_fill += (int) len;
    qemu_mutex_unlock(&qube_audio_lock);

    return len;
}

static int qube_init_out(HWVoiceOut *hw, struct audsettings *as, void *drv_opaque)
{
    struct audsettings fixed = {
        .freq = QUBE_AUDIO_FREQ,
        .nchannels = QUBE_AUDIO_CHANNELS,
        .fmt = AUDIO_FORMAT_S16,
        .endianness = AUDIO_HOST_ENDIANNESS,
    };

    qube_audio_ring_init();
    audio_pcm_init_info(&hw->info, &fixed);
    hw->samples = 1024;
    return 0;
}

static void qube_fini_out(HWVoiceOut *hw)
{
    (void) hw;
}

static void qube_enable_out(HWVoiceOut *hw, bool enable)
{
    (void) hw;
    if (!enable) {
        // Playback stopped/paused guest-side: drop whatever's queued so a
        // resume doesn't dump a burst of stale audio.
        qemu_mutex_lock(&qube_audio_lock);
        qube_audio_head = qube_audio_tail = qube_audio_fill = 0;
        qemu_mutex_unlock(&qube_audio_lock);
    }
}

static void *qube_audio_init(Audiodev *dev)
{
    assert(dev->driver == AUDIODEV_DRIVER_QUBE);
    return dev;
}

static void qube_audio_fini(void *opaque)
{
    (void) opaque;
}

static struct audio_pcm_ops qube_pcm_ops = {
    .init_out = qube_init_out,
    .fini_out = qube_fini_out,
    .write = qube_write,
    .buffer_get_free = audio_generic_buffer_get_free,
    .run_buffer_out = audio_generic_run_buffer_out,
    .enable_out = qube_enable_out,
};

static struct audio_driver qube_audio_driver = {
    .name = "qube",
    .descr = "Qube AAudio driver",
    .init = qube_audio_init,
    .fini = qube_audio_fini,
    .pcm_ops = &qube_pcm_ops,
    .can_be_default = 0,
    .max_voices_out = 1,
    .max_voices_in  = 0,
    .voice_size_out = sizeof(QubeVoiceOut),
    .voice_size_in  = 0,
};

static void register_audio_qube(void)
{
    audio_driver_register(&qube_audio_driver);
}
type_init(register_audio_qube);

// Called from the AAudio data callback thread. Returns bytes actually copied
// (0 if empty), caller fills the rest with silence, never blocks on qemu.
int qube_audio_read(void *dst, int max_bytes)
{
    uint8_t *out = (uint8_t *) dst;
    int n;

    if (!qube_audio_lock_ready) {
        return 0;
    }

    qemu_mutex_lock(&qube_audio_lock);
    n = qube_audio_fill < max_bytes ? qube_audio_fill : max_bytes;
    for (int i = 0; i < n; i++) {
        out[i] = qube_audio_ring[qube_audio_tail];
        qube_audio_tail = (qube_audio_tail + 1) % QUBE_AUDIO_RING_BYTES;
    }
    qube_audio_fill -= n;
    qemu_mutex_unlock(&qube_audio_lock);

    return n;
}

// Fixed output format, exported so the native side never has to guess it.
void qube_audio_get_format(int *rate, int *channels, int *bits)
{
    *rate = QUBE_AUDIO_FREQ;
    *channels = QUBE_AUDIO_CHANNELS;
    *bits = 16;
}

#endif /* __QUBE__ */
