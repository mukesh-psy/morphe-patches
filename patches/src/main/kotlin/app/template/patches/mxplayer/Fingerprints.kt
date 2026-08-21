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

// ─── Native protection module (com.ddx.axx.axx.axx.BH / libmx-bh.so) ──────────
//
// BH loads libmx-bh.so and drives it through JNI: integrity watchdog threads are
// spawned natively and results are reported to androidapi.mxplay.com. On patched,
// re-signed builds the native thread teardown aborts the process (SIGABRT in
// std::thread::~thread via std::terminate), so the module must never initialize.
//
// Only these Java members reach the natives: <clinit> (System.loadLibrary),
// b(LlV;I)V / d(App)V (call native n), c/g (Java-only helpers).
// Native declarations n/n1/t/w have no remaining Java callers once the entries
// below are neutralized (App.f()/g() were their last callers), so they stay
// untouched — bytecode cannot be injected into ACC_NATIVE methods anyway.

/** BH.<clinit>()V — System.loadLibrary("mx-bh"). */
internal val BhClassInitFingerprint = Fingerprint(
    definingClass = "Lcom/ddx/axx/axx/axx/BH;",
    name = "<clinit>",
    returnType = "V",
    parameters = listOf(),
)

/** AS.<clinit>()V — second loadLibrary site for the same library. */
internal val AsClassInitFingerprint = Fingerprint(
    definingClass = "Lcom/ddx/axx/axx/axx/AS;",
    name = "<clinit>",
    returnType = "V",
    parameters = listOf(),
)

/** BH.b(lV, I)V — report entry called from Application lifecycle. lV = base Application class (jadx AbstractApplicationC3166lV). */
internal val BhReportFingerprint = Fingerprint(
    definingClass = "Lcom/ddx/axx/axx/axx/BH;",
    name = "b",
    returnType = "V",
    parameters = listOf("LlV;", "I"),
)

/** BH.d(App)V — report entry carrying the encrypted config blob. */
internal val BhBlobReportFingerprint = Fingerprint(
    definingClass = "Lcom/ddx/axx/axx/axx/BH;",
    name = "d",
    returnType = "V",
    parameters = listOf("Lcom/mxtech/videoplayer/pro/App;"),
)

/** BH.c(Context)Z — obfuscated integrity check helper (no Java callers left after init stubs). */
internal val BhIntegrityCheckFingerprint = Fingerprint(
    definingClass = "Lcom/ddx/axx/axx/axx/BH;",
    name = "c",
    returnType = "Z",
    parameters = listOf("Landroid/content/Context;"),
)

/** BH.g()String — device/session fingerprint builder. */
internal val BhSessionIdFingerprint = Fingerprint(
    definingClass = "Lcom/ddx/axx/axx/axx/BH;",
    name = "g",
    returnType = "Ljava/lang/String;",
    parameters = listOf(),
)

/** App.f()V — BH.d(this) + BH.w(hosts) on startup. */
internal val AppProtectionInitFingerprint = Fingerprint(
    definingClass = "Lcom/mxtech/videoplayer/pro/App;",
    name = "f",
    returnType = "V",
    parameters = listOf(),
)

/** App.g()V — BH.b(this, 1) + BH.w(hosts) on startup. */
internal val AppProtectionAltInitFingerprint = Fingerprint(
    definingClass = "Lcom/mxtech/videoplayer/pro/App;",
    name = "g",
    returnType = "V",
    parameters = listOf(),
)
