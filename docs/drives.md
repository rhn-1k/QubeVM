# Virtual Disk Drives

## Drives

QubeVM supports Non-Removable drives like Hard disk drives and removable
drives like CD ROMs and SD cards (for ARM guest machines only). QubeVM is
using QEMU to emulate IDE disk interfaces and attache virtual disk
drives to your virtual machine. Even though QubeVM use the default IDE
interface you can set up your Hard disk drives with other emulated
interfaces like SCSI or virtio for better performance using the extra
parameters field, to learn more go to the **[Advanced](advanced.md)**
section of the tutorials.

------------------------------------------------------------------------

## Hard Disk Drives

QubeVM supports up to 4 Hard Disk Drives labeled HDA, HDB, HDC, HDD.
These correspond to the 4 IDE interface Primary 0, 1 and Secondary 0, 1.
Note that HDC and CDROM cannot be used at the same time since they
occupy the same interface. You can attach any virtual image file of a
preinstalled operating system or you can create a new virtual disk image
by pressing on the drop down list and choose "New".

Currently QubeVM supports creating only qcow2 images files with a limited
file size, though you can create your own images on your desktop using
QEMU and use the parameters you want:
For example to create a virtual disk image using the qemu-img command
line utility:

qemu-img create -f qcow harddisk.qcow2

The qcow2 format format is compact and grows only as the data inside the
disk increase. It also supports preallocation and encryption. The
qemu-img utility let's you also specify the cluster size and provides
other useful parameters that you can use to optimize your image
read/write speeds with your operating system. There are more image
formats you can choose from depending on how you will use the virtual
disk for that scroll down to read more on the different formats that
QEMU supports.

If you want to learn more on how to install an operating system on your
Desktop, which is the recommended way, go to the section **Install an OS
with QEMU **in  [**Tutorials**](tutorials.md).

------------------------------------------------------------------------

## Removable Drives

QubeVM supports 1 slot for CDROM and 2 slots for Floppy drives. You can
insert an ISO image type for the CDROM and img for the floppy
disks. QubeVM expects up to 4 hard disk drives that can be created and
attached to a virtual machine. These usually are labelled as HDA, HDB,
HDC, and HDD. You can insert any of the supported image types, see below
for a list. Keep in mind that you cannot use a Hard disk drive in HDC
and a CDROM at the same time!

------------------------------------------------------------------------

## Virtual Image Types

QubeVM can use (read/write) the following different virtual image
types:


- img: This is a raw image format used for Hard disk and Floppy disk
  images. It does not support resize so that means that all space needs
  to be preallocated at creation time which make it sometimes hard to
  transfer (unless you archive it with a utility like zip). Though there
  are advantages of this format, the first is that it is plain
  compatible with most emulators out there so you can move your image
  and assuming you know what you're doing you can have it boot in other
  emulators. This can be very helpful if you want to move it to your
  desktop and use your favorite emulator to fix or customize the
  installed operating system in the image. The second major advantage is
  that it is a raw format that means that it merely reads and
  writes without compression which practically makes it the fastest
  solution of them all.
- qcow: This is the original old qcow format that QEMU used it is
  resizable which means that it grows as data are written so images when
  created are almost empty.
- qcow2: This is an advanced version of the qcow image format that
  allows encryption and compression as well as some other additional
  enhancements. This is the default format that QubeVM is using to create
  Hard Disk images on the fly.
- vmdk: Originally made to work with VMWare virtualization products
  though QEMU can also support. This is a good option if you want to use
  your operating system with VMWare.
- vdi: Originally made for Oracle's VirtualBox products, this is perhaps
  the best option if you want to be able to run the virtual disk image
  on your desktop since oracle's virtual box comes with a very
  comprehensive gui that makes creating a virtual machine very easy.
- vpc: Virtual PC disk format.
- vhd,vhdx: Made for Microsoft's hyper-v virtual machines.
- iso: Used for CD ROM images. Keep in min other CDROM file image types
  like bin and cue are not supported!

------------------------------------------------------------------------

## Android filesystem and Image files

There are 2 ways to attach a virtual disk image file in QubeVM:

1) Using the Android file picker. This is the standard way that android
allows a user to select a file from the filesystem. This supports
external SD cards which are recommended to store your images since they
usually have fast read/write speeds and don't interfere with any other
apps and the Android system.

2) Using the Legacy QubeVM file manager. This is a custom file chooser
which has only access to the internal file storage. This is recommended
if your android stock or otherwise ROM does not have Android Storage
Framework support or generally if you have problems with the Android
file chooser. If you try to use the QubeVM Legacy file manager you will
be prompted for write disk access to your media and files. If you don't
want to give access to the whole storage then choose the first option.

------------------------------------------------------------------------

## File access after import

If you have just imported virtual machine(s) from an older version of
QubeVM or another Android device you might not have immediate access to
your image files. This is due to Android and the way it allow file
permissions to apps. If do encounter an error during starting the
virtual machine you have to re-select the image file from the disk drive
dropdown list choosing "Open". This is additional step is needed for
Hard Disk files and Removable drives as well.

Note that if you use the legacy file manager (no SD Card support) to
open your files you might not have this problem as long as the file path
remains the same. If you are unsure we suggest you always re-open and
select your Hard Drives image and ISO files once you import a virtual
machine.

------------------------------------------------------------------------

## What happened to the Shared Drive?

The shared drive is removed in recent versions of QubeVM because it was
not maintained and being buggy. Luckily there are alternatives to
sharing files between your Android device and your virtual machine, to
learn more go to **[Share Files](share-files.md)**

------------------------------------------------------------------------

## Boot Drive

You can set the drive that will be set as the first to boot using the
**Boot from device** option under the Boot section in QubeVM. If you use
QubeVM to install your operating system from a CDROM iso file then you
should set this value to CDROM otherwise you can leave to default or for
older Operating system you might want to set it to boot from a floppy
drive.

------------------------------------------------------------------------

## Kernel and initrd

QubeVM provides a gui section that you can use to set your kernel and
initrd image files for use with the QubeVM ARM emulator. There is also a
field to specify the append options you want to boot with. Make sure you
follow the instructions of the operating system kernel you're booting.

------------------------------------------------------------------------

## Changing drives

You can change removable drives like CD and Floppy images after you boot
the virtual machine by pressing on the icon with the external drive on
it if you use the QGE interface. If you use the VNC interface you can
change image from the main screen under the **Removable Drives**
section. To eject an ISO or Floppy image just choose "None" from the
drop down list menu.

Note: You can eject/change only Removable Drives, Hard Disk Drives
cannot be changed after the virtual machine has started!

[TUTORIALS](tutorials.md)
