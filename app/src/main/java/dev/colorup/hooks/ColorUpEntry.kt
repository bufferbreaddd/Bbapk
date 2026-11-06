package dev.colorup.hooks

import android.app.Application
import android.content.Context
import android.net.Uri
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/** Runtime bootstrap for the legacy Xposed API. No unverified animation hooks are installed. */
class ColorUpEntry : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        val pkg = lpparam.packageName
        if (pkg != "com.android.systemui" && pkg != "com.sec.android.app.launcher") return
        try {
            XposedHelpers.findAndHookMethod(
                Application::class.java,
                "attach",
                Context::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val context = param.args[0] as? Context ?: return
                        try {
                            context.contentResolver.query(
                                Uri.parse("content://dev.colorup.config/config"),
                                arrayOf("json"), null, null, null
                            )?.use { cursor ->
                                if (cursor.moveToFirst()) {
                                    val configJson = cursor.getString(0)
                                    XposedBridge.log("ColorUp: config bridge connected in $pkg (${configJson.length} chars)")
                                }
                            }
                        } catch (error: Throwable) {
                            XposedBridge.log("ColorUp: config bridge unavailable in $pkg: $error")
                        }
                    }
                }
            )
        } catch (error: Throwable) {
            XposedBridge.log("ColorUp: could not attach config bridge in $pkg: $error")
        }
    }
}
