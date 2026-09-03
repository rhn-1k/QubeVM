LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
	qube_compat_filesystem.c \
	qube_compat.c \
	qube_compat_qemu.c \
	qube_compat_signals.cpp \
	qube_compat_missing.c \
	$(NDK_ROOT)/sources/android/cpufeatures/cpu-features.c

LOCAL_MODULE := compat-qube

LOCAL_C_INCLUDES :=			\
	$(LOCAL_PATH)/.. \
	$(LOCAL_PATH)/signals

LOCAL_CFLAGS += -include $(LOGUTILS)
LOCAL_ARM_MODE := $(ARM_MODE)
include $(BUILD_SHARED_LIBRARY)

