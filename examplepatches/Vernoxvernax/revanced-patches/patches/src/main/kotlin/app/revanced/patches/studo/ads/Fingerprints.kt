package app.revanced.patches.studo.ads

import app.revanced.patcher.accessFlags
import app.revanced.patcher.definingClass
import app.revanced.patcher.gettingFirstMethodDeclaratively
import app.revanced.patcher.name
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patcher.returnType
import com.android.tools.smali.dexlib2.AccessFlags

internal val BytecodePatchContext.showAdPatch by gettingFirstMethodDeclaratively {
    definingClass("Lcom/moshbit/studo/util/mb/ad/WebViewAdProvider;")
    name("showAd")
    accessFlags(AccessFlags.PUBLIC)
    returnType("V")
}
