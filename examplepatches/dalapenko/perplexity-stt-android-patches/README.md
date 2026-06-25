# Perplexity Force Native STT ReVanced Patch

A custom ReVanced patch for the Perplexity Android application that disables the third-party Soniox realtime transcription engine and forces the use of the native Google Speech-to-Text (STT) engine.

## 🚀 Usage with ReVanced Manager (Phone)

You can import these patches into the ReVanced Manager using one of the following methods:

### Option 1: Remote Patches Source (Recommended)
1. Open **ReVanced Manager**.
2. Go to **Settings** > **Sources**.
3. Under **Patches source**, add the following URL:
   ```text
   https://raw.githubusercontent.com/dalapenko/perplexity-stt-android-patches/main/patches.json
   ```

### Option 2: Local Patches Bundle
1. Go to the [Releases](https://github.com/dalapenko/perplexity-stt-android-patches/releases) page and download the latest `patches.rvp` file.
2. In **ReVanced Manager**, go to the patcher screen and import the downloaded `.rvp` file locally.

---

## Known issues

1. Google authorization does not work. Login is only available via email
2. Does not open tracks in Spotify. It only launches the app, but the specific track doesn't turn on.

The problems are related to the mechanics of the ReVanced patches, which leads to signature changes

## 🛠️ Build from Source

If you want to compile the patch (`.rvp` file) manually:

### 1. Configure GitHub Packages Credentials
Because ReVanced libraries are hosted on the GitHub Packages registry, Gradle requires authentication to resolve dependencies. Add your GitHub credentials to the `gradle.properties` file in the root of the project:

```properties
githubPackagesUsername=YOUR_GITHUB_USERNAME
githubPackagesPassword=YOUR_GITHUB_PERSONAL_ACCESS_TOKEN
```
*(Your Personal Access Token requires the `read:packages` scope).*

### 2. Compile the Patch
Run the Android build task to compile the Kotlin code and bundle it into an Android-compatible `.rvp` format:
```bash
./gradlew buildAndroid
```
The compiled patch will be generated at `patches/build/libs/patches-1.0.0.rvp`.
