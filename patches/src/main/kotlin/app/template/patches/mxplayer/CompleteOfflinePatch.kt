package app.template.patches.mxplayer

import app.morphe.patcher.patch.resourcePatch
import app.template.patches.shared.Constants.MX_PLAYER_PRO_COMPATIBILITY
import app.template.patches.shared.universal.universalDisableClipboardAccessPatch
import org.w3c.dom.Element

/**
 * Hard offline mode: removes android.permission.INTERNET so the OS denies every
 * socket connection (Java and native) for the whole process. Streaming, subtitle
 * downloads, cloud drives and all telemetry become unreachable.
 *
 * Clipboard access is force-disabled as well (depends on the universal patch),
 * so no data can leave or enter the app while it is network-isolated.
 */
@Suppress("unused")
val mxPlayerCompleteOfflinePatch = resourcePatch(
    name = "Complete offline mode",
    description = "Removes the INTERNET permission so the app cannot make any network connection, and blocks clipboard access.",
    default = true,
) {
    compatibleWith(MX_PLAYER_PRO_COMPATIBILITY)
    dependsOn(universalDisableClipboardAccessPatch)

    execute {
        document("AndroidManifest.xml").use { document ->
            val nodes = document.getElementsByTagName("uses-permission")
            for (i in nodes.length - 1 downTo 0) {
                val element = nodes.item(i) as? Element ?: continue
                if (element.getAttribute("android:name") == "android.permission.INTERNET") {
                    element.parentNode.removeChild(element)
                }
            }
        }
    }
}
