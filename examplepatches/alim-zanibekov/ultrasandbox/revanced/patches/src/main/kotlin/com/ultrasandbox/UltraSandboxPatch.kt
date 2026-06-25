@file:Suppress("unused", "DEPRECATION")

package com.ultrasandbox

import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction3rc
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference

// Sandbox modules
enum class Sandbox(val smaliClass: String) {
    Network("Lcom/ultrasandbox/NetworkSandbox;"),
    Package("Lcom/ultrasandbox/PackageSandbox;"),
    Wireless("Lcom/ultrasandbox/WifiBluetoothSandbox;"),
    Device("Lcom/ultrasandbox/DeviceSandbox;"),
    Content("Lcom/ultrasandbox/ContentSandbox;"),
    FileSystem("Lcom/ultrasandbox/FileSystemSandbox;"),
    Process("Lcom/ultrasandbox/ProcessSandbox;"),
    Location("Lcom/ultrasandbox/LocationSandbox;"),
}

// Redirect.instance(class, method, desc, sandbox) - same method name
// Redirect.instance(class, method, desc, sandbox, "alt") - different target method name
// Redirect.static(class, method, desc, sandbox) - invoke-static calls
data class Redirect(
    val originalClass: String,
    val originalMethod: String,
    val originalDesc: String,
    val targetSandbox: Sandbox,
    val targetMethod: String,
    val targetDesc: String,
    val isOriginallyStatic: Boolean = false,
) {
    companion object {
        fun instance(
            cls: String,
            method: String,
            desc: String,
            sandbox: Sandbox,
            targetName: String = method,
        ) = Redirect(
            cls, method, desc, sandbox, targetName,
            targetDesc = "($cls${desc.substringAfter("(")}",
        )

        fun static(
            cls: String,
            method: String,
            desc: String,
            sandbox: Sandbox,
            targetName: String = method,
        ) = Redirect(
            cls,
            method,
            desc,
            sandbox,
            targetName,
            targetDesc = desc,
            isOriginallyStatic = true,
        )
    }
}

val REDIRECTS = listOf(
    // Network
    Redirect.instance(
        "Landroid/net/NetworkCapabilities;", "hasTransport", "(I)Z", Sandbox.Network
    ),
    Redirect.instance(
        "Landroid/net/NetworkCapabilities;", "hasCapability", "(I)Z", Sandbox.Network
    ),
    Redirect.instance(
        "Landroid/net/NetworkCapabilities;", "toString", "()Ljava/lang/String;", Sandbox.Network,
        "capsToString"
    ),
    Redirect.static(
        "Ljava/net/NetworkInterface;", "getNetworkInterfaces", "()Ljava/util/Enumeration;",
        Sandbox.Network
    ),
    Redirect.instance("Ljava/net/NetworkInterface;", "getMTU", "()I", Sandbox.Network),
    Redirect.instance(
        "Ljava/net/Socket;", "connect", "(Ljava/net/SocketAddress;I)V", Sandbox.Network,
        "socketConnect"
    ),
    Redirect.instance(
        "Ljava/net/Socket;", "connect", "(Ljava/net/SocketAddress;)V", Sandbox.Network,
        "socketConnectNoTimeout"
    ),
    Redirect.instance(
        "Landroid/net/LinkProperties;", "getDnsServers", "()Ljava/util/List;", Sandbox.Network
    ),
    Redirect.instance(
        "Landroid/net/LinkProperties;", "getRoutes", "()Ljava/util/List;", Sandbox.Network
    ),
    Redirect.static(
        "Ljava/lang/System;", "getProperty", "(Ljava/lang/String;)Ljava/lang/String;",
        Sandbox.Network
    ),
    Redirect.static(
        "Ljava/lang/System;", "getProperty",
        "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
        Sandbox.Network, "getPropertyDefault"
    ),

    // Packages
    Redirect.instance(
        "Landroid/content/pm/PackageManager;",
        "getPackageInfo", "(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;",
        Sandbox.Package
    ),
    Redirect.instance(
        "Landroid/content/pm/PackageManager;", "getInstalledPackages", "(I)Ljava/util/List;",
        Sandbox.Package
    ),
    Redirect.instance(
        "Landroid/content/pm/PackageManager;", "getInstalledApplications", "(I)Ljava/util/List;",
        Sandbox.Package
    ),
    Redirect.instance(
        "Landroid/content/pm/PackageManager;", "queryIntentActivities",
        "(Landroid/content/Intent;I)Ljava/util/List;", Sandbox.Package
    ),
    Redirect.instance(
        "Landroid/content/pm/PackageManager;",
        "queryIntentServices", "(Landroid/content/Intent;I)Ljava/util/List;", Sandbox.Package
    ),
    Redirect.instance(
        "Landroid/content/pm/PackageManager;", "getPackageInfo",
        "(Ljava/lang/String;Landroid/content/pm/PackageManager\$PackageInfoFlags;)Landroid/content/pm/PackageInfo;",
        Sandbox.Package
    ),
    Redirect.instance(
        "Landroid/content/pm/PackageManager;", "getInstalledPackages",
        "(Landroid/content/pm/PackageManager\$PackageInfoFlags;)Ljava/util/List;", Sandbox.Package
    ),
    Redirect.instance(
        "Landroid/content/pm/PackageManager;", "getInstalledApplications",
        "(Landroid/content/pm/PackageManager\$ApplicationInfoFlags;)Ljava/util/List;", Sandbox.Package
    ),
    Redirect.instance(
        "Landroid/content/pm/PackageManager;", "queryIntentActivities",
        "(Landroid/content/Intent;Landroid/content/pm/PackageManager\$ResolveInfoFlags;)Ljava/util/List;",
        Sandbox.Package
    ),
    Redirect.instance(
        "Landroid/content/pm/PackageManager;", "queryIntentServices",
        "(Landroid/content/Intent;Landroid/content/pm/PackageManager\$ResolveInfoFlags;)Ljava/util/List;",
        Sandbox.Package
    ),

    // WiFi / Bluetooth
    Redirect.instance(
        "Landroid/net/wifi/WifiManager;", "getScanResults", "()Ljava/util/List;", Sandbox.Wireless
    ),
    Redirect.instance(
        "Landroid/net/wifi/WifiManager;", "getConfiguredNetworks", "()Ljava/util/List;",
        Sandbox.Wireless
    ),
    Redirect.instance(
        "Landroid/net/wifi/WifiInfo;", "getSSID", "()Ljava/lang/String;", Sandbox.Wireless
    ),
    Redirect.instance(
        "Landroid/net/wifi/WifiInfo;", "getBSSID", "()Ljava/lang/String;", Sandbox.Wireless
    ),
    Redirect.instance(
        "Landroid/net/wifi/WifiInfo;", "getMacAddress", "()Ljava/lang/String;", Sandbox.Wireless
    ),
    Redirect.instance(
        "Landroid/bluetooth/BluetoothAdapter;", "getBondedDevices", "()Ljava/util/Set;",
        Sandbox.Wireless
    ),

    // Device
    Redirect.static(
        "Landroid/provider/Settings\$Secure;", "getString",
        "(Landroid/content/ContentResolver;Ljava/lang/String;)Ljava/lang/String;",
        Sandbox.Device, "settingsGetString"
    ),
    Redirect.instance(
        "Landroid/telephony/TelephonyManager;", "getDeviceId", "()Ljava/lang/String;",
        Sandbox.Device
    ),
    Redirect.instance(
        "Landroid/telephony/TelephonyManager;", "getImei", "()Ljava/lang/String;", Sandbox.Device
    ),
    Redirect.instance(
        "Landroid/telephony/TelephonyManager;", "getSubscriberId", "()Ljava/lang/String;",
        Sandbox.Device
    ),
    Redirect.instance(
        "Landroid/telephony/TelephonyManager;", "getSimSerialNumber", "()Ljava/lang/String;",
        Sandbox.Device
    ),
    Redirect.instance(
        "Landroid/telephony/TelephonyManager;", "getLine1Number", "()Ljava/lang/String;",
        Sandbox.Device
    ),
    Redirect.instance(
        "Landroid/accounts/AccountManager;", "getAccounts", "()[Landroid/accounts/Account;",
        Sandbox.Device
    ),
    Redirect.instance(
        "Landroid/content/ClipboardManager;", "getPrimaryClip", "()Landroid/content/ClipData;",
        Sandbox.Device
    ),
    Redirect.instance(
        "Landroid/content/ClipboardManager;", "hasPrimaryClip", "()Z", Sandbox.Device
    ),

    // Content providers
    Redirect.instance(
        "Landroid/content/ContentResolver;", "query",
        "(Landroid/net/Uri;[Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;Ljava/lang/String;)Landroid/database/Cursor;",
        Sandbox.Content
    ),

    // FS
    Redirect.instance("Ljava/io/File;", "exists", "()Z", Sandbox.FileSystem, "fileExists"),
    Redirect.instance("Ljava/io/File;", "canRead", "()Z", Sandbox.FileSystem, "fileCanRead"),

    // Process
    Redirect.instance(
        "Ljava/lang/Runtime;", "exec", "([Ljava/lang/String;)Ljava/lang/Process;", Sandbox.Process,
        "execArray"
    ),
    Redirect.instance(
        "Ljava/lang/Runtime;", "exec", "(Ljava/lang/String;)Ljava/lang/Process;", Sandbox.Process,
        "execString"
    ),
    Redirect.static("Landroid/os/Debug;", "isDebuggerConnected", "()Z", Sandbox.Process),
    Redirect.instance(
        "Ljava/lang/ProcessBuilder;", "start", "()Ljava/lang/Process;", Sandbox.Process,
        "processBuilderStart"
    ),

    // Location
    Redirect.instance(
        "Landroid/telephony/TelephonyManager;",
        "getCellLocation", "()Landroid/telephony/CellLocation;", Sandbox.Location
    ),
    Redirect.instance(
        "Landroid/telephony/TelephonyManager;", "getAllCellInfo", "()Ljava/util/List;",
        Sandbox.Location
    ),
)

val ANDROID_ID_PLACEHOLDER = "__ULTRASANDBOX_ANDROID_ID_PLACEHOLDER__"

data class MethodSignature(
    val definingClass: String,
    val name: String,
    val descriptor: String,
)

data class CallSiteMatch(
    val classDef: com.android.tools.smali.dexlib2.iface.ClassDef,
    val methodName: String,
    val methodParams: List<CharSequence>,
    val instructionIndex: Int,
    val redirect: Redirect,
)

val INVOKE_OPCODES = setOf(
    Opcode.INVOKE_VIRTUAL,
    Opcode.INVOKE_STATIC,
    Opcode.INVOKE_INTERFACE,
    Opcode.INVOKE_VIRTUAL_RANGE,
    Opcode.INVOKE_STATIC_RANGE,
    Opcode.INVOKE_INTERFACE_RANGE,
)

val STATIC_OPCODES = setOf(
    Opcode.INVOKE_STATIC,
    Opcode.INVOKE_STATIC_RANGE,
)

fun Instruction35c.registerString(): String =
    intArrayOf(registerC, registerD, registerE, registerF, registerG)
        .take(registerCount)
        .joinToString(", ") { "v$it" }

val ultrasandboxPatch = bytecodePatch(
    name = "UltraSandbox",
    description = "Patched app sees a fresh stock phone",
) {
    extendWith("ultrasandbox.rve")
    apply {
        val randomAndroidId = java.util.UUID.randomUUID().toString().replace("-", "").take(16)

        val redirectLookup = REDIRECTS.associateBy {
            MethodSignature(it.originalClass, it.originalMethod, it.originalDesc)
        }

        val callSiteMatches = mutableListOf<CallSiteMatch>()

        for (classDef in classDefs.toList()) {
            val isSandboxClass =
                classDef.type.contains("/ultrasandbox/") && classDef.type.contains("Sandbox")

            for (method in classDef.methods) {
                val instructions = method.implementation?.instructions?.toList() ?: continue
                for ((index, instruction) in instructions.withIndex()) {

                    // Bake our generated android ID
                    if (classDef.type.contains("DeviceSandbox") && instruction.opcode == Opcode.CONST_STRING) {
                        val str =
                            (instruction as? ReferenceInstruction)?.reference as? StringReference
                        if (str?.string == ANDROID_ID_PLACEHOLDER) {
                            val reg = (instruction as OneRegisterInstruction).registerA
                            val mm = classDefs.getOrReplaceMutable(classDef).methods.first {
                                it.name == method.name && it.parameterTypes == method.parameterTypes
                            }
                            mm.replaceInstruction(index, "const-string v$reg, \"$randomAndroidId\"")
                        }
                        continue
                    }

                    if (isSandboxClass) continue
                    if (instruction.opcode !in INVOKE_OPCODES) continue

                    val ref = (instruction as? ReferenceInstruction)
                        ?.reference as? MethodReference ?: continue
                    val descriptor =
                        "(" + ref.parameterTypes.joinToString("") + ")" + ref.returnType
                    val signature = MethodSignature(ref.definingClass, ref.name, descriptor)
                    val redirect = redirectLookup[signature] ?: continue

                    if (redirect.isOriginallyStatic != (instruction.opcode in STATIC_OPCODES)) continue

                    callSiteMatches += CallSiteMatch(
                        classDef, method.name, method.parameterTypes,
                        index, redirect,
                    )
                }
            }
        }

        val grouped = callSiteMatches.groupBy { Triple(it.classDef, it.methodName, it.methodParams) }
        for ((key, matches) in grouped) {
            val (classDef, methodName, params) = key

            val mutableMethod = classDefs.getOrReplaceMutable(classDef).methods.first {
                it.name == methodName && it.parameterTypes == params
            }

            for (match in matches.sortedByDescending { it.instructionIndex }) {
                val instructions = mutableMethod.implementation?.instructions?.toList() ?: continue
                if (match.instructionIndex >= instructions.size) continue
                val inst = instructions[match.instructionIndex]
                val r = match.redirect
                val target = "${r.targetSandbox.smaliClass}->${r.targetMethod}${r.targetDesc}"

                when (inst) {
                    is Instruction35c ->
                        mutableMethod.replaceInstruction(
                            match.instructionIndex,
                            "invoke-static {${inst.registerString()}}, $target",
                        )

                    is Instruction3rc ->
                        mutableMethod.replaceInstruction(
                            match.instructionIndex,
                            "invoke-static/range {v${inst.startRegister} .. v${inst.startRegister + inst.registerCount - 1}}, $target",
                        )
                }
            }
        }

    }
}
