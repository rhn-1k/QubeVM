#Generic defs first
include $(QUBE_JNI_ROOT)/android-config/android-device-config/android-generic.mak

ARCH_CFLAGS += -D__ANDROID_API__=$(NDK_PLATFORM_API)
ARCH_LD_FLAGS += -latomic

TARGET_PREFIX = i686-none-linux-android
ARCH_CLANG_FLAGS += -target $(TARGET_PREFIX)$(NDK_PLATFORM_API)
ARCH_CFLAGS += $(ARCH_CLANG_FLAGS) -D__ANDROID_API__=$(NDK_PLATFORM_API)
#ARCH_CFLAGS += -fno-integrated-as
ARCH_LD_FLAGS += -Wc,-target -Wc,i686-none-linux-android$(NDK_PLATFORM_API)

#TARGET ARCH
APP_ABI = x86




