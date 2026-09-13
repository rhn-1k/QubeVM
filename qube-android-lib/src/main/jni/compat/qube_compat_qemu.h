#ifndef QUBE_COMPAT_QEMU_H
#define QUBE_COMPAT_QEMU_H

#ifndef __ASSEMBLER__

#include <jni.h>

#ifdef __QUBE__
// declares QGE init hooks so vl.c stops implicitly declaring them under newer clang
typedef struct QemuConsole QemuConsole;
void qube_gfx_init(QemuConsole *con);
void qube_input_init(QemuConsole *con);
#endif

#endif /* __ASSEMBLER__ */

#endif
