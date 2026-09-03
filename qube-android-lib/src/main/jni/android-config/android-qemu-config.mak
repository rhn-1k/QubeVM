#### DO NOT CHANGE
QEMU_TARGET_LIST = $(BUILD_GUEST)
QEMU_CONFIG_DIR=$(QUBE_JNI_ROOT)/android-config


ifeq ($(USE_QEMU_VERSION),7.2.22)
include $(QEMU_CONFIG_DIR)/android-qemu-config-7.2.22.mak

else ifeq ($(USE_QEMU_VERSION),11.1.1)
include $(QEMU_CONFIG_DIR)/android-qemu-config-11.1.1.mak
else
$(error Unsupported QEMU version = $(USE_QEMU_VERSION))
endif

##### QEMU generic configuration

#use coroutine
#ucontext is deprecated and also not avail in Bionic
# gthread is not working right AFAIK
# possible values: gthread, ucontext, sigaltstack, windows
COROUTINE=sigaltstack
#COROUTINE=gthread

#COROUTINE_POOL=--disable-coroutine-pool 
COROUTINE_POOL = --enable-coroutine-pool 

#Enable Internal profiler
#CONFIG_PROFILER = --enable-gprof

# SDL removed - display is VNC (-vnc), not SDL
SDL = --disable-sdl

# no SDL audio driver either
AUDIO += --audio-drv-list=

# NOT USED
#AUDIO += --audio-card-list= --audio-drv-list=
#--enable-mixemu

#USB redir
#USB_REDIR = --enable-usb-redir
USB_REDIR = --disable-usb-redir

#USB Lib
#USB_LIB = --enable-libusb
USB_LIB = --disable-libusb

#NETWORK
NET = --enable-slirp

#DISPLAY
DISPLAY = --disable-curses --disable-cocoa --disable-gtk

#VNC 
VNC +=  --enable-vnc
#VNC += --enable-vnc-jpeg
VNC += --disable-vnc-jpeg
#VNC += --enable-vnc-png
# vnc-png renamed to png in QEMU 7.x+ (handled in version-specific mak)
ifneq ($(filter $(USE_QEMU_VERSION),7.2.22 11.1.1),)
else
VNC += --disable-vnc-png
endif

VNC += --disable-vnc-sasl


#VNC THREAD (DONT USE FOR 2.3.0+)
#VNC_THREAD += --enable-vnc-thread
#VNC_THREAD += --disable-vnc-thread


# NEEDS ABOVE ENCODING
#INCLUDE_ENC += -I$(QUBE_JNI_ROOT)/png -I$(QUBE_JNI_ROOT)/jpeg

#SMART CARD
SMARTCARD = --disable-smartcard

#FDT is needed for system emulation
#FDT = --disable-fdt
FDT = --enable-fdt
FDT_INC = -I$(QUBE_JNI_ROOT)/qemu/dtc/libfdt

#Disable nptl
#NPTL += --disable-nptl 

#For 2.3.0
#Misc
MISC += --enable-lto
MISC += --disable-tools --disable-libnfs --disable-tpm
MISC += --disable-qom-cast-debug
MISC += --disable-libnfs --disable-libiscsi --disable-docs
MISC += --disable-rdma --disable-brlapi --disable-curl
MISC += --disable-vde --disable-netmap --disable-cap-ng
MISC += --disable-attr --disable-guest-agent --disable-pie
MISC += --disable-rbd --disable-lzo  --disable-snappy
# xfsctl was removed in QEMU 6+; only pass for older versions
ifneq ($(filter $(USE_QEMU_VERSION),7.2.22 11.1.1),)
else
MISC += --disable-xfsctl
endif
MISC += --disable-seccomp --disable-bzip2 
MISC += --disable-vte
MISC += --disable-werror
MISC += --disable-gnutls
MISC += --disable-nettle
MISC += --disable-user
MISC += $(SSH2)

#Stack protector, this doesn't make any difference since we override in android-generic.mak
#MISC += --enable-stack-protector
#MISC += --disable-stack-protector

#Trying nop doesn't work
#MISC += --enable-trace-backends=nop

#NUMA
NUMA = --disable-numa

#VHOST
#VHOST
# vhost-scsi was removed in QEMU 7.x+ (now covered by vhost-kernel in version-specific mak)
ifneq ($(filter $(USE_QEMU_VERSION),7.2.22 11.1.1),)
VHOST = --disable-vhost-net
else
VHOST = --disable-vhost-net --disable-vhost-scsi
endif

#VIRT
VIRT = --disable-virtfs

#AIO (Not supported yet)
LINUX_AIO = --disable-linux-aio


#Enable debugging for QEMU
DEBUG =
ifeq ($(NDK_DEBUG), 1)
	DEBUG = --enable-debug
else
	DEBUG = --disable-debug-tcg --disable-debug-info --disable-sparse
endif

#KVM
ifeq ($(USE_KVM),true)
	#ENABLE KVM
	KVM = --enable-kvm
else 
	# DISABLE
	KVM = --disable-kvm
endif

#XEN
XEN = --disable-xen --disable-xen-pci-passthrough

#SPICE
SPICE = --disable-spice
#SPICE = --enable-spice 
#SPICE_INC= -I$(QUBE_JNI_ROOT)/spice-protocol  \
	-I$(QUBE_JNI_ROOT)/spice/server


#TCI
#TCI = --enable-tcg-interpreter

WARNING_FLAGS ?= -Wno-redundant-decls -Wno-unused-variable \
	-Wno-maybe-uninitialized -Wno-unused-function \
	-Wunused-but-set-variable -Wno-unknown-warning-option \
	-Wno-unknown-attributes
	
ifeq ($(APP_ABI), armeabi)
    QEMU_HOST_CPU = arm
else ifeq ($(APP_ABI), armeabi-v7a)
    QEMU_HOST_CPU = arm
else ifeq ($(APP_ABI), armeabi-v7a-hard)
    QEMU_HOST_CPU = arm
else ifeq ($(APP_ABI), arm64-v8a)
	QEMU_HOST_CPU = aarch64
else ifeq ($(APP_ABI), x86)
    QEMU_HOST_CPU = i686
else ifeq ($(APP_ABI), x86_64)
    QEMU_HOST_CPU = x86_64
endif

GLIB_BUILD_DIR=$(QUBE_JNI_ROOT)/glib/build

QEMU_OBJ_LOCAL_DIR=$(QUBE_JNI_ROOT)/../obj/local/$(APP_ABI)

QEMU_LDFLAGS=\
	-L$(QEMU_OBJ_LOCAL_DIR) \
	-lcompat-qube \
	-lcompat-musl \
	-lgio-2.0 \
	-lgobject-2.0 \
	-lffi \
	-lglib-2.0 \
	-lpixman-1 \
	-lc -lm -llog \
	$(INCLUDE_SYMS)

# Qube Android .so flag
QUBE_ANDROID_SO ?=
# NM override
QEMU_NM ?=
QEMU_EXTRA_CFLAGS ?=
QEMU_EXTRA_LDFLAGS ?=

config:
	echo NM = $(QEMU_NM)
	echo TOOLCHAIN DIR: $(TOOLCHAIN_DIR)
	echo NDK ROOT: $(NDK_ROOT) 
	echo NDK PLATFORM: $(NDK_PLATFORM) 
	echo USR INCLUDE: $(NDK_INCLUDE)
	echo USE_VIRGL: $(USE_VIRGL)
	echo USE_VENUS: $(USE_VENUS)
	cd ./qemu	; \
	$(QEMU_NM) ./configure \
	--cc=$(CC) \
	--target-list=$(QEMU_TARGET_LIST) \
	--cpu=$(QEMU_HOST_CPU) \
	$(PIXMAN) \
	$(FDT) \
	$(VNC) \
	$(VNC_THREAD) \
	$(SMARTCARD) \
	$(NPTL) \
	$(KVM) \
	$(SPICE) \
	$(XEN) \
	$(NUMA) \
	$(TCI) \
	$(LINUX_AIO) \
	$(VIRT) \
	$(VHOST) \
	$(DISPLAY) \
	$(USB_REDIR) \
	$(USB_LIB) \
	$(BLUETOOTH) \
	$(NET) \
	$(SDL) \
	$(AUDIO) \
	$(COROUTINE_POOL) \
	$(MISC) \
	--cross-prefix=$(TOOLCHAIN_PREFIX)- \
	--extra-ldflags=\
	" \
	$(QEMU_LDFLAGS) \
	$(QEMU_EXTRA_LDFLAGS) \
	-shared \
	" \
	--extra-cflags=\
	"\
	$(QEMU_EXTRA_CFLAGS) \
	$(SYSTEM_INCLUDE) \
	-I$(QUBE_JNI_ROOT)/glib \
	-I$(QUBE_JNI_ROOT)/glib/glib \
	-I$(QUBE_JNI_ROOT)/glib/build \
	-I$(QUBE_JNI_ROOT)/glib/build/glib \
	-I$(QUBE_JNI_ROOT)/glib/gio \
	-I$(QUBE_JNI_ROOT)/glib/gmodule \
	-I$(QUBE_JNI_ROOT)/pixman \
	-I$(QUBE_JNI_ROOT)/pixman/pixman \
	-I$(QUBE_JNI_ROOT)/scsi \
	-I$(QUBE_JNI_ROOT)/compat  \
	$(SPICE_INC) \
	$(FDT_INC) \
	$(INCLUDE_ENC) \
	$(ENV_EXTRA) \
	$(WARNING_FLAGS) \
	$(ARCH_CFLAGS) \
	" \
	--with-coroutine=$(COROUTINE) \
	$(DEBUG) \
	$(QUBE_ANDROID_SO) \
	$(CONFIG_PROFILER)

