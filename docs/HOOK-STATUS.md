# ColorUp hook status

Last reviewed against the repository's FluidMotion reference APK. This is a source-level status report, not proof of behavior on a retail One UI 8.5 build.

## Implemented candidate: app unlock duration

- Config ID: `unlock_to_app`
- Process: `com.android.systemui`
- Reference-derived controller class: `com.android.systemui.keyguard.KeyguardSecLegacyUnlockAnimationControllerImpl`
- Hooked API: `android.animation.ValueAnimator.setDuration(long)`
- Guard: the hook changes the argument only when the current call stack contains the exact controller class.
- Range: 0–2000 ms.
- Enable state and duration are loaded through the read-only ColorUp provider.
- A ContentObserver refreshes the in-memory configuration after Save, so this setting does not require restarting SystemUI.
- If the controller class is absent, the hook is not installed and a diagnostic is written to the Xposed log.

This target and call pattern were found in the supplied FluidMotion APK. They have **not** yet been confirmed against the user's exact One UI 8.5 build. Treat the hook as experimental until device testing confirms it.

## Preview/configuration-only transitions

The other 17 IDs have independent configuration and in-app preview controls, but no system hook is installed for them yet. Their enable switches are intentionally unavailable in the UI. Their values are still importable/exportable and can be previewed inside ColorUp.

## Configuration bridge

- Provider URI: `content://dev.colorup.config/config`
- The provider is read-only.
- It accepts only ColorUp itself, SystemUI and Samsung Launcher, and verifies the calling package and UID.
- SystemUI registers a content observer and refreshes the app-unlock configuration when ColorUp saves.

## Verification still required

1. Build the APK in GitHub Actions.
2. Load it with a compatible legacy Xposed API runtime and scope SystemUI.
3. Confirm the provider heartbeat appears in ColorUp.
4. Enable only `unlock_to_app`, save, and test unlock-to-app behavior.
5. Confirm other SystemUI animators retain their original durations.
6. Disable the setting and save; verify the original behavior returns.
7. Test with the exact One UI build and record the build fingerprint.

If the target class is missing or behavior differs, keep the hook disabled and update the capability record rather than guessing at a replacement method.
