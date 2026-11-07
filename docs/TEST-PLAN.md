# One UI 8.5 device test plan

This checklist is required before calling ColorUp stable. CI success proves that the Android source compiles and its unit tests pass; it does not prove that Samsung's runtime classes and resource IDs match.

## Record the test environment

- Device model:
- Android version:
- One UI version:
- Full build fingerprint:
- Root solution/version:
- Xposed-compatible runtime/version:
- ColorUp version:
- Date:

## Initial activation

1. Install the debug APK.
2. Open ColorUp once.
3. Enable ColorUp in the legacy Xposed module manager.
4. Scope SystemUI only (`com.android.systemui`). Samsung Launcher has no implemented hooks in v2.
5. Reboot.
6. Open ColorUp and confirm the runtime heartbeat.
7. Leave all experimental controls disabled for the first launch.

## Transition checks

Test one setting at a time. Record whether it changes the intended transition, whether unrelated transitions change, and whether disabling it restores stock behavior.

| Transition | Parameters currently applied | Result |
|---|---|---|
| App unlock | Duration | Not tested |
| Keyguard show | Duration, only if the guarded callsite exists | Not tested |
| Keyguard hide | Duration, only if the guarded callsite exists | Not tested |
| Screen on | Duration, only if the guarded callsite exists | Not tested |
| Screen off | Duration, only if the guarded callsite exists | Not tested |
| Shade open | Scale, offset, curve, spring, anchor | Not tested |
| Shade close | Scale, offset, curve, spring, anchor | Not tested |
| Quick Settings expand | Scale, offset, curve, spring, anchor | Not tested |
| Quick Settings collapse | Scale, offset, curve, spring, anchor | Not tested |
| Notification appearance | Scale, fade, offset, duration, delay, curve, spring, anchor | Not tested |
| Notification dismissal | Duration | Not tested |

## Pass criteria

- No SystemUI or Launcher crash/restart loop.
- No visible change when a setting is disabled.
- Only the selected transition changes.
- Saving a new config updates the running process without requiring a reboot.
- Disable-all restores stock behavior.
- Reboot with ColorUp disabled returns to stock behavior.

## Recovery

If SystemUI becomes unstable, disable ColorUp in the module manager and reboot. If ColorUp opens normally, use **Disable all hooks & save** before further testing.

## Unsupported transitions

Unlock-to-home, app launch/close, home return, recents open/close and notification expansion remain preview/configuration-only. Do not treat their sliders as active system hooks.


## Version 2 app checks

- Overview/Motion/Presets/Setup navigation; search and category filters.
- Light, dark and wallpaper colours; system bar insets, landscape, large fonts.
- Change a slider, rotate, save; compare saved JSON through the provider.
- Start import, rotate during a slow stream, confirm the retained result and save.
- Export/import legacy backup; reject future schema and oversized files unchanged.
- Load Fluid then Disable all: all enables off, custom duration/curve values retained.
- Preview-only transitions have no enabled system switch.
- Unauthorized external provider queries fail; self/SystemUI reads are accepted.
- Check the packaged `xposedscope` resource and `assets/xposed_init` entry.

These app checks do not validate Samsung animation hooks in an AOSP emulator.
