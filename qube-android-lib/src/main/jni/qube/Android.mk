LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_ARM_MODE:= $(APP_ARM_MODE)

LOCAL_SRC_FILES :=			\
	vm-executor-jni.c \
	qge-jni.c \
	qge-input-jni.c \
	qge-audio-jni.c

LOCAL_MODULE := qube

LOCAL_C_INCLUDES :=			\
	$(LOCAL_PATH)/.. \
	$(QUBE_JNI_ROOT)/qemu \
	$(QUBE_JNI_ROOT)/qemu/include \
	$(QUBE_JNI_ROOT)/compat \
	$(QUBE_JNI_ROOT)/glib/glib \
	$(QUBE_JNI_ROOT)/glib \
	$(QUBE_JNI_ROOT)/glib/android

LOCAL_SHARED_LIBRARIES := glib-2.0

# additional libs we don't use right now
#LOCAL_SHARED_LIBRARIES += spice png

LOCAL_LDLIBS := -ldl -landroid -llog -ljnigraphics

#LOCAL_CFLAGS += $(ARCH_CFLAGS)
LOCAL_CFLAGS += -include $(LOGUTILS)

# for exceptions we don't use right now
#LOCAL_CFLAGS += -fexceptions
#LOCAL_CFLAGS += -D__STDC_CONSTANT_MACROS

LOCAL_ARM_MODE := $(ARM_MODE)

LOCAL_SHARED_LIBRARIES := compat-musl compat-qube

include $(BUILD_SHARED_LIBRARY)
