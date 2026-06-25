# UltraSandbox

[![🇷🇺 На Русском](https://img.shields.io/badge/🇷🇺-на_русском-green.svg)](README.ru.md)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/lang-Java-orange.svg)]()
[![Kotlin](https://img.shields.io/badge/lang-Kotlin-purple.svg)]()
[![Release](https://img.shields.io/github/v/release/alim-zanibekov/ultrasandbox)](https://github.com/alim-zanibekov/ultrasandbox/releases/latest)

ReVanced patch that makes Android apps think they're running on a fresh stock phone.

## What it does

- Hides VPN interfaces and transport flags.
- Blocks WiFi/Bluetooth scans.
- Blocks all non-system localhost connections.
- Filters /proc/net to system and self.
- Strips Frida/Xposed/Magisk from /proc/self/maps.
- Hides root binaries.
- Returns only system apps from PackageManager.
- Nulls IMEI, IMSI, SIM serial.
- Randomizes Android ID.
- Returns empty contacts, call log, SMS queries.

## Install

### ReVanced Manager (phone)

1. Open ReVanced Manager > Patches > Add patches
2. Select "Enter URL" > paste:
   `https://github.com/alim-zanibekov/ultrasandbox/releases/latest/download/patches.json`
3. Go to Patcher > select target app > enable UltraSandbox > Patch

The Manager will auto-update the patch from this URL.

### revanced-cli (PC)

```
java -jar revanced-cli.jar patch \
  -p ultrasandbox-patches.rvp -b \
  --force target.apk
```

The result is a signed APK file. Before installing patched version you should uninstall the original
app.

## Build from source

Requires [Nix](https://nixos.org/download.html) with flakes enabled.

```bash
# Build the patch bundle
nix develop --command build-rvp

# Or patch an APK directly (builds .rvp + applies it)
nix develop --command patch-apk target.apk
```

## How it works

**Runtime extension** (`revanced/extensions/`) contains Java classes with static wrapper methods.
These get injected into the target APK as a DEX file. Each method calls the real Android API, checks
the result, and returns a sanitized version.

For example, `NetworkSandbox.hasTransport(caps, TRANSPORT_VPN)` calls the real `hasTransport`, but
returns
`false` when the transport is VPN.

**Patch** (`revanced/patches/`) is a ReVanced bytecode patch.
It scans every method in the target APK for calls to target APIs, then rewrites them to point at the
sandbox wrappers.

## Disclaimer

This project is a privacy research tool. It demonstrates how Android apps collect data and how that
collection can be neutralized at the API level. It is intended for personal use on your own devices.
The authors are not responsible for how you use it. Patching third-party apps may violate their
terms of service. This software is provided as-is with no warranty.

## License

MIT
