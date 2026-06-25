package app.revanced.patches.macrofactor.misc.subscription

import app.revanced.patcher.fingerprint

internal val spoofAndroidCertFingerprint = fingerprint {
    custom { methodDef, classDef ->
        methodDef.name == "getPackageCertificateHashBytes" && classDef.endsWith("Lcom/google/android/gms/common/util/AndroidUtilsLight;")
    }
}

internal val customerInfoFactoryBuildCustomerInfoFingerprint = fingerprint {
    strings("subscriber")
    custom { method, classDef ->
        classDef.endsWith("/CustomerInfoFactory;") && method.name == "buildCustomerInfo"
    }
}
