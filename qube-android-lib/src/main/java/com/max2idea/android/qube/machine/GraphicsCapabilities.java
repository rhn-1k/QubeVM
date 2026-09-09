/*
Copyright (C) Rhn 2026
 */
package com.max2idea.android.qube.machine;

public final class GraphicsCapabilities {
    // Some variants gets -device instead of -vga, see VMExecutor.java
    // x86
    public static final String VIRTIO = "virtio";
    public static final String VIRTIO_VGA = "virtio-vga";
    public static final String VIRTIO_VGA_GL = "virtio-vga-gl";
    // Arm
    public static final String VIRTIO_GPU_GL_DEVICE = "virtio-gpu-gl-device";
    public static final String VIRTIO_GPU_GL_PCI = "virtio-gpu-gl-pci";

    public static boolean isDeviceBackedGpu(String vga) {
        return vga != null && (
                vga.startsWith("virtio-gpu") ||
                vga.startsWith("virtio-vga") ||
                vga.equals("ati-vga") ||
                vga.equals("cirrus-vga") ||
                vga.equals("bochs-display")
        );
    }

    public static boolean isVirglGpu(String vga) {
        return vga != null && vga.contains("-gl");
    }

    public static boolean requiresGlDisplay(String vga) {
        return isVirglGpu(vga);
    }

    // Venus needs host blob resources, only works for the wired virtio devices, other virgl entries aren't
    // Venus validated, so the accelerator switch is only offered there
    public static boolean supportsVenus(String vga) {
        return VIRTIO_VGA_GL.equals(vga) || VIRTIO_GPU_GL_DEVICE.equals(vga) || VIRTIO_GPU_GL_PCI.equals(vga);
    }

    public static String getDisplayBackend(String vga) {
        if (requiresGlDisplay(vga)) {
            // qube-qge is our own -display type
            // it's more optimized because receives frames directly from qemu
            return "qube-qge,gl=on";
        } else {
            return null;
        }
    }
}
