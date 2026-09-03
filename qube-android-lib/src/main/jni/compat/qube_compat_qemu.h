#ifndef QUBE_COMPAT_QEMU_H
#define QUBE_COMPAT_QEMU_H

#ifndef __ASSEMBLER__

#include <jni.h>

typedef struct sdl_res_t {
	int width;
	int height;
} sdl_res_t;

void Android_JNI_SetVMResolution(int width, int height);

#endif /* __ASSEMBLER__ */

#endif
