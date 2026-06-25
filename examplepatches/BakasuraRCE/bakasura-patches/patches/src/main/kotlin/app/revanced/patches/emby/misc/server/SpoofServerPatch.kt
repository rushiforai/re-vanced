package app.revanced.patches.emby.misc.server

import app.revanced.patcher.extensions.replaceInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.rawResourcePatch
import app.revanced.patcher.patch.stringOption
import app.revanced.util.getReference
import com.android.tools.smali.dexlib2.ReferenceType
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import app.revanced.util.forEachInstructionAsSequence

private const val ORIGINAL_HOST = "mb3admin.com"
private const val DEFAULT_REPLACEMENT_HOST = "xexample.com"

@Suppress("unused")
val spoofServerPatch = bytecodePatch(
    name = "Spoof server host",
    description = "Replaces the Emby server validation host with a custom one to bypass premiere checks.",
) {
    compatibleWith("com.mb.android")

    val replacementHost by stringOption(
        default = DEFAULT_REPLACEMENT_HOST,
        name = "Server host",
        description = "The replacement host for Emby server validation.",
        required = true,
    )

    dependsOn(
        // Replace the host in asset files.
        rawResourcePatch {
            apply {
                val assetFiles = listOf(
                    "assets/www/embypremiere/embypremiere.js",
                    "assets/www/modules/emby-apiclient/connectionmanager.js",
                    "assets/www/plugins/addplugin.html",
                    "assets/www/plugins/addpluginpage.js",
                )

                assetFiles.forEach { filePath ->
                    val targetFile = get(filePath, true)
                    if (targetFile.exists()) {
                        val content = targetFile.readText()
                        targetFile.writeText(content.replace(ORIGINAL_HOST, replacementHost!!))
                    }
                }
            }
        },
    )

    // Replace the host in all bytecode string references.
    apply {
        forEachInstructionAsSequence(
            match = match@{ _: ClassDef, _: Method, instruction: Instruction, instructionIndex: Int ->
                if (instruction.opcode.referenceType != ReferenceType.STRING) return@match null

                val stringReference = instruction.getReference<StringReference>()!!.string
                if (ORIGINAL_HOST !in stringReference) return@match null

                Triple(instructionIndex, instruction as OneRegisterInstruction, stringReference)
            },
            transform = { mutableMethod, entry ->
                val (instructionIndex, instruction, stringReference) = entry

                val newString = stringReference.replace(ORIGINAL_HOST, replacementHost!!)
                mutableMethod.replaceInstruction(
                    instructionIndex,
                    "${instruction.opcode.name} v${instruction.registerA}, \"$newString\"",
                )
            },
        )
    }
}
