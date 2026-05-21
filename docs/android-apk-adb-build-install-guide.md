# Build and Install Forge Android APK with ADB

This guide builds Forge from this repo, creates an Android APK, signs it with the local Android debug keystore, and installs it to a phone over USB with `adb`.

## Paths Used

Repo:

```bash
cd /home/lou/Development/forge-1
```

Android SDK:

```bash
export ANDROID_HOME=/home/lou/Android/Sdk
export ADB=/home/lou/Android/Sdk/platform-tools/adb
export BUILD_TOOLS=/home/lou/Android/Sdk/build-tools/35.0.0
```

## 1. Prepare the Phone

On the Android phone:

1. Enable Developer options.
2. Enable USB debugging.
3. Plug the phone into the computer over USB.
4. When Android asks whether to allow USB debugging from this computer, tap **Allow**.

Check that the computer sees the phone:

```bash
$ADB devices -l
```

Good output looks like this:

```text
List of devices attached
13d2f04f device usb:... product:... model:...
```

If it says `unauthorized`, run:

```bash
$ADB kill-server
$ADB start-server
$ADB devices -l
```

Then unplug and reconnect the phone, unlock it, and accept the USB debugging prompt.

## 2. Build Forge and Generate the APK

From the repo root:

```bash
cd /home/lou/Development/forge-1

MAVEN_OPTS="--add-exports=java.base/sun.security.pkcs=ALL-UNNAMED --add-exports=java.base/sun.security.x509=ALL-UNNAMED" \
ANDROID_HOME=/home/lou/Android/Sdk \
_JAVA_OPTIONS=-Xmx2g \
mvn -pl forge-gui-android -am -P android-debug verify
```

After a successful build, the raw APK should be in:

```text
forge-gui-android/target/forge-android-2.0.13-SNAPSHOT-05.21.apk
```

If the date suffix changes, list the generated APKs:

```bash
find forge-gui-android/target -maxdepth 1 -type f -name '*.apk' -printf '%T@ %p %s bytes\n' | sort -n
```

## 3. Zipalign and Sign the APK

Set the APK filenames. Adjust the raw APK name if the build date changed:

```bash
RAW_APK=/home/lou/Development/forge-1/forge-gui-android/target/forge-android-2.0.13-SNAPSHOT-05.21.apk
ALIGNED_APK=/home/lou/Development/forge-1/forge-gui-android/target/forge-android-2.0.13-SNAPSHOT-05.21-aligned.apk
SIGNED_APK=/home/lou/Development/forge-1/forge-gui-android/target/forge-android-2.0.13-SNAPSHOT-05.21-debug-signed.apk
```

Align it:

```bash
$BUILD_TOOLS/zipalign -f -p 4 "$RAW_APK" "$ALIGNED_APK"
```

Sign it with the Android debug keystore:

```bash
$BUILD_TOOLS/apksigner sign \
  --ks /home/lou/.android/debug.keystore \
  --ks-key-alias androiddebugkey \
  --ks-pass pass:android \
  --key-pass pass:android \
  --out "$SIGNED_APK" \
  "$ALIGNED_APK"
```

Verify the signed APK:

```bash
$BUILD_TOOLS/apksigner verify --verbose "$SIGNED_APK"
```

Good output includes:

```text
Verifies
Verified using v2 scheme (APK Signature Scheme v2): true
Verified using v3 scheme (APK Signature Scheme v3): true
```

## 4. Install the APK to the Phone

Install or update the app:

```bash
$ADB install -r "$SIGNED_APK"
```

Good output:

```text
Performing Incremental Install
Success
```

Confirm the package is installed:

```bash
$ADB shell pm list packages | rg forge
```

Expected:

```text
package:forge.app
```

Launch Forge from the phone normally, or launch it from adb:

```bash
$ADB shell monkey -p forge.app 1
```

## Common Problems

### `adb: failed to stat ... No such file or directory`

The APK path is wrong or the signed APK has not been created yet. List APKs:

```bash
find /home/lou/Development/forge-1/forge-gui-android/target -maxdepth 1 -type f -name '*.apk' -printf '%T@ %p %s bytes\n' | sort -n
```

Then use the actual generated filename.

### Device Shows `unauthorized`

Run:

```bash
$ADB kill-server
$ADB start-server
$ADB devices -l
```

Then unlock the phone, reconnect USB, and accept the USB debugging authorization dialog.

If no prompt appears, on the phone go to:

```text
Developer options -> Revoke USB debugging authorizations
```

Then reconnect the cable and run:

```bash
$ADB devices -l
```

### `INSTALL_FAILED_UPDATE_INCOMPATIBLE`

This means the installed Forge app was signed with a different key. To replace it, uninstall the old package first:

```bash
$ADB uninstall forge.app
$ADB install "$SIGNED_APK"
```

Warning: uninstalling may remove Forge app data on the phone.

### Phone Can Ping Tailscale but Forge Cannot Reach the Sidecar

Make sure the sidecar URL compiled into Forge points to the Tailscale address, for example:

```java
DECK_RECOGNITION_SIDECAR_URL("http://100.110.52.69:18970")
```

That setting is in:

```text
forge-ai/src/main/java/forge/ai/AiProps.java
```

Then rebuild, sign, and reinstall the APK.
