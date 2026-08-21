package app.template.patches.mxplayer

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.MX_PLAYER_PRO_COMPATIBILITY
import app.template.patches.shared.returnEarly
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

/**
 * Disables MX's native protection module (libmx-bh.so, driven by com.ddx.axx.axx.axx.BH).
 *
 * The module spawns native watchdog threads and reports install state to
 * androidapi.mxplay.com. On patched/re-signed builds it kills the process:
 *  - original package: SIGABRT (std::terminate from std::thread::~thread in libmx-bh)
 *  - renamed package : silent exit() right after the media list opens
 *
 * Three load sites exist and all are neutralized:
 *  1. BH.<clinit> / AS.<clinit>  → return-void (never reach System.loadLibrary)
 *  2. lV.onCreate direct call    → NOP the loadLibrary invoke; execution falls
 *     through with D=false so Application init continues normally.
 *  Java entry points (BH.b/d/c/g, App.f/g) are stubbed as well — they are the
 *  only callers of the native methods n/n1/t/w.
 */
@Suppress("unused")
val mxPlayerDisableProtectionModulePatch = bytecodePatch(
    name = "Disable protection module",
    description = "Disables the native MX protection module (integrity watchdog + MX server reporting). Fixes startup crashes and silent exits on patched builds.",
    default = true,
) {
    compatibleWith(MX_PLAYER_PRO_COMPATIBILITY)

    execute {
        // Never load libmx-bh.so via class initializers.
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

        // Direct loadLibrary("mx-bh") inside base Application.onCreate:
        // const-string vX, "mx-bh" / invoke-static {vX}, System->loadLibrary
        // → replace the invoke with nop. D stays false and onCreate continues.
        // (lV.onCreate contains exactly one System.loadLibrary call.)
        val onCreate = ApplicationOnCreateFingerprint.method
        val loadIndex = onCreate.implementation!!.instructions.indexOfFirst { instruction ->
            val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
            reference?.definingClass == "Ljava/lang/System;" && reference.name == "loadLibrary"
        }
        if (loadIndex < 0) throw PatchException("Could not find System.loadLibrary in lV.onCreate")
        onCreate.replaceInstruction(loadIndex, "nop")
    }
}
