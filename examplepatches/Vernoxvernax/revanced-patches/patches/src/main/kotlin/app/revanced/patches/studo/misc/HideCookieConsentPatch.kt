package app.revanced.patches.studo.misc

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch

@Suppress("unused")
val hideCookieConsentPatch = bytecodePatch(
    name = "Hide Cookie Consent Dialog",
    description = "Hides the cookie consent window that advertises Studo Pro.",
) {
    compatibleWith("com.moshbit.studo"("4.72.2"))

    apply {
        webFragmentOpenAppPatch.addInstructions(0, "return-void")
    }
}
