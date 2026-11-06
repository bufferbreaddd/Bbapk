# ColorUp

A per-transition animation configuration studio for Samsung One UI, inspired by fluid ColorOS-style motion.

**Compatibility target:** One UI 8.5, device-model agnostic by design. Actual support is determined by the installed hook runtime and the tested One UI build.  
**App:** ColorUp · **Current preview:** 0.7.0

## What works in this preview

- 18 individually stored transition profiles across lock screen, power, apps/tasks, SystemUI and notifications.
- Independent enable, duration, curve, scale, fade, vertical offset, delay, spring damping, spring stiffness and direction values.
- Fluid, Crisp, Gentle and Stock starting presets.
- In-app motion preview for editing values.
- JSON import/export.
- Read-only configuration provider restricted to SystemUI and Samsung Launcher callers, with live config refresh.
- Experimental notification appearance animation for guarded notification-row insertions.
- Experimental app-unlock duration hook, guarded by Samsung's keyguard controller call stack.
- Experimental notification-shade open/close and Quick Settings expand/collapse scale/vertical-motion hooks, guarded by Samsung expansion callbacks and exact Compose root IDs.
- Legacy Xposed entry point scoped to SystemUI and Samsung Launcher.
- Root export to `/data/adb/colorup/config.json` for backup/inspection.

## Important: animation engine status

**0.7.0 is an experimental preview, not a complete system animation engine.** Hooks can now affect app-unlock duration, notification-row appearance, notification-shade open/close and Quick Settings expand/collapse scale, vertical offset, curve, spring response and direction anchor. They are guarded by runtime class/callsite/resource checks, but have not yet been tested on a retail One UI 8.5 device. The remaining 12 transitions are editor/preview-only. See [hook status](docs/HOOK-STATUS.md) for exact boundaries and verification requirements.

The reference FluidMotion APK is analysed by a GitHub Actions workflow. Only a technical inventory of hook sites and setting identifiers is committed; decompiled source is not redistributed. See [the current inventory](docs/reference-analysis/FLUIDMOTION-HOOK-INVENTORY.md) and [hook status](docs/HOOK-STATUS.md).

## Build

Open the repository in Android Studio with JDK 17 and Android SDK 36, then build the `app` module. GitHub Actions runs the Android build and publishes a `ColorUp-debug` artifact from successful runs.

## Install/runtime notes

See [the install and recovery guide](docs/INSTALL.md) for enabling the module, first test and rollback.


The APK is a normal launcher app and a legacy Xposed-compatible module (API 82). Vector's upstream project documents support for both legacy Xposed modules and modern libxposed modules, so ColorUp's legacy entry point is compatible in principle. It still needs to be enabled in the framework manager and scoped to the target processes. Root by itself is not enough. The current hook scope is:
- `com.android.systemui`
- `com.sec.android.app.launcher`

The module is intentionally detection/config-bridge only until exact One UI method signatures are verified. Do not expect animation changes from this preview.

## Safety and development rules

- All transition toggles default off.
- Imported values are clamped to safe ranges and unknown transition IDs are ignored.
- The provider is read-only and checks the caller package allowlist.
- No unverified method hooks, system APK patches or boot-image changes are applied.
- Keep stock behavior as the fallback and provide a reliable disable/recovery route.
