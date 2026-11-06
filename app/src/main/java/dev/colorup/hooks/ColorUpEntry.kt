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
 * - app-unlock duration, guarded by Samsung's keyguard controller call stack
 * - notification shade and Quick Settings canvas transforms, guarded by
 *   Samsung expansion callbacks and their exact Compose root resource IDs
 *
 * All are disabled by default. Missing targets result in no hook or no-op.
 */
class ColorUpEntry : IXposedHookLoadPackage {
    private data class AppUnlockConfig(val enabled: Boolean, val durationMs: Long)
    private data class UnlockHomeConfig(val enabled: Boolean, val durationMs: Long)
    private data class PanelConfig(val enabled: Boolean, val scale: Float, val translateY: Int)

    private val appUnlockConfig = AtomicReference<AppUnlockConfig?>(null)
    private val unlockHomeConfig = AtomicReference<UnlockHomeConfig?>(null)
    private val shadeOpenConfig = AtomicReference<PanelConfig?>(null)
    private val shadeCloseConfig = AtomicReference<PanelConfig?>(null)
    private val qsExpandConfig = AtomicReference<PanelConfig?>(null)
    private val qsCollapseConfig = AtomicReference<PanelConfig?>(null)
    private val panelProgress = AtomicReference(0f)
    private val panelExpanding = AtomicBoolean(true)
    private val appUnlockHookInstalled = AtomicBoolean(false)
    private val unlockHomeHookInstalled = AtomicBoolean(false)
    private val panelHooksInstalled = AtomicBoolean(false)
    private var configObserver: ContentObserver? = null

    private val configUri = Uri.parse("content://dev.colorup.config/config")
    private val appUnlockClass = "com.android.systemui.keyguard.KeyguardSecLegacyUnlockAnimationControllerImpl"
    private val cellLayoutClass = "com.honeyspace.ui.common.CellLayout"
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
                            installPanelExpansionHooks(lpparam)
                        } else {
                            installUnlockHomeDurationHook(lpparam)
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
        appUnlockConfig.set(findAnimation(root, "unlock_to_app")?.let {
            AppUnlockConfig(it.optBoolean("enabled", false), it.optInt("durationMs", 320).coerceIn(0, 2000).toLong())
        })
        unlockHomeConfig.set(findAnimation(root, "unlock_to_home")?.let {
            UnlockHomeConfig(it.optBoolean("enabled", false), it.optInt("durationMs", 320).coerceIn(0, 2000).toLong())
        })
        shadeOpenConfig.set(panelConfig(root, "shade_open"))
        shadeCloseConfig.set(panelConfig(root, "shade_close"))
        qsExpandConfig.set(panelConfig(root, "qs_expand"))
        qsCollapseConfig.set(panelConfig(root, "qs_collapse"))
        XposedBridge.log(
            "ColorUp: config refreshed; unlockHome=" + (unlockHomeConfig.get()?.enabled == true) +
                ", appUnlock=" + (appUnlockConfig.get()?.enabled == true) +
                ", shade=" + (shadeOpenConfig.get()?.enabled == true || shadeCloseConfig.get()?.enabled == true) +
                ", quickSettings=" + (qsExpandConfig.get()?.enabled == true || qsCollapseConfig.get()?.enabled == true)
        )
    }

    private fun panelConfig(root: JSONObject, id: String): PanelConfig? =
        findAnimation(root, id)?.let {
            PanelConfig(
                enabled = it.optBoolean("enabled", false),
                scale = it.optDouble("scale", 1.0).toFloat().coerceIn(0.5f, 1.5f),
                translateY = it.optInt("translateY", 24).coerceIn(-150, 150)
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

    private fun installUnlockHomeDurationHook(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (!unlockHomeHookInstalled.compareAndSet(false, true)) return
        val target = XposedHelpers.findClassIfExists(cellLayoutClass, lpparam.classLoader)
        if (target == null) {
            unlockHomeHookInstalled.set(false)
            XposedBridge.log("ColorUp: unlock-to-home duration unsupported; CellLayout class missing")
            return
        }
        try {
            val candidates = target.declaredMethods.filter { method ->
                val types = method.parameterTypes
                types.size == 3 && types[0] == View::class.java &&
                    types[1] == java.lang.Long.TYPE && types[2] == java.lang.Long.TYPE
            }
            if (candidates.isEmpty()) {
                unlockHomeHookInstalled.set(false)
                XposedBridge.log("ColorUp: unlock-to-home duration unsupported; matching CellLayout method missing")
                return
            }
            candidates.forEach { method ->
                XposedBridge.hookMethod(method, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val config = unlockHomeConfig.get() ?: return
                        if (!config.enabled) return
                        val view = param.args.getOrNull(0) as? View ?: return
                        if (view.isAttachedToWindow) return
                        param.args[2] = config.durationMs
                    }
                })
            }
            XposedBridge.log("ColorUp: unlock-to-home duration hook installed (" + candidates.size + " target method(s))")
        } catch (error: Throwable) {
            unlockHomeHookInstalled.set(false)
            XposedBridge.log("ColorUp: unlock-to-home duration hook unavailable: " + error)
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
            XposedBridge.log("ColorUp: app-unlock duration hook installed")
        } catch (error: Throwable) {
            appUnlockHookInstalled.set(false)
            XposedBridge.log("ColorUp: app-unlock duration hook unavailable: " + error)
        }
    }

    private fun installPanelExpansionHooks(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (!panelHooksInstalled.compareAndSet(false, true)) return
        val splitClass = XposedHelpers.findClassIfExists(splitHelperClass, lpparam.classLoader)
        val composeClass = XposedHelpers.findClassIfExists(composeViewClass, lpparam.classLoader)
        if (splitClass == null || composeClass == null) {
            panelHooksInstalled.set(false)
            XposedBridge.log("ColorUp: panel hooks unsupported; required SystemUI/Compose class missing")
            return
        }
        try {
            val expansionCallback = object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val fraction = param.args.firstOrNull { it is Float } as? Float ?: return
                    val bounded = fraction.coerceIn(0f, 1f)
                    val previous = panelProgress.getAndSet(bounded)
                    panelExpanding.set(bounded >= previous)
                }
            }
            val splitHooks = XposedBridge.hookAllMethods(splitClass, "onPanelExpansionChanged", expansionCallback)
            val depthClass = XposedHelpers.findClassIfExists(depthControllerClass, lpparam.classLoader)
            val depthHooks = if (depthClass != null) {
                XposedBridge.hookAllMethods(depthClass, "onPanelExpansionChanged", expansionCallback)
            } else emptySet()
            if (splitHooks.isEmpty() && depthHooks.isEmpty()) {
                panelHooksInstalled.set(false)
                XposedBridge.log("ColorUp: panel hooks unsupported; no expansion callback found")
                return
            }

            XposedHelpers.findAndHookMethod(
                composeClass,
                "dispatchDraw",
                Canvas::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val view = param.thisObject as? View ?: return
                        val progress = panelProgress.get()
                        if (progress <= 0f) return
                        val config = when {
                            isInsideNamedRoot(view, "sec_quick_panel_compose_root") ->
                                if (panelExpanding.get()) qsExpandConfig.get() else qsCollapseConfig.get()
                            isInsideNamedRoot(view, "shadeRootView") ->
                                if (panelExpanding.get()) shadeOpenConfig.get() else shadeCloseConfig.get()
                            else -> null
                        } ?: return
                        if (!config.enabled) return
                        val canvas = param.args.firstOrNull { it is Canvas } as? Canvas ?: return
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
            XposedBridge.log("ColorUp: panel expansion hooks installed (split=" + splitHooks.size + ", depth=" + depthHooks.size + ")")
        } catch (error: Throwable) {
            panelHooksInstalled.set(false)
            XposedBridge.log("ColorUp: panel hook unavailable: " + error)
        }
    }

    private fun isInsideNamedRoot(view: View, targetName: String): Boolean {
        var current: View? = view
        var depth = 0
        while (current != null && depth++ < 10) {
            val candidate = current
            val id = candidate.id
            if (id != View.NO_ID) {
                val entryName = runCatching { candidate.resources.getResourceEntryName(id) }.getOrNull()
                if (entryName == targetName) return true
            }
            current = candidate.parent as? View
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
