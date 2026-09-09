#Generic defs first
include $(QUBE_JNI_ROOT)/android-config/android-device-config/android-generic.mak

ARCH_CFLAGS += -D__ANDROID_API__=$(NDK_PLATFORM_API)

TARGET_PREFIX = armv7-none-linux-androideabi
ARCH_CLANG_FLAGS += -target $(TARGET_PREFIX)$(NDK_PLATFORM_API)
ARCH_CFLAGS += $(ARCH_CLANG_FLAGS) -D__ANDROID_API__=$(NDK_PLATFORM_API)
# ARCH_CFLAGS += -fno-integrated-as
ARCH_LD_FLAGS += -Wc,-target -Wc,armv7-none-linux-androideabi$(NDK_PLATFORM_API)

#LINKER SPECIFIC
#ARCH_LD_FLAGS += -Wl,--fix-cortex-a8

#TARGET ARCH
APP_ABI = armeabi-v7a
ARM_MODE=arm

ARCH_CFLAGS += -march=armv7-a

#FLOAT
ARCH_CFLAGS += -mfloat-abi=softfp

# Use VFP (Optional)
#ARCH_CFLAGS += -mfpu=vfpv3-d16
#ARCH_CFLAGS += -mfpu=vfpv3

# Tuning (Optional)
#ARCH_CFLAGS += -mtune=arm7




