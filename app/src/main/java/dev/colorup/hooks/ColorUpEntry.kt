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
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
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
    private data class PanelConfig(val enabled: Boolean, val scale: Float, val translateY: Int, val curve: String, val springDamping: Float, val springStiffness: Float, val direction: String)
    private data class NotificationConfig(val enabled: Boolean, val durationMs: Long, val offsetDp: Int, val scale: Float, val fade: Float, val delayMs: Long, val curve: String, val damping: Float, val stiffness: Float, val direction: String)
    private data class NotificationState(val config: NotificationConfig, val alpha: Float, val scaleX: Float, val scaleY: Float, val translationY: Float)
    private data class NotificationDismissConfig(val enabled: Boolean, val durationMs: Long)

    private val appUnlockConfig = AtomicReference<AppUnlockConfig?>(null)
    private val shadeOpenConfig = AtomicReference<PanelConfig?>(null)
    private val shadeCloseConfig = AtomicReference<PanelConfig?>(null)
    private val qsExpandConfig = AtomicReference<PanelConfig?>(null)
    private val qsCollapseConfig = AtomicReference<PanelConfig?>(null)
    private val notificationAppearConfig = AtomicReference<NotificationConfig?>(null)
    private val notificationDismissConfig = AtomicReference<NotificationDismissConfig?>(null)
    private val panelProgress = AtomicReference(0f)
    private val panelExpanding = AtomicBoolean(true)
    private val appUnlockHookInstalled = AtomicBoolean(false)
    private val panelHooksInstalled = AtomicBoolean(false)
    private val notificationHookInstalled = AtomicBoolean(false)
    private val notificationDismissHookInstalled = AtomicBoolean(false)
    private var configObserver: ContentObserver? = null

    private val configUri = Uri.parse("content://dev.colorup.config/config")
    private val appUnlockClass = "com.android.systemui.keyguard.KeyguardSecLegacyUnlockAnimationControllerImpl"
    private val splitHelperClass = "com.android.systemui.shade.SecPanelSplitHelper"
    private val depthControllerClass = "com.android.systemui.statusbar.NotificationShadeDepthController"
    private val composeViewClass = "androidx.compose.ui.platform.AndroidComposeView"
    private val notificationStackClass = "com.android.systemui.statusbar.notification.stack.NotificationStackScrollLayout"
    private val notificationRowClass = "com.android.systemui.statusbar.notification.row.ExpandableNotificationRow"

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
                        installConfigObserver(context, pkg)
                        if (pkg == "com.android.systemui") {
                            installAppUnlockDurationHook(lpparam)
                            installPanelExpansionHooks(lpparam)
                            installNotificationAppearHook(lpparam)
                            installNotificationDismissDurationHook(lpparam)
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
        shadeOpenConfig.set(panelConfig(root, "shade_open"))
        shadeCloseConfig.set(panelConfig(root, "shade_close"))
        qsExpandConfig.set(panelConfig(root, "qs_expand"))
        qsCollapseConfig.set(panelConfig(root, "qs_collapse"))
        notificationAppearConfig.set(findAnimation(root, "notification_appear")?.let {
            NotificationConfig(
                enabled = it.optBoolean("enabled", false),
                durationMs = it.optInt("durationMs", 320).coerceIn(0, 2000).toLong(),
                offsetDp = it.optInt("translateY", 24).coerceIn(-150, 150),
                scale = it.optDouble("scale", 1.0).toFloat().coerceIn(0.5f, 1.5f),
                fade = it.optDouble("fade", 1.0).toFloat().coerceIn(0f, 1f),
                delayMs = it.optInt("delayMs", 0).coerceIn(0, 1000).toLong(),
                curve = it.optString("curve", "STANDARD"),
                damping = it.optDouble("springDamping", 0.82).toFloat().coerceIn(0.1f, 2f),
                stiffness = it.optDouble("springStiffness", 420.0).toFloat().coerceIn(50f, 1500f),
                direction = it.optString("direction", "UP")
            )
        })
        notificationDismissConfig.set(findAnimation(root, "notification_dismiss")?.let {
            NotificationDismissConfig(
                enabled = it.optBoolean("enabled", false),
                durationMs = it.optInt("durationMs", 320).coerceIn(0, 2000).toLong()
            )
        })
        XposedBridge.log(
            "ColorUp: config refreshed; appUnlock=" + (appUnlockConfig.get()?.enabled == true) +
                ", shade=" + (shadeOpenConfig.get()?.enabled == true || shadeCloseConfig.get()?.enabled == true) +
                ", quickSettings=" + (qsExpandConfig.get()?.enabled == true || qsCollapseConfig.get()?.enabled == true)
        )
    }

    private fun panelConfig(root: JSONObject, id: String): PanelConfig? =
        findAnimation(root, id)?.let {
            PanelConfig(
                enabled = it.optBoolean("enabled", false),
                scale = it.optDouble("scale", 1.0).toFloat().coerceIn(0.5f, 1.5f),
                translateY = it.optInt("translateY", 24).coerceIn(-150, 150),
                curve = it.optString("curve", "STANDARD"),
                springDamping = it.optDouble("springDamping", 0.82).toFloat().coerceIn(0.1f, 2f),
                springStiffness = it.optDouble("springStiffness", 420.0).toFloat().coerceIn(50f, 1500f),
                direction = it.optString("direction", "UP")
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
            XposedBridge.log("ColorUp: app-unlock duration hook installed")
        } catch (error: Throwable) {
            appUnlockHookInstalled.set(false)
            XposedBridge.log("ColorUp: app-unlock duration hook unavailable: " + error)
        }
    }

    private fun installNotificationAppearHook(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (!notificationHookInstalled.compareAndSet(false, true)) return
        val stackClass = XposedHelpers.findClassIfExists(notificationStackClass, lpparam.classLoader)
        val rowClass = XposedHelpers.findClassIfExists(notificationRowClass, lpparam.classLoader)
        if (stackClass == null || rowClass == null) {
            notificationHookInstalled.set(false)
            XposedBridge.log("ColorUp: notification appear unsupported; SystemUI row/stack class missing")
            return
        }
        try {
            XposedHelpers.findAndHookMethod(
                ViewGroup::class.java,
                "addView",
                View::class.java,
                java.lang.Integer.TYPE,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (param.thisObject.javaClass.name != notificationStackClass) return
                        val row = param.args.getOrNull(0) as? View ?: return
                        if (row.javaClass.name != notificationRowClass) return
                        val config = notificationAppearConfig.get() ?: return
                        if (!config.enabled) return
                        val state = NotificationState(config, row.alpha, row.scaleX, row.scaleY, row.translationY)
                        val offsetPx = config.offsetDp * row.resources.displayMetrics.density
                        row.animate().cancel()
                        row.alpha = state.alpha * (1f - config.fade)
                        row.scaleX = state.scaleX * config.scale
                        row.scaleY = state.scaleY * config.scale
                        row.translationY = state.translationY + offsetPx
                        param.setObjectExtra(NOTIFICATION_CONFIG_KEY, state)
                    }

                    override fun afterHookedMethod(param: MethodHookParam) {
                        val state = param.getObjectExtra(NOTIFICATION_CONFIG_KEY) as? NotificationState ?: return
                        val row = param.args.getOrNull(0) as? View ?: return
                        row.post {
                            if (notificationAppearConfig.get()?.enabled != true) {
                                row.alpha = state.alpha
                                row.scaleX = state.scaleX
                                row.scaleY = state.scaleY
                                row.translationY = state.translationY
                                return@post
                            }
                            val pivot = when (state.config.direction) {
                                "UP" -> (row.width / 2f) to row.height.toFloat()
                                "DOWN" -> (row.width / 2f) to 0f
                                "LEFT" -> row.width.toFloat() to (row.height / 2f)
                                "RIGHT" -> 0f to (row.height / 2f)
                                else -> (row.width / 2f) to (row.height / 2f)
                            }
                            row.pivotX = pivot.first
                            row.pivotY = pivot.second
                            row.animate().alpha(state.alpha).scaleX(state.scaleX).scaleY(state.scaleY)
                                .translationY(state.translationY)
                                .setStartDelay(state.config.delayMs)
                                .setDuration(state.config.durationMs)
                                .setInterpolator(notificationInterpolator(state.config))
                                .start()
                        }
                    }
                }
            )
            XposedBridge.log("ColorUp: notification appear hook installed")
        } catch (error: Throwable) {
            notificationHookInstalled.set(false)
            XposedBridge.log("ColorUp: notification appear hook unavailable: " + error)
        }
    }

    private fun installNotificationDismissDurationHook(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (!notificationDismissHookInstalled.compareAndSet(false, true)) return
        val rowClass = XposedHelpers.findClassIfExists(notificationRowClass, lpparam.classLoader)
        if (rowClass == null) {
            notificationDismissHookInstalled.set(false)
            XposedBridge.log("ColorUp: notification dismiss unsupported; row class missing")
            return
        }
        try {
            val candidates = rowClass.declaredMethods.filter { method ->
                val types = method.parameterTypes
                method.name == "performRemoveAnimation" &&
                    method.returnType == java.lang.Long.TYPE &&
                    types.size >= 6 &&
                    types[0] == java.lang.Long.TYPE &&
                    types[1] == java.lang.Long.TYPE &&
                    types[2] == java.lang.Float.TYPE
            }
            if (candidates.isEmpty()) {
                notificationDismissHookInstalled.set(false)
                XposedBridge.log("ColorUp: notification dismiss unsupported; remove-animation method missing")
                return
            }
            candidates.forEach { method ->
                XposedBridge.hookMethod(method, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val config = notificationDismissConfig.get() ?: return
                        if (config.enabled) param.args[0] = config.durationMs
                    }
                })
            }
            XposedBridge.log("ColorUp: notification dismiss duration hook installed (" + candidates.size + " method(s))")
        } catch (error: Throwable) {
            notificationDismissHookInstalled.set(false)
            XposedBridge.log("ColorUp: notification dismiss hook unavailable: " + error)
        }
    }

    private fun notificationInterpolator(config: NotificationConfig): android.view.animation.Interpolator =
        when (config.curve) {
            "LINEAR" -> LinearInterpolator()
            "EASE_IN" -> AccelerateInterpolator()
            "EASE_OUT" -> DecelerateInterpolator()
            "EASE_IN_OUT", "STANDARD" -> AccelerateDecelerateInterpolator()
            "SPRING" -> OvershootInterpolator(
                ((config.stiffness / 420f) * (1f - config.damping)).coerceIn(0.1f, 3f)
            )
            else -> DecelerateInterpolator()
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
                        val eased = dev.colorup.MotionMath.ease(progress, config.curve, config.springDamping, config.springStiffness)
                        val scale = config.scale + (1f - config.scale) * eased
                        val offsetPx = config.translateY * view.resources.displayMetrics.density * (1f - eased)
                        val pivot = when (config.direction) {
                            "UP" -> (view.width / 2f) to view.height.toFloat()
                            "DOWN" -> (view.width / 2f) to 0f
                            "LEFT" -> view.width.toFloat() to (view.height / 2f)
                            "RIGHT" -> 0f to (view.height / 2f)
                            else -> (view.width / 2f) to (view.height / 2f)
                        }
                        val saveCount = canvas.save()
                        canvas.translate(0f, offsetPx)
                        canvas.scale(scale, scale, pivot.first, pivot.second)
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
        private const val NOTIFICATION_CONFIG_KEY = "colorup.notification.config"
    }
}
