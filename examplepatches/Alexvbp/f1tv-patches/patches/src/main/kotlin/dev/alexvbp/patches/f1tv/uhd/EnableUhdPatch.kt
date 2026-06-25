package dev.alexvbp.patches.f1tv.uhd

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch

@Suppress("unused")
val enableUhdPatch = bytecodePatch(
    name = "Enable UHD/4K",
    description = "Enables UHD/4K streaming on all devices by bypassing the device whitelist check.",
) {
    compatibleWith("com.formulaone.production")

    execute {
        // Replace the method body to always return Pair(true, null)
        validateIsUhdSupportedDeviceFingerprint.method.addInstructions(
            0,
            """
                new-instance v0, Lkotlin/Pair;
                const/4 v1, 0x1
                invoke-static {v1}, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;
                move-result-object v1
                const/4 p1, 0x0
                invoke-direct {v0, v1, p1}, Lkotlin/Pair;-><init>(Ljava/lang/Object;Ljava/lang/Object;)V
                return-object v0
            """,
        )
    }
}
