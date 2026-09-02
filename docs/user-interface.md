# User Interface Tutorial

## User Interface

QubeVM provides 2 different user interfaces to control your virtual
machines: QGE and VNC.

There are some key differences between these two options that basically
boil down to a trade off between usability and performance:
1) **QGE** has **audio support** which is preferred for media output while VNC does not support audio.
2) **QGE** has native communication with QEMU and provides a
more **responsive** graphical user interface while VNC relies on a
network protocol to control the virtual machine.
3) **VNC** allows you to **zoom** and **pan** on the screen (most of the
VNC clients anyway) while QGE does not.

There is more detailed information about each user interface, scroll
down to read more and decide what interface suits you better. Note that
if you pause the virtual machine you won't be able to change the user
interface until the next time you reboot the machine.

------------------------------------------------------------------------

## QGE

QGE (Qube Graphics Engine) is QubeVM's own native display wrapper, built
specifically for Android instead of relying on an external desktop
library. It talks to QEMU directly and hands guest frames straight to
Android's native bitmap/surface APIs, cutting out the extra abstraction
and copy overhead that a general-purpose desktop display layer would
add. This gives QGE lower input latency, native audio support via
AAudio, and a display path that's tuned for mobile rather than adapted
from a desktop-first design.

## Keyboard

QubeVM makes uses of the native android virtual keyboard events which are
forwarded to the virtual machine. You can enable the android virtual
keyboard by pressing on the little keyboard icon on the QubeVM toolbar.
Currently only US Keyboard layout is supported though you can try using
another layout by entering the following option in the advanced
parameters (QubeVM bottom main screen). For example if you want to use
the German keyboard layout enter in extra parameters:
*-k de*

## Mouse

QubeVM provides the following modes to control the mouse in the virtual
machine:

1.  **Trackpad Mode**: slide your finger on the screen to move the mouse
    like you would do on a trackpad on a laptop. This is the recommended
    mode for Phones or Android devices with a small display.
2.  **Touch Screen**: tap to click like on a touch screen on a tablet.
    This mode requires you set the mouse under the Interface section in
    QubeVM to "usb-tablet". You will also need to install the correct
    drives in the guest Operating System. This mouse mode is recommended
    on larger displays like tablets.

<!-- -->

1.  **External Mouse**: plug in your usb or bluetooth mouse and control
    the mouse. This mouse mode works with an emulated "usb-tablet"
    device like the touch screen mode above so you will need to the same
    setup as well. This mouse mode is recommended if you use QubeVM on a
    Chrome Notebook, Samsung Dex station, ASUS Zenfone connected to a
    monitor, or any desktop PC that can run QubeVM.

## Touch Gestures

QubeVM provides several touch screen gestures for controlling the mouse
on a virtual machine. The following gestures are supported:

- Tap to Click.
- Press Double Tap to Double Click.
- Press Volume Up for Mouse Middle Button.

<!-- -->

- Press Volume Down to right Click
- Hold and drag to click and drag. Make sure you press for a least a
  second, if the gesture is not detect increase the Mouse Button delay
  option under Settings in QubeVM.

## Display

You can set the refresh rate for the monitor display by clicking on the
little monitor icon on the toolbar. There are 2 options:

- **Default refresh rate**: changes how fast the virtual machine display
  refreshes.
- **Idle refresh rate**: changes how fast the virtual machine display
  refreshes when there are no display changes or events.

Note: Higher values provide faster refresh rates but are more expensive
in processing power and might slow down the virtual machine so use
caution.

## Audio Volume

While you're in QGE interface you won't be able to use the volume
buttons on your device since they are reserved for the Middle and Right
button mouse for the virtual machine. Instead to change the Android
device volume press on the menu item that looks like a soundcard on the
QubeVM toolbar and use the slider on the popup window to change the
device audio volume.

## **Key Mapper**

QubeVM provides a new feature called Key Mapper for QGE that provides a
tiled-layout keymap that you can customize with keyboard keys and mouse
buttons. This is a great feature that lets you creating your own game
controller keypad if you want to use with games.

**Instructions**
Start you virtual machine using QGE interface.
Bring up the Key Mapper under edit mode by tapping on the option "Toggle
Key Mapper" from the menu.
To create a new Mapper press on the "**+**" button.
Give a name to your new Key Mapper.
Now you can assign key and mouse buttons to the tiled layout that appear
on your screen.
Press on a tile to highlight it and then press a key from your virtual
keyboard to assign to the tile.
You can use any virtual keyboard to assign keyboard keys but we
recommend Hacker's Keyboard.
You can also assign a mouse button key by using the Volume Buttons:
*Volume Up: Middle Button
Volume Down: Right Button*

For advanced keyboard keys (ie Shift, Alt, Ctrl) and mouse buttons you
can press on the Button with the Toolset icon.
You can assign multiple keys and mouse buttons for a tile with a maximum
of 6 keys and/or buttons.
To clear a key press on the "**x**" button from the menu.
If you want the action to repeat for a tile you can press on the
Rotation button.
To use the Key Mapper you just created and set up press the Checkmark
button.
If you want to delete a Key Mapper press the trash bin button.
To select a Key Mapper use the scroll list and press on the name of the
Key Mapper you want to use/edit.
To rename a Key Mapper press on the white strip containing the name and
type the new name.

------------------------------------------------------------------------

## VNC

VNC is a network user interface that allows you to display and control
the virtual machine remotely as well as locally. Newer versions of QubeVM
do not have the built in VNC client so you have to download an external
VNC Client.

**VNC Viewers:**
Newer QubeVM versions require a VNC Viewer installed on your android
device. We recommend RealVNC viewer as it has advanced features like
zoom and panning that makes navigation easier than the obsolete VNC
Viewer that QubeVM used to have..

**Connect**
Start the virtual machine using the VNC user interface.
Open your VNC viewer and connect to the virtual machine to this
addresss:
*localhost:5901*

Make sure you also set a password via the QubeVM settings for increased
security. If you want to connect to your virtual machine from an
external android device, pc, or other device you can allow external
connection option found in settings. Since VNC connections to QubeVM are
not encrypted make sure your network is secure in order to prevent
others from viewing and controlling your virtual machine. This generally
means that you should not use public Wifi access points while using the
VNC interface.

**Touch Gestures:**
VNC is very similar to the QGE interface when it comes to the touch
gestures but some of the gestures might depend on the VNC client you
choose to install. The most common mouse mode that vnc viewers support
is the trackpad mode. Though if you want to use the emulated touch
screen mode you should use the QGE interface instead with "usb-tablet"
mouse emulation. Keep in mind that your operating system can support
touch screens and you also have drivers installed inside the guest
machine.

**Recommendations:**
QubeVM Emulator can run even faster under VNC Mode if you change to a 256
color mode as a trade off since you lose colors for gaining more speed.
Lowering your display resolution inside the guest machine is also
another way to increase the performance.

**Limitations:**

- Sound is not supported via the VNC network protocol so choose the QGE
  interface if you need to use sound.
- If your mouse doesn't align to the local cursor in your VNC viewer try
  disabling the mouse acceleration inside your Operating System. This
  setting is usually found in the control panel of your operating system
  or preferences. If you have Linux you can also try "xset m 1" on the
  command line to disable mouse acceleration. If your virtual machine
  operating system supports touch screens you can use "usb-tablet" under
  the mouse emlation mode.

[TUTORIALS](tutorials.md)
