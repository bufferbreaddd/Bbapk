# FluidMotion reference APK: configuration caveat

The supplied FluidMotion APK is a background Xposed-style module and does not necessarily expose a launcher activity. Do not instruct users to open it from the app drawer.

The strings extracted from its DEX indicate configuration keys for unlock, app-unlock and shade motion. The presence of keys does not prove that a settings UI exists or establish how settings are edited.

Before implementing compatibility or copying its configuration model, inspect:
- AndroidManifest.xml for launcher/exported activities, services and receivers
- Xposed/LSPosed entry-point metadata and scope declarations
- SharedPreferences / file / ContentProvider / broadcast access patterns
- Hook code to identify which process reads each setting and when values are applied

For ColorUp, provide its own explicit configuration UI and a well-defined settings bridge to the hook layer. Do not assume FluidMotion's internal preferences are user-editable.
