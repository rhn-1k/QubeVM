#include <jni.h>
#include <stdlib.h>
#include <stdint.h>
#include <string.h>
#include <pthread.h>
#include <unistd.h>
#include <stdatomic.h>
#include <aaudio/AAudio.h>
#include <dlfcn.h>
#include "qube_logutils.h"

#define TAG "QubeAudio"

// Blocking-write push thread instead of an AAudio data callback, started/stopped
// from Java in lockstep with gfxLoop, no wait on the guest sound card.
typedef int (*qube_audio_read_t)(void *dst, int max_bytes);
typedef void (*qube_audio_get_format_t)(int *rate, int *channels, int *bits);

// handle is declared in vm-executor-jni.c, set once qemu's .so is dlopen'd
extern void *handle;

static qube_audio_read_t qube_audio_read_fn = NULL;
static qube_audio_get_format_t qube_audio_get_format_fn = NULL;

static AAudioStream *stream = NULL;
static int logged_started = 0;

// Gate for the thread below, true while gfxLoop wants sound (same span as
// gfxLoopRunning in QubeQGEActivity), false across onPause like everything else.
static atomic_bool audio_enabled = false;

// Frames pulled/written per loop iteration, kept small for low latency.
#define QUBE_AUDIO_WRITE_FRAMES 240

static void error_callback(AAudioStream *stream, void *userData, aaudio_result_t error)
{
	LOGE("AAudio error: %s", AAudio_convertResultToText(error));
}

static int resolve_audio_syms(void)
{
	if (qube_audio_read_fn && qube_audio_get_format_fn) {
		return 1;
	}
	if (!handle) {
		return 0;
	}
	dlerror();
	qube_audio_read_fn = (qube_audio_read_t) dlsym(handle, "qube_audio_read");
	qube_audio_get_format_fn = (qube_audio_get_format_t) dlsym(handle, "qube_audio_get_format");
	if (!qube_audio_read_fn || !qube_audio_get_format_fn) {
		LOGE("Cannot resolve qube_audio_* symbols: %s", dlerror());
		return 0;
	}
	return 1;
}

// Blocking-write AAudio stream, no data callback, same push model a PC backend uses.
static int open_stream(void)
{
	AAudioStreamBuilder *builder = NULL;
	aaudio_result_t result;
	int rate, channels, bits;

	result = AAudio_createStreamBuilder(&builder);
	if (result != AAUDIO_OK) {
		LOGE("AAudio_createStreamBuilder failed: %s", AAudio_convertResultToText(result));
		return 0;
	}

	qube_audio_get_format_fn(&rate, &channels, &bits);

	AAudioStreamBuilder_setDirection(builder, AAUDIO_DIRECTION_OUTPUT);
	AAudioStreamBuilder_setSampleRate(builder, rate);
	AAudioStreamBuilder_setChannelCount(builder, channels);
	AAudioStreamBuilder_setFormat(builder, AAUDIO_FORMAT_PCM_I16);
	AAudioStreamBuilder_setPerformanceMode(builder, AAUDIO_PERFORMANCE_MODE_LOW_LATENCY);
	AAudioStreamBuilder_setErrorCallback(builder, error_callback, NULL);

	result = AAudioStreamBuilder_openStream(builder, &stream);
	AAudioStreamBuilder_delete(builder);
	if (result != AAUDIO_OK) {
		LOGE("AAudio_openStream failed: %s", AAudio_convertResultToText(result));
		stream = NULL;
		return 0;
	}

	result = AAudioStream_requestStart(stream);
	if (result != AAUDIO_OK) {
		LOGE("AAudioStream_requestStart failed: %s", AAudio_convertResultToText(result));
		AAudioStream_close(stream);
		stream = NULL;
		return 0;
	}

	if (!logged_started) {
		LOGI("AAudio started: %dHz, %d ch, %d bits", rate, channels, bits);
		logged_started = 1;
	}
	return 1;
}

// Runs for the whole process lifetime, but only touches AAudio while
// audio_enabled is set, same on/off span as gfxLoop, just push instead of pull.
static void *audio_thread_main(void *arg)
{
	int16_t buf[QUBE_AUDIO_WRITE_FRAMES * 2]; // stereo S16

	for (;;) {
		if (!atomic_load(&audio_enabled)) {
			usleep(20 * 1000);
			continue;
		}
		if (!resolve_audio_syms() || (!stream && !open_stream())) {
			usleep(100 * 1000);
			continue;
		}

		int bytes_to_read = sizeof(buf);
		int bytes_read = qube_audio_read_fn(buf, bytes_to_read);
		if (bytes_read < bytes_to_read) {
			memset((uint8_t *) buf + bytes_read, 0, bytes_to_read - bytes_read);
		}

		aaudio_result_t written = AAudioStream_write(stream, buf, QUBE_AUDIO_WRITE_FRAMES,
				100 * 1000 * 1000LL); // 100ms timeout
		if (written < 0) {
			LOGE("AAudioStream_write failed: %s", AAudio_convertResultToText(written));
			AAudioStream_requestStop(stream);
			AAudioStream_close(stream);
			stream = NULL;
		}
	}
	return NULL;
}

// Thread is parked from load, waiting on audio_enabled, nothing AAudio-side
// happens until nativeStartAudio flips it, so cold boot still costs nothing.
__attribute__((constructor))
static void qube_audio_spawn_thread(void)
{
	pthread_t t;
	pthread_create(&t, NULL, audio_thread_main, NULL);
	pthread_detach(t);
}

// Same call site/timing as before (startGfxLoop), just flips the gate now,
// the thread itself was already alive from process start.
JNIEXPORT jboolean JNICALL Java_com_max2idea_android_qube_jni_QubeAudio_nativeStartAudio(
		JNIEnv *env, jclass clazz)
{
	atomic_store(&audio_enabled, true);
	return JNI_TRUE;
}

// Mirrors stopGfxLoop, close the stream so nothing keeps playing after onPause.
JNIEXPORT void JNICALL Java_com_max2idea_android_qube_jni_QubeAudio_nativeStopAudio(
		JNIEnv *env, jclass clazz)
{
	atomic_store(&audio_enabled, false);
	if (stream) {
		AAudioStream_requestStop(stream);
		AAudioStream_close(stream);
		stream = NULL;
		LOGI("AAudio stopped");
	}
}
