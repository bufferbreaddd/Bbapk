# ColorUp hook status

Last reviewed against the repository's FluidMotion reference APK and the current ColorUp source. This is a source-level status report, not proof of behavior on a retail One UI 8.5 build.

## Experimental candidate: app-unlock duration

- Config ID: `unlock_to_app`
- Process: `com.android.systemui`
- Controller guard: `com.android.systemui.keyguard.KeyguardSecLegacyUnlockAnimationControllerImpl`
- Hooked API: `android.animation.ValueAnimator.setDuration(long)`
- Changes duration only when the call stack contains the exact controller class.
- Duration range: 0–2000 ms.

## Experimental candidates: keyguard show and hide duration

- Config IDs: `keyguard_show`, `keyguard_hide`
- Process: `com.android.systemui`
- Runtime callsite guard: `KeyguardViewMediator.handleShowInner` for showing and `handleHide` / `handleStartKeyguardExitAnimation` for hiding.
- Hooked API: `ValueAnimator.setDuration(long)`.
- Only animator duration calls whose stack contains the matching keyguard method are changed.
- Duration is clamped to 0–2000 ms.

These method names are based on AOSP's keyguard mediator flow and are guarded by exact stack-frame matching. Samsung may have changed or replaced these methods, so the controls remain experimental until verified on One UI 8.5.

## Experimental candidates: shade and Quick Settings motion

| Config ID | Panel direction | Root resource name |
|---|---|---|
| `shade_open` | Notification shade opening | `shadeRootView` |
| `shade_close` | Notification shade closing | `shadeRootView` |
| `qs_expand` | Quick Settings expanding | `sec_quick_panel_compose_root` |
| `qs_collapse` | Quick Settings collapsing | `sec_quick_panel_compose_root` |

The pipeline uses Samsung panel-expansion callbacks and `AndroidComposeView.dispatchDraw(Canvas)`. It transforms only the Compose view whose ancestry contains the matching resource ID. Canvas state is saved and restored around the original draw. Only scale and vertical offset are applied; fade, duration, spring, direction and delay remain preview/config values.

## Experimental candidate: notification appearance

- Config ID: `notification_appear`
- Process: `com.android.systemui`
- Parent guard: `NotificationStackScrollLayout`
- Child guard: `ExpandableNotificationRow`
- Hooked API: `ViewGroup.addView(View, int)`
- Before insertion, the row uses the configured start scale, fade amount and vertical offset; after insertion it animates back to its original properties.
- Duration and delay are clamped; curve, spring response and direction anchor are configurable.
- If the exact parent or child classes are missing, the hook is not installed.

This hook is a narrow ColorUp implementation and is not copied from FluidMotion. It is not yet confirmed against the user's exact One UI 8.5 build.

## Experimental candidate: notification dismissal duration

- Config ID: `notification_dismiss`
- Process: `com.android.systemui`
- Row class guard: `ExpandableNotificationRow`
- Method guard: `performRemoveAnimation` returning `long`, with leading parameters `(long, long, float)`.
- Changes only the animation duration argument; all other removal behavior and callbacks remain owned by SystemUI.
- Duration is clamped to 0–2000 ms.
- If the row class or matching method is missing, the hook is not installed.

This signature is based on the AOSP notification-row API and is runtime-checked; Samsung may use a different implementation.

## Preview/configuration-only transitions

The remaining 9 IDs have independent configuration and in-app preview controls, but no system hook is installed for them yet. Their enable switches are unavailable in the UI.

## Configuration bridge

- Provider URI: `content://dev.colorup.config/config`
- Read-only provider; caller package and UID are checked.
- SystemUI and Launcher register content observers and refresh settings after ColorUp saves.
- The app records a runtime heartbeat when a scoped process successfully queries the provider.

## Verification still required

1. Build the APK in GitHub Actions.
2. Enable ColorUp in a compatible legacy Xposed runtime and scope SystemUI and Samsung Launcher. Launcher remains in the scope list for future work, but no active Launcher transition hook is installed; unlock-to-home remains preview-only.
3. Confirm the runtime heartbeat appears in ColorUp.
4. Enable one experimental transition at a time and save.
5. Test keyguard show/hide, app-unlock, shade open/close, Quick Settings expand/collapse, notification appearance and notification dismissal separately.
6. Confirm unrelated animations remain unchanged.
7. Disable each setting and save; verify stock behavior returns.
8. Test on the exact One UI build and record the build fingerprint.

Missing classes or resources must result in an unavailable capability, not guessed replacement hooks.
