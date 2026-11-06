# ColorUp hook status

Last reviewed against the repository's FluidMotion reference APK. This is a source-level status report, not proof of behavior on a retail One UI 8.5 build.

## Experimental candidate: unlock to home duration

- Config ID: `unlock_to_home`
- Process: `com.sec.android.app.launcher`
- Reference-derived class: `com.honeyspace.ui.common.CellLayout`
- Method signature guard: exactly three parameters `(View, long, long)`.
- The hook changes only the third long argument, and only when the first View is not yet attached to its window.
- Duration range: 0–2000 ms.
- Launcher also observes configuration updates, so saving settings refreshes the value without restarting the process.

## Experimental candidate: app-unlock duration

- Config ID: `unlock_to_app`
- Process: `com.android.systemui`
- Controller guard: `com.android.systemui.keyguard.KeyguardSecLegacyUnlockAnimationControllerImpl`
- Hooked API: `android.animation.ValueAnimator.setDuration(long)`
- Changes duration only when the call stack contains the exact controller class.
- Duration range: 0–2000 ms.
- The controller class is checked before installing the hook.

## Experimental candidates: shade and Quick Settings motion

Four separate settings are wired into the same guarded panel-expansion pipeline:

| Config ID | Panel direction | Root resource name |
|---|---|---|
| `shade_open` | Notification shade opening | `shadeRootView` |
| `shade_close` | Notification shade closing | `shadeRootView` |
| `qs_expand` | Quick Settings expanding | `sec_quick_panel_compose_root` |
| `qs_collapse` | Quick Settings collapsing | `sec_quick_panel_compose_root` |

The pipeline uses:
- Expansion callbacks from `SecPanelSplitHelper.onPanelExpansionChanged` and, when present, `NotificationShadeDepthController.onPanelExpansionChanged`.
- `androidx.compose.ui.platform.AndroidComposeView.dispatchDraw(Canvas)`.
- An ancestor resource-name check before transforming anything.
- Canvas save/restore around the original draw call.

Only scale and vertical offset are currently applied to those panel transitions. Fade, duration, spring, direction and delay are still editor/preview values for these hooks. If the required classes or expansion callbacks are missing, the transform is not installed.

The class names and hook layout are derived from FluidMotion's reference APK; the transform implementation is ColorUp's own. None of these candidates has yet been confirmed on the user's exact One UI 8.5 build.

## Preview/configuration-only transitions

The remaining 12 IDs have independent configuration and in-app preview controls, but no system hook is installed for them yet. Their enable switches are intentionally unavailable in the UI. Their values remain importable/exportable and can be previewed inside ColorUp.

## Configuration bridge

- Provider URI: `content://dev.colorup.config/config`
- Read-only provider; caller package and UID are checked.
- SystemUI and Launcher register content observers and refresh in-memory settings after ColorUp saves.
- The app records a runtime heartbeat when a scoped process successfully queries the provider.

## Verification still required

1. Build the APK in GitHub Actions.
2. Load it with a compatible legacy Xposed API runtime and scope SystemUI and Samsung Launcher.
3. Confirm the runtime heartbeat appears in ColorUp.
4. Enable one experimental transition at a time and save.
5. Test unlock-to-home duration, app-unlock duration, shade open/close and Quick Settings expand/collapse separately.
6. Confirm unrelated animations remain unchanged.
7. Disable each setting and save; verify stock behavior returns.
8. Test on the exact One UI build and record the build fingerprint.

If a target class, method or resource is missing, keep that capability unavailable rather than guessing at replacement internals.
