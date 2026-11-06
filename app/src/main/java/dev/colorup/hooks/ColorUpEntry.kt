package dev.colorup.hooks

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.io.File

/** Initial runtime probe. Transition hooks are intentionally not installed until verified per build. */
class ColorUpEntry : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        val pkg=lpparam.packageName
        if (pkg!="com.android.systemui" && pkg!="com.sec.android.app.launcher") return
        val config=File("/data/adb/colorup/config.json")
        XposedBridge.log("ColorUp: attached to $pkg; configPresent=${config.exists()}")
        XposedBridge.log("ColorUp: probe-only milestone; no animation hooks active")
    }
}
