// Qube Graphics Engine input bridge, JNI side
#include <jni.h>
#include <dlfcn.h>
#include "qube_compat.h"

// defined in vm-executor-jni.c, set once qemu's .so is dlopen'd
extern void *handle;

typedef void (*qube_input_send_key_t)(uint32_t sym, int down);
typedef void (*qube_input_send_pointer_t)(int x, int y, int button_mask, int width, int height);
typedef void (*qube_input_send_pointer_rel_t)(int dx, int dy, int button_mask);

static qube_input_send_key_t qube_input_send_key_fn = NULL;
static qube_input_send_pointer_t qube_input_send_pointer_fn = NULL;
static qube_input_send_pointer_rel_t qube_input_send_pointer_rel_fn = NULL;
static int qube_input_syms_resolved = 0;

static int resolve_qube_input_syms(void) {
	if (qube_input_syms_resolved) {
		return qube_input_send_key_fn && qube_input_send_pointer_fn;
	}
	if (!handle) {
		return 0;
	}
	dlerror();
	qube_input_send_key_fn = (qube_input_send_key_t) dlsym(handle, "qube_input_send_key");
	qube_input_send_pointer_fn = (qube_input_send_pointer_t) dlsym(handle, "qube_input_send_pointer");
	// present on supported qemu versions, resolved as optional so a missing symbol
	// just no-ops the rel entrypoint instead of crashing on an unexpected qemu build
	qube_input_send_pointer_rel_fn = (qube_input_send_pointer_rel_t) dlsym(handle, "qube_input_send_pointer_rel");
	qube_input_syms_resolved = 1;
	if (!qube_input_send_key_fn || !qube_input_send_pointer_fn) {
		LOGE("Cannot resolve qube_input symbols: %s\n", dlerror());
		return 0;
	}
	return 1;
}

JNIEXPORT void JNICALL Java_com_max2idea_android_qube_jni_QubeInput_nativeSendKeyEvent(
		JNIEnv* env, jclass clazz, jlong keysym, jboolean down) {
	if (!resolve_qube_input_syms()) {
		return;
	}
	qube_input_send_key_fn((uint32_t) keysym, down);
}

JNIEXPORT void JNICALL Java_com_max2idea_android_qube_jni_QubeInput_nativeSendPointerEvent(
		JNIEnv* env, jclass clazz, jint x, jint y, jint buttonMask, jint width, jint height) {
	if (!resolve_qube_input_syms()) {
		return;
	}
	qube_input_send_pointer_fn(x, y, buttonMask, width, height);
}

JNIEXPORT void JNICALL Java_com_max2idea_android_qube_jni_QubeInput_nativeSendPointerEventRel(
		JNIEnv* env, jclass clazz, jint dx, jint dy, jint buttonMask) {
	if (!resolve_qube_input_syms() || !qube_input_send_pointer_rel_fn) {
		return;
	}
	qube_input_send_pointer_rel_fn(dx, dy, buttonMask);
}
