#### QEMU 7.2.22 version-specific options
#### Based on actual ./configure --help output

# QEMU 7.x does not use a stab lib
USE_QEMUSTAB ?= false

# slirp is built internally as a subproject (libslirp)
# qemu/build/subprojects/libslirp/src/libslirp.a
USE_SLIRP_LIB ?= true

# Disable plugins: avoids gmodule-no-export-2.0 pkg-config requirement
MISC += --disable-plugins
MISC += --disable-capstone
MISC += --disable-malloc-trim
MISC += --disable-libssh
MISC += --disable-png
MISC += --disable-install-blobs

# If the user want or doesn't want VirGL
ifeq ($(USE_VIRGL),true)
MISC += --enable-opengl --enable-virglrenderer
QEMU_EXTRA_CFLAGS += -I$(QUBE_JNI_ROOT)/virglrenderer/build-android/install/include/virgl
QEMU_EXTRA_CFLAGS += -I$(QUBE_JNI_ROOT)/libepoxy/build-android/install/include
QEMU_EXTRA_LDFLAGS += -L$(QUBE_JNI_ROOT)/virglrenderer/build-android/install/lib
QEMU_EXTRA_LDFLAGS += -L$(QUBE_JNI_ROOT)/libepoxy/build-android/install/lib
QEMU_EXTRA_LDFLAGS += -lvirglrenderer -lepoxy -lEGL -lGLESv2
else
MISC += --disable-opengl
MISC += --disable-virglrenderer
endif

# Disable vhost features for Android
MISC += --disable-vhost-kernel
MISC += --disable-vhost-user
MISC += --disable-vhost-user-blk-server
MISC += --disable-vhost-vdpa
MISC += --disable-vhost-crypto

# Features in 7.x that must be disabled for Android
MISC += --disable-spice-protocol
MISC += --disable-virtiofsd
MISC += --disable-libvduse
MISC += --disable-multiprocess
MISC += --disable-dbus-display
MISC += --disable-linux-io-uring
MISC += --disable-fuse
MISC += --disable-fuse-lseek
MISC += --disable-bpf
MISC += --disable-iconv
MISC += --disable-sdl-image
MISC += --disable-selinux
MISC += --disable-slirp-smbd
MISC += --disable-pvrdma
MISC += --disable-live-block-migration
MISC += --disable-replication
MISC += --disable-blkio
MISC += --disable-libudev
MISC += --disable-avx2
MISC += --disable-membarrier


# NM tool: use llvm-nm from clang toolchain (NDK r23+ has no binutils nm)
# NDK_ROOT must be set (passed as env var from parent make)
QUBE_NDK_CLANG_BIN = $(NDK_ROOT)/toolchains/llvm/prebuilt/linux-x86_64/bin
QEMU_NM = NM=$(QUBE_NDK_CLANG_BIN)/llvm-nm

# Qube Android .so support
# Tell meson to build qemu-system-* as shared_library() instead of executable()
QUBE_ANDROID_SO = --enable-qube-android-so
