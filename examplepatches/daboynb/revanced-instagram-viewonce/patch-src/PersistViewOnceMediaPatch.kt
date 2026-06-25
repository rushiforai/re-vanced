package app.revanced.patches.instagram.direct.viewonce

import app.revanced.patcher.classDef
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.instagram.misc.extension.sharedExtensionPatch
import app.revanced.util.Utils.trimIndentMultiline
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

private const val EXTENSION_CLASS =
    "Lapp/revanced/extension/instagram/direct/viewonce/PersistViewOnceMediaPatch;"

@Suppress("unused")
val persistViewOnceMediaPatch = bytecodePatch(
    name = "Save view-once media",
    description = """
        Intercepts the view-once media viewer callback and saves photos/videos
        to Pictures/InstagramViewOnce before they expire.
    """.trimIndentMultiline(),
    use = false,
) {
    dependsOn(sharedExtensionPatch)
    compatibleWith("com.instagram.android")

    apply {
        // ── Hook 1: View-once success callback ──

        val callbackClassType = photoErrorCallbackMethod.classDef.type
        val callbackClass = classes.first { it.type == callbackClassType }
        val errorMethodName = photoErrorCallbackMethod.name

        val successMethod = callbackClass.methods.first { method ->
            method.name != errorMethodName &&
                method.returnType == "V" &&
                method.parameters.isNotEmpty() &&
                method.implementation?.instructions?.any { insn ->
                    insn is ReferenceInstruction &&
                        insn.reference.toString().contains("DirectVisualMessageViewerController")
                } == true
        }

        val mutableCallbackClass = proxy(callbackClass).mutableClass
        val mutableSuccessMethod = mutableCallbackClass.methods.first { m ->
            m.name == successMethod.name &&
                m.returnType == successMethod.returnType &&
                m.parameters.map { it.type } == successMethod.parameters.map { it.type }
        }

        val regCount = mutableSuccessMethod.implementation!!.registerCount
        val paramWordCount = 1 + mutableSuccessMethod.parameters.sumOf {
            if (it.type == "J" || it.type == "D") 2 else 1
        }
        val thisReg = regCount - paramWordCount
        val paramReg = thisReg + 1

        mutableSuccessMethod.addInstructions(
            0,
            """
                invoke-static {v$thisReg, v$paramReg}, $EXTENSION_CLASS->onViewOnceMediaLoaded(Ljava/lang/Object;Ljava/lang/Object;)V
            """,
        )

    }
}
