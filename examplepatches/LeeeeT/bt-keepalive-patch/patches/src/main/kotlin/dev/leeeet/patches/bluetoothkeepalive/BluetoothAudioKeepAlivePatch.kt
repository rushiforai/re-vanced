package dev.leeeet.patches.bluetoothkeepalive

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.resourcePatch
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList

private const val PROVIDER_CLASS_DESCRIPTOR =
    "dev.leeeet.extension.bluetoothkeepalive.BluetoothKeepAliveProvider"

@Suppress("unused")
val bluetoothAudioKeepAlivePatch = resourcePatch(
    name = "Bluetooth audio keep alive",
    description = "Plays an inaudible looping PCM track for the lifetime of the app to keep " +
        "connected Bluetooth headphones in active audio mode, avoiding the first 100-300 ms " +
        "of audio being cut off when a clip starts.",
    use = true,
) {
    dependsOn(
        bytecodePatch {
            extendWith("extensions/bluetooth-keep-alive.rve")
        },
    )

    apply {
        document("AndroidManifest.xml").use { document ->
            val providers = document.getElementsByTagName("provider")
            if (providers.iterate().any {
                    it.attributes?.getNamedItem("android:name")?.nodeValue == PROVIDER_CLASS_DESCRIPTOR
                }
            ) {
                return@apply
            }

            val manifest = document.getElementsByTagName("manifest").item(0) as Element
            val packageName = manifest.getAttribute("package")
                .ifEmpty { error("AndroidManifest.xml is missing a package attribute") }

            val application = document.getElementsByTagName("application").item(0) as Element
            val provider = document.createElement("provider").apply {
                setAttribute("android:name", PROVIDER_CLASS_DESCRIPTOR)
                setAttribute("android:authorities", "$packageName.$PROVIDER_CLASS_DESCRIPTOR")
                setAttribute("android:exported", "false")
            }
            application.appendChild(provider)
        }
    }
}

private fun NodeList.iterate(): Sequence<Node> = sequence {
    for (i in 0 until length) yield(item(i))
}
