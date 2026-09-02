# Settings for QubeVM Emulator

## Settings

Many of the options you have used on the main screen are now moved under
settings with new versions of QubeVM. Under settings you can find some of
these options:

**Enable Legacy File Manager**
This option will enable a custom file manager to choose hdd and iso
images. This is provided instead of the standard Android file choosers
in cases you have a custom ROM or your stock ROM does not support the
new Android Storage Framework. Bear in mind that this feature does not
support external storage like SD cards. It also requires full access to
your internal storage for which it will request at the first time you
use it.

**Orientation**
This is the orientation that the virtual machine will be displayed. You
can lock the orientation if you frequently move your Android device.
Keep in mind this is not the orientation for the main screen which will
be always be auto rotated.

**Always Toolbar**
It is helpful sometimes to always show the toolbar if you use it a lot
especially if you are on desktop mode and you can spare some extra
pixels.

**Fullscreen**
If you are on an Android device with a small display you might want to
get the most out of your Android display and hide the notification
bar.

**Key Mapper Layout**
You can customize how many buttons your key mapper should display. On
smaller devices it is better to use a 3x6 grid which should provide with
large enough icons to press. Keep in mind the key mapper is helpful for
gaming on handheld Android devices but on Desktop will just get in the
way of your external mouse.

**Prevent Mouse out of Bounds**
Virtual machines you have configured with a "usb-tablet" and use a
"Touch Screen" mouse mode might show an unexpected behavior where the
mouse seems to warp on the other side of the screen. Use this option to
limit the mouse movement and prevent this effect. Note that if the
virtual machine does not support usb-tablet or does not have the drives
installed it will show an invisible wall.

**Key Press and Mouse Button click delay**
This is a workaround for the virtual machines that does not detecting
keyboard and mouse clicks. It is basically an artificial delay in
between presses and clicks to allow the event loop detect all events. If
you use lower values your changes are that the events might not all be
detected.

**Enable QMP Server**
QMP Server is QEMU's communication means that allows you to do many
things like pause and restore the virtual machine state, change the vnc
password, and change CD ROM and Floppy disks.

**Allow External QMP Connections
**If you want to connect to the QMP server from your desktop.

**Enable VNC Password
**It is highly recommended you add a password to your VNC service. This
is because any application on your phone can use the vnc service. You
don't have to add a password but your device will notify you before it
starts the VNC service.

**Allow External VNC Connections**
You can allow a computer on the same network to connect to the VNC
service. The communication is not encrypted so make sure you trust the
network you are using!

**Enable Aaudio
**Aaudio is a native audio stream that is provided by Android and can
give QubeVM better performance. This is only available for devices with
Android Oreo and above.

[TUTORIALS](tutorials.md)
