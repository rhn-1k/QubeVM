# Virtual Disk Drives

## Drives

QubeVM supports non-removable drives such as hard disk drives and removable drives such as CD-ROMs. QubeVM uses QEMU to emulate IDE disk interfaces and attach virtual disk drives to your virtual machine. Although QubeVM uses the default IDE interface, you can configure your hard disk drives with other emulated interfaces, such as SCSI or virtio, for better performance by using the extra parameters field. To learn more, see below

------------------------------------------------------------------------

## Hard Disk Drives

QubeVM supports up to four hard disk drives labeled HDA, HDB, HDC, and HDD. These correspond to the four IDE interfaces: Primary 0, Primary 1, Secondary 0, and Secondary 1. Note that HDC and CD-ROM cannot be used at the same time because they occupy the same interface. You can attach any virtual image file containing a preinstalled operating system, or create a new virtual disk image by pressing the drop-down list and choosing **New**.

Currently, QubeVM supports creating only qcow2 image files with a limited file size. You can also create your own images on your desktop using QEMU and specify the parameters you want.

For example, to create a virtual disk image using the `qemu-img` command-line utility:

```bash
qemu-img create -f qcow2 harddisk.qcow2
```

The qcow2 format is compact and grows as data is added to the disk. It also supports preallocation and encryption. The `qemu-img` utility lets you specify the cluster size and provides other useful parameters for optimizing image read/write speeds. QEMU supports several additional image formats, depending on how you intend to use the virtual disk.

If you want to learn more about installing an operating system on your desktop, which is the recommended approach, go to **[Install an OS with QEMU](tutorials.md)** in the **[Tutorials](tutorials.md)** section.

------------------------------------------------------------------------

## Removable Drives

QubeVM supports one slot for a CD-ROM and two slots for floppy drives. You can insert an ISO image into the CD-ROM drive and an IMG image into a floppy drive. QubeVM also supports up to four hard disk drives that can be created and attached to a virtual machine. These are usually labeled HDA, HDB, HDC, and HDD. You can insert any supported image type listed below. Keep in mind that you cannot use a hard disk drive in HDC and a CD-ROM at the same time.

------------------------------------------------------------------------

## Virtual Image Types

QubeVM can use the following virtual image types for reading and writing:

- **img:** A raw image format used for hard disk and floppy disk images. It does not support resizing, so all space must be preallocated when the image is created, which can make the image difficult to transfer unless it is archived with a utility such as `zip`. Its advantages include broad compatibility with emulators and fast read/write performance because the format does not use compression.
- **qcow:** The original qcow format used by QEMU. It is resizable and grows as data is written, so newly created images are nearly empty.
- **qcow2:** An advanced version of the qcow format that supports encryption, compression, and other enhancements. This is the default format QubeVM uses to create hard disk images on the fly.
- **vmdk:** A format originally created for VMware virtualization products. QEMU also supports it, making it a useful option when you intend to use the image with VMware.
- **vdi:** A format originally created for Oracle VirtualBox. It is a good option when you want to run the virtual disk image on your desktop using VirtualBox.
- **vpc:** The Virtual PC disk format.
- **vhd, vhdx:** Formats created for Microsoft's Hyper-V virtual machines.
- **iso:** Used for CD-ROM images. Other CD-ROM image types, such as BIN and CUE, are not supported.

------------------------------------------------------------------------

## File Access After Import

If you have just imported one or more virtual machines from an older version of QubeVM or from another Android device, you might not have immediate access to the image files. This is due to Android file permissions. If an error occurs while starting the virtual machine, re-select the image file from the drive drop-down list by choosing **Open**. This additional step may be needed for both hard disk files and removable-drive images.

We recommend reopening and selecting the hard disk image and ISO files after importing a virtual machine to ensure that QubeVM has the required access.

------------------------------------------------------------------------

## Shared Folder

Shared Folder support is available again in QubeVM. You can use the shared folder to exchange files between your Android device and your virtual machine. To learn more, go to **[Share Files](share-files.md)**.

------------------------------------------------------------------------

## Boot Drive

You can set the drive used as the first boot device with the **Boot from device** option under the Boot section in QubeVM. If you use QubeVM to install an operating system from a CD-ROM ISO file, set this value to **CD-ROM**. Otherwise, you can leave it at the default value. For older operating systems, you might need to set it to boot from a floppy drive.

------------------------------------------------------------------------

## Kernel and initrd

QubeVM provides a GUI section that you can use to set the kernel and initrd image files for the QubeVM ARM emulator. There is also a field for specifying the append options with which you want to boot. Make sure you follow the instructions for the operating system kernel you are booting.

------------------------------------------------------------------------

## Changing Drives

You can change removable drives, such as CD-ROM and floppy images, after starting the virtual machine. With the QGE interface, press the icon showing the external drive. With the VNC interface, change the image from the main screen under the **Removable Drives** section. To eject an ISO or floppy image, choose **None** from the drop-down list.

**Note:** You can eject or change only removable drives. Hard disk drives cannot be changed after the virtual machine has started.

[TUTORIALS](tutorials.md)
