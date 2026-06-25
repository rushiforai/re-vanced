package app.revanced.patches.studo.misc

import app.revanced.patcher.definingClass
import app.revanced.patcher.gettingFirstMethodDeclaratively
import app.revanced.patcher.name
import app.revanced.patcher.parameterTypes
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patcher.returnType

internal val BytecodePatchContext.longTextBindPatch by gettingFirstMethodDeclaratively {
    definingClass("Lcom/moshbit/studo/home/news/NewsAdapter\$LongTextAboveImageVH;")
    name("bind")
    returnType("V")
    parameterTypes("Lcom/moshbit/studo/db/OrganizationPostNewsFeedItem;")
}

internal val BytecodePatchContext.longTextBindWhitelistPatch by gettingFirstMethodDeclaratively {
    definingClass("Lcom/moshbit/studo/home/news/NewsAdapter\$LongTextAboveImageVH;")
    name("bind")
    returnType("V")
    parameterTypes("Lcom/moshbit/studo/db/OrganizationPostNewsFeedItem;")
}

internal val BytecodePatchContext.webFragmentOpenAppPatch by gettingFirstMethodDeclaratively {
    definingClass("Lcom/moshbit/studo/home/WebFragment\$Companion;")
    name("open")
    returnType("V")
    parameterTypes(
        "Lcom/moshbit/studo/app/App;",
        "Ljava/lang/String;",
        "Z",
        "Z",
        "Lcom/moshbit/studo/util/FragmentHostActivity\$Params\$Browser\$ClickSource;",
        "Z"
    )
}
