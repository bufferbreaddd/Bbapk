# ColorUp 2

An independent motion editor and experimental Xposed module for Samsung One UI.
**Device target:** Galaxy A35 5G / One UI 8.5. **App version:** 2.0.0 (20).
Exact reported firmware: `SM-A356B`, `BP4A.251205.006.A356BXXSCDZH1` (Android 16).
See [device target](docs/DEVICE-TARGET.md) for the validation boundary.
The package ID remains `dev.colorup`; existing configuration is migrated in place.

## Version 2

- Material 3 interface, wallpaper-derived Material You colours, system/light/dark themes.
- Overview, searchable categorized Motion library, Presets, and Setup tabs.
- A focused editor for each of 18 independent transition profiles, with sliders,
  curves, spring settings, anchor selection, curve graph and motion preview.
- Explicit support labels: duration-only, gesture-driven, full notification motion,
  or preview-only. Controls are never represented as live features without hooks.
- Draft preservation across navigation, rotation and app restarts; explicit Save.
- Immediate disable-all keeps your tuning and returns enabled hooks to stock.
- JSON backup/import, bounded to 256 KB; invalid/future versions are rejected.
- Correct **legacy Xposed scope metadata** recommending **SystemUI only**.
- Setup instructions, last connection, installed hook candidates, runtime version,
  and copyable device/build diagnostics.
- Save no longer runs a blocking root shell. A restricted content provider supplies
  configuration and notifies the runtime. File backup uses the Android picker.

## Actual engine coverage

There are **11 experimental transition candidates**, not 18 verified Samsung hooks:

| Transitions | Runtime controls |
| --- | --- |
| Unlock to app, keyguard show/hide, screen on/off, notification dismiss | Duration |
| Shade open/close, Quick Settings expand/collapse | Scale, vertical offset, easing/spring and scale anchor; follows panel progress |
| Notification appearance | Duration, delay, scale, fade, vertical offset, easing/spring and anchor |
| Unlock to home, app launch/close, home return, recents open/close, notification expansion | Preview/configuration only |

All hooks default off. Missing classes/methods safely leave candidates unavailable.
An installed candidate or a successful config connection **does not verify that a
Samsung animation invokes that candidate on a particular firmware**. No retail
Galaxy A35 5G test has been performed for this release. Exact build diagnostics and
on-device tests remain necessary for engine validation. See [hook status](docs/HOOK-STATUS.md).

## Enable the module

1. Install the APK and open ColorUp once.
2. Enable ColorUp in LSPosed/Vector, with **System UI (`com.android.systemui`)** scoped.
3. Reboot after installation, updates or scope changes.
4. Open Setup to inspect the last connection and installed candidates.
5. Enable one experimental transition or load a preset, then **Save changes**.

Samsung One UI Home (`com.sec.android.app.launcher`) and Android System Framework
are not required: there are no implemented hooks for those processes in this release.
The manager controls scope; ColorUp cannot silently grant it to itself.

See [installation/recovery](docs/INSTALL.md) and [device testing](docs/TEST-PLAN.md).

## Build and verify

JDK 17, Android SDK 36 and Build Tools 36.0.0:

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
./gradlew :app:connectedDebugAndroidTest
```

The second command requires an Android emulator or connected device and tests the
app UI, not Samsung/Xposed hook behavior. The debug APK is written to
`app/build/outputs/apk/debug/app-debug.apk`. GitHub Actions publishes build artifacts
for main and pull requests. Preserve your existing signing key for in-place updates;
a separately signed debug APK may require backup and reinstall.

The repository includes a FluidMotion reference APK and technical hook inventory;
decompiled reference source is not redistributed.
