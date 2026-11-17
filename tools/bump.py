#!/usr/bin/env python3
# Copyright (C) Rhn 2026
# A simple script to bump the version for QubeVM apk bundles
# Example: python3 update.py 1.1.1

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
VERSION_FILE = ROOT / "VERSION"

if len(sys.argv) != 2:
    print(f"Usage: python3 {sys.argv[0]} <major.minor.patch>")
    sys.exit(1)

# user input should use the 0.0.0 format
version = sys.argv[1]
parts = version.split(".")
if len(parts) != 3 or not all(p.isdigit() for p in parts):
    print(f"Invalid format {version}, Expected: major.minor.patch")
    sys.exit(1)

major, minor, patch = (int(p) for p in parts)
# raw 00000 format
version_code = major * 10000 + minor * 100 + patch

manifests = sorted(ROOT.glob("qube-android-*/src/main/AndroidManifest.xml"))
if not manifests:
    print("No AndroidManifest found. Are you inside QubeVM directory?")
    sys.exit(1)
first = manifests[0].read_text()
old_code = int(re.search(r'android:versionCode="(\d+)"', first).group(1))
old_name = re.search(r'android:versionName="([^"]*)"', first).group(1).split("-")[0]

for manifest in manifests:
    text = manifest.read_text()
    text = re.sub(r'android:versionCode="\d+"', f'android:versionCode="{version_code}"', text)
    text = re.sub(r'android:versionName="([^"]*)"', lambda m: f'android:versionName="{version}{m.group(1)[len(m.group(1).split("-")[0]):]}"', text)
    manifest.write_text(text)

# update VERSION also so it can send notification to users
if VERSION_FILE.exists():
    old_file_version = VERSION_FILE.read_text().strip()
    # 000.00 format
    new_file_version = f"{version_code // 100}.{patch:02d}"
    VERSION_FILE.write_text(new_file_version + "\n")
else:
    print(f"No {VERSION_FILE} file found, this can break the software update feature")

print(f"Bumped bundles from {old_name} ({old_code}) to {version} ({version_code})")
