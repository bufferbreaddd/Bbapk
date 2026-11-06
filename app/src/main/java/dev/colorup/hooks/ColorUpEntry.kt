package dev.colorup.hooks

import android.animation.ValueAnimator
import android.app.Application
import android.content.Context
import android.database.ContentObserver
import android.graphics.Canvas
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewParent
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
 *
 * Experimental reference-derived hooks:
 * - app-unlock duration: ValueAnimator.setDuration from Samsung's keyguard controller
 * - Quick Settings expansion: transform only the Compose view inside the named QS root
 *
 * Both are disabled by default and guarded by runtime class/resource checks.
 */
class ColorUpEntry : IXposedHookLoadPackage {
    private data class AppUnlockConfig(val enabled: Boolean, val durationMs: Long)
    private data class QuickSettingsConfig(val enabled: Boolean, val scale: Float, val translateY: Int)

    private val appUnlockConfig = AtomicReference<AppUnlockConfig?>(null)
    private val quickSettingsConfig = AtomicReference<QuickSettingsConfig?>(null)
    private val quickSettingsProgress = AtomicReference(0f)
    private val appUnlockHookInstalled = AtomicBoolean(false)
    private val quickSettingsHooksInstalled = AtomicBoolean(false)
    private var configObserver: ContentObserver? = null

    private val configUri = Uri.parse("content://dev.colorup.config/config")
    private val appUnlockClass = "com.android.systemui.keyguard.KeyguardSecLegacyUnlockAnimationControllerImpl"
    private val splitHelperClass = "com.android.systemui.shade.SecPanelSplitHelper"
    private val depthControllerClass = "com.android.systemui.statusbar.NotificationShadeDepthController"
    private val composeViewClass = "androidx.compose.ui.platform.AndroidComposeView"

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
                            installQuickSettingsExpansionHooks(lpparam)
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
        val appItem = findAnimation(root, "unlock_to_app")
        appUnlockConfig.set(appItem?.let {
            AppUnlockConfig(
                enabled = it.optBoolean("enabled", false),
                durationMs = it.optInt("durationMs", 320).coerceIn(0, 2000).toLong()
            )
        })
        val qsItem = findAnimation(root, "qs_expand")
        quickSettingsConfig.set(qsItem?.let {
            QuickSettingsConfig(
                enabled = it.optBoolean("enabled", false),
                scale = it.optDouble("scale", 1.0).toFloat().coerceIn(0.5f, 1.5f),
                translateY = it.optInt("translateY", 24).coerceIn(-150, 150)
            )
        })
        XposedBridge.log(
            "ColorUp: config refreshed; appUnlock=" + (appUnlockConfig.get()?.enabled == true) +
                ", quickSettings=" + (quickSettingsConfig.get()?.enabled == true)
        )
    }

    private fun installConfigObserver(context: Context, pkg: String) {
        try {
            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    refreshConfig(context, pkg)
                }
            }
            configObserver = observer
            context.contentResolver.registerContentObserver(configUri, false, observer)
        } catch (error: Throwable) {
            XposedBridge.log("ColorUp: live config observer unavailable: " + error)
        }
    }

    private fun installAppUnlockDurationHook(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (!appUnlockHookInstalled.compareAndSet(false, true)) return
        if (XposedHelpers.findClassIfExists(appUnlockClass, lpparam.classLoader) == null) {
            appUnlockHookInstalled.set(false)
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
                        if (!isCallFrom(appUnlockClass)) return
                        val config = appUnlockConfig.get() ?: return
                        if (config.enabled) param.args[0] = config.durationMs
                    }
                }
            )
            XposedBridge.log("ColorUp: app-unlock duration hook installed; waiting for enabled config")
        } catch (error: Throwable) {
            appUnlockHookInstalled.set(false)
            XposedBridge.log("ColorUp: app-unlock duration hook unavailable: " + error)
        }
    }

    private fun installQuickSettingsExpansionHooks(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (!quickSettingsHooksInstalled.compareAndSet(false, true)) return
        val splitClass = XposedHelpers.findClassIfExists(splitHelperClass, lpparam.classLoader)
        val composeClass = XposedHelpers.findClassIfExists(composeViewClass, lpparam.classLoader)
        if (splitClass == null || composeClass == null) {
            quickSettingsHooksInstalled.set(false)
            XposedBridge.log("ColorUp: Quick Settings hook unsupported; required Compose/SystemUI class missing")
            return
        }
        try {
            val expansionCallback = object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val fraction = param.args.firstOrNull { it is Float } as? Float ?: return
                    quickSettingsProgress.set(fraction.coerceIn(0f, 1f))
                }
            }
            val splitHooks = XposedBridge.hookAllMethods(splitClass, "onPanelExpansionChanged", expansionCallback)
            val depthClass = XposedHelpers.findClassIfExists(depthControllerClass, lpparam.classLoader)
            val depthHooks = if (depthClass != null) {
                XposedBridge.hookAllMethods(depthClass, "onPanelExpansionChanged", expansionCallback)
            } else emptySet()

            XposedHelpers.findAndHookMethod(
                composeClass,
                "dispatchDraw",
                Canvas::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val config = quickSettingsConfig.get() ?: return
                        if (!config.enabled) return
                        val view = param.thisObject as? View ?: return
                        if (!isQuickSettingsRoot(view)) return
                        val canvas = param.args.firstOrNull { it is Canvas } as? Canvas ?: return
                        val progress = quickSettingsProgress.get()
                        val scale = config.scale + (1f - config.scale) * progress
                        val offsetPx = config.translateY * view.resources.displayMetrics.density * (1f - progress)
                        val saveCount = canvas.save()
                        canvas.translate(0f, offsetPx)
                        canvas.scale(scale, scale, view.width / 2f, view.height / 2f)
                        param.setObjectExtra(CANVAS_SAVE_KEY, saveCount)
                    }

                    override fun afterHookedMethod(param: MethodHookParam) {
                        val saveCount = param.getObjectExtra(CANVAS_SAVE_KEY) as? Int ?: return
                        val canvas = param.args.firstOrNull { it is Canvas } as? Canvas ?: return
                        canvas.restoreToCount(saveCount)
                    }
                }
            )
            XposedBridge.log("ColorUp: Quick Settings expansion hooks installed (split=" + splitHooks.size + ", depth=" + depthHooks.size + ")")
        } catch (error: Throwable) {
            quickSettingsHooksInstalled.set(false)
            XposedBridge.log("ColorUp: Quick Settings hook unavailable: " + error)
        }
    }

    private fun isQuickSettingsRoot(view: View): Boolean {
        var current: View? = view
        var depth = 0
        while (current != null && depth++ < 10) {
            val id = current.id
            if (id != View.NO_ID) {
                val entryName = runCatching { current.resources.getResourceEntryName(id) }.getOrNull()
                if (entryName == "sec_quick_panel_compose_root") return true
            }
            current = current.parent as? View
        }
        return false
    }

    private fun isCallFrom(className: String): Boolean =
        Thread.currentThread().stackTrace.any { it.className == className }

    private fun findAnimation(root: JSONObject, id: String): JSONObject? {
        val animations = root.optJSONArray("animations") ?: return null
        for (index in 0 until animations.length()) {
            val item = animations.optJSONObject(index) ?: continue
            if (item.optString("id") == id) return item
        }
        return null
    }

    companion object {
        private const val CANVAS_SAVE_KEY = "colorup.canvas.save"
    }
}
