package dev.colorup.hooks

import android.animation.ValueAnimator
import android.app.Application
import android.content.Context
import android.net.Uri
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import org.json.JSONObject

/**
 * Legacy Xposed runtime.
 * First real hook: change app-unlock animator duration only when called from
 * Samsung's KeyguardSecLegacyUnlockAnimationControllerImpl.
 */
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
                        val config = readConfig(context, pkg) ?: return
                        if (pkg == "com.android.systemui") installAppUnlockDurationHook(lpparam, config)
                    }
                }
            )
        } catch (error: Throwable) {
            XposedBridge.log("ColorUp: bootstrap failed in " + pkg + ": " + error)
        }
    }

    private fun readConfig(context: Context, pkg: String): JSONObject? {
        return try {
            context.contentResolver.query(
                Uri.parse("content://dev.colorup.config/config"),
                arrayOf("json"), null, null, null
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return null
                XposedBridge.log("ColorUp: config bridge connected in " + pkg)
                JSONObject(cursor.getString(0))
            }
        } catch (error: Throwable) {
            XposedBridge.log("ColorUp: config bridge unavailable in " + pkg + ": " + error)
            null
        }
    }

    private fun installAppUnlockDurationHook(
        lpparam: XC_LoadPackage.LoadPackageParam,
        root: JSONObject
    ) {
        val targetClass = "com.android.systemui.keyguard.KeyguardSecLegacyUnlockAnimationControllerImpl"
        if (XposedHelpers.findClassIfExists(targetClass, lpparam.classLoader) == null) {
            XposedBridge.log("ColorUp: app-unlock duration unsupported; target class missing")
            return
        }
        val item = findAnimation(root, "unlock_to_app") ?: return
        if (!item.optBoolean("enabled", false)) {
            XposedBridge.log("ColorUp: app-unlock duration is disabled")
            return
        }
        val duration = item.optInt("durationMs", 320).coerceIn(0, 2000).toLong()
        try {
            XposedHelpers.findAndHookMethod(
                ValueAnimator::class.java,
                "setDuration",
                java.lang.Long.TYPE,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (isAppUnlockCall()) param.args[0] = duration
                    }
                }
            )
            XposedBridge.log("ColorUp: app-unlock duration hook installed (" + duration + "ms)")
        } catch (error: Throwable) {
            XposedBridge.log("ColorUp: app-unlock duration hook unavailable: " + error)
        }
    }

    private fun findAnimation(root: JSONObject, id: String): JSONObject? {
        val animations = root.optJSONArray("animations") ?: return null
        for (index in 0 until animations.length()) {
            val item = animations.optJSONObject(index) ?: continue
            if (item.optString("id") == id) return item
        }
        return null
    }

    private fun isAppUnlockCall(): Boolean =
        Thread.currentThread().stackTrace.any {
            it.className == "com.android.systemui.keyguard.KeyguardSecLegacyUnlockAnimationControllerImpl"
        }
}
