<p align="center">
  <img src="assets/qube.png" alt="QubeVM" width="140"/>
</p>

<h1 align="center">QubeVM</h1>

QubeVM is a QEMU-based virtual machine emulator for Android

<p align="center">
  <a href="https://discord.gg/dZbQeakC6Y"><img src="https://img.shields.io/badge/Discord-5865F2?logo=discord&logoColor=ffffff&style=flat" alt="Discord"></a>
</p>

## What it does

Emulates a full virtual machine on Android, allowing installation and execution of guest operating systems inside the app

QubeVM currently supports **QEMU 11.1.2 and 7.2.22**, compiled natively for Android as a `.so` library without relying on third-party wrappers

## Requirements

- **Android 11+**
- **2 GB RAM minimum** (more depending on guest OS)
- **64-bit CPU** with a capable GPU, Adreno recommended over Mali if VirGL is used
- **Root (optional)** needed for TAP networking and KVM

## Supported CPU Architecture

**Guest** is the operating system you plan to run

**Host** is your device running the guest operating system

Guest architectures: x86, ARM, PowerPC, m68k

Host architectures: ARM64 and x86_64

### Acceleration
QubeVM offers different ways to run your virtual machine depending on your device:

| Mode | Status |
|------|--------|
| KVM | Supported for users with a custom kernel exposing /dev/kvm, hardware accelerated and significantly faster than TCG. Not available on stock Android by default, read below for more info |
| TCG (software emulation, single-threaded) | Default, works on all devices |
| MTTCG (Multi-Threaded TCG) | Supported, runs guest vCPUs on separate host threads for better SMP performance |
| High Priority Mode | Runs the VM emulation thread at its greatest speed for improved performance |

> [!CAUTION]
> **High Priority Mode** may cause device overheating. Ensure your device has adequate cooling during extended sessions

## Display

Qube supports two display modes:

- **QGE (Qube Graphics Engine)**, the default mode, displays frames natively, also supports native audio output using AAudio, The default and recommended option
- **VNC (Virtual Network Computing)**, requires an external VNC client, uses more resources

## Network Support

- **TAP**, bridges the guest directly to the host's network stack with its own IP, like a real physical machine. Requires root (`/dev/net/tun`)
- **User (SLIRP)**, guest sits behind a virtual router. No root needed (in short, acts like ethernet)

## KVM Support

Not available by default, stock Android does not expose `/dev/kvm` to apps, so TCG is used as default Accelerator

Users running a **custom kernel** with KVM enabled can use KVM acceleration instead of TCG for matching host/guest architectures.

## Docs
For a detailed information, check the [Docs](docs/index.md)

## License
This project is licensed under the **GNU General Public License v3.0**
See [COPYING](COPYING) for more info

## Credits

QubeVM is a forked and improved version of the [Limbo PC Emulator](https://github.com/limboemu/limbo), however it depends on these projects too:

- [qemu](https://github.com/qemu/qemu)
- [glib](https://gitlab.gnome.org/GNOME/glib.git)
- [virglrenderer](https://gitlab.freedesktop.org/virgl/virglrenderer)
- [libepoxy](https://github.com/anholt/libepoxy)
- [pixman](https://gitlab.freedesktop.org/pixman)
- [libffi](https://github.com/libffi/libffi)
- [libslirp](https://gitlab.freedesktop.org/slirp/libslirp)

QubeVM also includes code from:
- [avnc](https://github.com/gujjwal00/avnc) Virtual keys
