import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Copies forge-gui/res onto a connected Android device so a dev build uses the local resources
 * (UI layouts, sprites, translations, ...) instead of the downloaded assets.zip.
 *
 * Run from the project root (single-file source launch, no compilation needed):
 *   java forge-gui-android/tools/PushResToDevice.java [package]
 *
 * The package defaults to forge.app.dev (the android-dev-build profile). Only files that changed
 * since the last push are sent (adb push --sync). res/cardsfolder is skipped: the device keeps the
 * cardsfolder.zip from assets.zip, and loose card scripts would be loaded in addition to it.
 * Afterwards the build.txt of the APK in forge-gui-android/target is pushed as Forge/apkbuild.txt, so run this
 * after building the APK and before installing it.
 */
public class PushResToDevice {
    public static void main(String[] args) throws Exception {
        String pkg = args.length > 0 ? args[0] : "forge.app.dev";
        File res = new File("forge-gui/res");
        if (!res.isDirectory()) {
            System.err.println("forge-gui/res not found; run this from the project root.");
            System.exit(1);
        }

        List<String> command = new ArrayList<>(Arrays.asList(adb(), "push", "--sync"));
        File[] entries = res.listFiles();
        Arrays.sort(entries);
        for (File entry : entries) {
            if (!entry.getName().equals("cardsfolder")) {
                command.add(entry.getPath());
            }
        }
        String forgeDir = "/storage/emulated/0/Android/obb/" + pkg + "/Forge/";
        command.add(forgeDir + "res/");

        System.out.println("Pushing forge-gui/res (without cardsfolder) to " + pkg + "...");
        int exitCode = new ProcessBuilder(command).inheritIO().start().waitFor();
        if (exitCode != 0) {
            System.exit(exitCode);
        }

        // Tell the app that the installed res belongs to the APK that was just built, so it doesn't offer to
        // replace the pushed files with the release assets.zip (see apkbuild.txt in AssetsDownloader).
        File apkBuild = new File("forge-gui-android/target/classes/assets/build.txt");
        if (apkBuild.isFile()) {
            exitCode = new ProcessBuilder(adb(), "push", apkBuild.getPath(), forgeDir + "apkbuild.txt").inheritIO().start().waitFor();
        } else {
            System.out.println("No built APK found (" + apkBuild.getPath() + "), so the app may offer to download assets.zip on its next start.");
        }
        System.exit(exitCode);
    }

    private static String adb() {
        String exe = System.getProperty("os.name").toLowerCase().startsWith("windows") ? "adb.exe" : "adb";
        for (String var : new String[] {"ANDROID_HOME", "ANDROID_SDK_ROOT"}) {
            String sdk = System.getenv(var);
            if (sdk != null) {
                File adb = new File(sdk, "platform-tools/" + exe);
                if (adb.isFile()) {
                    return adb.getPath();
                }
            }
        }
        return "adb"; // fall back to adb on the PATH
    }
}
