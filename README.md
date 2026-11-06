# ColorUp

A per-transition animation configuration studio for Samsung One UI, inspired by fluid ColorOS-style motion.

**Compatibility target:** One UI 8.5, device-model agnostic by design. Actual support is determined by the installed hook runtime and the tested One UI build.  
**App:** ColorUp · **Current preview:** 0.2.0

## What works in this preview

- 18 individually stored transition profiles across lock screen, power, apps/tasks, SystemUI and notifications.
- Independent enable, duration, curve, scale, fade, vertical offset, delay, spring damping, spring stiffness and direction values.
- Fluid, Crisp, Gentle and Stock starting presets.
- In-app motion preview for editing values.
- JSON import/export.
- Read-only configuration provider restricted to SystemUI and Samsung Launcher callers.
- Legacy Xposed entry point scoped to SystemUI and Samsung Launcher.
- Root export to `/data/adb/colorup/config.json` for backup/inspection.

## Important: animation engine status

**0.2.0 is a configuration-studio preview, not a complete system animation engine.** The app can edit, preview, import, export and save configuration. The Xposed bootstrap can connect to the read-only configuration provider. It does not yet alter One UI's real system transitions. No transition should be treated as supported until its owner method has been identified, hooked and tested on a real One UI 8.5 device.

The reference FluidMotion APK is analysed by a GitHub Actions workflow. Only a technical inventory of hook sites and setting identifiers is committed; decompiled source is not redistributed. See [the current inventory](docs/reference-analysis/FLUIDMOTION-HOOK-INVENTORY.md).

## Build

Open the repository in Android Studio with JDK 17 and Android SDK 36, then build the `app` module. GitHub Actions runs the Android build and publishes a `ColorUp-debug` artifact from successful runs.

## Install/runtime notes

The APK is a normal launcher app and a legacy Xposed-compatible module. Root by itself is not enough: a compatible Xposed API runtime must load the module and its configured scope. The current hook scope is:
- `com.android.systemui`
- `com.sec.android.app.launcher`

The module is intentionally detection/config-bridge only until exact One UI method signatures are verified. Do not expect animation changes from this preview.

## Safety and development rules

- All transition toggles default off.
- Imported values are clamped to safe ranges and unknown transition IDs are ignored.
- The provider is read-only and checks the caller package allowlist.
- No unverified method hooks, system APK patches or boot-image changes are applied.
- Keep stock behavior as the fallback and provide a reliable disable/recovery route.
