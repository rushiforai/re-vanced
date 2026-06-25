package app.revanced.patches.perplexity

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.extensions.instructions
import app.revanced.patcher.extensions.addInstructions
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction22c
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

@Suppress("unused")
val forceNativeSttPatch = bytecodePatch(
    name = "Force Native Speech-to-Text",
    description = "Forces the application to use the native Google Speech-to-Text engine instead of the third-party Soniox engine by disabling the Soniox realtime configuration."
) {
    compatibleWith("ai.perplexity.app.android"("2.90.0"))

    apply {
        // 1. Resolve target config class using the fingerprint
        val configMatch = sonioxConfigToStringMatch
        val configClassDef = configMatch.classDef
        
        // Retrieve a mutable copy of the class definition to apply changes
        val mutableConfigClass = classDefs.getOrReplaceMutable(configClassDef)

        // 2. Identify the 'enabled' boolean field dynamically
        // Since it is the only boolean field (type "Z") in the config class, we can find it without hardcoding
        val enabledField = mutableConfigClass.fields.firstOrNull { it.type == "Z" }
            ?: throw Exception("Could not find boolean enabled field in Soniox config class")
        val enabledFieldName = enabledField.name

        // 3. Patch all constructors (<init>) to initialize the enabled field to false
        mutableConfigClass.methods.filter { it.name == "<init>" }.forEach { constructor ->
            val iputIndex = constructor.instructions.indexOfFirst { instruction ->
                instruction.opcode == Opcode.IPUT_BOOLEAN && 
                (instruction as Instruction22c).reference.let { ref ->
                    ref is FieldReference && ref.name == enabledFieldName
                }
            }
            if (iputIndex != -1) {
                val iput = constructor.instructions[iputIndex] as Instruction22c
                val sourceRegister = iput.registerA
                // Inject 'const/4 vX, 0x0' to clear the register before it gets written to the field
                constructor.addInstructions(iputIndex, "const/4 v$sourceRegister, 0x0")
            }
        }

        // 4. Locate and patch the getter method (returns boolean Z, has no parameters, is not a constructor)
        val getterMethod = mutableConfigClass.methods.firstOrNull { 
            it.returnType == "Z" && it.parameterTypes.isEmpty() && it.name != "<init>"
        } ?: throw Exception("Could not find boolean getter method in Soniox config class")

        // Force the getter method to return false early by overriding its instructions
        getterMethod.addInstructions(
            0,
            """
            const/4 v0, 0x0
            return v0
            """
        )
    }
}
