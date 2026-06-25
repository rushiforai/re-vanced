# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

ReVanced bytecode patch for Instagram that converts **view-once media to permanent media**. The patch intercepts the Raven protobuf `ViewMode` getter and remaps `ONCE (1)` → `PERMANENT (3)`, so view-once photos/videos remain in the conversation and can be saved with Instagram's built-in save functionality.

This is **not a standalone buildable project**. The source files are meant to be integrated into the [ReVanced Patches](https://github.com/ReVanced/revanced-patches) repository and built with its Gradle-based build system.

## Architecture

The patch follows the standard ReVanced two-layer pattern:

1. **Fingerprint** (`patch-src/Fingerprints.kt`) — Locates the target method at patch time using string anchors `"viewMode_"` + `"content_"` to disambiguate the Raven class from its base class `X/484`.

2. **Bytecode patch** (`patch-src/PersistViewOnceMediaPatch.kt`) — Finds the `viewMode` getter by opcode pattern (`IGET` + `IF_EQ`), then injects a `invoke-static` call to the extension after the `IGET` instruction.

3. **Extension (runtime)** (`patch-src/PersistViewOnceMediaPatch.java`) — The Java code that gets compiled into the patched APK. Simple integer remap: if viewMode == 1, return 3.

## Key Details

- Target app: `com.instagram.android`
- Depends on `sharedExtensionPatch` from ReVanced's shared extension infrastructure.
- `build-slim-rvp.sh` (run inside the Docker image) prunes the upstream
  `revanced-patches` clone to the minimal subset needed by this patch,
  rebrands the bundle metadata, compiles to DEX with `:patches:buildAndroid`,
  and writes the final bundle to `output/viewonce-standalone.rvp`.

## Raven ViewMode Enum Reference

From the protobuf enum in `QRF.smali`:

| Value | Name | Behavior |
|-------|------|----------|
| 0 | RAVEN_VIEW_MODEL_UNSPECIFIED | Default/unknown |
| 1 | RAVEN_VIEW_MODEL_ONCE | View-once, auto-expires |
| 2 | RAVEN_VIEW_MODEL_REPLAYABLE | Can be replayed once |
| 3 | RAVEN_VIEW_MODEL_PERMANENT | Permanent, saveable |
