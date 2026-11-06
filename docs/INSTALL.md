# Installing the ColorUp preview

## Build artifact

1. Open the repository's [GitHub Actions](https://github.com/bufferbreaddd/Bbapk/actions).
2. Open the latest successful **Build ColorUp** run.
3. Download the `ColorUp-debug` artifact and extract the APK.
4. Install the APK on the test device.

The artifact is a debug build. It is not a Play Store release.

## Enable the module

1. Open the installed ColorUp app once.
2. In Vector's module manager, enable ColorUp.
3. Scope it to:
   - `com.android.systemui`
   - `com.sec.android.app.launcher`
4. Restart the device so the module is loaded into those processes.
5. Open ColorUp again. A successful provider connection should show a runtime heartbeat.

ColorUp uses the legacy Xposed API. The root manager alone is not the module runtime; the module manager must actually load the Xposed entry point.

## First test

Enable one experimental control at a time:
- App unlock duration
- Notification shade open/close
- Quick Settings expand/collapse
- Notification appearance

Save after each change and test the corresponding transition. Only the parameters listed in [hook status](HOOK-STATUS.md) currently affect One UI. The rest are configuration and in-app preview controls.

## Recovery

- Use **Disable all hooks & save** in ColorUp to return its settings to stock values.
- If SystemUI becomes unstable, disable ColorUp in the module manager and restart the device.
- All experimental hooks are off by default.
