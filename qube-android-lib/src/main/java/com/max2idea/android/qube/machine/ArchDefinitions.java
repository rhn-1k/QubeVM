/*
Copyright (C) Max Kastanas 2012
Copyright (C) Rhn 2026
 */
package com.max2idea.android.qube.machine;

import android.content.Context;

import com.qube.emu.lib.R;
import com.max2idea.android.qube.install.Installer;
import com.max2idea.android.qube.main.Config;
import com.max2idea.android.qube.main.QubeApplication;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * A simple utility class to retrieved often long architecture attribute lists
 */
public class ArchDefinitions {
    private static String TAG = "ArchDefinitions";

    public static ArrayList<String> getSoundcards(Context context) {
        ArrayList<String> commonSoundcards = new ArrayList<>();
        commonSoundcards.addAll(Arrays.asList(Installer.getAttrs(context, R.raw.common_soundcards)));
        return commonSoundcards;
    }

    public static ArrayList<String> getNetworkDevices(Context context) {
        ArrayList<String> commonNetworkCards = new ArrayList<>();
        commonNetworkCards.addAll(Arrays.asList(Installer.getAttrs(context, R.raw.common_nic_cards)));

        ArrayList<String> networkCards = new ArrayList<>();
        switch (QubeApplication.arch) {
            case x86:
            case x86_64:
                networkCards.add("Default");
                networkCards.addAll(commonNetworkCards);
                break;
            case arm:
            case arm64:
                networkCards.add("Default");
                networkCards.addAll(commonNetworkCards);
                networkCards.addAll(Arrays.asList(Installer.getAttrs(context, R.raw.arm_nic_cards)));
                break;
            case ppc:
            case ppc64:
                networkCards.add("Default");
                networkCards.addAll(commonNetworkCards);
                break;
        }
        return networkCards;
    }

    public static ArrayList<String> getVGAValues(Context context) {
        ArrayList<String> vgaValues = new ArrayList<>();
        if (QubeApplication.arch == Config.Arch.x86 || QubeApplication.arch == Config.Arch.x86_64
                || QubeApplication.arch == Config.Arch.arm || QubeApplication.arch == Config.Arch.arm64
                || QubeApplication.arch == Config.Arch.ppc || QubeApplication.arch == Config.Arch.ppc64) {
            vgaValues.add("std");
        }

        if (QubeApplication.arch == Config.Arch.x86 || QubeApplication.arch == Config.Arch.x86_64) {
            vgaValues.add("cirrus");
            vgaValues.add("vmware");
            vgaValues.add("virtio-gpu-pci");
            vgaValues.add(GraphicsCapabilities.VIRTIO);
            vgaValues.add(GraphicsCapabilities.VIRTIO_GPU_GL_PCI);
            vgaValues.add(GraphicsCapabilities.VIRTIO_VGA);
            vgaValues.add(GraphicsCapabilities.VIRTIO_VGA_GL);
        }

        if (QubeApplication.arch == Config.Arch.arm || QubeApplication.arch == Config.Arch.arm64) {
            vgaValues.add("virtio-gpu-pci");
            vgaValues.add(GraphicsCapabilities.VIRTIO_GPU_GL_DEVICE);
            vgaValues.add(GraphicsCapabilities.VIRTIO_GPU_GL_PCI);
        }

        if (QubeApplication.arch == Config.Arch.ppc || QubeApplication.arch == Config.Arch.ppc64) {
            vgaValues.add("ati-vga");
            vgaValues.add("cirrus-vga");
            vgaValues.add("bochs-display");
            vgaValues.add("virtio-gpu-pci");
            vgaValues.add(GraphicsCapabilities.VIRTIO_VGA);
            vgaValues.add(GraphicsCapabilities.VIRTIO_GPU_GL_PCI);
            vgaValues.add(GraphicsCapabilities.VIRTIO_VGA_GL);
        }

        //XXX: some archs don't support vga on QEMU
        vgaValues.add("nographic");
        return vgaValues;
    }

    public static ArrayList<String> getKeyboardValues(Context context) {
        ArrayList<String> arrList = new ArrayList<>();
        arrList.add("en-us");
        return arrList;
    }

    public static ArrayList<String> getMouseValues(Context context) {
        ArrayList<String> arrList = new ArrayList<>();
        arrList.add("ps2");
        arrList.add("usb-mouse");
        arrList.add("usb-tablet");
        return arrList;
    }

    public static ArrayList<String> getUIValues() {
        ArrayList<String> arrList = new ArrayList<>();
        arrList.add("QGE");
        arrList.add("VNC");
        return arrList;
    }

    public static ArrayList<String> getMachineValues(Context context) {
        ArrayList<String> machinesList = new ArrayList<>();
        machinesList.add("None");
        machinesList.add("New");
        return machinesList;
    }

    public static ArrayList<String> getCpuValues(Context context) {
        ArrayList<String> arrList = new ArrayList<>();
        switch (QubeApplication.arch) {
            case x86:
            case x86_64:
                arrList.add("Default");
                arrList.addAll(Arrays.asList(Installer.getAttrs(context, R.raw.x86_cpu)));
                break;
            case arm:
            case arm64:
                arrList.add("Default");
                arrList.addAll(Arrays.asList(Installer.getAttrs(context, R.raw.arm_cpu)));
                break;
            case ppc:
            case ppc64:
                arrList.add("Default");
                arrList.addAll(Arrays.asList(Installer.getAttrs(context, R.raw.ppc_cpu)));
                break;
        }

        if (QubeApplication.arch == Config.Arch.x86 || QubeApplication.arch == Config.Arch.x86_64 || QubeApplication.arch == Config.Arch.arm || QubeApplication.arch == Config.Arch.arm64)
            arrList.add("host");
        return arrList;
    }

    public static ArrayList<String> getMachineTypeValues(Context context) {
        ArrayList<String> arrList = new ArrayList<>();
        switch (QubeApplication.arch) {
            case x86:
            case x86_64:
                arrList.add("Default");
                arrList.addAll(Arrays.asList(Installer.getAttrs(context, R.raw.x86_machine_types)));
                break;
            case arm:
            case arm64:
                arrList.addAll(Arrays.asList(Installer.getAttrs(context, R.raw.arm_machine_types)));
                break;
            case ppc:
            case ppc64:
                arrList.add("Default");
                arrList.addAll(Arrays.asList(Installer.getAttrs(context, R.raw.ppc_machine_types)));
                break;
        }
        return arrList;
    }
}
