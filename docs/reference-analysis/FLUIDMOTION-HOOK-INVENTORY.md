# FluidMotion hook inventory

Generated from the repository reference APK by JADX. This report records identifiers and configuration keys only; decompiled source is not redistributed.

## Hook registration sites
dev/flexy/fluidmotion/Refl.java :                 XposedBridge.hookMethod(m, hook);
dev/flexy/fluidmotion/AppUnlock.java :             XposedBridge.hookMethod(setDuration, SafeHook.before("app unlock stretch", new SafeHook.Body() { // from class: dev.flexy.fluidmotion.AppUnlock$$ExternalSyntheticLambda2
dev/flexy/fluidmotion/UnlockFlyIn.java :                 XposedBridge.hookMethod(m, SafeHook.before("unlock animation", new SafeHook.Body() { // from class: dev.flexy.fluidmotion.UnlockFlyIn$$ExternalSyntheticLambda0

## Target class constants
dev/flexy/fluidmotion/Entry.java:8:    private static final String LAUNCHER = "com.sec.android.app.launcher";
dev/flexy/fluidmotion/Entry.java:9:    private static final String SYSTEMUI = "com.android.systemui";
dev/flexy/fluidmotion/XLog.java:8:    private static final String TAG = "FluidMotion";
dev/flexy/fluidmotion/Settings.java:7:    static final String APP_ENABLED = "app_unlock_enabled";
dev/flexy/fluidmotion/Settings.java:8:    static final String APP_ZOOM_DURATION = "app_zoom_duration";
dev/flexy/fluidmotion/Settings.java:9:    static final String APP_ZOOM_SCALE = "app_zoom_scale";
dev/flexy/fluidmotion/Settings.java:24:    static final String FILE = "settings";
dev/flexy/fluidmotion/Settings.java:25:    static final String SHADE_DROP = "shade_drop";
dev/flexy/fluidmotion/Settings.java:26:    static final String SHADE_NOTIF = "shade_notif_enabled";
dev/flexy/fluidmotion/Settings.java:27:    static final String SHADE_QS = "shade_qs_enabled";
dev/flexy/fluidmotion/Settings.java:28:    static final String SHADE_SCALE = "shade_scale";
dev/flexy/fluidmotion/Settings.java:29:    static final String UNLOCK_ANCHOR = "unlock_anchor";
dev/flexy/fluidmotion/Settings.java:30:    static final String UNLOCK_BOUNCE = "unlock_bounce";
dev/flexy/fluidmotion/Settings.java:31:    static final String UNLOCK_DURATION = "unlock_duration";
dev/flexy/fluidmotion/Settings.java:32:    static final String UNLOCK_ENABLED = "unlock_enabled";
dev/flexy/fluidmotion/Settings.java:33:    static final String UNLOCK_FADE = "unlock_fade";
dev/flexy/fluidmotion/Settings.java:34:    static final String UNLOCK_SCALE = "unlock_scale";
dev/flexy/fluidmotion/Settings.java:35:    static final String UNLOCK_STAGGER = "unlock_stagger";
dev/flexy/fluidmotion/SettingsProvider.java:13:    static final String METHOD_GET = "get";
dev/flexy/fluidmotion/ShadeFlyIn.java:27:    private static final String BINDER = "com.android.systemui.blur.ui.viewbinder.SecPanelWindowBlurBinder";
dev/flexy/fluidmotion/ShadeFlyIn.java:28:    private static final String COMPOSE = "androidx.compose.ui.platform.AndroidComposeView";
dev/flexy/fluidmotion/ShadeFlyIn.java:29:    private static final String DEPTH = "com.android.systemui.statusbar.NotificationShadeDepthController";
dev/flexy/fluidmotion/ShadeFlyIn.java:31:    private static final String PKG = "com.android.systemui";
dev/flexy/fluidmotion/ShadeFlyIn.java:34:    private static final String SPLIT = "com.android.systemui.shade.SecPanelSplitHelper";
dev/flexy/fluidmotion/AppUnlock.java:23:    private static final String CONTROLLER = "com.android.systemui.keyguard.KeyguardSecLegacyUnlockAnimationControllerImpl";
dev/flexy/fluidmotion/UnlockFlyIn.java:18:    private static final String CELL_LAYOUT = "com.honeyspace.ui.common.CellLayout";
dev/flexy/fluidmotion/UnlockFlyIn.java:20:    private static final String STUB = "com.android.systemui.shared.system.smartspace.ILauncherUnlockAnimationController$Stub";

## Reflected classes, methods and fields
dev/flexy/fluidmotion/RemoteSettings.java:28:            ctx = (Context) Class.forName("android.app.ActivityThread").getMethod("currentApplication", new Class[0]).invoke(null, new Object[0]);
dev/flexy/fluidmotion/Refl.java:24:    public static Class<?> findClass(String name, ClassLoader cl) {
dev/flexy/fluidmotion/Refl.java:26:            return Class.forName(name, false, cl);
dev/flexy/fluidmotion/Refl.java:45:                f = c.getDeclaredField(name);
dev/flexy/fluidmotion/Refl.java:116:            for (Method cand : c.getDeclaredMethods()) {
dev/flexy/fluidmotion/Refl.java:162:        for (Method m : cls.getDeclaredMethods()) {
dev/flexy/fluidmotion/Refl.java:164:                XposedBridge.hookMethod(m, hook);
dev/flexy/fluidmotion/Refl.java:175:        Class<?> cls = findClass(className, cl);
dev/flexy/fluidmotion/Refl.java:183:        Class<?> cls = findClass(className, cl);
dev/flexy/fluidmotion/ShadeFlyIn.java:117:        INSTANCE.composeClass = Refl.findClass(COMPOSE, cl);
dev/flexy/fluidmotion/AppUnlock.java:45:        Class<?> cls = Refl.findClass(CONTROLLER, cl);
dev/flexy/fluidmotion/AppUnlock.java:76:            Method setDuration = ValueAnimator.class.getDeclaredMethod("setDuration", Long.TYPE);
dev/flexy/fluidmotion/AppUnlock.java:77:            XposedBridge.hookMethod(setDuration, SafeHook.before("app unlock stretch", new SafeHook.Body() { // from class: dev.flexy.fluidmotion.AppUnlock$$ExternalSyntheticLambda2
dev/flexy/fluidmotion/AppUnlock.java:125:                Context ctx = (Context) Class.forName("android.app.ActivityThread").getMethod("currentApplication", new Class[0]).invoke(null, new Object[0]);
dev/flexy/fluidmotion/AppUnlock.java:193:        a.setDuration(duration + delay + 3000);
dev/flexy/fluidmotion/UnlockFlyIn.java:53:        Class<?> cell = Refl.findClass(CELL_LAYOUT, cl);
dev/flexy/fluidmotion/UnlockFlyIn.java:82:        for (Method m : cls.getDeclaredMethods()) {
dev/flexy/fluidmotion/UnlockFlyIn.java:85:                XposedBridge.hookMethod(m, SafeHook.before("unlock animation", new SafeHook.Body() { // from class: dev.flexy.fluidmotion.UnlockFlyIn$$ExternalSyntheticLambda0

## Method-selection predicates
dev/flexy/fluidmotion/Refl.java:9:import java.lang.reflect.Modifier;
dev/flexy/fluidmotion/Refl.java:38:        String key = cls.getName() + "#" + name;
dev/flexy/fluidmotion/Refl.java:110:        String key = cls.getName() + "#" + name + "/" + paramCount;
dev/flexy/fluidmotion/Refl.java:117:                if (cand.getName().equals(name) && cand.getParameterTypes().length == paramCount) {
dev/flexy/fluidmotion/Refl.java:163:            if (m.getName().equals(name) && !Modifier.isAbstract(m.getModifiers())) {
dev/flexy/fluidmotion/Refl.java:169:            XLog.i("nothing to hook: " + cls.getName() + "#" + name);
dev/flexy/fluidmotion/Refl.java:216:            java.lang.String r0 = r7.getName()
dev/flexy/fluidmotion/Refl.java:284:            java.lang.Class[] r0 = r7.getParameterTypes()
dev/flexy/fluidmotion/Refl.java:299:            java.lang.Class r1 = r7.getReturnType()
dev/flexy/fluidmotion/UnlockFlyIn.java:83:            Class<?>[] t = m.getParameterTypes();
dev/flexy/fluidmotion/UnlockFlyIn.java:92:                XLog.i("unlock animation hooked: " + cls.getName() + "." + m.getName());
dev/flexy/fluidmotion/UnlockFlyIn.java:97:            XLog.i("unlock animation method not found in " + cls.getName());
dev/flexy/fluidmotion/UnlockFlyIn.java:264:        String name = v.getClass().getName();
dev/flexy/fluidmotion/UnlockFlyIn.java:265:        if (name.endsWith(".QSBContainer") || name.endsWith(".PageIndicatorView")) {
dev/flexy/fluidmotion/ShadeFlyIn.java:642:                if ((name instanceof String) && ELEMENTS.contains(name)) {
dev/flexy/fluidmotion/ShadeFlyIn.java:645:                if ("TestTag".equals(name) && String.valueOf(((Map.Entry) e).getValue()).endsWith("tile_layout_background")) {
dev/flexy/fluidmotion/ShadeFlyIn.java:1025:        return n.contains("Footer") || n.contains("Shelf") || n.contains("EmptyShade");

## Unlock method signature filters

## Shade hook registration references
28:    private static final String COMPOSE = "androidx.compose.ui.platform.AndroidComposeView";
118:        int n = Refl.hookAll(cl, SPLIT, "onPanelExpansionChanged", SafeHook.before("shade expansion", new SafeHook.Body() { // from class: dev.flexy.fluidmotion.ShadeFlyIn$$ExternalSyntheticLambda0
125:            n += Refl.hookAll(cl, DEPTH, "onPanelExpansionChanged", SafeHook.before("shade expansion depth", new SafeHook.Body() { // from class: dev.flexy.fluidmotion.ShadeFlyIn$$ExternalSyntheticLambda1
132:        int n2 = n + Refl.hookConstructors(cl, BINDER, SafeHook.after("shade window", new SafeHook.Body() { // from class: dev.flexy.fluidmotion.ShadeFlyIn$$ExternalSyntheticLambda2
139:            n2 += Refl.hookAll(INSTANCE.composeClass, "dispatchDraw", SafeHook.before("qs fly-in draw", new SafeHook.Body() { // from class: dev.flexy.fluidmotion.ShadeFlyIn$$ExternalSyntheticLambda3
146:        XLog.i("shade fly-in hooks: " + n2);

## Animation and preference identifiers
"android.app.ActivityThread"
"app_unlock_enabled"
"app_zoom_duration"
"app_zoom_scale"
"applyTransaction"
"com.android.systemui.shade.SecPanelSplitHelper"
"com.android.systemui.shared.system.InteractionJankMonitorWrapper"
"com.sec.android.app.launcher"
"notification_stack_scroller"
"sec_quick_panel_compose_root"
"shadeRootView"
"shade_drop"
"shade_notif_enabled"
"shade_qs_enabled"
"shade_scale"
"unlock_anchor"
"unlock_bounce"
"unlock_duration"
"unlock_enabled"
"unlock_fade"
"unlock_scale"
"unlock_stagger"

## Manifest summary
