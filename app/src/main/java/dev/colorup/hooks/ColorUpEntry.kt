package dev.colorup.hooks

import android.animation.ValueAnimator
import android.app.Application
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Legacy Xposed runtime.
 * The first system hook adjusts ValueAnimator duration only when the caller
 * is Samsung's keyguard app-unlock controller. Configuration is refreshed
 * through a read-only ContentProvider observer without restarting SystemUI.
 */
class ColorUpEntry : IXposedHookLoadPackage {
    private data class AppUnlockConfig(val enabled: Boolean, val durationMs: Long)

    private val appUnlockConfig = AtomicReference<AppUnlockConfig?>(null)
    private val hookInstalled = AtomicBoolean(false)
    private val configUri = Uri.parse("content://dev.colorup.config/config")
    private val targetClass = "com.android.systemui.keyguard.KeyguardSecLegacyUnlockAnimationControllerImpl"

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
                        refreshConfig(context, pkg)
                        if (pkg == "com.android.systemui") {
                            installConfigObserver(context, pkg)
                            installAppUnlockDurationHook(lpparam)
                        }
                    }
                }
            )
        } catch (error: Throwable) {
            XposedBridge.log("ColorUp: bootstrap failed in " + pkg + ": " + error)
        }
    }

    private fun readConfig(context: Context, pkg: String): JSONObject? {
        return try {
            context.contentResolver.query(configUri, arrayOf("json"), null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return null
                XposedBridge.log("ColorUp: config bridge connected in " + pkg)
                JSONObject(cursor.getString(0))
            }
        } catch (error: Throwable) {
            XposedBridge.log("ColorUp: config bridge unavailable in " + pkg + ": " + error)
            null
        }
    }

    private fun refreshConfig(context: Context, pkg: String) {
        val root = readConfig(context, pkg) ?: return
        val item = findAnimation(root, "unlock_to_app")
        val next = item?.let {
            AppUnlockConfig(
                enabled = it.optBoolean("enabled", false),
                durationMs = it.optInt("durationMs", 320).coerceIn(0, 2000).toLong()
            )
        }
        appUnlockConfig.set(next)
        XposedBridge.log("ColorUp: app-unlock config refreshed; enabled=" + (next?.enabled == true))
    }

    private fun installConfigObserver(context: Context, pkg: String) {
        try {
            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    refreshConfig(context, pkg)
                }
            }
            context.contentResolver.registerContentObserver(configUri, false, observer)
        } catch (error: Throwable) {
            XposedBridge.log("ColorUp: live config observer unavailable: " + error)
        }
    }

    private fun installAppUnlockDurationHook(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (!hookInstalled.compareAndSet(false, true)) return
        if (XposedHelpers.findClassIfExists(targetClass, lpparam.classLoader) == null) {
            hookInstalled.set(false)
            XposedBridge.log("ColorUp: app-unlock duration unsupported; target class missing")
            return
        }
        try {
            XposedHelpers.findAndHookMethod(
                ValueAnimator::class.java,
                "setDuration",
                java.lang.Long.TYPE,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (!isAppUnlockCall()) return
                        val config = appUnlockConfig.get() ?: return
                        if (config.enabled) param.args[0] = config.durationMs
                    }
                }
            )
            XposedBridge.log("ColorUp: app-unlock duration hook installed; waiting for enabled config")
        } catch (error: Throwable) {
            hookInstalled.set(false)
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
        Thread.currentThread().stackTrace.any { it.className == targetClass }
}
