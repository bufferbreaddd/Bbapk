# ColorUp

A root-powered, per-transition animation customization project for Samsung One UI, inspired by ColorOS-style system motion.

**Compatibility target:** One UI 8.5. Device model is not restricted; support is determined by Android/One UI build and available hook runtime.  
**App:** ColorUp · **Current source milestone:** 0.1.0

## Goal

Build a user-facing animation studio with granular, independently configurable controls. This is not a global animation-scale tweak: each transition has its own settings record.

## Initial transition catalogue

- Lock screen: unlock to home/app, show/hide keyguard
- Power: screen on/off
- Apps & tasks: app launch/close, return home, open/close recents
- SystemUI: open/close notification shade, expand/collapse Quick Settings
- Notifications: appear, dismiss, expand

Each transition has separate enable, duration, curve, scale, fade and vertical-offset values. The current UI exposes those controls independently.

## Source layout

- `app/`: Kotlin Android app and settings editor
- `app/src/main/java/dev/colorup/hooks/`: Xposed-compatible runtime entry point
- `docs/ARCHITECTURE.md`: process ownership and hook implementation policy
- `FluidMotion.apk`: user-supplied technical reference, not ColorUp code

## Build

Open the repository in Android Studio with JDK 17 and Android SDK 36. Build the `app` module. The app is configured as an Xposed/LSPosed module and also exposes a normal launcher activity.

## Current status: 0.1.0 scaffold

The app has an independent settings editor and writes a versioned JSON snapshot to `/data/adb/colorup/config.json` using root. The runtime entry point currently performs package/config detection only. **No animation transition hooks are active yet.** This is deliberate: each One UI 8.5 method must be identified and verified before a setting is allowed to affect SystemUI or Launcher.

Root alone does not provide Xposed method hooks. A compatible hook runtime is a separate requirement. The initial scope file targets SystemUI and Samsung Launcher.

## Development rules

1. Never label a transition supported until its hook is implemented and tested on-device.
2. Keep settings independent unless the platform forces a shared parameter.
3. Disabled means stock behavior.
4. Keep a kill switch and a recovery route.
5. Record exact One UI/Android builds tested and known limitations.
