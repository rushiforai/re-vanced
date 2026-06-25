package app.revanced.patches.studo.ads

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch

@Suppress("unused")
val hideAdsPatch = bytecodePatch(
    name = "Hide Ads",
    description = "Removes Advertisements from the app interface.",
) {
    compatibleWith("com.moshbit.studo"("4.72.2"))

    apply {
        showAdPatch.addInstructions(0, "return-void")
    }
}
