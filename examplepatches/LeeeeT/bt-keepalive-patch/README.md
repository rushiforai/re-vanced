# Bluetooth Audio Keep Alive

A standalone [ReVanced](https://revanced.app) patch that prevents the
first 100&ndash;300&nbsp;ms of audio from being cut off when an app starts
playing through Bluetooth headphones.

## Why

Many Bluetooth headphones&mdash;and the audio HAL on the Android side&mdash;drop
the active A2DP stream to a low-power state after a short period of silence.
When the next clip starts, the link has to wake up, and the first fraction of
a second is lost.

This patch injects a `ContentProvider` into the target app whose `onCreate`
starts an inaudible looping PCM `AudioTrack` (44.1&nbsp;kHz, 16-bit mono,
all-zero samples). The provider is registered in the host manifest, so
Android creates it during process startup&mdash;before `Application.onCreate`
&mdash;and the silent track plays for the entire process lifetime, keeping the
A2DP link continuously active.

The patch is universal: it doesn't fingerprint any host-app code, so it
applies cleanly to any APK.

## Using the patch

Add the bundle to [ReVanced Manager](https://github.com/ReVanced/revanced-manager)
by URL. Manager will then track this repository's releases and offer updates
whenever a new tag is published.

1. Open ReVanced Manager and go to the **Patches** tab.
2. Tap the ✏️ button in the bottom-right, then the **+** button.
3. Choose **Enter URL** and paste:

   ```
   https://github.com/LeeeeT/bt-keepalive-patch/releases/latest/download/revanced-asset.json
   ```

4. Save. The bundle now appears as a patch source in Manager and can be
   selected when patching any APK. The patch is enabled by default.

Manager re-fetches that JSON on its update check; when a new tag is pushed
here, the workflow rebuilds the bundle, refreshes the JSON's `version`, and
Manager picks it up automatically.

## Building from source

### Requirements

* JDK&nbsp;21
* Android SDK with `build-tools;34.0.0` and a recent platform
  (the build will download what it needs automatically if not present)
* A GitHub Personal Access Token with the `read:packages` scope, to pull the
  ReVanced gradle plugin from GitHub Packages

Put the token credentials in `~/.gradle/gradle.properties`:

```properties
githubPackagesUsername=<your GitHub username>
githubPackagesPassword=<your token>
```

Then:

```sh
./gradlew :patches:buildAndroid
```

The bundle lands at `patches/build/libs/patches-<version>.rvp`.

### Nix users

A `shell.nix` is provided that pins JDK, Android SDK and works around a NixOS
quirk where AGP's downloaded `aapt2` binary can't run under the stub dynamic
linker. Just:

```sh
nix-shell --run './gradlew :patches:buildAndroid'
```

## Project layout

```
patches/        ReVanced patch (Kotlin) -- edits the manifest
extensions/     Android library compiled into a .dex bundled with the patch
                (contains BluetoothKeepAliveProvider)
```

## License

GPLv3, same as ReVanced patches. See [LICENSE](LICENSE).
