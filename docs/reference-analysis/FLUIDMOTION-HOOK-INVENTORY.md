# FluidMotion hook inventory

Generated from the repository reference APK by JADX. This report records identifiers and configuration keys only; decompiled source is not redistributed.

## Hook registration sites
dev/flexy/fluidmotion/Refl.java :                 XposedBridge.hookMethod(m, hook);
dev/flexy/fluidmotion/AppUnlock.java :             XposedBridge.hookMethod(setDuration, SafeHook.before("app unlock stretch", new SafeHook.Body() { // from class: dev.flexy.fluidmotion.AppUnlock$$ExternalSyntheticLambda2
dev/flexy/fluidmotion/UnlockFlyIn.java :                 XposedBridge.hookMethod(m, SafeHook.before("unlock animation", new SafeHook.Body() { // from class: dev.flexy.fluidmotion.UnlockFlyIn$$ExternalSyntheticLambda0

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
