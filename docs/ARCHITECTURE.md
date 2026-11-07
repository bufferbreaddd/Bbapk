# ColorUp v2 architecture

## App

Kotlin Android Views with AppCompat and Material 3; DynamicColors applies wallpaper
colours where supported. `MainActivity` owns navigation, focused editors and drafts;
`StudioViews` draws motion/curve previews. `HookCapabilities` documents live fields.

`config_json` is the saved runtime snapshot in private `colorup` preferences.
`draft_json` retains unsaved changes without exposing them to system hooks. Drafts
also survive activity recreation. The explicit Save action serializes schema 2 and
notifies `content://dev.colorup.config/config`. Disable-all immediately saves all
enables off and keeps per-transition values. File operations use a background worker.

## Config/runtime

`ConfigCodec` accepts legacy schema 1/missing-version backups and schema 2, rejects
future versions and files over 256 KB, ignores unknown IDs and clamps numeric values.
Encoding normalizes all 18 known IDs and invalid enums/nonfinite numeric fields.
`MotionMath` is shared by previews, notification interpolation and gesture transforms.
Spring curves normalize their resting endpoint to one.

`ConfigProvider` allows only the verified calling package/UID for SystemUI or ColorUp
itself. Public mutation APIs are rejected. SystemUI queries internally record the
last connection, runtime version and registered candidate IDs for diagnostics.
A connection is not evidence of animation invocation.

The legacy Xposed entry only bootstraps in `com.android.systemui`. Config reads and
change observation use a dedicated worker; UI/draw callbacks read in-memory atomics.
Invalid/unavailable configuration clears enables. Hooks are installed once with
runtime class/method guards, and missing targets leave stock behavior intact.
Disabled duration hooks avoid stack walking. Disabled panel hooks avoid resource
ancestry lookups. Gesture motion does not replace Samsung gesture timing.

## Scope and compatibility

Legacy metadata `xposedscope` references `@array/xposed_scope`, containing SystemUI.
The pre-existing scope list mirrors it. No active Launcher/system_server hook exists.
Method registration, class detection and a config heartbeat are distinct from proof
of interception on the exact Samsung firmware. See HOOK-STATUS and TEST-PLAN.
