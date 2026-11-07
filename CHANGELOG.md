# 2.0.0 — Material motion studio

- Rebuilt the settings UI with Material 3, wallpaper colours, light/dark/system themes,
  four destinations, category filters, search, focused editors and sliders.
- Added curve graphs and motion previews sharing runtime easing conventions.
- Preserved drafts and v1 profiles; added explicit support labels and per-transition reset.
- Fixed legacy scope recommendations: SystemUI only. Added setup, runtime candidate
  reports, device/build diagnostics and an immediate disable-all that keeps tuning.
- Removed UI-blocking automatic root export; background JSON document backup/import.
- Kept status notices from blocking navigation or the disable-all control.
- Rejected oversized/future backups and normalized invalid export values.
- Fixed spring endpoint discontinuity; runtime config reads moved off the UI thread,
  bridge failure disables hooks, and disabled hooks avoid expensive per-frame work.
- Added reproducible Gradle wrapper and regression/UI checks.

Engine status: 11 experimental candidates; 7 transitions remain preview-only.
Galaxy A35 5G / One UI 8.5 hardware validation remains pending.

# Changelog

## 1.0.0-rc1
- First functional release candidate with 11 guarded experimental transition hooks.
- Added screen-on/off and keyguard show/hide duration routing.
- Added install, recovery and One UI 8.5 device test documentation.
- Seven catalogued transitions remain preview/configuration-only pending verified hooks.

## 0.11.0
- Added guarded screen-on and screen-off duration routing through keyguard mediator call stacks.

## 0.10.0
- Added guarded keyguard show/hide duration routing through keyguard mediator call stacks.

## 0.9.0
- Added a guarded notification dismissal duration hook using the row's remove-animation API.

## 0.8.0
- Expanded notification appearance animation to use scale, fade, offset, delay, curve, spring and direction settings.

## 0.7.0
- Applied per-panel motion curves, spring response and direction anchors to shade and Quick Settings hooks.
- Kept unlock-to-home preview-only after confirming FluidMotion's third CellLayout argument is a delay, not a duration.

## 0.6.0
- Added guarded notification-row appearance animation.
- Added experimental notification appearance controls and updated preset validation.

## 0.5.0
- Added guarded unlock-to-home duration hook in Samsung Launcher.
- Added app-unlock duration hook in SystemUI.
- Added separate notification-shade open/close and Quick Settings expand/collapse canvas transforms.
- Added live config refresh through the read-only provider.
- Added one-tap disable-all action.
- Added import validation so only implemented hook IDs can be enabled.
- Remaining transition controls stay preview/config-only.

## 0.4.0
- Added separate shade and Quick Settings panel motion settings.

## 0.3.0
- Added the first reference-derived app-unlock duration hook.

## 0.2.0
- Added presets, motion preview, JSON import/export and the read-only config bridge.

## 0.1.0
- Initial Android project, transition catalog and settings editor.
