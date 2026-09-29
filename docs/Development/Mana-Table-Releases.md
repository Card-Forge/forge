# Packaging and releases

The current package script builds **Windows x64** on Windows x64. It bundles
Electron, a Java runtime, `forge-engine.jar`, and the required Forge resources.
Other platform packaging is not implemented.

## Prepare a build

1. Work from a reviewed commit. For a new beta number, update `forge-desktop/package.json`,
   both project version fields in its lockfile, the two version labels in
   `renderer/index.html`, and `BETA.md`. Keep player-visible changes in the guide.
2. From the repository root, run `mvn -pl forge-api -am verify` with JDK 17+.
   Packaging uses the existing JAR; it does not build Java or detect a stale JAR.
3. In `forge-desktop`, run `npm ci`, `npm run doctor`, `npm run check`,
   `npm run test:unit`, `npm run test:engine`, and the relevant UI tests. Use the
   full UI suite for a release candidate.
4. Set `JAVA_HOME` to the JDK containing `bin/jlink.exe`, then run `npm run package`.
   `FORGE_JAVA` selects an engine executable, not the JDK used by `jlink`.

The script stages files under ignored `.tools/desktop-beta-<timestamp>` and writes
a new `dist/ManaTable-<version>-<timestamp>/Mana Table-win32-x64` directory.
Packages include only the explicit host-file list and the renderer, so update
that list when extracting a new runtime module. Test helpers and development
dependencies are not shipped.

## Data preservation

If `dist/latest-beta.json` exists, packaging copies `UserData/decks` and
`UserData/art` and `UserData/preferences.json` from that previous package to the
new one. Every copied deck JSON is verified with SHA-256; play preferences are
verified byte for byte. The manifest changes only after packaging and copying
succeed; older packages remain in place. It does not copy development `.data`,
live match state, or every Electron preference.

Finish saving and close the player's old app before a release handoff so edits
made after the copy do not remain only in the old package. Do not delete or patch
the old package in place. To launch an older build, run its executable directly;
its profile contains the data last saved in that build.

## Verify and hand off

Use the [packaged smoke commands](Mana-Table-Testing.md#packaged-verification).
Check the expected version and the app's bundled Java by leaving `FORGE_JAVA`
unset. `Launch Mana Table.cmd` reads the generated manifest; the legacy
`Launch Workshop.cmd` delegates to it. A source checkout has no manifest until a
package is built.

Distribute the **entire package directory**, including runtime/resource folders,
`START-HERE.md`, `SOURCE.txt`, and license notices. A local package may contain the
previous player's copied `UserData`; use a clean build workspace for distributable
artifacts, and retain personal packages separately. Never upload player profiles,
test profiles, or cached artwork as a release artifact.

Preserve GPL and third-party notices and provide the corresponding source revision
with a distribution. `SOURCE.txt` points to this fork and the upstream engine;
record the exact commit in release notes. Creating a local package does not publish
a GitHub release, upload binaries, or modify the source branch.
