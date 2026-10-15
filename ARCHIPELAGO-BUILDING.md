# Building, debugging and releasing Forge Archipelago

How to build and debug the Android app locally, and how to put together a release for
Windows/Linux and Android (including `assets.zip`). All commands run from the **root of your clone**
(the folder with the top-level `pom.xml`).

## How Android Forge gets its files

The APK (~12 MB) only contains code. Everything in `forge-gui/res` (adventure maps, UI layouts,
sprites, translations, card scripts, ...) is a separate download: on first start the app downloads
`assets.zip` from the **latest release of this fork** and unpacks it on the device in
`Android/obb/<package>/Forge/res`.

So:

- Java changes need a new **APK**.
- Changes in `forge-gui/res` need a new **`assets.zip`** (for players) or a **res push** (on your own device, see below).
- If code depends on a new/changed `res` file (for example a new UI layout), ship both, or the app
  crashes looking for the file.

## 1. One-time setup

1. **JDK 17.** In IntelliJ: *File > Project Structure > Project > SDK* = a JDK 17.
2. **Android SDK** with *build-tools 35.0.0* and *platform 35* (Android Studio's SDK Manager is the easiest way).
3. **`ANDROID_HOME` system environment variable** pointing at the SDK folder
   (Windows default: `C:\Users\<USER>\AppData\Local\Android\Sdk`). Restart IntelliJ afterwards.
4. **Card-Forge's android-maven-plugin 4.6.3.** Download `android-maven-plugin-4.6.3.jar` and
   `android-maven-plugin-4.6.3.pom` from
   https://github.com/Card-Forge/android-maven-plugin/releases/tag/4.6.3, then run this in the folder you saved them to:
   ```
   mvn install:install-file -Dfile=android-maven-plugin-4.6.3.jar -DpomFile=android-maven-plugin-4.6.3.pom
   ```
   This fork of the plugin is needed on Windows (it avoids the "command line is too long" error).
   It installs into the local Maven repository that the build uses. If you changed IntelliJ's
   *Settings > Build Tools > Maven > Local repository*, run the command with
   `-Dmaven.repo.local=<that folder>` as well.
5. **Phone:** enable *Developer options > USB debugging* and connect it (`adb devices` should list it).
6. **`PushResToDevice.java`** must be at `forge-gui-android/tools/PushResToDevice.java`. It is part
   of the repository, so a normal clone already has it. See [PushResToDevice](#pushrestodevice).

## 2. IntelliJ run configurations

Create these with *Run > Edit Configurations > + > Maven*. Settings that apply to **all** of them:

- **Working directory:** the root folder of your clone (shown as **forge** in the dropdown).
  Pick it with the folder button; IntelliJ stores it as `$PROJECT_DIR$` for you.
- **Profiles** field: leave **empty** (the profiles are passed with `-P` in the command line).
- **Environment variables** (*Modify options > Environment variables*): add one variable using the
  table editor (the icon at the end of the field), not by typing into the single-line field, otherwise
  IntelliJ escapes the `=` signs and Java refuses to start:

  | Name | Value |
  |---|---|
  | `JDK_JAVA_OPTIONS` | `--add-exports=java.base/sun.security.pkcs=ALL-UNNAMED --add-exports=java.base/sun.security.x509=ALL-UNNAMED --add-exports=java.base/sun.security.util=ALL-UNNAMED -Dfile.encoding=UTF-8` |

  The `--add-exports` options are needed on JDK 17 for the plugin's APK signing code.
  `-Dfile.encoding=UTF-8` keeps file names with special characters (for example `Chandra’s Tome`)
  intact when the release builds pack `res`; IntelliJ may otherwise run Maven with the console code page
  (`Picked up JAVA_TOOL_OPTIONS: -Dfile.encoding=IBM437`), and the desktop installer build then fails.
  Add `ANDROID_HOME` here too only if you did not set it system-wide.
- Do **not** add `-f ../pom.xml`. If IntelliJ ever adds a POM file setting by itself, it must be `pom.xml`.

| Name | Command line (*Run* field) | Extra settings |
|---|---|---|
| **forge-android** | `clean verify -P android-dev-build -pl forge-gui-android -am -DskipTests` | For debugging, add property `android.release` = `false` (*Modify options > Java options > Properties*): a debug build keeps the debugging information that the default release-mode build strips, and is debuggable |
| **forge-android-push-res** | `org.codehaus.mojo:exec-maven-plugin:3.4.1:exec -N -Dexec.executable=java -Dexec.args=forge-gui-android/tools/PushResToDevice.java` | |
| **forge-android-deploy** | `initialize android:deploy-apk android:run -P android-dev-build -pl forge-gui-android` | Property `android.deployapk.filename` = `${project.build.directory}/${project.build.finalName}-aligned-debugSigned.apk` (type it literally). For debugging, add property `android.run.debug` = `true`. *Before launch*: **forge-android**, then **forge-android-push-res** |
| **forge-android-assets** | `clean package -P android-release-build -pl forge-gui-android -am -DskipTests` | |
| **forge-desktop-release** | `clean install -P windows-linux -DskipTests` | |

**Where is "Before launch"?** It is hidden by default: open the configuration, click *Modify options*
and choose *Add before launch task*. A *Before launch* list appears at the bottom of the dialog; use
*+ > Run Another Configuration* to add the entries in the order listed above.

## 3. Build and run on a device

Run **forge-android-deploy**. It builds the APK (**forge-android**), syncs your `forge-gui/res` to the
phone (**forge-android-push-res**), installs the app and starts it.

- The dev app installs as **Forge (dev)** (package `forge.app.dev`), next to a normal Forge install.
- On a brand-new install, let the app download its assets once (that needs an `assets.zip` on the
  latest release) before pushing res; the push only updates an existing installation.
- Just building without a device: run **forge-android**. The installable APK is
  `forge-gui-android/target/forge-dev-android-<version>-aligned-debugSigned.apk`
  (the other `.apk` next to it is unsigned).

## 4. Debugging on a device

1. Add the two debugging properties from the table (`android.release` = `false` on
   **forge-android**, `android.run.debug` = `true` on **forge-android-deploy**) and run **forge-android-deploy**.
2. Allow any permissions the app asks for. It then shows *"Waiting for debugger"*.
3. In IntelliJ press `Ctrl+Shift+A`, search for **Attach Debugger to Android Process**, pick your device
   and the process **`forge.app.dev`**.

Remove the two properties again when you are not debugging: a debug build is slower, and **must not be
released** (see the check at the start of [Making a release](#5-making-a-release-windowslinux--android)).

## 5. Making a release (Windows/Linux + Android)

> **Before you build a release: remove the debugging properties.** Open your run configurations and
> delete `android.release` = `false` from **forge-android** and `android.run.debug` = `true` from
> **forge-android-deploy** (*Modify options > Java options > Properties*). A release built with them is
> a slower debug build that waits for a debugger on start-up and must not be shipped to players.

Build in this order, and copy each result to a folder outside `target/` before the next step,
because every build starts with `clean`.

1. **Windows/Linux:** run **forge-desktop-release** (this is the same build upstream's CI uses).
   Upload from `forge-installer/target/`: `forge-installer-<version>.jar` and `forge-installer-<version>.tar.bz2`.
   This also creates `forge-gui-desktop/target/CHANGES.txt`, which step 3 needs.
2. **Android APK:** run **forge-android** (with the debugging properties removed, see above).
   **Check the build output** for the line `Generating release apk.`; if it says `Generating debug apk.`,
   the `android.release` = `false` property is still set: remove it and build again. Then upload
   `forge-gui-android/target/forge-dev-android-<version>-aligned-debugSigned.apk`.
3. **`assets.zip`:** run **forge-android-assets**. Upload `forge-gui-android/target/assets.zip` (~190 MB).
4. Create the GitHub release on https://github.com/BramTeurlings/forge-archipelago/releases and attach the files.

**Every release needs `assets.zip`**, desktop-only releases included: Android always downloads it from
`releases/latest/download/assets.zip` (see `ARCHIPELAGO_ASSETS_URL` in `ForgeConstants.java`). Do not
attach `version.txt`/`build.txt` from the Android build: the app would then also look for an APK update
named `forge-android-<version>-signed-aligned.apk`.

**Asset updates:** all builds report the upstream development version (for example `2.0.16-SNAPSHOT`)
until the next upstream merge, so the version string alone can't tell the phone that a release has new
assets. After downloading assets, the app also saves the APK's own `build.txt` in
`assets/apkbuild.txt`. When a different APK is installed, the app offers the `assets.zip` download once
(*Download* / *Ignore* / *Exit*; *Ignore* is remembered for that APK). That's why every release needs a
new APK *and* a matching `assets.zip`: players only get new `res` files after installing a new APK.
A dev build deployed with **forge-android-deploy** does not ask: **forge-android-push-res** also writes
the new APK's `build.txt` to `apkbuild.txt`. If you install a dev build some other way and the app asks,
choose *Ignore*, or *Download* replaces your pushed res with the release `assets.zip`.

**Android signing:** the APK is signed with uber-apk-signer's built-in debug key, so newer releases
install over older ones. Upstream's `android-release-build` signing needs Card-Forge's private
`forge.keystore` and produces the package `forge.app`, which would clash with an official Forge install.

## 6. Updating from upstream

Don't merge the release tag itself (for example `forge-2.0.15`): it pins every pom to the release
version, which makes the app look for assets on a dead server. Merge the last of the pom-only commits
that follow the tag instead (for 2.0.15: `f1ecb5d40e8` "Restoring pom.xml"), using a merge, not a cherry-pick.

## PushResToDevice

`forge-gui-android/tools/PushResToDevice.java` copies `forge-gui/res` onto the connected device so a dev
build uses your local resources. It must stay at that path, and is run from the project root (the
**forge-android-push-res** configuration does that). Without IntelliJ:

```
java forge-gui-android/tools/PushResToDevice.java [package]
```

- `package` defaults to `forge.app.dev`.
- It uses `adb push --sync`: the first run sends everything (~1.5 minutes), later runs only send changed files (seconds).
- `res/cardsfolder` is skipped (the device keeps its `cardsfolder.zip`; loose card scripts would be loaded on top of it). Changed card scripts need a new `assets.zip`.
- Files you delete from `res` are not deleted on the device.
- Afterwards it pushes `forge-gui-android/target/classes/assets/build.txt` as `Forge/apkbuild.txt`, so the
  app treats the pushed res as matching the freshly built APK and doesn't offer to replace it with the
  release `assets.zip`. Run it after building the APK (the *Before launch* order above does that).
- It finds `adb` through `ANDROID_HOME` (or `ANDROID_SDK_ROOT`), otherwise it uses `adb` from the `PATH`.

## Troubleshooting

| Problem | Fix |
|---|---|
| `The command line is too long` | android-maven-plugin 4.6.3 is not installed (setup step 4). |
| `Unresolveable build extension ... android-maven-plugin:4.6.3` | Same: install the plugin into the local repository the build uses. |
| `IllegalAccessError ... sun.security.pkcs` | `JDK_JAVA_OPTIONS` is missing. |
| `Unrecognized option: --add-exports\=...` | The variable was typed into the single-line field; re-enter it in the table editor. |
| `Could not find the selected project in the reactor: forge-gui-android` | Working directory is not the project root. |
| `The parameter android.deployapk.filename is missing` | Add the property from the table to **forge-android-deploy**. |
| `Could not find artifact forge:forge:pom:${revision}` | Build from the project root with `-pl forge-gui-android -am` instead of building the module on its own. |
| App freezes or crashes looking for a `ui/....json` / sprite file | The device's assets are older than your code: run **forge-android-push-res** (or ship a new `assets.zip`). |
| `forge-gui-desktop/target/CHANGES.txt` not found while building `assets.zip` | Run **forge-desktop-release** first. |
| izpack fails with `...\Chandra?s Tome.fullborder.jpg (The filename, directory name, or volume label syntax is incorrect)` | `-Dfile.encoding=UTF-8` is missing from `JDK_JAVA_OPTIONS`. |
| `Failed to delete ...\target\...jar` during `clean` | Another build (or a running IntelliJ debug session) still has the file open. |
