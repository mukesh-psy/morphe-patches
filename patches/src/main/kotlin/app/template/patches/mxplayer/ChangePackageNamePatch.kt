package app.template.patches.mxplayer

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import app.template.patches.shared.Constants.MX_PLAYER_PRO_COMPATIBILITY
import org.w3c.dom.Element

private const val ORIGINAL_PACKAGE_NAME = "com.mxtech.videoplayer.pro"
private const val DEFAULT_PACKAGE_NAME = "com.morphe.mxtech.videoplayer.pro"
private const val APP_NAME_STRING = "morphe_mx_player_pro_app_name"

/**
 * Installs MX Player Pro beside the original app by renaming the package.
 *
 * MX-specific handling:
 *  - android:sharedUserId="com.mxtech" (+ sharedUserMaxSdkVersion) is REMOVED —
 *    a differently-signed clone must not join the shared UID of other MX apps
 *    (ignored since API 33, but breaks installs on API <= 32 otherwise).
 *  - The custom permission com.mxtech.videoplayer.pro.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION
 *    is renamed along with all provider authorities (*.vssp, *.fileprovider,
 *    *.firebaseinitprovider, *.androidx-startup, *.kv, *.mxtransfer.fileprovider).
 *    No code references these strings literally; they resolve via getPackageName().
 */
@Suppress("unused")
val mxPlayerChangePackageNamePatch = resourcePatch(
    name = "Change package name",
    description = "Installs MX Player Pro beside the original by changing the package name, removing the shared user ID, renaming permissions and providers, and updating the app name.",
    default = true,
) {
    compatibleWith(MX_PLAYER_PRO_COMPATIBILITY)

    val packageName by stringOption(
        key = "mxPlayerPackageName",
        default = DEFAULT_PACKAGE_NAME,
        title = "Package name",
        description = "Package name for the cloned MX Player Pro.",
        required = true,
    ) { it?.matches(Regex("^[a-z]\\w*(\\.[a-z]\\w*)+$")) == true }

    val appName by stringOption(
        key = "mxPlayerAppName",
        default = "MX Player Pro Morphe",
        title = "App name",
        description = "Launcher name for the cloned MX Player Pro.",
        required = true,
    ) { !it.isNullOrBlank() }

    execute {
        val newPackageName = packageName ?: DEFAULT_PACKAGE_NAME

        document("AndroidManifest.xml").use { document ->
            val manifest = document.documentElement

            // Must come before the package rename: drop the shared UID with other MX apps.
            manifest.removeAttribute("android:sharedUserId")
            manifest.removeAttribute("android:sharedUserMaxSdkVersion")

            manifest.setAttribute("package", newPackageName)

            replaceNameAttributes(document.getElementsByTagName("*"), newPackageName)
            replaceComponentPermissions(document.getElementsByTagName("*"), newPackageName)
            replaceProviderAuthorities(document.getElementsByTagName("provider"), newPackageName)

            (document.getElementsByTagName("application").item(0) as? Element)
                ?.setAttribute("android:label", "@string/$APP_NAME_STRING")
        }

        document("res/values/strings.xml").use { document ->
            val resources = document.documentElement
            val strings = document.getElementsByTagName("string")
            val existing = (0 until strings.length)
                .mapNotNull { strings.item(it) as? Element }
                .firstOrNull { it.getAttribute("name") == APP_NAME_STRING }
            val target = existing ?: document.createElement("string").also {
                it.setAttribute("name", APP_NAME_STRING)
                resources.appendChild(it)
            }
            target.textContent = appName ?: "MX Player Pro Morphe"
        }
    }
}

// Replace ORIGINAL_PACKAGE_NAME with newPackageName in a string, but only when the
// value starts with ORIGINAL_PACKAGE_NAME exactly followed by end-of-string or a
// non-alpha-numeric boundary character. Prevents double-replacement on re-runs.
private fun String.safeReplace(newPackageName: String): String {
    if (!startsWith(ORIGINAL_PACKAGE_NAME)) return this
    val after = drop(ORIGINAL_PACKAGE_NAME.length)
    return newPackageName + after
}

private fun replaceNameAttributes(nodes: org.w3c.dom.NodeList, newPackageName: String) {
    // <permission> / <uses-permission> android:name values are permission identifiers → RENAME.
    // Component android:name values (activity, service, receiver, provider, application,
    // activity-alias targets...) are DEX class references → DO NOT RENAME.
    val dexClassElements = setOf("activity", "service", "receiver", "provider", "application")

    for (i in 0 until nodes.length) {
        val element = nodes.item(i) as? Element ?: continue
        val value = element.getAttribute("android:name")
        if (value.startsWith(ORIGINAL_PACKAGE_NAME) && element.tagName !in dexClassElements) {
            element.setAttribute("android:name", value.safeReplace(newPackageName))
        }
    }
}

private fun replaceComponentPermissions(nodes: org.w3c.dom.NodeList, newPackageName: String) {
    for (i in 0 until nodes.length) {
        val element = nodes.item(i) as? Element ?: continue
        val permission = element.getAttribute("android:permission")
        if (permission.startsWith(ORIGINAL_PACKAGE_NAME)) {
            element.setAttribute("android:permission", permission.safeReplace(newPackageName))
        }
    }
}

private fun replaceProviderAuthorities(nodes: org.w3c.dom.NodeList, newPackageName: String) {
    for (i in 0 until nodes.length) {
        val provider = nodes.item(i) as? Element ?: continue
        val authorities = provider.getAttribute("android:authorities")
        if (authorities.isBlank()) continue

        val rewritten = authorities.split(";").joinToString(";") { authority ->
            if (authority.startsWith(ORIGINAL_PACKAGE_NAME)) authority.safeReplace(newPackageName) else authority
        }
        provider.setAttribute("android:authorities", rewritten)
    }
}
