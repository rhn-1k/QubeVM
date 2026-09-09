# Do not modify this file, all configuration is under directory android-config

QUBE_JNI_ROOT:=$(shell dirname $(realpath $(lastword $(MAKEFILE_LIST))))
include $(QUBE_JNI_ROOT)/android-config/android-qube-config.mak

# prepend the NDK_ROOT in the path so the ndk-build is the correct one
PATH  := $(NDK_ROOT):$(PATH)
SHELL := env PATH=$(PATH) /bin/bash

#PLATFORM CONFIG
# Ideally App platform used to compile should be equal or lower than the minSdkVersion in AndroidManifest.xml
APP_PLATFORM = android-$(NDK_PLATFORM_API)
NDK_PLATFORM = platforms/$(APP_PLATFORM)

ifeq ($(USE_NDK_PLATFORM21),true)
USE_PLATFORM21_FLAGS = -D__ANDROID_HAS_SIGNAL__ \
	-D__ANDROID_HAS_FS_IOC__ \
	-D__ANDROID_HAS_SYS_GETTID__ \
	-D__ANDROID_HAS_PARPORT__ \
	-D__ANDROID_HAS_IEEE__ \
	-D__ANDROID_HAS_STATVFS__ \
	-D__ANDROID__HAS_PTHREAD_ATFORK_
endif

ifeq ($(USE_NDK_PLATFORM26),true)
USE_PLATFORM26_FLAGS = -D__ANDROID_HAVE_STRCHRNUL__
endif

#SET/RESET vars
ARCH_CFLAGS := -D__QUBE__ -D__ANDROID__ -DANDROID -D__linux__ -DCONFIG_LINUX $(USE_NDK11) \
  $(USE_PLATFORM21_FLAGS) $(USE_PLATFORM26_FLAGS)
ARCH_LD_FLAGS=

ifeq ($(BUILD_HOST), arm64-v8a)
######### Armv8 64 bit (Newest ARM phones only)
include $(QUBE_JNI_ROOT)/android-config/android-device-config/android-armv8.mak
else ifeq ($(BUILD_HOST), armeabi-v7a)
######### ARMv7 Soft Float (Most ARM phones
include $(QUBE_JNI_ROOT)/android-config/android-device-config/android-armv7a-softfp.mak
else ifeq ($(BUILD_HOST), x86)
######### x86 (x86 Phones only)
include $(QUBE_JNI_ROOT)/android-config/android-device-config/android-x86.mak
else ifeq ($(BUILD_HOST), x86_64)
######### x86_64 (x86 64bit Phones only)
include $(QUBE_JNI_ROOT)/android-config/android-device-config/android-x86_64.mak
endif

ifeq ($(APP_ABI),armeabi-v7a)
    HOST_PREFIX = arm-linux-androideabi
    GNU_HOST = arm-unknown-linux-android
    TARGET_ARCH=arm
    APP_ABI_DIR=$(APP_ABI)
else ifeq ($(APP_ABI),arm64-v8a)
    HOST_PREFIX = aarch64-linux-android
    GNU_HOST = aarch64-unknown-linux-android
    TARGET_ARCH=arm64
    APP_ABI_DIR=$(APP_ABI)
else ifeq ($(APP_ABI),x86)
    HOST_PREFIX = i686-linux-android
    GNU_HOST = i686-unknown-linux-android
    TARGET_ARCH=x86
    APP_ABI_DIR=$(APP_ABI)
else ifeq ($(APP_ABI),x86_64)
    HOST_PREFIX = x86_64-linux-android
    GNU_HOST = x86_64-unknown-linux-android
    TARGET_ARCH=x86_64
    APP_ABI_DIR=$(APP_ABI)
endif


# Since we need ndk 11 and above we need to fix some missing calls
USE_NDK11 = -D__NDK11_FUNC_MISSING__

# Qube: Clang-only toolchain (unified NDK toolchain, r23+)
TOOLCHAIN_CLANG_DIR = $(NDK_ROOT)/toolchains/llvm/prebuilt/$(NDK_ENV)

NDK_PROJECT_PATH := $(QUBE_JNI_ROOT)/../
TOOLCHAIN_CLANG_PREFIX := $(TOOLCHAIN_CLANG_DIR)/bin

NDK_SYSROOT_INC=-I$(NDK_ROOT)/sysroot/usr/include
##### CLANG binaries
CC=$(TOOLCHAIN_CLANG_PREFIX)/clang
#CXX=$(TOOLCHAIN_CLANG_PREFIX)/clang++
AR=$(TOOLCHAIN_CLANG_PREFIX)/llvm-ar
AS=$(TOOLCHAIN_CLANG_PREFIX)/llvm-as
LNK=$(TOOLCHAIN_CLANG_PREFIX)/clang
#LD=$(TOOLCHAIN_CLANG_PREFIX)/llvm-ld
#NM=$(TOOLCHAIN_CLANG_PREFIX)/llvm-nm
OBJ_COPY=$(TOOLCHAIN_CLANG_PREFIX)/llvm-objcopy
STRIP=$(TOOLCHAIN_CLANG_PREFIX)/llvm-strip

AR_FLAGS = crs
SYSROOT = $(TOOLCHAIN_CLANG_DIR)/sysroot

SYS_ROOT = --sysroot=$(SYSROOT)

NDK_INCLUDE = $(NDK_ROOT)/$(NDK_PLATFORM)/arch-$(TARGET_ARCH)/usr/include

# INCLUDE_FIXED contains overrides for include files found under the toolchain's /usr/include.
# Currently we don't use, left here as a placeholder.
INCLUDE_FIXED = $(QUBE_JNI_ROOT)/include-fixed

# The logutils header is injected into all compiled files in order to redirect
# output to the Android console, and provide debugging macros.
LOGUTILS = $(QUBE_JNI_ROOT)/compat/qube_logutils.h

#Some fixes for Android compatibility
COMPATUTILS_FD = $(QUBE_JNI_ROOT)/compat/qube_compat_filesystem.h
COMPATUTILS_QEMU = $(QUBE_JNI_ROOT)/compat/qube_compat_qemu.h
COMPATMACROS = $(QUBE_JNI_ROOT)/compat/qube_compat_macros.h
COMPATANDROID = $(QUBE_JNI_ROOT)/compat/qube_compat.h
	
# Needed for some c++ source code to compile some ARM 64 disas
# We don't need them right now
#STL port
#APP_STL := stlport_shared
#APP_STL := c++_shared
#STL_INCLUDE := -I$(NDK_ROOT)/sources/android/support/include
#STL_INCLUDE += -I$(NDK_ROOT)/sources/cxx-stl/stlport/stlport
#STL_INCLUDE += -I$(NDK_ROOT)/sources/cxx-stl/llvm-libc++/include
#STL_INCLUDE += -I$(NDK_ROOT)/sources/cxx-stl/llvm-libc++abi/include
#STL_INCLUDE += -D__STDC_CONSTANT_MACROS
#STL_LIB :=$(QUBE_JNI_ROOT)/../obj/local/$(APP_ABI)/libstlport_shared.so
#STL_LIB :=$(QUBE_JNI_ROOT)/../obj/local/$(APP_ABI)/libc++_shared.so

SYSTEM_INCLUDE = \
    -I$(INCLUDE_FIXED) \
    $(SYS_ROOT) \
    -I$(QUBE_JNI_ROOT)/qemu/linux-headers \
    -I$(NDK_INCLUDE) \
    $(NDK_SYSROOT_INC) \
    $(STL_INCLUDE) \
    -include $(LOGUTILS) \
    -include $(COMPATUTILS_FD) \
    -include $(COMPATUTILS_QEMU) \
    -include $(COMPATMACROS) \
    -include $(COMPATANDROID)

#info
$(info VARIABLES)
$(info PATH = $(PATH))
$(info NDK_ROOT = $(NDK_ROOT))
$(info NDK_TOOLCHAIN_VERSION = $(NDK_TOOLCHAIN_VERSION))
$(info APP_PLATFORM = $(APP_PLATFORM))
$(info USE_NDK_PLATFORM21 = $(USE_NDK_PLATFORM21))
$(info USE_NDK_PLATFORM26 = $(USE_NDK_PLATFORM26))
$(info APP_ABI = $(APP_ABI))
$(info USE_OPTIMIZATION = $(USE_OPTIMIZATION))
$(info USE_SECURITY = $(USE_SECURITY))
$(info BUILD_THREADS = $(BUILD_THREADS))
$(info NDK_ENV = $(NDK_ENV))
$(info BUILD_HOST = $(BUILD_HOST))
$(info BUILD_GUEST= $(BUILD_GUEST))
$(info USE_QEMU_VERSION = $(USE_QEMU_VERSION))
$(info USE_KVM = $(USE_KVM))
