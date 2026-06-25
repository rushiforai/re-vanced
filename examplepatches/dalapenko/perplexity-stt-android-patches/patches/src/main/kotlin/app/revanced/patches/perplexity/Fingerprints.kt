package app.revanced.patches.perplexity

import app.revanced.patcher.composingFirstMethod
import app.revanced.patcher.returnType
import app.revanced.patcher.opcodes
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.Opcode

/**
 * Fingerprint to locate the Soniox realtime configuration class (ai.perplexity...SonioxRealtimeConfig).
 * We match its toString() method by detecting the unique auto-generated data class string prefix:
 * "SonioxRealtimeConfig(enabled=".
 * This uniquely distinguishes it from its serializer class (which also contains Soniox URL strings).
 */
internal val BytecodePatchContext.sonioxConfigToStringMatch by composingFirstMethod(
    "SonioxRealtimeConfig(enabled="
) {
    returnType("Ljava/lang/String;")
    opcodes(Opcode.CONST_STRING)
}
