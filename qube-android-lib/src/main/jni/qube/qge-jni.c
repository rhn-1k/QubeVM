 /*
Copyright (C) Rhn 2026
 */
// Qube Graphics Engine bridge, JNI side
#include <jni.h>
#include <dlfcn.h>
#include <android/bitmap.h>
#include "qube_compat.h"

// defined in vm-executor-jni.c, set once qemu's .so is dlopen'd
extern void *handle;

typedef int (*qube_gfx_dim_t)(void);
typedef int (*qube_gfx_copy_frame_t)(void *dst, int dst_stride, int max_w, int max_h);
typedef void (*qube_gfx_set_interval_t)(int ms);

static qube_gfx_dim_t qube_gfx_get_width_fn = NULL;
static qube_gfx_dim_t qube_gfx_get_height_fn = NULL;
static qube_gfx_copy_frame_t qube_gfx_copy_frame_fn = NULL;
static qube_gfx_set_interval_t qube_gfx_set_interval_fn = NULL;
static int qube_gfx_syms_resolved = 0;
// Track which handle we resolved symbols against to re-resolve after VM restarts
// (dlclose/dlopen can return a different library address)
static void *qube_gfx_resolved_handle = NULL;

static int resolve_qube_gfx_syms(void) {
	if (qube_gfx_syms_resolved && qube_gfx_resolved_handle == handle) {
		return qube_gfx_get_width_fn && qube_gfx_get_height_fn
				&& qube_gfx_copy_frame_fn;
	}
	if (!handle) {
		qube_gfx_syms_resolved = 0;
		qube_gfx_resolved_handle = NULL;
		return 0;
	}
	dlerror();
	qube_gfx_get_width_fn = (qube_gfx_dim_t) dlsym(handle, "qube_gfx_get_width");
	qube_gfx_get_height_fn = (qube_gfx_dim_t) dlsym(handle, "qube_gfx_get_height");
	qube_gfx_copy_frame_fn = (qube_gfx_copy_frame_t) dlsym(handle, "qube_gfx_copy_frame");
	// Older qemu .so builds might not export these yet, don't fail resolve() over it.
	qube_gfx_set_interval_fn = (qube_gfx_set_interval_t) dlsym(handle, "qube_gfx_set_interval_ms");
	qube_gfx_syms_resolved = 1;
	qube_gfx_resolved_handle = handle;
	if (!qube_gfx_get_width_fn || !qube_gfx_get_height_fn || !qube_gfx_copy_frame_fn) {
		LOGE("Cannot resolve qube_gfx_* symbols: %s\n", dlerror());
		return 0;
	}
	return 1;
}

JNIEXPORT jint JNICALL Java_com_max2idea_android_qube_jni_QubeGfx_nativeGetWidth(
		JNIEnv* env, jclass clazz) {
	if (!resolve_qube_gfx_syms()) {
		return 0;
	}
	return qube_gfx_get_width_fn();
}

JNIEXPORT jint JNICALL Java_com_max2idea_android_qube_jni_QubeGfx_nativeGetHeight(
		JNIEnv* env, jclass clazz) {
	if (!resolve_qube_gfx_syms()) {
		return 0;
	}
	return qube_gfx_get_height_fn();
}

// Copies the current guest frame into bitmap, sized to nativeGetWidth/Height, ARGB_8888.
// Returns the generation counter, or 0 if unavailable, compare to the previous call to skip redundant redraws.
JNIEXPORT jint JNICALL Java_com_max2idea_android_qube_jni_QubeGfx_nativeCopyFrame(
		JNIEnv* env, jclass clazz, jobject bitmap) {
	if (!resolve_qube_gfx_syms()) {
		return 0;
	}

	AndroidBitmapInfo info;
	void *pixels;
	if (AndroidBitmap_getInfo(env, bitmap, &info) < 0) {
		return 0;
	}
	if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
		return 0;
	}
	if (AndroidBitmap_lockPixels(env, bitmap, &pixels) < 0) {
		return 0;
	}

	int generation = qube_gfx_copy_frame_fn(pixels, (int) info.stride,
			(int) info.width, (int) info.height);

	AndroidBitmap_unlockPixels(env, bitmap);
	return generation;
}

// Pushes the Refresh Rate setting (Hz) into qemu's frame pump
// Safe to call any time, including mid-session
JNIEXPORT void JNICALL Java_com_max2idea_android_qube_jni_QubeGfx_nativeSetRefreshRate(
		JNIEnv* env, jclass clazz, jint hz) {
	if (!resolve_qube_gfx_syms() || !qube_gfx_set_interval_fn) {
		return;
	}
	if (hz < 1) {
		hz = 1;
	}
	int ms = (1000 + hz / 2) / hz; // round to nearest ms
	qube_gfx_set_interval_fn(ms);
}
