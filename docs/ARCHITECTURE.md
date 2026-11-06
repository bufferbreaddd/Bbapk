# ColorUp architecture

## Configuration

The app stores a versioned JSON snapshot at `/data/adb/colorup/config.json`. Every transition has a stable ID and independent values for enable, duration, curve, scale, fade and vertical offset. Unknown IDs and invalid values must be ignored by future hook implementations.

## Runtime ownership

- SystemUI: shade, Quick Settings, keyguard and notification transitions.
- Samsung Launcher: home, app/task and recents transitions where Launcher owns them.
- Framework/window manager: screen and task transitions where the platform owns them.

A control is only marked supported after its owning method and behavior have been verified on the current One UI build.

## Hook policy

1. Identify the owning process and method on the exact build.
2. Verify invocation timing and parameter semantics.
3. Implement a narrow hook with bounds and disabled-by-default configuration.
4. Test enable/disable, repeated transitions, rotation, multi-window, accessibility and low-memory behavior.
5. Keep a kill switch and fall back to stock behavior on invalid configuration.
