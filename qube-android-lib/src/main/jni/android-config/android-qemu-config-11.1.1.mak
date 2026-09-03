#### QEMU 11.1.1 version-specific options
#### Based on actual ./configure --help output

# Clang-compatible warning flags (NDK clang does not support GCC-specific flags
WARNING_FLAGS = -Wno-redundant-decls -Wno-unused-variable \
	-Wno-uninitialized -Wno-unused-function \
	-Wno-unknown-warning-option

# slirp is built internally as a subproject (libslirp)
# qemu/build/subprojects/libslirp/src/libslirp.a
USE_SLIRP_LIB ?= true

# Disable unnecessary features for Android
MISC += --disable-plugins
MISC += --disable-capstone
MISC += --disable-malloc-trim
MISC += --disable-libssh
MISC += --disable-png
MISC += --disable-install-blobs
MISC += --disable-gtk
MISC += --disable-vnc-jpeg
MISC += --disable-vnc-sasl

# If the user want or doesn't want VirGL
ifeq ($(USE_VIRGL),true)
MISC += --enable-opengl --enable-virglrenderer
QEMU_EXTRA_CFLAGS += -DCONFIG_QUBE_VIRGL
QEMU_EXTRA_CFLAGS += -I$(QUBE_JNI_ROOT)/virglrenderer/build-android/install/include/virgl
QEMU_EXTRA_CFLAGS += -I$(QUBE_JNI_ROOT)/libepoxy/build-android/install/include
QEMU_EXTRA_LDFLAGS += -L$(QUBE_JNI_ROOT)/virglrenderer/build-android/install/lib
QEMU_EXTRA_LDFLAGS += -L$(QUBE_JNI_ROOT)/libepoxy/build-android/install/lib
QEMU_EXTRA_LDFLAGS += -lvirglrenderer -lepoxy -lEGL -lGLESv2
else
MISC += --disable-opengl
MISC += --disable-virglrenderer
endif
MISC += --disable-curses
MISC += --disable-brlapi
MISC += --disable-gnutls
MISC += --disable-nettle
MISC += --disable-gcrypt
MISC += --disable-spice
MISC += --disable-spice-protocol

# Disable vhost features for Android
MISC += --disable-vhost-kernel
MISC += --disable-vhost-user
MISC += --disable-vhost-user-blk-server
MISC += --disable-vhost-vdpa
MISC += --disable-vhost-crypto
MISC += --disable-vhost-net

# Features in 11.x that must be disabled for Android
MISC += --disable-multiprocess
MISC += --disable-dbus-display
MISC += --disable-linux-io-uring
MISC += --disable-fuse
MISC += --disable-fuse-lseek
MISC += --disable-bpf
MISC += --disable-selinux
MISC += --disable-slirp-smbd
MISC += --disable-libudev
MISC += --disable-membarrier
MISC += --disable-qom-cast-debug
MISC += --disable-qpl
MISC += --disable-uadk
MISC += --disable-qatzip
MISC += --disable-libvduse
MISC += --disable-replication
MISC += --disable-blkio
MISC += --disable-libcbor
MISC += --disable-igvm


# NM tool: use llvm-nm from clang toolchain
QUBE_NDK_CLANG_BIN = $(NDK_ROOT)/toolchains/llvm/prebuilt/linux-x86_64/bin
QEMU_NM = NM=$(QUBE_NDK_CLANG_BIN)/llvm-nm

# Qube Android .so support
# Tell meson to build qemu-system-* as shared_library() instead of executable()
QUBE_ANDROID_SO = --enable-qube-android-so
