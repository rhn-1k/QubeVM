#Generic defs first
include $(QUBE_JNI_ROOT)/android-config/android-device-config/android-generic.mak

ARCH_CFLAGS += -D__ANDROID_API__=$(NDK_PLATFORM_API)

TARGET_PREFIX = aarch64-none-linux-android
ARCH_CLANG_FLAGS += -target $(TARGET_PREFIX)$(NDK_PLATFORM_API)
ARCH_CFLAGS += $(ARCH_CLANG_FLAGS) -D__ANDROID_API__=$(NDK_PLATFORM_API)
# ARCH_CFLAGS += -fno-integrated-as
ARCH_LD_FLAGS += -Wc,-target -Wc,aarch64-none-linux-android$(NDK_PLATFORM_API)

#TARGET ARCH
APP_ABI = arm64-v8a
ARM_MODE=arm

ARCH_CFLAGS += -march=armv8-a

# Tuning (Optional)
#ARCH_CFLAGS += -mtune=arm8

