{ pkgs ? import <nixpkgs> { config.allowUnfree = true; config.android_sdk.accept_license = true; } }:

let
  buildToolsVersion = "34.0.0";

  androidComposition = pkgs.androidenv.composeAndroidPackages {
    buildToolsVersions = [ buildToolsVersion "35.0.0" "36.0.0" ];
    platformVersions = [ "34" "35" "36" ];
    abiVersions = [];
    includeEmulator = false;
    includeNDK = false;
    includeSources = false;
    includeSystemImages = false;
  };
  sdkRoot = "${androidComposition.androidsdk}/libexec/android-sdk";
  aapt2 = "${sdkRoot}/build-tools/${buildToolsVersion}/aapt2";
in
pkgs.mkShell {
  packages = [ pkgs.jdk21 ];
  shellHook = ''
    export ANDROID_HOME=${sdkRoot}
    export ANDROID_SDK_ROOT=${sdkRoot}
    # Force AGP to use the patchelf'd aapt2 from nixpkgs instead of downloading
    # its own (which is a generic-Linux ELF that NixOS's stub-ld refuses to run).
    export GRADLE_OPTS="$GRADLE_OPTS -Dorg.gradle.project.android.aapt2FromMavenOverride=${aapt2}"
  '';
}
