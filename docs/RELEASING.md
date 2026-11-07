# Publishing beta and release-candidate builds

The Build ColorUp workflow publishes a GitHub prerelease after both assembly and
Android UI checks pass on pushes to main or the current v2 development branch.
Pull-request runs never publish.

For the next beta:

1. Increment Android versionCode (and versionName for a new app build).
2. Set release-tag.txt to a new tag, e.g. v2.0.0-beta.2 or v2.0.0-rc.1.
3. Add matching release notes at docs/releases/<tag>.md, including compatibility limits.
4. Push the changes. Successful checks publish the APK, checksum and installation notes.

Existing releases are left intact. A normal source push with the same release tag
builds/tests but does not overwrite a release. Stable tags are rejected by this
prerelease workflow; stable publishing needs a separate explicit release decision
and a permanent signing key. Current beta APKs use debug signing and may require
backup/reinstall when signing keys differ between builds.

A failed asset upload can leave an unpublished draft; inspect and repair that draft
before reusing its tag. Published beta tags/assets should remain immutable; use the
next beta number for changes.
