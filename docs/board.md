# Machine Board Tutorial

## Machine Board

QubeVM provides the below main options for selecting the components of
your virtual machine for each supported architecture. This allows you to
specify the emulated hardware that your specific operating system
requires. If you are not sure what to choose you can visit the Operating
System (if its a supported OS) or use the default values.

**Machine Type**
The machine type describes the board and architectural details and can
have multiple values for each of the guest architectures that QubeVM
supports. For example for x86 emulation the major board chipset
emulation options are: pc (i440fx/PIIX3 for most operating system), q35
(with modern ICH9 chipset), or ISA pc (compatible with older operating
systems). In contrast, ARM and PowerPC guest machines have a lot of
different types that have to be used with the correct CPU type to
emulate the correct combination that your operating system was built for
so it is recommended to follow the instructions that came with your
OS.

**CPU Model**
There can be several options for the CPU type you would like to attach
to your virtual machine. You can use from older CPU types to newer ones
but you have to keep in mind that not everything is compatible. If you
experience problems choose Default or qemu32 if the operating system is
made for x86 32 bit architecture or qemu64 if the guest architecture is
made for x86 64 bit architecture. Some operating systems are only made
for x86 64bit CPUs and should inform you during boot up that they
require a 64 bit processor. For PowerPC, SPARC, and ARM emulators you
need to specify the combination of CPU and Board your operating system
is made for otherwise it won't boot up.

**CPU Cores**
You can specify the number of CPU cores to be emulated for the virtual
machine. Note these are emulated CPU cores not real cores that means
that if you don't have MTTCG enabled the processing will be still single
threaded and will probably end up with slow performance instead. Older
Operating system would normally not have any benefit from multiple CPUS
so you can leave the value as the default.

**RAM Memory**
You can specify the amount of RAM your OS requires though note that
Android devices will limit how much memory an app can request so make
sure you don't go overboard. Another thing to remember is that adding
more memory doesn't mean it will necessarily speed up the virtual
machine. Use the recommended value for your setup, as virtual
machines with too much RAM used might take long to
pause!

------------------------------------------------------------------------

## CPU Features

QubeVM provides certain CPU features, these are due to some limitations
on the arm host implementation and others for acceleration purposes.

**MTTCG:** Multi Thread TCG is a feature of QEMU that allows multiple
emulated CPU cores to be able to utilize multiple host CPU cores
providing better performance. Prior to MTTCG older versions of QEMU
would just interleave all emulated CPU tasks into a single thread. This
option is applicable to 64bit hosts but might not always work as
expected. If you experience slowness disable this option and use only 1
CPU core.

**KVM:** QEMU allows near native speeds using virtualization via the
Linux KVM module. Your android device needs to have a compiled and
working KVM module installed. In order to make use of KVM the guest
operating system needs to be the same architecture as your Android
device. For example you can use KVM when you run DSL Linux on an Android
x86 device running QubeVM x86 emulator. Unfortunately the same is not
true for stock Android ARM devices which don't come with a KVM module.
Currently KVM on QubeVM is only tested with **[Android x86
5.1](https://www.android-x86.org/)** live cd and it performs as
expected.

**Disable TSC:** QEMU timer implementation for Time Stamp Counter, it's not recommended to enable it if you're using a modern operating system. Disable if your Operating System does not boot or there are problems opearating the virtual machine.

**Disable HPET**: Use this option to fix some timing issues in the guest
virtual machine. If you are not sure leave is as default.

**Disable ACPI**: If the QEMU advanced power management emulation might
be problematic with some operating systems or you might want to prevent
the virtual machine to go to sleep disable this option.

------------------------------------------------------------------------

## KVM

This is a guide for making QubeVM running guest OSes in nearly native
speed leveraging KVM.

**Step 1 - Android with prebuilt KVM**
First you need to install an Android version with a KVM enabled Kernel,
unfortunately there aren't many choices the version we used is
Android-x86 5.1. You might also be able to find an custom ROM for ARM
devices that has a KVM module prebuilt or at least is easy to built
it.

**Android x86**
QubeVM fully supports KVM for Android-x86 OS. Android-x86 project is a
port of Android for x86 devices like PCs and Laptops. ISOs and other
downloads can be found
in <https://osdn.net/projects/android-x86/releases/65697>. Android-x86
version 5.1 already contains support for KVM so you can download the
ISO, burn it in a CD, and use it to boot your PC computer. If you don't
want to install it in your hard disk you can instead run the live CD
option selecting it from the menu. When you have installed Android x86
you can move to step 3.

**Android ARM**
QubeVM is currently NOT tested under KVM for Android ARM devices. Find
below the defconfig options that will help you compile KVM support for
your kernel. Keep in mind the example below generic and your device
might need different options. Additionally, you will need to flash the
resulting boot image and perhaps disable the secure boot depending on
your Android ARM device. Proceed only if you know what you're doing and
at your own risk.

These should be the parameters in your defconfig file that compile
KVM:
CONFIG_VIRTUALIZATION=y
CONFIG_KVM=y
CONFIG_KVM_MMIO=y
CONFIG_KVM_ARM_HOST=y

**Step 2 - Setup the KVM device file**
Once Android boots up open the android terminal and type the following
to make sure that kvm is detected:
modprobe -l \| grep kvm

Then make sure you have the device file /dev/kvm exists and has
appropriate file permissions:
ls /dev/kvm
If permissions are not set correctly the change them to allow QubeVM to
use the KVM module.
chmod 666 /dev/kvm

**Step 3 - Install correct QubeVM version**
Finally install the correct QubeVM emulator supporting your host
Architecture. Make sure the host and the guest architecture should be
the same, for example if you have a PC with Android-x86 and want to boot
an x86 guest OS you will need to install the QubeVM x86 PC Emulator. Now
you can proceed with creating your virtual machine and running your
operating system. If there are any errors during boot up or crashes make
sure you read the log within QubeVM as it might contain useful
information of what went wrong.

This guide is tested with **[Android-x86 5.1 ISO
image](https://www.android-x86.org/download).** Note that 5.1 version is
the only one that contains a prebuilt KVM module, newer versions of
Android-x86 don't come with KVM preinstalled so you would have to build
it on your own!

[TUTORIALS](tutorials.md)
