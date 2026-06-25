#!/usr/bin/env bash
set -euo pipefail

log() { echo "[$(date +%H:%M:%S)] $*"; }

UPSTREAM="/build/revanced-patches"
WORK="/tmp/slim-patches"

# ─────────────────────────────────────────────────────────
# 1. Copy upstream to a scratch dir (cached clone stays untouched)
# ─────────────────────────────────────────────────────────
log "Copying upstream revanced-patches → $WORK"
rm -rf "$WORK"
cp -a "$UPSTREAM" "$WORK"

# Rebrand the bundle metadata exposed to ReVanced Manager
# (patches { about { name / description } } block in patches/build.gradle.kts)
BUNDLE_NAME="daboynb / Instagram View-Once"
BUNDLE_DESC="Saves Instagram view-once photos and videos to Pictures/InstagramViewOnce before they expire."
log "Rebranding bundle: \"$BUNDLE_NAME\""
sed -i "s|name = \"ReVanced Patches\"|name = \"$BUNDLE_NAME\"|" \
       "$WORK/patches/build.gradle.kts"
sed -i "s|description = \"Patches for ReVanced\"|description = \"$BUNDLE_DESC\"|" \
       "$WORK/patches/build.gradle.kts"

# ─────────────────────────────────────────────────────────
# 2. Prune patches: keep only the minimal subset needed by our view-once patch
#    and its transitive closure.
#      - instagram/misc/extension/   (sharedExtensionPatch for instagram)
#      - shared/misc/extension/      (sharedExtensionPatch factory)
#      - shared/misc/mapping/        (resourceMappingPatch / ResourceType)
#    util/ lives outside patches/ and is kept intact — it imports from
#    shared/misc/mapping/, which is in the keep-set above.
# ─────────────────────────────────────────────────────────
PATCHES_ROOT="$WORK/patches/src/main/kotlin/app/revanced/patches"
log "Pruning patches under $PATCHES_ROOT"

KEEP_TMP="/tmp/keep-patches"
rm -rf "$KEEP_TMP"
mkdir -p "$KEEP_TMP/instagram/misc" "$KEEP_TMP/shared/misc"
cp -a "$PATCHES_ROOT/instagram/misc/extension" "$KEEP_TMP/instagram/misc/"
cp -a "$PATCHES_ROOT/shared/misc/extension"    "$KEEP_TMP/shared/misc/"
cp -a "$PATCHES_ROOT/shared/misc/mapping"      "$KEEP_TMP/shared/misc/"

rm -rf "$PATCHES_ROOT"
mkdir -p "$PATCHES_ROOT"
cp -a "$KEEP_TMP/instagram" "$PATCHES_ROOT/"
cp -a "$KEEP_TMP/shared"    "$PATCHES_ROOT/"

# ─────────────────────────────────────────────────────────
# 3. Prune patch resources (no strings in our patch; keep addresources
#    skeleton so strings-processing.gradle.kts has its expected input)
# ─────────────────────────────────────────────────────────
RES_ROOT="$WORK/patches/src/main/resources"
log "Pruning resources under $RES_ROOT"
find "$RES_ROOT" -maxdepth 1 -mindepth 1 -type d ! -name "addresources" -exec rm -rf {} +

# ─────────────────────────────────────────────────────────
# 4. Prune extensions: keep only 'shared' (Utils DEX) and 'instagram'
#    (target module for our Java extension code)
# ─────────────────────────────────────────────────────────
EXT_ROOT="$WORK/extensions"
log "Pruning extensions under $EXT_ROOT"
find "$EXT_ROOT" -maxdepth 1 -mindepth 1 -type d ! -name "shared" ! -name "instagram" -exec rm -rf {} +

# ─────────────────────────────────────────────────────────
# 5. Inject our patch sources
# ─────────────────────────────────────────────────────────
PATCH_DST="$PATCHES_ROOT/instagram/direct/viewonce"
mkdir -p "$PATCH_DST"
cp /input/patch-src/Fingerprints.kt              "$PATCH_DST/"
cp /input/patch-src/PersistViewOnceMediaPatch.kt "$PATCH_DST/"
log "Bytecode patch → $PATCH_DST"

EXT_DST="$WORK/extensions/instagram/src/main/java/app/revanced/extension/instagram/direct/viewonce"
mkdir -p "$EXT_DST"
cp /input/patch-src/PersistViewOnceMediaPatch.java "$EXT_DST/"
log "Extension      → $EXT_DST"

# ─────────────────────────────────────────────────────────
# 6. Build
# ─────────────────────────────────────────────────────────
log "Building slim .rvp (gradle :patches:buildAndroid)..."
cd "$WORK"
./gradlew --no-daemon :patches:buildAndroid 2>&1 | tail -40

# ─────────────────────────────────────────────────────────
# 7. Locate and publish the produced bundle
# ─────────────────────────────────────────────────────────
RVP=$(find "$WORK" -path "*/build/*.rvp" -type f | head -1)
if [ -z "$RVP" ]; then
    log "ERROR: no .rvp produced by slim build"
    exit 1
fi

OUT="/output/viewonce-standalone.rvp"
mkdir -p /output
cp "$RVP" "$OUT"
log "Done → $OUT ($(du -h "$OUT" | cut -f1))"
