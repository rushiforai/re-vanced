package app.revanced.patches.debridstream.misc.pro

import app.revanced.patcher.fingerprint

internal val customerInfoFactoryBuildCustomerInfoFingerprint = fingerprint {
    strings("subscriber")
    custom { method, classDef ->
        classDef.endsWith("/CustomerInfoFactory;") && method.name == "buildCustomerInfo"
    }
}
