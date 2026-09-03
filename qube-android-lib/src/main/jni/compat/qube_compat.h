#ifndef QUBE_COMPAT_H
#define QUBE_COMPAT_H

#ifndef __ASSEMBLER__
// Qube: this header is force-included into every compiled source,
// including .S files. __ASSEMBLER__ is predefined by clang when
// preprocessing assembly, so guard the C-only content.

#include <jni.h>
#include <pthread.h>

extern JavaVM *jvm;
extern jobject jobj;
extern jclass jcls;
extern pthread_mutex_t fd_lock;
extern const char * storage_base_dir;
extern const char * qube_base_dir;

void set_jni(JNIEnv* env, jobject obj1, jclass jclass1, const char * storage_dir, const char * qube_dir);
void * valloc (size_t size);

#endif /* __ASSEMBLER__ */

#endif
