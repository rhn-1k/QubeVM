> [!WARNING]
> This project is still in an early alpha state, errors and instability are expected

<p align="center">
  <img src="assets/qube.png" alt="QubeVM" width="140"/>
</p>

<h1 align="center">QubeVM</h1>

QEMU-based virtual machine emulator for Android

## What it does

Emulates a full virtual machine (CPU, RAM, disk, display, sound, network) on Android, allowing installation and execution of guest operating systems (Windows, Linux, BSD, legacy OSes) inside the app

QubeVM currently runs on **QEMU 7.2.22**, built natively from the source for Android as a `.so` library, no third-party wrapper is used

## Supported CPU Architecture

x86 / x86_64 for now, planning to support more in the future

## Acceleration

| Mode | Status |
|------|--------|
| KVM | Supported for users with a custom kernel exposing /dev/kvm, hardware accelerated and significantly faster than TCG. Not available on stock Android by default, read below for more info |
| TCG (software emulation, single-threaded) | Default, works on all devices |
| MTTCG (Multi-Threaded TCG) | Supported, runs guest vCPUs on separate host threads for better SMP performance |
| High Priority Mode | Supported, runs the VM emulation thread at its greatest state for improved performance |

## Display

Qube supports two display modes:

- **SDL**, the default mode, smooth and supports audio, good for high stability
- **VNC**, requires an external VNC client, runs at 60Hz refresh rate, good for reducing screen tearing

## Features

- **Disk image creation**, create new virtual disks in **qcow2** format directly from the app (with preset templates: 1G, 2G, 4G, 10G, 20G maximum), as well as raw image support
- **Floppy drives**, attach/mount `.img` floppy disk images
- **CD-ROM / optical drives**, attach/mount `.iso` images for OS installation media
- **Shared folder**, share a folder between the host (Android) and the guest OS for file exchange, without needing network transfer

## Screenshots

<p align="center">
  <img src="assets/1.jpg" alt="KolibriOS runs on Qube" width="250"/>
  <img src="assets/2.jpg" alt="KolibriOS runs with network access (User type)" width="250"/>
  <img src="assets/3.jpg" alt="KolibriOS runs glxgears" width="250"/>
</p>

<p align="center">
  <sub>KolibriOS runs on Qube &nbsp;&nbsp;|&nbsp;&nbsp; KolibriOS runs with network access &nbsp;&nbsp;|&nbsp;&nbsp; KolibriOS runs glxgears</sub>
</p>

## Network Support

- **TAP**, bridges the guest directly to the host's network stack with its own IP, like a real physical machine. Requires root (`/dev/net/tun`)
- **User (SLIRP)**, guest sits behind a virtual router. No root needed (in short, acts like ethernet)

## KVM Support

Not available by default, stock Android does not expose `/dev/kvm` to apps, so TCG is used

Users running a **custom kernel** with KVM enabled can use KVM acceleration instead of TCG for matching host/guest architectures. Currently isn't supported because we didn't release an ARM version yet

## Translations

Contributions from translators are welcome. Feel free to open a pull request to add or improve a language

[translation/](translation/)

## Credits
QubeVM is a forked version of the <a href="https://github.com/limboemu/limbo">Limbo PC Emulator</a>
