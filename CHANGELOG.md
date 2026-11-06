# Changelog

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
