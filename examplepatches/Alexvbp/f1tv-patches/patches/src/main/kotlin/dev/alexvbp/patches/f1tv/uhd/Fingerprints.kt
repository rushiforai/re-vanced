package dev.alexvbp.patches.f1tv.uhd

import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

/**
 * Matches DeviceSupportImpl.validateIsUhdSupportedDevice(DeviceCapabilities) -> Pair<Boolean, String>
 *
 * This method checks if the current device's Build.BRAND and Build.PRODUCT
 * are in the uhdSupport whitelist from the config JSON.
 * If not matched, it returns Pair(false, "Device with brand X and product Y is not supported...").
 */
internal val validateIsUhdSupportedDeviceFingerprint = fingerprint {
    accessFlags(AccessFlags.PRIVATE, AccessFlags.FINAL)
    returns("Lkotlin/Pair;")
    parameters("Lcom/avs/f1/ui/tiledmediaplayer/DeviceCapabilities;")
    strings(
        "` is not supported for UHD playback. Supported devices: ",
    )
}
