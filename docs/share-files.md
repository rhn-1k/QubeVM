# File Sharing Tutorial

## Share Files

QubeVM has three ways of sharing your device files:

1. **Shared Folder** this uses QEMU VVFAT by default, but also supports VirtIO-9P as an alternative interface. You can choose between them in the `Disks` section.

2. **Shared Folder Server** that shares your Android Downloads
folder with the guest over HTTP, no extra apps or network setup
required.

3. **ISO images** creating iso images and mounting them to the virtual machine

To switch between the Shared Folder interfaces (VVFAT and VirtIO-9P), click on the **Shared Folder icon** in the toolbar.

------------------------------------------------------------------------

## Shared Folder

QubeVM adds your shared folder as a drive to make the operating system detect it as a hard drive.

There are two interfaces you can choose between:

- **VVFAT** - the default option, works with all guests including Windows.
- **VirtIO-9P** - a paravirtualized file system that provides better performance and live file sharing, but it is only supported by Linux guests (Windows guests do not support it).

## Limitations of Shared Folder (VVFAT)
1. The shared folder is limited on 500mb size, more than 500mb can make the virtual machine unbootable
2. The Shared Folder doesn't support live updates for files when changes occur on the host's side, which means you should restart the whole VM to see the changes, this is a QEMU limitation for the VVFAT device and we can't do anything about it.

## VirtIO-9P
VirtIO-9P is a paravirtualized file system that allows the guest to access
a shared folder with better performance and live updates (changes on the
host are reflected immediately in the guest).

**Note:** VirtIO-9P only works with **Linux guests**. Windows guests do
not support this device, so for Windows you should use the VVFAT interface instead.

## Shared Folder Server

QubeVM runs a lightweight built-in HTTP server that exposes your
Android device's Downloads folder to the guest virtual machine. Turn it
on from the menu item on the QubeVM toolbar, once enabled QubeVM shows
you the server address to use from inside the guest.

The server is reachable from the guest at:

> http://10.0.2.2:19000

**Downloading a file from Android to the guest**
From inside the guest, open that address in a browser or download tool
to browse and pull files from your Downloads folder.

**Uploading a file from the guest to Android**
You can upload straight from the guest with curl:

> curl -T myfile.txt http://10.0.2.2:19000/myfile.txt

The file will show up in your Android Downloads folder.

**Notes**
The server only exposes the Downloads folder, not the whole filesystem,
and only runs while enabled from the toolbar. This is the recommended
way to move files in and out of the virtual machine for most guests
since it doesn't need a network card, drivers, or a separate FTP app.

------------------------------------------------------------------------

## Using ISO image files

You can create ISO image files of your files and access them via the
CDROM device manager within QubeVM. The QEMU emulated CDROM/DVD interface
should also support large files that would not otherwise fit in the
virtual fat shared disk. Another benefit is that using ISO files totally
isolates your virtual machine and provides better security. The
disadvantage is that your virtual machine does not have write access to
your Android filesystem but that could be a good thing. To be able to
add Android files to ISO images download and
install **[ISOCraft](https://play.google.com/store/apps/details?id=com.pb.android.isocraft)** from
the Play Store, it even supports large files that would normally fit in
a DVD size disk.

[TUTORIALS](tutorials.md)
