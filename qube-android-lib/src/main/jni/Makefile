include android-qube-build.mak

# Meson needs specific CPU definitions
ifeq ($(APP_ABI),arm64-v8a)
	QUBE_CPU_FAMILY = aarch64
	QUBE_CPU = aarch64
else ifeq ($(APP_ABI),x86_64)
	QUBE_CPU_FAMILY = x86_64
	QUBE_CPU = x86_64
else ifeq ($(APP_ABI),x86)
	QUBE_CPU_FAMILY = x86
	QUBE_CPU = i686
endif

ifeq ($(NDK_ENV), windows)
	NDK_BUILD = ndk-build.cmd
else ifeq ($(NDK_ENV), windows-x86_64)
	NDK_BUILD = ndk-build.cmd
else
	NDK_BUILD = ndk-build
endif

ndkbuild=cd $(NDK_PROJECT_PATH) && $(NDK_BUILD) -j $(BUILD_THREADS)
ndkclean=cd $(NDK_PROJECT_PATH) && $(NDK_BUILD) clean
ndkcleanobj=rm -rf $(NDK_PROJECT_PATH)/obj/

ifeq ($(NDK_ENV), windows)
	ndkgdb=cd $(NDK_PROJECT_PATH) && ndk-gdb.cmd
else ifeq ($(NDK_ENV), windows-x86_64)
	ndkgdb=cd $(NDK_PROJECT_PATH) && ndk-gdb.cmd
else
	ndkgdb=cd $(NDK_PROJECT_PATH) && ndk-gdb
endif

CREATE_OBJDIR=mkdir -p $(NDK_PROJECT_PATH)/obj/local/$(APP_ABI)/
CREATE_LIBSDIR=mkdir -p $(NDK_PROJECT_PATH)/libs/$(APP_ABI_DIR)/
DEL_LIBSDIR=rm -rf $(NDK_PROJECT_PATH)/libs/* \
	$(NDK_PROJECT_PATH)/jniLibs/* \
	$(QUBE_APP_X86)/jniLibs/* \
	$(QUBE_APP_ARM)/jniLibs/* \
	$(QUBE_APP_PPC)/jniLibs/*

GLIB_BUILD_DIR=$(QUBE_JNI_ROOT)/glib/build
GLIB_CROSS_FILE=$(QUBE_JNI_ROOT)/glib-android-cross.ini
GLIB_FAKE_PC_DIR=$(QUBE_JNI_ROOT)/glib-pkgconfig

INSTALL_GLIB_LIBS=cp -Lf \
    $(GLIB_BUILD_DIR)/glib/libglib-2.0.so \
    $(NDK_PROJECT_PATH)/obj/local/$(APP_ABI)/ && \
    cp -f \
    $(GLIB_BUILD_DIR)/gobject/libgobject-2.0.a \
    $(GLIB_BUILD_DIR)/gio/libgio-2.0.a \
    $(NDK_PROJECT_PATH)/obj/local/$(APP_ABI)/

INSTALL_FFI_LIBS=cp -f libffi/$(GNU_HOST)/.libs/*.a \
    $(NDK_PROJECT_PATH)/obj/local/$(APP_ABI)/

PIXMAN_BUILD_DIR=$(QUBE_JNI_ROOT)/pixman/build
PIXMAN_CROSS_FILE=$(QUBE_JNI_ROOT)/pixman-android-cross.ini

INSTALL_PIXMAN_LIBS=cp -Lf $(PIXMAN_BUILD_DIR)/pixman/*.so \
    $(NDK_PROJECT_PATH)/obj/local/$(APP_ABI)/


EPOXY_BUILD_DIR=$(QUBE_JNI_ROOT)/libepoxy/build-android
EPOXY_INSTALL_DIR=$(EPOXY_BUILD_DIR)/install
EPOXY_CROSS_FILE=$(QUBE_JNI_ROOT)/epoxy-android-cross.ini

VIRGL_BUILD_DIR=$(QUBE_JNI_ROOT)/virglrenderer/build-android
VIRGL_INSTALL_DIR=$(VIRGL_BUILD_DIR)/install
VIRGL_CROSS_FILE=$(QUBE_JNI_ROOT)/virgl-android-cross.ini

INSTALL_VIRGL_LIBS=cp -Lf \
    $(EPOXY_INSTALL_DIR)/lib/libepoxy.so \
    $(VIRGL_INSTALL_DIR)/lib/libvirglrenderer.so \
    $(NDK_PROJECT_PATH)/obj/local/$(APP_ABI)/

MOD_QEMU_LIBS=$(OBJ_COPY) --redefine-sym open=android_open  \
    qemu/x86_64-softmmu/qemu-system-x86_64

# QEMU 7.x: meson puts .so directly in qemu/build/ (not in a subdirectory)
# Run redefine-sym on each .so then copy to NDK obj dir
QEMU7_BUILD_DIR=qemu/build
QEMU7_RELINK_SYMS=--redefine-sym open=android_open \
    --redefine-sym fopen=android_fopen \
    --redefine-sym close=android_close \
    --redefine-sym stat=android_stat \
    --redefine-sym mkstemp=android_mkstemp \
    --redefine-sym __open_2=android_open

relink-qemu7:
	@echo "--- Relinking QEMU 7.x .so symbols for Android ---"
	@for so in $(QEMU7_BUILD_DIR)/libqemu-system-*.so; do \
	    echo "  relink: $$so"; \
	    $(OBJ_COPY) $(QEMU7_RELINK_SYMS) $$so; \
	done

install-qemu7: relink-qemu7
	@echo "--- Installing QEMU 7.x .so to NDK obj dir ---"
	@mkdir -p $(NDK_PROJECT_PATH)/obj/local/$(APP_ABI)
	@cp -f $(QEMU7_BUILD_DIR)/libqemu-system-*.so \
	    $(NDK_PROJECT_PATH)/obj/local/$(APP_ABI)/

QEMU11_BUILD_DIR=qemu/build

install-qemu11:
	@mkdir -p $(NDK_PROJECT_PATH)/obj/local/$(APP_ABI)
	@cp -f $(QEMU11_BUILD_DIR)/libqemu-system-*.so \
	    $(NDK_PROJECT_PATH)/obj/local/$(APP_ABI)/

INSTALL_LIBS=cp -f $(NDK_PROJECT_PATH)/obj/local/$(APP_ABI)/*.so \
	../libs/$(APP_ABI_DIR)/

REMOVE_GDBSETUP=rm -f ../libs/$(APP_ABI_DIR)/gdb.setup
REMOVE_GDBSERVER=rm -f ../libs/$(APP_ABI_DIR)/gdbserver

SYNC_LIBS=rsync -av --exclude libqemu-system-* ../libs/* ../jniLibs/

QUBE_PRJ_ROOT=../../../..
QUBE_APP_X86=$(QUBE_PRJ_ROOT)/qube-android-x86/src/main
QUBE_APP_ARM=$(QUBE_PRJ_ROOT)/qube-android-arm/src/main
QUBE_APP_PPC=$(QUBE_PRJ_ROOT)/qube-android-ppc/src/main

SYNC_LIBS_X86=rsync -av --include='libqemu-system-x86*.so' \
    --include='gdbserver' --include='gdb.setup' \
    --include='*/' --exclude='*' \
    ../libs/ $(QUBE_APP_X86)/jniLibs/
SYNC_LIBS_ARM=rsync -av --include='libqemu-system-arm*.so' \
    --include='libqemu-system-aarch64*.so' \
    --include='gdbserver' --include='gdb.setup' \
    --include='*/' --exclude='*' \
    ../libs/ $(QUBE_APP_ARM)/jniLibs/
SYNC_LIBS_PPC=rsync -av --include='libqemu-system-ppc*.so' \
    --include='gdbserver' --include='gdb.setup' \
    --include='*/' --exclude='*' \
    ../libs/ $(QUBE_APP_PPC)/jniLibs/

COPY_ROMS=-mkdir -p ../assets/roms && cp -rf ./qemu/pc-bios/*.rom \
./qemu/pc-bios/*.bin \
./qemu/pc-bios/*.img \
./qemu/pc-bios/*.bmp \
./qemu/pc-bios/*.ico \
./qemu/pc-bios/*.svg \
./qemu/pc-bios/*.rsrc \
./qemu/pc-bios/openbios-* \
./qemu/pc-bios/README \
../assets/roms/

# Extract QEMU edk2 firmware for Android UEFI assets
COPY_UEFI_X86=mkdir -p ../../../../qube-android-x86/src/main/assets/roms/ && \
    bzip2 -dc ./qemu/pc-bios/edk2-x86_64-code.fd.bz2 > ../../../../qube-android-x86/src/main/assets/roms/edk2-x86_64-code.fd && \
    bzip2 -dc ./qemu/pc-bios/edk2-i386-vars.fd.bz2 > ../../../../qube-android-x86/src/main/assets/roms/edk2-i386-vars.fd
COPY_UEFI_ARM=mkdir -p ../../../../qube-android-arm/src/main/assets/roms/ && \
    bzip2 -dc ./qemu/pc-bios/edk2-aarch64-code.fd.bz2 > ../../../../qube-android-arm/src/main/assets/roms/edk2-aarch64-code.fd && \
    bzip2 -dc ./qemu/pc-bios/edk2-arm-vars.fd.bz2 > ../../../../qube-android-arm/src/main/assets/roms/edk2-arm-vars.fd

COPY_KEYMAPS=cp -rf ./qemu/pc-bios/keymaps ../assets/roms/
COPY_QEMU_VERSION=cp -f ./qemu/VERSION ../assets/QEMU_VERSION
COPY_RESOLV_CONF=mkdir -p ../assets/roms/etc/ && printf "nameserver 1.1.1.1\n\n" > ../assets/roms/etc/resolv.conf

COPY_ASSETS_X86=mkdir -p ../../../../qube-android-x86/src/main/assets/ && \
    cp -rf ../assets/* ../../../../qube-android-x86/src/main/assets/
COPY_ASSETS_ARM=mkdir -p ../../../../qube-android-arm/src/main/assets/ && \
    cp -rf ../assets/* ../../../../qube-android-arm/src/main/assets/
COPY_ASSETS_PPC=mkdir -p ../../../../qube-android-ppc/src/main/assets/ && \
    cp -rf ../assets/* ../../../../qube-android-ppc/src/main/assets/

#RENAME_TUNCTL=mv -f $(NDK_PROJECT_PATH)/libs/$(APP_ABI)/tunctl \
	$(NDK_PROJECT_PATH)/libs/$(APP_ABI)/libtunctl.so
#RENAME_PARPROUTED=mv -f $(NDK_PROJECT_PATH)/libs/$(APP_ABI)/parprouted \
	$(NDK_PROJECT_PATH)/libs/$(APP_ABI)/libparprouted.so
	
#GDB needs the gdb.setup under ./libs
COPY_GDB_SETUP_LIBS=-cp -f $(NDK_PROJECT_PATH)/libs/$(APP_ABI)/gdb.setup \
	$(NDK_PROJECT_PATH)/libs/gdb.setup
	
ifneq ($(NDK_DEBUG),1)
	STRIP_LIBS=$(STRIP) $(NDK_PROJECT_PATH)/libs/$(APP_ABI_DIR)/*.so
endif

DEBUG =
ifeq ($(NDK_DEBUG), 1)
	LIBFFI_DEBUG = --enable-debug
	LIBGLIB_DEBUG = --enable-debug
else
	LIBFFI_DEBUG = --disable-debug
    LIBGLIB_DEBUG = --disable-debug
endif

export

.PHONY: qube clean distclean clean-ndk createdirs clean-ffi clean-glib clean-qemu clean-pixman clean-epoxy clean-virgl

default: qube
	
createdirs:
	$(CREATE_OBJDIR)
	$(DEL_LIBSDIR)
	$(CREATE_LIBSDIR)

ndk:
	$(ndkbuild)

clean-ndk:
	$(ndkclean)
	$(ndkcleanobj)

clean-ffi:
	-cd $(QUBE_JNI_ROOT)/libffi && $(MAKE) distclean
	-cd $(QUBE_JNI_ROOT)/libffi && $(MAKE) clean

config-ffi:
	cd $(QUBE_JNI_ROOT)/libffi && ./configure --host=$(GNU_HOST) $(LIBFFI_DEBUG) --enable-static --disable-shared \
	CFLAGS="$(ARCH_CFLAGS) -I$(NDK_INCLUDE) --sysroot=$(NDK_SYSROOT) $(NDK_SYSROOT_ARCH_INC) " \
	LDFLAGS="$(ARCH_LD_FLAGS) $(SYS_ROOT) -nostdlib -fno-unwind-tables "

ffi:
	cd $(QUBE_JNI_ROOT)/libffi && $(MAKE) -j $(BUILD_THREADS) \
	CFLAGS="$(ARCH_CFLAGS) -I$(NDK_INCLUDE) --sysroot=$(NDK_SYSROOT) $(NDK_SYSROOT_ARCH_INC) " \
	LDFLAGS="$(ARCH_LD_FLAGS) $(SYS_ROOT) -nostdlib -fno-unwind-tables "
	$(INSTALL_FFI_LIBS)

gen-glib-cross:
	@echo '[binaries]' > $(GLIB_CROSS_FILE)
	@echo "c = '$(CC)'" >> $(GLIB_CROSS_FILE)
	@echo "cpp = '$(CXX)'" >> $(GLIB_CROSS_FILE)
	@echo "ar = '$(AR)'" >> $(GLIB_CROSS_FILE)
	@echo "strip = '$(STRIP)'" >> $(GLIB_CROSS_FILE)
	@echo '' >> $(GLIB_CROSS_FILE)
	@echo '[host_machine]' >> $(GLIB_CROSS_FILE)
	@echo "system = 'android'" >> $(GLIB_CROSS_FILE)
	@echo "cpu_family = '$(QUBE_CPU_FAMILY)'" >> $(GLIB_CROSS_FILE)
	@echo "cpu = '$(QUBE_CPU)'" >> $(GLIB_CROSS_FILE)
	@echo "endian = 'little'" >> $(GLIB_CROSS_FILE)
	@mkdir -p $(GLIB_FAKE_PC_DIR)
	@echo 'Name: libffi' > $(GLIB_FAKE_PC_DIR)/libffi.pc
	@echo 'Description: libffi' >> $(GLIB_FAKE_PC_DIR)/libffi.pc
	@echo 'Version: $(LIBFFI_VERSION)' >> $(GLIB_FAKE_PC_DIR)/libffi.pc
	@echo 'Cflags: -I$(QUBE_JNI_ROOT)/libffi/$(GNU_HOST)/include' >> $(GLIB_FAKE_PC_DIR)/libffi.pc
	@echo 'Libs: $(QUBE_JNI_ROOT)/../obj/local/$(APP_ABI)/libffi.a' >> $(GLIB_FAKE_PC_DIR)/libffi.pc

config-glib: ndk gen-glib-cross
	cd $(QUBE_JNI_ROOT)/glib && \
	PKG_CONFIG_PATH=$(GLIB_FAKE_PC_DIR) \
	PKG_CONFIG_LIBDIR=$(GLIB_FAKE_PC_DIR) \
	meson setup $(GLIB_BUILD_DIR) \
		--cross-file $(GLIB_CROSS_FILE) \
		--default-library=both \
		--prefix=/ \
		--wrap-mode=forcefallback \
		-Dlibmount=disabled \
		-Ddtrace=disabled \
		-Dsystemtap=disabled \
		-Dxattr=false \
		-Dtests=false \
		-Dlibffi:tests=false \
		-Dlibffi:default_library=static \
		-Dzlib:tests=disabled \
		-Dzlib:default_library=static \
		-Dpcre2:default_library=static \
		-Dproxy-libintl:default_library=static \
		-Dintrospection=disabled \
		-Dlibelf=disabled \
		-Dnls=disabled \
		-Dc_args="$(ARCH_CFLAGS) -I$(NDK_INCLUDE) -Wno-error=format-nonliteral -Wno-unused-parameter \
		$(NDK_SYSROOT_INC) $(NDK_SYSROOT_ARCH_INC) \
		-I$(SYSROOT)/usr/include $(SYS_ROOT) \
		-I$(QUBE_JNI_ROOT)/compat \
		-I$(QUBE_JNI_ROOT)/compat/musl \
		-I$(QUBE_JNI_ROOT)/compat/musl/include" \
		-Dc_link_args="$(ARCH_CLANG_FLAGS) $(ARCH_LD_CLANG_FLAGS) $(ARCH_LD_FLAGS) $(SYS_ROOT) \
		-L$(QUBE_JNI_ROOT)/../obj/local/$(APP_ABI) \
		-lc -lcompat-musl -llog -lcompat-qube"

glib: ndk
	ninja -C $(GLIB_BUILD_DIR) -j $(BUILD_THREADS) \
		glib/libglib-2.0.so \
		gobject/libgobject-2.0.a \
		gio/libgio-2.0.a \
		gmodule/libgmodule-2.0.a \
		gthread/libgthread-2.0.a
	$(INSTALL_GLIB_LIBS)

clean-glib:
	-rm -rf $(GLIB_BUILD_DIR)
	-rm -f $(GLIB_CROSS_FILE)
	-rm -rf $(GLIB_FAKE_PC_DIR)

gen-pixman-cross:
	@echo '[binaries]' > $(PIXMAN_CROSS_FILE)
	@echo "c = '$(CC)'" >> $(PIXMAN_CROSS_FILE)
	@echo "ar = '$(AR)'" >> $(PIXMAN_CROSS_FILE)
	@echo "strip = '$(STRIP)'" >> $(PIXMAN_CROSS_FILE)
	@echo '' >> $(PIXMAN_CROSS_FILE)
	@echo '[host_machine]' >> $(PIXMAN_CROSS_FILE)
	@echo "system = 'android'" >> $(PIXMAN_CROSS_FILE)
	@echo "cpu_family = '$(QUBE_CPU_FAMILY)'" >> $(PIXMAN_CROSS_FILE)
	@echo "cpu = '$(QUBE_CPU)'" >> $(PIXMAN_CROSS_FILE)
	@echo "endian = 'little'" >> $(PIXMAN_CROSS_FILE)

config-pixman: ndk gen-pixman-cross
	cd $(QUBE_JNI_ROOT)/pixman && \
	meson setup $(PIXMAN_BUILD_DIR) \
		--cross-file $(PIXMAN_CROSS_FILE) \
		--default-library=shared \
		--prefix=/ \
		-Dmmx=disabled \
		-Dsse2=disabled \
		-Dssse3=disabled \
		-Dloongson-mmi=disabled \
		-Dmips-dspr2=disabled \
		-Drvv=disabled \
		-Dgtk=disabled \
		-Dlibpng=disabled \
		-Dtests=disabled \
		-Ddemos=disabled \
		-Dc_args="$(ARCH_CFLAGS) -I$(NDK_INCLUDE) \
		$(NDK_SYSROOT_INC) \
		$(NDK_SYSROOT_ARCH_INC) \
		-I$(SYSROOT)/usr/include $(SYS_ROOT) -I$(NDK_ROOT)/sources/android/support/include \
		-I$(NDK_ROOT)/sources/android/cpufeatures -I$(QUBE_JNI_ROOT)/compat" \
		-Dc_link_args="$(ARCH_CLANG_FLAGS) $(ARCH_LD_CLANG_FLAGS) $(ARCH_LD_FLAGS) $(SYS_ROOT) \
		-L$(QUBE_JNI_ROOT)/../obj/local/$(APP_ABI) -lc -llog -lcompat-qube"

pixman: ndk
	ninja -C $(PIXMAN_BUILD_DIR) -j $(BUILD_THREADS)
	$(INSTALL_PIXMAN_LIBS)

clean-pixman:
	-rm -rf $(PIXMAN_BUILD_DIR)
	-rm -f $(PIXMAN_CROSS_FILE)

# libepoxy
gen-epoxy-cross:
	@echo '[binaries]' > $(EPOXY_CROSS_FILE)
	@echo "c = '$(CC)'" >> $(EPOXY_CROSS_FILE)
	@echo "ar = '$(AR)'" >> $(EPOXY_CROSS_FILE)
	@echo "strip = '$(STRIP)'" >> $(EPOXY_CROSS_FILE)
	@echo "pkgconfig = 'pkg-config'" >> $(EPOXY_CROSS_FILE)
	@echo '' >> $(EPOXY_CROSS_FILE)
	@echo '[host_machine]' >> $(EPOXY_CROSS_FILE)
	@echo "system = 'android'" >> $(EPOXY_CROSS_FILE)
	@echo "cpu_family = '$(QUBE_CPU_FAMILY)'" >> $(EPOXY_CROSS_FILE)
	@echo "cpu = '$(QUBE_CPU)'" >> $(EPOXY_CROSS_FILE)
	@echo "endian = 'little'" >> $(EPOXY_CROSS_FILE)

config-epoxy: ndk gen-epoxy-cross
	cd $(QUBE_JNI_ROOT)/libepoxy && \
	meson setup $(EPOXY_BUILD_DIR) \
		--cross-file $(EPOXY_CROSS_FILE) \
		--default-library=shared \
		--prefix=$(EPOXY_INSTALL_DIR) \
		-Degl=yes \
		-Dglx=no \
		-Dx11=false \
		-Dtests=false \
		-Ddocs=false \
		-Dc_args="$(ARCH_CFLAGS) -I$(NDK_INCLUDE) \
		$(NDK_SYSROOT_INC) $(NDK_SYSROOT_ARCH_INC) \
		-I$(SYSROOT)/usr/include $(SYS_ROOT) \
		-I$(QUBE_JNI_ROOT)/compat" \
		-Dc_link_args="$(ARCH_CLANG_FLAGS) $(ARCH_LD_FLAGS) $(SYS_ROOT) -lc -llog"

epoxy: ndk
	ninja -C $(EPOXY_BUILD_DIR) -j $(BUILD_THREADS) install
	$(CREATE_OBJDIR)
	cp -Lf $(EPOXY_INSTALL_DIR)/lib/libepoxy.so \
	    $(NDK_PROJECT_PATH)/obj/local/$(APP_ABI)/

clean-epoxy:
	-rm -rf $(EPOXY_BUILD_DIR)
	-rm -f $(EPOXY_CROSS_FILE)

# virglrenderer
gen-virgl-cross:
	@echo '[binaries]' > $(VIRGL_CROSS_FILE)
	@echo "c = '$(CC)'" >> $(VIRGL_CROSS_FILE)
	@echo "ar = '$(AR)'" >> $(VIRGL_CROSS_FILE)
	@echo "strip = '$(STRIP)'" >> $(VIRGL_CROSS_FILE)
	@echo "pkgconfig = 'pkg-config'" >> $(VIRGL_CROSS_FILE)
	@echo '' >> $(VIRGL_CROSS_FILE)
	@echo '[host_machine]' >> $(VIRGL_CROSS_FILE)
	@echo "system = 'android'" >> $(VIRGL_CROSS_FILE)
	@echo "cpu_family = '$(QUBE_CPU_FAMILY)'" >> $(VIRGL_CROSS_FILE)
	@echo "cpu = '$(QUBE_CPU)'" >> $(VIRGL_CROSS_FILE)
	@echo "endian = 'little'" >> $(VIRGL_CROSS_FILE)

config-virgl: ndk gen-virgl-cross epoxy
	cd $(QUBE_JNI_ROOT)/virglrenderer && \
	meson setup $(VIRGL_BUILD_DIR) \
		--cross-file $(VIRGL_CROSS_FILE) \
		--default-library=shared \
		--prefix=$(VIRGL_INSTALL_DIR) \
		-Dtests=false \
		-Dcheck-gl-errors=false \
		-Dvenus=$(USE_VENUS) \
		-Dvulkan-dload=true \
		-Drender-server-mode=thread \
		-Dc_args="$(ARCH_CFLAGS) -I$(NDK_INCLUDE) \
		$(NDK_SYSROOT_INC) $(NDK_SYSROOT_ARCH_INC) \
		-I$(SYSROOT)/usr/include $(SYS_ROOT) \
		-I$(EPOXY_INSTALL_DIR)/include \
		-I$(QUBE_JNI_ROOT)/compat" \
		-Dc_link_args="$(ARCH_CLANG_FLAGS) $(ARCH_LD_FLAGS) $(SYS_ROOT) \
		-L$(EPOXY_INSTALL_DIR)/lib -lepoxy -lEGL -lGLESv2 -lc -llog"

virglrenderer: ndk
	ninja -C $(VIRGL_BUILD_DIR) -j $(BUILD_THREADS) install
	cp -Lf $(VIRGL_INSTALL_DIR)/lib/libvirglrenderer.so \
	    $(NDK_PROJECT_PATH)/obj/local/$(APP_ABI)/

clean-virgl:
	-rm -rf $(VIRGL_BUILD_DIR)
	-rm -f $(VIRGL_CROSS_FILE)

QEMU_ADDONS_DIR=$(QUBE_JNI_ROOT)/qemu-addons

# injecting our addon files, not part of qemu, defined by patches
install-qube-src:
	cp -r $(QEMU_ADDONS_DIR)/$(USE_QEMU_VERSION)/* $(QUBE_JNI_ROOT)/qemu

_qemu: ndk install-qube-src
	cd $(QUBE_JNI_ROOT)/qemu && $(MAKE) all -j $(BUILD_THREADS) BUILD_GUEST=$(BUILD_GUEST) CFLAGS="$(ARCH_CFLAGS)"

config-qemu: ndk install-qube-src
	$(MAKE) -f $(QUBE_JNI_ROOT)/android-config/android-qemu-config.mak

clean-qemu:
	-cd $(QUBE_JNI_ROOT)/qemu && $(MAKE) distclean
	-cd $(QUBE_JNI_ROOT)/qemu && $(MAKE) clean
	-rm $(NDK_PROJECT_PATH)/obj/local/arm64-v8a/libqemu-system-x86_64.a
	-rm -rf ../assets/roms/*
	-rm -rf $(QUBE_APP_X86)/assets/roms/*
	-rm -rf $(QUBE_APP_ARM)/assets/roms/*
	-rm -rf $(QUBE_APP_PPC)/assets/roms/*

config-deps: ndk config-ffi config-glib config-pixman config-epoxy epoxy config-virgl
deps: ndk ffi glib pixman epoxy virglrenderer
	$(INSTALL_VIRGL_LIBS)

_build-qemu: ndk 
	make _qemu
ifeq ($(USE_QEMU_VERSION),7.2.22)
	$(MAKE) install-qemu7
else
	$(MAKE) install-qemu11
endif
	$(INSTALL_LIBS)
	$(RENAME_TUNCTL)
	$(RENAME_PARPROUTED)
		$(COPY_ROMS)
		$(COPY_KEYMAPS)

	$(COPY_QEMU_VERSION)

	$(COPY_RESOLV_CONF)

qemu: ndk
	make _build-qemu
	make install

install-common:
	$(CREATE_LIBSDIR)
	$(INSTALL_LIBS)
	$(STRIP_LIBS)
	$(SYNC_LIBS)

install:
	make install-common
	$(SYNC_LIBS_X86)
	$(COPY_ASSETS_X86)
	$(COPY_UEFI_X86)
	$(SYNC_LIBS_ARM)
	$(COPY_ASSETS_ARM)
	$(COPY_UEFI_ARM)
	$(SYNC_LIBS_PPC)
	$(COPY_ASSETS_PPC)

qube:
	make ndk
	make config-deps
	make deps
	make config-qemu
	make qemu
	make install

EXPORT_GUEST_ALL=export BUILD_GUEST=x86_64-softmmu,aarch64-softmmu,ppc64-softmmu

#most common combinations
all:
	make distclean && $(EXPORT_GUEST_ALL) && export BUILD_HOST=arm64-v8a && make qube
	make distclean && $(EXPORT_GUEST_ALL) && export BUILD_HOST=x86 && make qube
	make distclean && $(EXPORT_GUEST_ALL) && export BUILD_HOST=x86_64 && make qube

ndk-gdb:
	@echo APP_ABI=$(APP_ABI)
	$(COPY_GDB_SETUP_LIBS)
	$(ndkgdb) --attach $(PKG_NAME) --verbose -t

distclean: clean-qemu clean-ffi clean-glib clean-pixman clean-epoxy clean-virgl

clean: 
	make clean-qemu 
	make clean-ffi 
	make clean-glib 
	make clean-pixman
	make clean-epoxy
	make clean-virgl
	make clean-ndk
	-find . -name "*.d" -exec rm -rf {} \;
	-find . -name "*.a" -exec rm -rf {} \;
	-find . -name "*.o" -exec rm -rf {} \;
	-find . -name "*.so" -exec rm -rf {} \;
	$(MAKE) createdirs
	
help:
	@echo " Usage: export BUILD_GUEST=<emu-arch>-softmmu && export BUILD_HOST=<eabi> && make qube"
	@echo "  emu-arch (emulator): x86_64, aarch64, ppc64"
	@echo "  eabi (device): arm64-v8a, x86, x86_64"
	@echo "  optional: export NDK_DEBUG=1 for debuggable binaries"
	@echo " example: export BUILD_GUEST=x86_64-softmmu && export BUILD_HOST=arm64-v8a && make qube"
