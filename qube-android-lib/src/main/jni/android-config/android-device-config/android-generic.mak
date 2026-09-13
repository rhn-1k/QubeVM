# generic defs

# override the log functions 
ARCH_EXTRA_CFLAGS += -include $(LOGUTILS)

# Qube: Clang-only toolchain (GCC support removed)
NDK_TOOLCHAIN_VERSION=clang
ARCH_LD_CLANG_FLAGS += -Wc,-shared

ARCH_CFLAGS += -Wno-macro-redefined
    
#libs
ARCH_LD_FLAGS += -lc -lm -llog

# add aaudio
ARCH_CFLAGS += -D__ENABLE_AAUDIO__

# Suppress some warnings
#ARCH_CFLAGS += -Wno-psabi
ARCH_CFLAGS += -Wno-error=declaration-after-statement -Wno-unused-variable

# Smaller code generation for shared libraries, usually faster
# if doesn't work use -fPIC
ARCH_CFLAGS += -fpic

#Standard Android NDK flags
ARCH_CFLAGS += -ffunction-sections -funwind-tables -no-canonical-prefixes -fomit-frame-pointer

ARCH_CFLAGS += -DANDROID
ARCH_CFLAGS += -DGL_GLEXT_PROTOTYPES
ARCH_CFLAGS += -Wa,--noexecstack -Wformat -Werror=format-security  -Wno-format-security
ARCH_LD_CFLAGS += -Wl,-z,noexecstack -Wl,-z,relro -Wl,-z,now -Wl,--warn-shared-textrel -Wl,--fatal-warnings

################## OPTIMIZATION
# Overriding any of these values might
# have an adverse effect do this only if
# you know what you're doing
ifeq ($(USE_OPTIMIZATION),true)
        #ARCH_CFLAGS += -O2
        # Below optimizations might not be safe
        ARCH_CFLAGS += -O3 -ffast-math
        # might not be supported by clang
        #ARCH_CFLAGS += -fforce-addr
        #ARCH_CFLAGS += -ffast-math
        #ARCH_CFLAGS += -finline-limit=99999
        #ARCH_CFLAGS += -fstrength-reduce
else
    # we disable optimization make things easier when debugging
    ARCH_CFLAGS += -O0
endif

        # Slight performance boost
        ARCH_CFLAGS += -U_FORTIFY_SOURCE
        ARCH_CFLAGS += -fno-stack-protector

###################### DEBUGGING
ifeq ($(NDK_DEBUG),1)
	ARCH_CFLAGS += -g
	# for Debugging only
	# this is set by the compiler automatically for ndk libs
	# For qemu we might need this
	# This also produces compilation errors in libffi so we suppress it there
	ARCH_CFLAGS += -funwind-tables
endif
