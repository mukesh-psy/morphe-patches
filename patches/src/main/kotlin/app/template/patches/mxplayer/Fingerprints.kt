package app.template.patches.mxplayer

import app.morphe.patcher.Fingerprint

/**
 * MX Player Pro 1.93.4 license enforcement (all in root-package R8 class gR, jadx: C2480gR "Licensor").
 *
 * Chain:
 *   App.m(activity) / ActivityMediaList → gR.a(ContextWrapper)V  ← starts verification
 *     → fR "LicenseManager" (jadx C2344fR) rate-limits via native timestamps (Apps.i(1)/i(2))
 *     → VF "GooglePlayStoreVerifier" + SF "GoogleLicenseChecker"  (Google Play LVL,
 *       demands the purchasing Google account; CHECK_LICENSE permission)
 *     → GV "MXVerifier" fallback (com.mxtech.lproxy companion + account emails)
 *   failure → gR.c(sK, II)V  ← finishes all activities + "Unable to validate the purchase" dialog
 *   query   → gR.b()Z        ← global isLicensed gate consumed by App.J()
 *
 * Obfuscated names verified against the DEX string pool of 1.93.4 (2001002584);
 * compatibility is pinned to that exact version.
 */

/** gR.a(ContextWrapper)V — verification entry point, invoked on every activity creation. */
internal val LicensorVerifyFingerprint = Fingerprint(
    definingClass = "LgR;",
    name = "a",
    returnType = "V",
    parameters = listOf("Landroid/content/ContextWrapper;"),
)

/** gR.b()Z — global isLicensed query. */
internal val LicensorIsLicensedFingerprint = Fingerprint(
    definingClass = "LgR;",
    name = "b",
    returnType = "Z",
    parameters = listOf(),
)

/** gR.c(sK, II)V — failure handler: kills all activities and shows the purchase dialog. sK = verifier callback interface (jadx InterfaceC4091sK). */
internal val LicensorFailureHandlerFingerprint = Fingerprint(
    definingClass = "LgR;",
    name = "c",
    returnType = "V",
    parameters = listOf("LsK;", "I", "I"),
)
