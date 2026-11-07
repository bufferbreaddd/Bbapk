# Install, connect and recover

Target: Galaxy A35 5G / One UI 8.5. ColorUp uses the legacy Xposed API (82),
so the installed framework must support legacy modules. Root alone is insufficient.

## Install/update

1. Export your existing profiles before replacing an older build.
2. Install ColorUp 2 and open it once. The package is still `dev.colorup`.
3. Enable ColorUp in your LSPosed/Vector manager.
4. Confirm **System UI (`com.android.systemui`)** is selected in scope. This is the
   module's declared legacy recommendation, not a normal Android permission dialog.
   No Launcher or System Framework scope is needed for the implemented hooks.
5. Reboot. Open Setup and inspect connection/runtime information.
6. Start with one experimental duration control; save and test its exact transition.

Settings are migrated from v1 JSON automatically. Preview-only transitions cannot be
enabled. Presets/imports stage a draft until Save. Disable all writes immediately
and preserves motion values. The saved configuration lives in private app preferences;
root access and `/data/adb/colorup/config.json` are not needed in v2.

Different debug builds may be signed with different debug keys. Android rejects
an in-place update with an incompatible signature. Export first; if a reinstall is
necessary, import the backup afterwards, re-enable the module and reboot. Do not
uninstall before preserving settings you want to keep.

## Connection and support

A last connection is a record, not a live activation check. The last runtime version
can be older than the app until you reboot. Installed candidates report successful
hook registration, not successful animation interception. The diagnostics report
includes model, build, fingerprint, runtime version and candidate IDs; copy it when
reporting compatibility issues.

## Recovery

- In ColorUp, use Overview/Setup → Disable all hooks. The provider notifies the runtime.
- If an animation remains altered, disable ColorUp in the framework manager and reboot.
- If SystemUI cannot open, use the framework's own module-disable/recovery procedure.
- No boot images or system APKs are patched by ColorUp.

If a provider read fails or configuration is invalid, runtime settings fall back to
all hooks off. Existing enabled settings are not retained indefinitely after bridge
failure. Samsung method/class differences require firmware-specific verification;
missing methods are not replaced with guessed hooks.
