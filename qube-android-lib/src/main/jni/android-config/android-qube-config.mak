############### Qube Configuration ##############
# if the  makefile doesn't recognize the project path you can override it here:
#QUBE_JNI_ROOT := /home/dev/qube/workspace_qube/qube-android-lib/src/main/jni

# Qube: Clang-only toolchain (GCC support removed)
NDK_ROOT ?= /home/dev/tools/ndk/android-ndk-r30

### the ndk api should be the same as the minSdkVersion in your AndroidManifest.xml 
NDK_PLATFORM_API=30

# Optimization, generally it is better set to false when debugging
USE_OPTIMIZATION ?= true

# Uncomment to enable debugging
# If you enable debugging you should turn off optimization as well
#NDK_DEBUG=1

# Uncomment if you use Linux 64bit, or macosx PC to compile
# Compiling on Windows is no longer supported
NDK_ENV ?= linux-x86_64
#NDK_ENV ?= darwin-x86

# Build threads (make -j ?) makes building faster
BUILD_THREADS ?= 4

############## QEMU Host and Guest

# Android device type (host arch)
# values: arm64-v8a, x86_64
BUILD_HOST?=arm64-v8a

# GUEST_ARCH is the Emulator type
# values: x86_64-softmmu,aarch64-softmmu,ppc64-softmmu
BUILD_GUEST?=x86_64-softmmu

# QEMU Version
# values: 7.2.22, 11.1.1
USE_QEMU_VERSION ?= 11.1.1

# Enable Venus (Vulkan passthrough) acceleration
# controls both app UI and native side
USE_VENUS ?= false

# Enable KVM
USE_KVM ?= true
