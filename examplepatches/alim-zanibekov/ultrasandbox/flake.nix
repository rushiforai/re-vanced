{
  description = "UltraSandbox — Android APK privacy patcher";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs =
    {
      self,
      nixpkgs,
      flake-utils,
    }:
    flake-utils.lib.eachDefaultSystem (
      system:
      let
        pkgs = import nixpkgs {
          inherit system;
          config.allowUnfree = true;
          config.android_sdk.accept_license = true;
        };

        jdk = pkgs.jdk21;

        androidSdk = pkgs.androidenv.composeAndroidPackages {
          buildToolsVersions = [ "36.0.0" ];
          platformVersions = [ "36" ];
          abiVersions = [
            "x86_64"
            "arm64-v8a"
          ];
          includeEmulator = false;
          includeNDK = false;
        };

        sdkPath = "${androidSdk.androidsdk}/libexec/android-sdk";
        buildTools = "${sdkPath}/build-tools/36.0.0";
        androidJar = "${sdkPath}/platforms/android-36/android.jar";

        revanced-cli-jar = pkgs.fetchurl {
          url = "https://github.com/ReVanced/revanced-cli/releases/download/v6.0.0/revanced-cli-6.0.0-all.jar";
          sha256 = "0n4r7a96y9ziz18713x85ivh4ympx5h6xy559ywjx7fm2yy4jmf2";
        };

        revanced-cli = pkgs.writeShellScriptBin "revanced-cli" ''
          exec ${jdk}/bin/java -jar ${revanced-cli-jar} "$@"
        '';

        build-rvp = pkgs.writeShellScriptBin "build-rvp" ''
          set -euo pipefail

          SRC="''${1:-.}"
          OUT="''${2:-$SRC/build/ultrasandbox-patches.rvp}"
          BUILD=$(mktemp -d)
          trap "rm -rf $BUILD" EXIT

          mkdir -p "$BUILD/rve" "$BUILD/patch" "$BUILD/out/META-INF"
          VERSION="''${VERSION:-dev}"

          echo "[+] Building .rve extension"
          ${jdk}/bin/javac -source 17 -target 17 -Xlint:-options -Xlint:-deprecation \
            -cp ${androidJar} \
            -d "$BUILD/rve" \
            "$SRC"/revanced/extensions/src/main/java/com/ultrasandbox/*.java 2>&1 \
            | grep -v "^Note:" || true

          ${buildTools}/d8 --release --min-api 26 \
            --output "$BUILD/rve" \
            $(find "$BUILD/rve" -name "*.class") 2>/dev/null || true

          echo "[+] Building patches"
          ${pkgs.kotlin}/bin/kotlinc \
            -Xskip-prerelease-check -jvm-target 17 \
            -cp ${revanced-cli-jar} \
            -d "$BUILD/patch" \
            "$SRC"/revanced/patches/src/main/kotlin/com/ultrasandbox/UltraSandboxPatch.kt 2>&1 \
            | grep -v "^w:" || true

          echo "[+] Packaging .rvp"
          cp -r "$BUILD/patch/"* "$BUILD/out/"
          ${buildTools}/d8 --release --min-api 26 \
            --lib ${revanced-cli-jar} \
            --output "$BUILD/out" \
            $(find "$BUILD/patch" -name "*.class") 2>/dev/null || true
          cp "$BUILD/rve/classes.dex" "$BUILD/out/ultrasandbox.rve"

          cat > "$BUILD/out/META-INF/MANIFEST.MF" << EOF
          Manifest-Version: 1.0
          Name: UltraSandbox
          Description: Patched app sees a fresh stock phone
          Version: $VERSION
          Timestamp: $(date +%s)
          Source: https://github.com/nicknsy/ultrasandbox
          Author: ultrasandbox
          License: MIT
          EOF

          mkdir -p "$(dirname "$OUT")"
          cd "$BUILD/out" && ${jdk}/bin/jar cfm "$OUT" META-INF/MANIFEST.MF .
          echo "[+] Ta-da $OUT"
        '';

        patch-apk = pkgs.writeShellScriptBin "patch-apk" ''
          set -euo pipefail
          APK="''${1:?Usage: patch-apk <input.apk> [output.apk]}"
          OUT="''${2:-''${APK%.apk}-patched.apk}"
          RVP=$(mktemp --suffix=.rvp)
          trap "rm -f $RVP" EXIT

          ${build-rvp}/bin/build-rvp . "$RVP"

          echo "[+] Patching $(basename "$APK")..."
          ${jdk}/bin/java -jar ${revanced-cli-jar} patch \
            -p "$RVP" -b \
            -o "$OUT" --force \
            "$APK"

          echo "[+] Output: $OUT"
        '';

      in
      {
        packages = {
          inherit build-rvp patch-apk;
          default = build-rvp;
        };

        devShells.default = pkgs.mkShell {
          name = "ultrasandbox";

          buildInputs = [
            jdk
            pkgs.kotlin
            pkgs.apktool
            pkgs.apksigner
            revanced-cli
            build-rvp
            patch-apk
          ];

          ANDROID_HOME = sdkPath;
          ANDROID_SDK_ROOT = sdkPath;
          JAVA_HOME = "${jdk}";

          shellHook = ''
            export PATH="${buildTools}:${sdkPath}/platform-tools:$PATH"
          '';
        };
      }
    );
}
