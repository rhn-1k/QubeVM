# Video, Audio, Network Tutorial

## Video, Audio, and Network devices

QubeVM supports most of the major emulated devices that QEMU supports.
Find below a guide for setting up your emulated devices with a virtual
machine and tweaking the guest operating system for better use for a
mobile device.

------------------------------------------------------------------------

## Graphics

QubeVM provides graphics emulation via QEMU under the following graphics
cards:

**std (standard)**
This is the standard VGA adapter works primarily with most Operating
Systems. If other graphics cards fail use this one.

**Cirrus (Cirrus Logic)**
Super VGA adapter emulation should provide larger resolution and more
colors within the guest. Make sure you have the appropriate drivers if
you are install other operating systems.

**VMWare**
This is an SVGA type emulation by VMWare that usually have optimized
performance with the VMWare Tools installed within the guest (not
supported).

**cg3 (Creator graphics card)**
This is the standard graphics card emulated by the SPARC emulator (not
available for other emulators)

**virtio with gl (Virgl 3D acceleration)**
Paravirtual graphics card with GPU-accelerated 3D via Virgl. Guest OpenGL
calls are translated through virglrenderer and rendered by your phone's
own GPU instead of being software-rendered by QEMU, so 3D-capable guests
get much better performance and smoother animation. Requires a guest OS
and drivers that support virtio-gpu/Virgl. You can enable it under
Settings -\> Screen.

**Note:** Windows guests do not support Virgl. Windows has no
virtio gl virgl driver, so use std or non-gl for Windows instead.
Virgl is best suited for Linux guests with virtio gl support.

## QGE (Qube Graphics Engine)

QubeVM renders through QGE, its own native display path, instead of the
SDL backend most QEMU-based emulators use. QGE hands guest frames
straight to Android's native bitmap/surface APIs instead of going
through SDL's abstraction layer, which cuts out a copy and translation
step on every frame. In practice this means lower input latency, less
per-frame overhead, and a display path that can be tuned specifically
for Android rather than inheriting SDL's generic desktop assumptions.
The Virgl/virtio-gpu 3D path above renders through QGE as well, using
Android's system EGL directly.

## Video tweaks

There are several settings you might find useful if you want to make
your virtual machine more usable on a mobile device.

First for the majority of OSes it is highly recommended to use standard
vga (std) for better performance and compatibility. The cirrus and the
vmware  drivers might push additional pressure on system resources so
they might have an impact on performance. Try also reducing the Display
Resolution and color depth within the virtual machine for better
results. If the fonts or icons within your virtual machine are too small
you can try to increase the dpi settings if such option is supported by
the guest operating system. If you use VNC you might want to lower the
color on the client settings this should also provide a slight
performance boost.

Other helpful things you can do is try locking the orientation or always
showing the toolbar if you use QubeVM on a Desktop or in Desktop mode. If
you want to have the Android notification tray visible then disable the
Fullscreen option. You can find most of these these options under
Settings -\> Screen.

------------------------------------------------------------------------

## Audio

QubeVM can emulate the following sound cards:
sb16 (Creative Soundblaster 16)
ac97 (Realtek)
adlib (Ad Lib MIDI sound card)
cs4231a (Crystal Semiconductor)
gus (Gravis Ultrasound)
es1370 (Ensoniq AudioPCI)
hda (Intel High Definition Audio)
pcspk (PC Speaker)

Most operating systems should be able to detect any of the above
sound card but the most recommended is trying sb16 with OSS or Alsa
setup. Some applications might not be able to detect either so you might
want to try the Pulse Audio drivers if you have to. Try sb16 for older
setups and ac97 for newer ones, just make sure you have the right
drivers installed.

**AAudio**
Recent versions of QubeVM are using AAudio and depending on it, following
enhancements that will allow for smoother audio:
1. Sampling rate of 48,000 Hz
2. Support for AAudio, Android's native low-latency audio API

AAudio is a native backend and is only available through QGE, it does
not work with the VNC display path yet.

**Limitations**
Audio is not available for the VNC interface it is only available for
QGE.
And the second most important note is that even though audio is
supported via QGE it can be choppy on some phones.
This is probably happening because of these 2 reasons:

1) Most android devices throttle the CPU to avoid overheating and to
conserve battery. To test whether this is the case with your device
place your finger on the toolbar or anywhere on the screen and notice if
the emulation speed changes or the audio is getting smoother. If so then
you your device is probably throttling the CPU, there is a good reason
for it, and there isn't much you can do.

2) Playback of encoded audio files like MP3 within the virtual machine
might have a delay due to decoding. If playback of wav files have better
performance then the explanation would be that your device might not be
powerful to provide a smooth sound emulation and you should consider
disabling audio.
------------------------------------------------------------------------

## Network

QubeVM supports 2 options for providing network and internet access to
the virtual machine: User and TAP.

**User: **This mode supports NAT and provides an internal ip address to
the guest virtual machine. Event though the guest OS is under a internal
network, you should always operate QubeVM under a network that you trust.
You will still be able to access the internet but the virtual machine
will be behind a private network so external devices will not be able to
access the virtual machine unless you use Host Forward rules, see
below.
**TAP:** This mode provides a bridge to the external network requesting
a separate ip address than the on used by your Android device. This mode
should generally be only available for rooted Android devices with a TAP
device already configured. If you can't setup a TAP device or don't have
a rooted Android device you should use the **User **mode above.

**Network Cards**
QubeVM provides the following Network cards to be enabled with QEMU:
e1000 (Intel 82545EM Gigabit Ethernet NIC)
pcnet (AMD PCNET Family Ethernet Adapter)
rtl8139 (Realtek RTL8139 10/100M Ethernet Adapter)
ne2k_pci (Novell's NE2000 Ethernet Card)
i82551 (Intel's 82551 Fast Ethernet Controller)
virtio-net (Virtio fast paravirtual network card)

For most older operating systems the ne2k_pci might be the most
compatible card to use. For advance setup you can try the virtio network
which might provide slightly faster network since it uses other means of
delivering faster network access to the virtual machine but it requires
a different set of drivers.

If you're interested on virtio you can read the documentation that comes
with Red Hat's virtio
drivers **[here](https://access.redhat.com/articles/2488201)** or more
specifically how to setup the virtio network interface on windows
guest **[here](https://access.redhat.com/documentation/en-us/red_hat_enterprise_linux/6/html/virtualization_host_configuration_and_guest_installation_guide/form-virtualization_host_configuration_and_guest_installation_guide-para_virtualized_drivers-mounting_the_image_with_virt_manager)**.
The instruction are using the virt-manager which you can ignore but you
can focus on the guide to install the virtio drivers inside the virtual
machine as well as the rest which should be familiar territory.

**DNS Settings**
Currently QubeVM is unaware of the DNS settings of your phone though you
can configure the DNS address for the Guest OS by entering the DNS
address in the box "DNS Address" under the Network section. The default
value is set to the Google public DNS server: 1.1.1.1 though you can
change it instead to your routers DNS server or any other DNS server you
wish to choose.

**Host Forward**
You can forward specific ports to the guest machine in order to access a
specific service on the guest virtual machine from your Android device
using an SSH or FTP client. This is helpful if you want to send files
via FTP or access the remote terminal inside a Linux virtual machine via
SSH.

**Format:**
In the latest versions of QubeVM the syntax has changed to a more
simplified format:
\<ip_protocol\>:\<host_port\>:\<guestport\>,\<ip_protocol2\>:\<host_port2\>:\<guestport2\>,...
where:
\<ip_protocol\> can be tcp or udp
\<host_port\> is the Android port you want to forward packets from
\<guest_port\> the virtual machine port that you want to forward
packets.

**Example:**
If you want to access the FTP and SSH service inside your guest virtual
machine then you use this rule:
tcp:22221:21,tcp:22222:22
Now if you connect to your android device at ports 22221 and 22222 you
will connect to the corresponding virtual machine FTP and SSH
services.

**Notes:**
When you forward ports always make sure you're on a private network
since anyone can use the virtual machine services!

[TUTORIALS](tutorials.md)
