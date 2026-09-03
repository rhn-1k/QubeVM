/*
Copyright (C) Max Kastanas 2012
Copyright (C) Rhn 2026
 */

#ifndef VM_EXECUTOR_JNI_H
#define VM_EXECUTOR_JNI_H

#include <stdbool.h>
#include <stdint.h>
#include <stdio.h>
#include "qemu/typedefs.h"

typedef struct Error Error;


void * loadLib(const char* lib_filename, const char * lib_path_str);

void setup_jni(JNIEnv* env, jobject thiz, jstring storage_dir, jstring base_dir);

int get_qemu_var(JNIEnv* env, jobject thiz, const char * var);

void set_qemu_var(JNIEnv* env, jobject thiz, const char * var, jint jvalue);

JNIEXPORT void JNICALL Java_com_max2idea_android_qube_jni_VMExecutor_nativeRefreshScreen(
                JNIEnv* env, jobject thiz, jint jvalue);
                
JNIEXPORT jstring JNICALL Java_com_max2idea_android_qube_jni_VMExecutor_start(
        JNIEnv* env, jobject thiz,
		jstring storage_dir, jstring base_dir,
		jstring lib_filename, jstring lib_path,
		jobjectArray params);
        
JNIEXPORT jstring JNICALL Java_com_max2idea_android_qube_jni_VMExecutor_stop(
		JNIEnv* env, jobject thiz, jint jint_restart);

#endif

