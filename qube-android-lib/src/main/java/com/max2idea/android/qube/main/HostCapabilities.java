/*

 */
package com.max2idea.android.qube.main;

import android.os.Build;
import android.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class HostCapabilities {
    private static final String TAG = "HostCapabilities";
    private static final Set<String> ARMV9_FEATURE_HINTS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "sve", "sve2", "sme", "sme2", "i8mm", "bf16", "flagm2"
    )));

    private HostCapabilities() {
    }

    public static void logHostCapabilities() {
        HostCapabilitySnapshot snapshot = readSnapshot();
        Log.d(TAG, "Supported ABIs: " + snapshot.supportedAbis);
        Log.d(TAG, "Supported 64-bit ABIs: " + snapshot.supported64BitAbis);
        Log.d(TAG, "ARMv8 baseline available: " + snapshot.arm64Baseline);
        Log.d(TAG, "ARMv9 optimization hints available: " + snapshot.armv9Candidate);
        if (!snapshot.cpuFeatures.isEmpty()) {
            Log.d(TAG, "CPU feature hints: " + String.join(",", snapshot.cpuFeatures));
        }
    }

    public static String getSummary() {
        HostCapabilitySnapshot snapshot = readSnapshot();
        StringBuilder summary = new StringBuilder();
        summary.append("Host ABIs: ")
                .append(snapshot.supportedAbis.isEmpty() ? "unknown" : snapshot.supportedAbis)
                .append('\n');
        summary.append("Host 64-bit ABIs: ")
                .append(snapshot.supported64BitAbis.isEmpty() ? "none" : snapshot.supported64BitAbis)
                .append('\n');
        summary.append("ARMv8 baseline: ")
                .append(snapshot.arm64Baseline ? "yes" : "no")
                .append('\n');
        summary.append("ARMv9 optimization hints: ")
                .append(snapshot.armv9Candidate ? "yes" : "no");
        return summary.toString();
    }

    private static HostCapabilitySnapshot readSnapshot() {
        Set<String> cpuFeatures = readCpuFeatures();
        String[] abis = Build.SUPPORTED_ABIS;
        String[] abis64 = Build.SUPPORTED_64_BIT_ABIS;
        String supportedAbis = abis == null ? "" : String.join(",", abis);
        String supported64BitAbis = abis64 == null ? "" : String.join(",", abis64);
        boolean arm64Baseline = abis64 != null && Arrays.asList(abis64).contains("arm64-v8a");
        boolean armv9Candidate = false;
        for (String feature : cpuFeatures) {
            if (ARMV9_FEATURE_HINTS.contains(feature)) {
                armv9Candidate = true;
                break;
            }
        }
        return new HostCapabilitySnapshot(
                supportedAbis, supported64BitAbis, cpuFeatures, arm64Baseline, armv9Candidate
        );
    }

    private static Set<String> readCpuFeatures() {
        File cpuInfo = new File("/proc/cpuinfo");
        if (!cpuInfo.canRead()) {
            return Collections.emptySet();
        }
        Set<String> features = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(cpuInfo))) {
            String line;
            while ((line = reader.readLine()) != null) {
                int index = line.indexOf(':');
                if (index <= 0) {
                    continue;
                }
                String key = line.substring(0, index).trim().toLowerCase(Locale.ROOT);
                if (!"features".equals(key) && !"flags".equals(key)) {
                    continue;
                }
                String[] values = line.substring(index + 1).trim().split("\\s+");
                for (String value : values) {
                    if (!value.isEmpty()) {
                        features.add(value.toLowerCase(Locale.ROOT));
                    }
                }
            }
            return features;
        } catch (IOException ex) {
            return Collections.emptySet();
        }
    }

    private static final class HostCapabilitySnapshot {
        private final String supportedAbis;
        private final String supported64BitAbis;
        private final Set<String> cpuFeatures;
        private final boolean arm64Baseline;
        private final boolean armv9Candidate;

        private HostCapabilitySnapshot(
                String supportedAbis,
                String supported64BitAbis,
                Set<String> cpuFeatures,
                boolean arm64Baseline,
                boolean armv9Candidate
        ) {
            this.supportedAbis = supportedAbis;
            this.supported64BitAbis = supported64BitAbis;
            this.cpuFeatures = cpuFeatures;
            this.arm64Baseline = arm64Baseline;
            this.armv9Candidate = armv9Candidate;
        }
    }
}
