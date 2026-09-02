# QubeVM Emulator Tutorials

## General

QubeVM is best suited to lightweight operating systems, and with Virgl
GPU acceleration enabled through virtio-gpu, even 3D-capable guests can
run smoothly on supported devices, well beyond what software rendering
alone could do. Very large or resource-heavy operating systems can still
be slow or fail to boot depending on your device, so check the specific
tutorial sections for your OS before assuming the worst.
Operating systems that use a lot of memory might be stressing your
device storage so it is recommended to store all Hard disk and ISO
images on an External SD Card. It is recommended that you also read the
specific tutorial sections to better tweak and customize your
operating system for usability as well as performance.

------------------------------------------------------------------------

## Host architecture

QubeVM is built for the following types of Android devices:

**ARM 32 and 64 bit devices**
This should include most Android phones and tablets including desktop
enabled devices like Samsung Dex capable devices as well as laptops
running Chrome OS on ARM.

**Intel x86 32/64 bit devices**
This should include older Android phones running on x86 cpus like Asus
Zenfone and laptops running Android x86 or Chromium OS.

**Note for developers:**
If you want to build QubeVM for other host architectures for example MIPS
you can modify the configuration files under android/device-config in
the source code. Make sure you also read the README.developers to
understand what other files might need to be modified.

------------------------------------------------------------------------

## Guest Architecture

QubeVM can currently emulate the following architectures:

Intel x86 32/64 bit
This is the most common architecture that people use to run a wide
range of operating systems.

ARM 32 and 64bit
You can run various operating systems for ARM CPUs, including boards
like Raspberry Pi.

**SPARC**
You can boot and run many older operating systems.

**PowerPC**
You can run various operating systems for PowerPC.

**Note for developers:**
Similarly if you would like to try another guest architecture that is
supported by QEMU but is not included in QubeVM you can modify the
configuration files under android/device-config in the source code.
Again, make sure you also read the README.developers to understand what
files need to be modified.

------------------------------------------------------------------------

## Emulated devices

QubeVM emulation for the following device families via its front end:

- **[Board](board.md)**: CPU, RAM Memory (up to 8 GB), TCG
  acceleration single and multithreaded, KVM acceleration, TSC, ACPI,
  HPET.
- **[Disk Drives](video-audio-network.md)**: IDE/SCSI up to 4 HDC
  cannot be used if CDROM is attached, CD ROM (1 IDE slot), Floppy
  Drives (up to 2), SD Card (only image files, does NOT bridge to
  Android external SD Card, only works for some emulators)
- **[Graphics](video-audio-network.md)**: (standard, SVGA cirrus, SVGA
  VM Ware, sg3 for SPARC)
- **[Audio](video-audio-network.md)**: (only suport under QGE
  interface)
- **[Network](video-audio-network.md)**: (User mode for NAT network,
  TAP not supported for non-rooted devices)
- **[Advanced](advanced.md)**: you can type additional QEMU parameters
  that are not provided on the GUI. For more information read
  the **[QEMU
  documentation](https://www.qemu.org/docs/master/system/invocation.html)** web
  page.

By now you should have QubeVM on your Android device if not

------------------------------------------------------------------------

## Quickstart

This is a quick start guide that will get you up and running with a
virtual machine.

**Create the virtual Machine**
To create a virtual machine press on the dropdown list on the top right
corner labeled "Machine" and select "New".
Type a name that best describes your virtual machine.
You will be prompted with a selection of Operating Systems that when you
click it will take you to an online installation guide with links to ISO
CDROM installation media and/or virtual hard disk images. If you have
your own disk images you want to install choose "Custom" instead.
The main screen is divided in sections so to start press on the Board
section and select the amount of RAM that your virtual machine will use.
The recommended value depends on your setup, but usually 256 MB is
fine for most cases.
You can also choose the cpu type you want, if it's a 64bit operating
system make sure you provide the right CPU that supports it.
Choose the disks from the filesystem and the ISO files if you have
one.
The rest of the options are optional if you want you can read more about
each section **[here](index.md).**

**Start the virtual Machine**
You can start your virtual machine by pressing on the play button. You
can pause it (save state to disk) by pressing the pause button. If you
start the virtual machine in QGE interface you can find similar menus
that do the same within the QGE screen from the main menu.

**Resume the virtual Machine**
To resume a virtual machine from a previous saved state press on the
play button.
If for some reason the operating system fails to boot exit and press on
"Discard VM State" from the menu. This will delete the state and you can
reboot your machine.

------------------------------------------------------------------------

## Install an OS with QEMU

It is recommended you install your operating system to a virtual disk
image using QEMU on your desktop instead of QubeVM. This way the
installation will be much faster and you will be able to easily
customize and tweak to your liking. If you don't have QEMU installed on

scroll to the bottom to find the links to download and install QEMU for
your desktop.

Once you downloaded QEMU you can use the command line to create and
start a virtual machine as follows:

First create a virtual disk image using the qemu-img utility, we'll use
the qcow2 format which is compact and grows only as the data inside the
disk increase. We'll just name it harddisk.qcow2:
*qemu-img create -f qcow harddisk.qcow2*

Then use the newly created hard disk image and your ISO image to start a
virtual machine.
*****qemu-system-x86_64.exe -cpu athlon -M pc -m 128 -cdrom
cdrom.iso -hda harddisk.qcow2*

Once QEMU starts you can proceed as normal and install your operating
system as it was in a computer. Once the installation is complete you
can transfer the qcow2 image to your Android device and attach it to the
HDA drive in QubeVM and boot the system.

For more information on the command line options including how to create
different disk formats and attaching emulated graphics, network, and
audio devices you can visit the official **[QEMU
documentation](https://www.qemu.org/docs/master/system/invocation.html)** web
page.

Note that you can always attach the emulated devices afterwards in QubeVM
but it is advised that you do that during the operating system
installation on your desktop so they can be detected and installed
faster. You will find that there is plenty of advanced options worth
reading about that you can use to further optimize your virtual disk
images on the QEMU documentation link above.

------------------------------------------------------------------------

## Tutorials

We have divided the tutorials as per the sections that appear on QubeVM's
main screen. If you want to learn more about what each section does and
does not click on the links below. We suggest you read all the section
tutorials to learn briefly what you can do with QubeVM.

**[User Interface](user-interface.md)
[Board](board.md)
[Drives](drives.md)
[Graphics](video-audio-network.md)
[Audio](index.md)
[Network](index.md)
[Advanced](advanced.md)
[Import and Export](index.md)
[Share Files](share-files.md)
[Settings](settings.md)**

[TUTORIALS](tutorials.md)
