package app.template.patches.mxplayer

import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.MX_PLAYER_PRO_COMPATIBILITY
import app.template.patches.shared.returnEarly

/**
 * Removes the purchase/login wall of MX Player Pro.
 *
 * The app verifies the Play Store license (LVL) or its own license proxy on a timer,
 * requires the purchasing Google account, and finishes all activities with a
 * "Unable to validate the purchase" dialog when verification fails. All three
 * enforcement points live in one R8 class (gR "Licensor"):
 *
 *   1. gR.a(ContextWrapper)V — never start any verification (LVL / MX proxy / retry paths).
 *   2. gR.b()Z — always report licensed so App.J() and every UI gate passes.
 *   3. gR.c(sK, II)V — neutralize the failure handler as belt-and-suspenders.
 */
@Suppress("unused")
val mxPlayerRemoveLicenseCheckPatch = bytecodePatch(
    name = "Remove license check",
    description = "Disables Google Play Licensing and MX license-proxy verification. No account login or purchase validation is requested.",
) {
    compatibleWith(MX_PLAYER_PRO_COMPATIBILITY)

    execute {
        // Entry point invoked from App.m() on every activity creation — no-op it.
        LicensorVerifyFingerprint.method.returnEarly()

        // Global isLicensed query consumed by App.J() and premium feature gates.
        LicensorIsLicensedFingerprint.method.returnEarly(true)

        // Failure handler that finished all activities and showed the buy dialog.
        LicensorFailureHandlerFingerprint.method.returnEarly()
    }
}
