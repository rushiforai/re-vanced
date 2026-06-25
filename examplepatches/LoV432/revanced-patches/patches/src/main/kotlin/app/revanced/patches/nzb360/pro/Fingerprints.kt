package app.revanced.patches.nzb360.pro

import app.revanced.patcher.definingClass
import app.revanced.patcher.gettingFirstMethodDeclaratively
import app.revanced.patcher.name
import app.revanced.patcher.returnType
import app.revanced.patcher.accessFlags
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags

val BytecodePatchContext.isUnlockedMethod by gettingFirstMethodDeclaratively {
    name("isUnlocked")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("Z")
}