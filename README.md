# ColorUp

A root-powered animation customization project for Samsung One UI, inspired by ColorOS-style system motion.

**Project codename:** ColorUp  
**Target device for initial development:** Samsung Galaxy A35 5G (SM-A356B), Android 16 / One UI 8  
**Repository:** this project lives in the existing `Bbapk` repository; the Android app itself will be named ColorUp.

## Goal

Build a user-facing animation studio with granular, independently configurable controls and reversible profiles. The aim is not to change Android's global animation scales, but to customize the actual transitions and motion used by SystemUI and app/task transitions wherever the platform exposes a reliable hook.

## Scope

The intended control surface includes:

- Lock and unlock transitions
- Unlock-to-home and unlock-into-app transitions
- App launch and app close
- Home/launcher return and task switching / recents
- Quick Settings panel open, close, expand and collapse
- Notification shade open, close and row/item motion
- Notifications appearing, dismissing and expanding
- Screen on/off and keyguard transitions
- Per-animation duration, interpolator/curve, spring response, scale, translation, fade, stagger, direction/anchor and delay where technically supported
- Per-feature enable switches, live preview, presets, profiles, import/export, reset and safe mode

Not every transition is controlled by the same Android component. Some are owned by SystemUI, some by Launcher, and others by framework/window-manager code. Each hook must be identified and tested independently; unsupported controls must be shown as unavailable rather than pretending to work.

## Architecture direction

- Kotlin Android application for the configuration UI.
- A privileged hook layer for SystemUI / framework / Launcher integration.
- A versioned settings contract shared between the UI and hook layer.
- Explicit capability detection by Android version, One UI version and target package.
- Atomic settings writes, backup/restore, and a safe-mode / disable-all path.
- No direct modification of system APKs or boot images as the default installation path.

The initial compatibility target is the user's rooted Galaxy A35 (SM-A356B, Android 16 / One UI 8, KernelSU with Zygisk Next). Compatibility must be verified on-device; root alone does not guarantee that a particular hook framework or SystemUI method is available.

## Reference APK

The supplied `FluidMotion.apk` is a technical reference only, not the ColorUp codebase. Its DEX contains an Xposed entry point and hook classes for unlock fly-in, shade fly-in and app-unlock zoom. Extracted setting names include:

- Unlock: enabled, duration, scale, fade, stagger, bounce, anchor
- App unlock: zoom scale and duration
- Shade: scale, Quick Settings, notifications and drop controls

Its embedded strings indicate that some spring/row-delay/fade settings are shared between unlock and shade animations, and that changes apply on the next unlock or shade pull. This is a useful starting point, but it does **not** provide the full system-wide per-transition control requested for ColorUp.

## Development principles

1. Never claim a control works until its hook is implemented and verified on a real device.
2. Keep every animation parameter independent unless the underlying platform forces a shared parameter; clearly label shared controls.
3. Preserve stock behavior when a feature is disabled.
4. Make changes reversible and provide a safe recovery path.
5. Keep device-specific code isolated behind capability checks.
6. Document tested One UI / Android builds and known limitations.

## Status

Planning and reference inspection. No ColorUp APK has been built yet.
