# ColorUp hook status

Last reviewed against the repository's FluidMotion reference APK. This is a source-level status report, not proof of behavior on a retail One UI 8.5 build.

## Experimental candidate: app-unlock duration

- Config ID: `unlock_to_app`
- Process: `com.android.systemui`
- Reference-derived controller: `com.android.systemui.keyguard.KeyguardSecLegacyUnlockAnimationControllerImpl`
- Hooked API: `android.animation.ValueAnimator.setDuration(long)`
- Guard: changes the duration only when the call stack contains the exact controller class.
- Range: 0–2000 ms.
- The controller class is checked before installing the hook.

## Experimental candidate: Quick Settings panel motion

- Config ID: `qs_expand`
- Process: `com.android.systemui`
- Expansion sources: `SecPanelSplitHelper.onPanelExpansionChanged` and, when present, `NotificationShadeDepthController.onPanelExpansionChanged`
- Draw target: `androidx.compose.ui.platform.AndroidComposeView.dispatchDraw(Canvas)`
- Guard: drawing is transformed only when the Compose view or one of its ancestors has the resource ID name `sec_quick_panel_compose_root`.
- Applied parameters: scale and vertical offset, interpolated against the observed expansion fraction.
- The canvas is saved and restored around the original draw call. The hook does not replace the panel's own expansion animation or allocate an off-screen alpha layer.
- If the required classes or expansion callback are missing, the hook is not installed.

This hook layout is derived from FluidMotion's reference targets, but the transform implementation is ColorUp's own. It has **not** yet been confirmed on the user's exact One UI 8.5 build.

## Preview/configuration-only transitions

The remaining 16 IDs have independent configuration and in-app preview controls, but no system hook is installed for them yet. Their enable switches are intentionally unavailable in the UI. Their values remain importable/exportable and can be previewed inside ColorUp.

For the two experimental hooks, only the parameters listed above are applied to One UI. Duration, fade, spring, direction and delay settings for Quick Settings are still preview/configuration values.

## Configuration bridge

- Provider URI: `content://dev.colorup.config/config`
- Read-only provider; caller package and UID are checked.
- SystemUI registers a content observer and refreshes the in-memory configuration after ColorUp saves.
- The app records a runtime heartbeat when a scoped process successfully queries the provider.

## Verification still required

1. Build the APK in GitHub Actions.
2. Load it with a compatible legacy Xposed API runtime and scope SystemUI.
3. Confirm the runtime heartbeat appears in ColorUp.
4. Enable one experimental hook at a time and save.
5. Test unlock-to-app duration and Quick Settings expansion separately.
6. Confirm unrelated SystemUI animations remain unchanged.
7. Disable the setting and save; verify stock behavior returns.
8. Test on the exact One UI build and record the build fingerprint.

If a target class, method or resource is missing, keep that capability unavailable rather than guessing at replacement internals.
