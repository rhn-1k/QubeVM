#include <jni.h>
#include <stdlib.h>
#include <stdint.h>
#include <stdbool.h>
#include <string.h>
#include <pthread.h>
#include <stdatomic.h>
#include <dlfcn.h>
#include <android/api-level.h>
#include <aaudio/AAudio.h>
#include "qube_logutils.h"

#define TAG "QubeAudio"
#define LIB_AAUDIO_SO "libaaudio.so"
#define QUBE_AUDIO_RATE 48000
#define QUBE_AUDIO_CHANNELS 2
#define QUBE_AUDIO_BYTES_PER_FRAME (QUBE_AUDIO_CHANNELS * sizeof(int16_t))
#define QUBE_AUDIO_CALLBACK_FRAMES (QUBE_AUDIO_RATE / 50) /* SDL default: 20 ms */

// QEMU exports these from the version-specific qube-audio.c driver.
typedef int (*qube_audio_read_t)(void *dst, int max_bytes);
typedef void (*qube_audio_flush_t)(void);
typedef void (*qube_audio_get_format_t)(int *rate, int *channels, int *bits);
extern void *handle;

// SDL loads AAudio dynamically so the backend remains safe on Android releases
// where AAudio is unavailable. Keep required and optional functions separate,
// just as SDL does in SDL_aaudiofuncs.h.
typedef struct QubeAAudioApi {
    void *library;
    const char *(*convertResultToText)(aaudio_result_t returnCode);
    aaudio_result_t (*createStreamBuilder)(AAudioStreamBuilder **builder);
    void (*builderSetSampleRate)(AAudioStreamBuilder *builder, int32_t sampleRate);
    void (*builderSetChannelCount)(AAudioStreamBuilder *builder, int32_t channelCount);
    void (*builderSetFormat)(AAudioStreamBuilder *builder, aaudio_format_t format);
    void (*builderSetDirection)(AAudioStreamBuilder *builder, aaudio_direction_t direction);
    void (*builderSetBufferCapacityInFrames)(AAudioStreamBuilder *builder, int32_t numFrames);
    void (*builderSetPerformanceMode)(AAudioStreamBuilder *builder, aaudio_performance_mode_t mode);
    void (*builderSetDataCallback)(AAudioStreamBuilder *builder, AAudioStream_dataCallback callback, void *userData);
    void (*builderSetFramesPerDataCallback)(AAudioStreamBuilder *builder, int32_t numFrames);
    void (*builderSetErrorCallback)(AAudioStreamBuilder *builder, AAudioStream_errorCallback callback, void *userData);
    aaudio_result_t (*builderOpenStream)(AAudioStreamBuilder *builder, AAudioStream **stream);
    aaudio_result_t (*builderDelete)(AAudioStreamBuilder *builder);
    aaudio_result_t (*streamClose)(AAudioStream *stream);
    aaudio_result_t (*streamRequestStart)(AAudioStream *stream);
    aaudio_result_t (*streamRequestPause)(AAudioStream *stream);
    aaudio_result_t (*streamRequestStop)(AAudioStream *stream);
    int32_t (*streamGetBufferCapacityInFrames)(AAudioStream *stream);
    int32_t (*streamGetFramesPerDataCallback)(AAudioStream *stream);
    int32_t (*streamGetSampleRate)(AAudioStream *stream);
    int32_t (*streamGetChannelCount)(AAudioStream *stream);
    aaudio_format_t (*streamGetFormat)(AAudioStream *stream);
    void (*builderSetUsage)(AAudioStreamBuilder *builder, aaudio_usage_t usage); /* API 28 */
} QubeAAudioApi;

static QubeAAudioApi aa;
static pthread_mutex_t stream_mutex = PTHREAD_MUTEX_INITIALIZER;
static pthread_mutex_t recovery_mutex = PTHREAD_MUTEX_INITIALIZER;
static pthread_cond_t recovery_condition = PTHREAD_COND_INITIALIZER;
static AAudioStream *stream;
static atomic_bool audio_enabled;
static atomic_bool recovery_thread_started;
static atomic_int error_callback_triggered;
static qube_audio_read_t qube_audio_read_fn;
static qube_audio_flush_t qube_audio_flush_fn;
static qube_audio_get_format_t qube_audio_get_format_fn;

static const char *aaudio_error(aaudio_result_t result)
{
    return aa.convertResultToText ? aa.convertResultToText(result) : "unknown AAudio error";
}

#define LOAD_REQUIRED(field, symbol) do { \
    *(void **)(&aa.field) = dlsym(aa.library, symbol); \
    if (!aa.field) { \
        LOGE("Missing AAudio symbol %s", symbol); \
        dlclose(aa.library); \
        memset(&aa, 0, sizeof(aa)); \
        return 0; \
    } \
} while (0)

static int load_aaudio(void)
{
    if (aa.library) {
        return 1;
    }
    // SDL avoids AAudio on Android 8.0 because of reference-counting crashes.
    if (android_get_device_api_level() < 27) {
        LOGI("AAudio disabled below Android API 27");
        return 0;
    }

    aa.library = dlopen(LIB_AAUDIO_SO, RTLD_NOW | RTLD_LOCAL);
    if (!aa.library) {
        LOGE("Could not load %s: %s", LIB_AAUDIO_SO, dlerror());
        memset(&aa, 0, sizeof(aa));
        return 0;
    }

    LOAD_REQUIRED(convertResultToText, "AAudio_convertResultToText");
    LOAD_REQUIRED(createStreamBuilder, "AAudio_createStreamBuilder");
    LOAD_REQUIRED(builderSetSampleRate, "AAudioStreamBuilder_setSampleRate");
    LOAD_REQUIRED(builderSetChannelCount, "AAudioStreamBuilder_setChannelCount");
    LOAD_REQUIRED(builderSetFormat, "AAudioStreamBuilder_setFormat");
    LOAD_REQUIRED(builderSetDirection, "AAudioStreamBuilder_setDirection");
    LOAD_REQUIRED(builderSetBufferCapacityInFrames, "AAudioStreamBuilder_setBufferCapacityInFrames");
    LOAD_REQUIRED(builderSetPerformanceMode, "AAudioStreamBuilder_setPerformanceMode");
    LOAD_REQUIRED(builderSetDataCallback, "AAudioStreamBuilder_setDataCallback");
    LOAD_REQUIRED(builderSetFramesPerDataCallback, "AAudioStreamBuilder_setFramesPerDataCallback");
    LOAD_REQUIRED(builderSetErrorCallback, "AAudioStreamBuilder_setErrorCallback");
    LOAD_REQUIRED(builderOpenStream, "AAudioStreamBuilder_openStream");
    LOAD_REQUIRED(builderDelete, "AAudioStreamBuilder_delete");
    LOAD_REQUIRED(streamClose, "AAudioStream_close");
    LOAD_REQUIRED(streamRequestStart, "AAudioStream_requestStart");
    LOAD_REQUIRED(streamRequestPause, "AAudioStream_requestPause");
    LOAD_REQUIRED(streamRequestStop, "AAudioStream_requestStop");
    LOAD_REQUIRED(streamGetBufferCapacityInFrames, "AAudioStream_getBufferCapacityInFrames");
    LOAD_REQUIRED(streamGetFramesPerDataCallback, "AAudioStream_getFramesPerDataCallback");
    LOAD_REQUIRED(streamGetSampleRate, "AAudioStream_getSampleRate");
    LOAD_REQUIRED(streamGetChannelCount, "AAudioStream_getChannelCount");
    LOAD_REQUIRED(streamGetFormat, "AAudioStream_getFormat");
    *(void **)(&aa.builderSetUsage) = dlsym(aa.library, "AAudioStreamBuilder_setUsage");
    return 1;
}
#undef LOAD_REQUIRED

static int resolve_qemu_audio(void)
{
    if (qube_audio_read_fn && qube_audio_flush_fn && qube_audio_get_format_fn) {
        return 1;
    }
    if (!handle) {
        return 0;
    }
    dlerror();
    qube_audio_read_fn = (qube_audio_read_t)dlsym(handle, "qube_audio_read");
    qube_audio_flush_fn = (qube_audio_flush_t)dlsym(handle, "qube_audio_flush");
    qube_audio_get_format_fn = (qube_audio_get_format_t)dlsym(handle, "qube_audio_get_format");
    if (!qube_audio_read_fn || !qube_audio_flush_fn || !qube_audio_get_format_fn) {
        LOGE("Cannot resolve qube audio symbols: %s", dlerror());
        qube_audio_read_fn = NULL;
        qube_audio_flush_fn = NULL;
        qube_audio_get_format_fn = NULL;
        return 0;
    }
    return 1;
}

static void signal_recovery(void)
{
    pthread_mutex_lock(&recovery_mutex);
    pthread_cond_signal(&recovery_condition);
    pthread_mutex_unlock(&recovery_mutex);
}

static void aaudio_error_callback(AAudioStream *audio_stream, void *user_data, aaudio_result_t error)
{
    (void)audio_stream;
    (void)user_data;
    LOGE("AAudio error callback: %d (%s)", error, aaudio_error(error));
    atomic_store_explicit(&error_callback_triggered, (int)error, memory_order_release);
    signal_recovery();
}

static aaudio_data_callback_result_t aaudio_data_callback(AAudioStream *audio_stream,
                                                            void *user_data,
                                                            void *audio_data,
                                                            int32_t num_frames)
{
    (void)audio_stream;
    (void)user_data;
    const int callback_bytes = num_frames * QUBE_AUDIO_BYTES_PER_FRAME;
    int bytes_read = 0;

    // The callback must never wait for QEMU. This matches SDL’s callback path:
    // copy what is ready and clear the rest with the device silence value.
    if (atomic_load_explicit(&audio_enabled, memory_order_acquire) && qube_audio_read_fn) {
        bytes_read = qube_audio_read_fn(audio_data, callback_bytes);
        if (bytes_read < 0 || bytes_read > callback_bytes) {
            bytes_read = 0;
        }
    }
    if (bytes_read < callback_bytes) {
        memset((uint8_t *)audio_data + bytes_read, 0, (size_t)(callback_bytes - bytes_read));
    }
    return AAUDIO_CALLBACK_RESULT_CONTINUE;
}

static int open_stream_locked(void)
{
    AAudioStreamBuilder *builder = NULL;
    AAudioStream *opened_stream = NULL;
    aaudio_result_t result;
    int qemu_rate, qemu_channels, qemu_bits;

    if (!resolve_qemu_audio()) {
        LOGE("QEMU audio symbols are not available yet");
        return 0;
    }
    qube_audio_get_format_fn(&qemu_rate, &qemu_channels, &qemu_bits);
    if (qemu_rate != QUBE_AUDIO_RATE || qemu_channels != QUBE_AUDIO_CHANNELS || qemu_bits != 16) {
        LOGE("Unsupported QEMU audio format: %d Hz, %d channels, %d bits", qemu_rate, qemu_channels, qemu_bits);
        return 0;
    }

    result = aa.createStreamBuilder(&builder);
    if (result != AAUDIO_OK || !builder) {
        LOGE("AAudio_createStreamBuilder failed: %d (%s)", result, aaudio_error(result));
        return 0;
    }

    aa.builderSetDirection(builder, AAUDIO_DIRECTION_OUTPUT);
    aa.builderSetFormat(builder, AAUDIO_FORMAT_PCM_I16);
    aa.builderSetSampleRate(builder, QUBE_AUDIO_RATE);
    aa.builderSetChannelCount(builder, QUBE_AUDIO_CHANNELS);
    // AAudio requires capacity >= 2 * callback size. SDL uses this exact rule.
    aa.builderSetBufferCapacityInFrames(builder, 2 * QUBE_AUDIO_CALLBACK_FRAMES);
    aa.builderSetFramesPerDataCallback(builder, QUBE_AUDIO_CALLBACK_FRAMES);
    aa.builderSetDataCallback(builder, aaudio_data_callback, NULL);
    aa.builderSetErrorCallback(builder, aaudio_error_callback, NULL);
    aa.builderSetPerformanceMode(builder, AAUDIO_PERFORMANCE_MODE_LOW_LATENCY);
    if (aa.builderSetUsage) {
        // SDL defaults to media when no stream-role hint is supplied.
        aa.builderSetUsage(builder, AAUDIO_USAGE_MEDIA);
    }

    LOGI("Opening AAudio output: %d Hz, %d channels, callback %d frames",
         QUBE_AUDIO_RATE, QUBE_AUDIO_CHANNELS, QUBE_AUDIO_CALLBACK_FRAMES);
    result = aa.builderOpenStream(builder, &opened_stream);
    aa.builderDelete(builder);
    if (result != AAUDIO_OK || !opened_stream) {
        LOGE("AAudioStreamBuilder_openStream failed: %d (%s)", result, aaudio_error(result));
        return 0;
    }

    const int actual_rate = aa.streamGetSampleRate(opened_stream);
    const int actual_channels = aa.streamGetChannelCount(opened_stream);
    const aaudio_format_t actual_format = aa.streamGetFormat(opened_stream);
    const int actual_callback_frames = aa.streamGetFramesPerDataCallback(opened_stream);
    const int actual_capacity_frames = aa.streamGetBufferCapacityInFrames(opened_stream);
    LOGI("AAudio opened: %d Hz, %d channels, format %d, callback %d, capacity %d",
         actual_rate, actual_channels, actual_format, actual_callback_frames, actual_capacity_frames);

    if (actual_rate != QUBE_AUDIO_RATE || actual_channels != QUBE_AUDIO_CHANNELS ||
            actual_format != AAUDIO_FORMAT_PCM_I16) {
        LOGE("AAudio negotiated an incompatible format");
        aa.streamClose(opened_stream);
        return 0;
    }

    atomic_store_explicit(&error_callback_triggered, 0, memory_order_release);
    result = aa.streamRequestStart(opened_stream);
    if (result != AAUDIO_OK) {
        LOGE("AAudioStream_requestStart failed: %d (%s)", result, aaudio_error(result));
        aa.streamClose(opened_stream);
        return 0;
    }
    stream = opened_stream;
    LOGI("AAudio output started");
    return 1;
}

static void close_stream_locked(void)
{
    if (stream) {
        aa.streamRequestStop(stream);
        aa.streamClose(stream);
        stream = NULL;
        LOGI("AAudio output stopped");
    }
}

static void recover_stream(void)
{
    pthread_mutex_lock(&stream_mutex);
    if (atomic_load_explicit(&audio_enabled, memory_order_acquire) && stream) {
        LOGW("Recovering AAudio output after callback error");
        close_stream_locked();
        open_stream_locked();
    }
    atomic_store_explicit(&error_callback_triggered, 0, memory_order_release);
    pthread_mutex_unlock(&stream_mutex);
}

static void *recovery_thread_main(void *unused)
{
    (void)unused;
    for (;;) {
        pthread_mutex_lock(&recovery_mutex);
        while (!atomic_load_explicit(&error_callback_triggered, memory_order_acquire) ||
               !atomic_load_explicit(&audio_enabled, memory_order_acquire)) {
            pthread_cond_wait(&recovery_condition, &recovery_mutex);
        }
        pthread_mutex_unlock(&recovery_mutex);
        recover_stream();
    }
    return NULL;
}

static int ensure_recovery_thread(void)
{
    bool expected = false;
    if (!atomic_compare_exchange_strong(&recovery_thread_started, &expected, true)) {
        return 1;
    }
    pthread_t thread;
    if (pthread_create(&thread, NULL, recovery_thread_main, NULL) != 0) {
        atomic_store(&recovery_thread_started, false);
        LOGE("Could not create AAudio recovery thread");
        return 0;
    }
    pthread_detach(thread);
    return 1;
}

JNIEXPORT jboolean JNICALL Java_com_max2idea_android_qube_jni_QubeAudio_nativeStartAudio(
        JNIEnv *env, jclass clazz)
{
    (void)env;
    (void)clazz;
    if (!load_aaudio() || !resolve_qemu_audio() || !ensure_recovery_thread()) {
        return JNI_FALSE;
    }

    atomic_store_explicit(&audio_enabled, true, memory_order_release);
    pthread_mutex_lock(&stream_mutex);
    if (!stream && !open_stream_locked()) {
        atomic_store_explicit(&audio_enabled, false, memory_order_release);
        pthread_mutex_unlock(&stream_mutex);
        return JNI_FALSE;
    }
    pthread_mutex_unlock(&stream_mutex);
    signal_recovery();
    return JNI_TRUE;
}

JNIEXPORT void JNICALL Java_com_max2idea_android_qube_jni_QubeAudio_nativeStopAudio(
        JNIEnv *env, jclass clazz)
{
    (void)env;
    (void)clazz;
    atomic_store_explicit(&audio_enabled, false, memory_order_release);
    atomic_store_explicit(&error_callback_triggered, 0, memory_order_release);
    if (qube_audio_flush_fn) {
        qube_audio_flush_fn();
    }
    pthread_mutex_lock(&stream_mutex);
    close_stream_locked();
    pthread_mutex_unlock(&stream_mutex);
}
