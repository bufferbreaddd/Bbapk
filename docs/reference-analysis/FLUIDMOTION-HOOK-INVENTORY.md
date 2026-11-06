# FluidMotion hook inventory

Generated from the repository reference APK by JADX. This report records identifiers and configuration keys only; decompiled source is not redistributed.

## Hook registration sites
dev/flexy/fluidmotion/Refl.java :                 XposedBridge.hookMethod(m, hook);
dev/flexy/fluidmotion/AppUnlock.java :             XposedBridge.hookMethod(setDuration, SafeHook.before("app unlock stretch", new SafeHook.Body() { // from class: dev.flexy.fluidmotion.AppUnlock$$ExternalSyntheticLambda2
dev/flexy/fluidmotion/UnlockFlyIn.java :                 XposedBridge.hookMethod(m, SafeHook.before("unlock animation", new SafeHook.Body() { // from class: dev.flexy.fluidmotion.UnlockFlyIn$$ExternalSyntheticLambda0

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
