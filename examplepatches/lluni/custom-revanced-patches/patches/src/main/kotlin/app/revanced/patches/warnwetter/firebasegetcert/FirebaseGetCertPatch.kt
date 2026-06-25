package app.revanced.patches.warnwetter.firebasegetcert

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch

val firebaseGetCertPatch = bytecodePatch(
    description = "Spoofs the X-Android-Cert header.",
) {
    compatibleWith("de.dwd.warnapp")

    execute {
        // the SHA-1 APK certificate fingerprint (see APKMirror) is "0799DDF0414D3B3475E88743C91C0676793ED450"
        getMessagingCertFingerprint.method.addInstructions(
            0,
            """
                const-string v0, "0799DDF0414D3B3475E88743C91C0676793ED450"
                return-object v0
            """,
        )
    }
}