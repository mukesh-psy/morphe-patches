package app.template.patches.mxplayer

import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.MX_PLAYER_PRO_COMPATIBILITY
import app.template.patches.shared.returnEarly

/**
 * Disables MX's native protection module (libmx-bh.so, driven by com.ddx.axx.axx.axx.BH).
 *
 * The module spawns native watchdog threads and reports install state to
 * androidapi.mxplay.com. On patched/re-signed builds its thread teardown aborts
 * the whole process at startup:
 *
 *   Fatal signal 6 (SIGABRT) — std::terminate() from std::thread::~thread()
 *   #06 pc 0x2fd1c libmx-bh.so
 *
 * Neutralizing every Java entry point prevents the library from ever loading or
 * spawning threads, which fixes the crash and removes its network reporting.
 */
@Suppress("unused")
val mxPlayerDisableProtectionModulePatch = bytecodePatch(
    name = "Disable protection module",
    description = "Disables the native MX protection module (integrity watchdog + MX server reporting). Fixes startup crashes on patched builds.",
    default = true,
) {
    compatibleWith(MX_PLAYER_PRO_COMPATIBILITY)

    execute {
        // Never load libmx-bh.so.
        BhClassInitFingerprint.method.returnEarly()
        AsClassInitFingerprint.method.returnEarly()

        // No-op the report/init entries invoked from Application lifecycle.
        BhReportFingerprint.method.returnEarly()
        BhBlobReportFingerprint.method.returnEarly()

        // Java-only helpers: report "clean" and an empty session id.
        BhIntegrityCheckFingerprint.method.returnEarly(true)
        BhSessionIdFingerprint.method.returnEarly("")

        // Startup hooks that call the native BH.w(hosts) entry (cannot be patched directly).
        AppProtectionInitFingerprint.method.returnEarly()
        AppProtectionAltInitFingerprint.method.returnEarly()
    }
}
