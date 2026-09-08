package forge.download;

import forge.util.BuildInfo;
import forge.util.OperatingSystem;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * Installs a downloaded Forge installer package into the directory Forge is running from,
 * without any further user interaction.
 * <p>
 * The installer is an IzPack package, which can install itself from an automation script
 * instead of its GUI ({@code java -jar forge-installer.jar auto-install.xml}). Newer packages
 * carry that script as a resource so it always matches their own panels; for older ones a
 * built-in copy is used. Either way the only thing filled in is the install path, so the
 * update lands on top of the existing installation instead of wherever the installer's
 * default happens to point.
 * <p>
 * Forge cannot install over itself while it is running - on Windows the jar it was started
 * from is locked, and daily snapshots reuse the same jar name - so the actual work is done by
 * a small generated helper script that waits for this process to exit, runs the installer and
 * starts Forge again.
 */
public final class UpdateInstaller {
    /** IzPack writes this into every install directory. */
    private static final String IZPACK_MARKER = ".installationinformation";
    /** Automation script carried by installers that know about unattended updates. */
    private static final String SCRIPT_RESOURCE = "resources/auto-install.xml";
    private static final String INSTALL_PATH_TOKEN = "@INSTALL_PATH@";
    /** IzPack's automated installer always exits 0, so its log has to be checked instead. */
    private static final String SUCCESS_MARKER = "Automated installation done";

    /** Used when the downloaded installer predates {@link #SCRIPT_RESOURCE}. Must list every
     * panel of forge-installer's install.xml - IzPack aborts if one is missing. */
    private static final String FALLBACK_SCRIPT =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
            + "<AutomatedInstallation langpack=\"eng\">\n"
            + "  <com.izforge.izpack.panels.htmlinfo.HTMLInfoPanel id=\"welcome\"/>\n"
            + "  <com.izforge.izpack.panels.target.TargetPanel id=\"install_dir\">\n"
            + "    <installpath>" + INSTALL_PATH_TOKEN + "</installpath>\n"
            + "  </com.izforge.izpack.panels.target.TargetPanel>\n"
            + "  <com.izforge.izpack.panels.packs.PacksPanel id=\"sdk_pack_select\">\n"
            + "    <pack index=\"0\" name=\"Forge pack\" selected=\"true\"/>\n"
            + "    <pack index=\"1\" name=\"Script pack\" selected=\"true\"/>\n"
            + "  </com.izforge.izpack.panels.packs.PacksPanel>\n"
            + "  <com.izforge.izpack.panels.install.InstallPanel id=\"install\"/>\n"
            + "  <com.izforge.izpack.panels.finish.FinishPanel id=\"finish\"/>\n"
            + "</AutomatedInstallation>\n";

    private static File installDir;
    private static boolean installDirChecked;

    private UpdateInstaller() {
    }

    /**
     * The installation Forge is running from, or null if it isn't running from one we can
     * write to (a source checkout, a read-only directory, a manually unpacked copy without
     * launchers).
     */
    public static synchronized File getInstallDir() {
        if (!installDirChecked) {
            installDirChecked = true;
            installDir = findInstallDir();
        }
        return installDir;
    }

    /** Whether an update can be installed without asking the user where it should go. */
    public static boolean isSupported(File installerPackage) {
        return getInstallDir() != null && getJavaExecutable() != null
                && installerPackage != null && installerPackage.isFile()
                && installerPackage.getName().endsWith(".jar");
    }

    /**
     * Hands the update over to a helper that installs it once Forge has exited, and returns
     * whether that succeeded. The caller is expected to quit Forge right afterwards; if this
     * returns false nothing was started and the update has to be installed by hand.
     */
    public static boolean install(File installerPackage) {
        if (!isSupported(installerPackage)) {
            return false;
        }
        File dir = getInstallDir();
        try {
            File workDir = new File(System.getProperty("java.io.tmpdir"), "forge-update");
            if (!workDir.isDirectory() && !workDir.mkdirs()) {
                return false;
            }
            File script = new File(workDir, "auto-install.xml");
            Files.write(script.toPath(), readScript(installerPackage, dir).getBytes(StandardCharsets.UTF_8));

            File log = new File(workDir, "install.log");
            File helper = writeHelper(workDir, installerPackage, script, log, dir);
            startDetached(helper, dir);
            System.out.println("Installing update into " + dir + ", log: " + log);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static File findInstallDir() {
        if (BuildInfo.getVersionString().contains("GIT")) {
            return null; // running from a checkout, there is nothing to update
        }
        File fromJar = null;
        try {
            CodeSource source = UpdateInstaller.class.getProtectionDomain().getCodeSource();
            if (source != null && source.getLocation() != null) {
                File jar = new File(source.getLocation().toURI());
                if (jar.isFile()) {
                    fromJar = jar.getParentFile();
                }
            }
        } catch (Exception e) {
            // fall back to the working directory below
        }
        if (isInstallDir(fromJar)) {
            return fromJar;
        }
        File workingDir = new File(System.getProperty("user.dir"));
        return isInstallDir(workingDir) ? workingDir : null;
    }

    private static boolean isInstallDir(File dir) {
        if (dir == null || !dir.isDirectory()) {
            return false;
        }
        // res and a launcher script mean this is an unpacked Forge; the IzPack marker is only
        // there if it was installed rather than extracted from the archive by hand.
        if (!new File(dir, "res").isDirectory() || getLauncher(dir) == null) {
            return false;
        }
        return isWritable(dir);
    }

    /** File.canWrite lies about directories on Windows, so actually try to write one. */
    private static boolean isWritable(File dir) {
        File probe = null;
        try {
            probe = File.createTempFile("forge-update", ".tmp", dir);
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            if (probe != null) {
                probe.delete();
            }
        }
    }

    private static File getLauncher(File dir) {
        List<String> names = new ArrayList<>();
        if (OperatingSystem.isWindows()) {
            names.add("forge.cmd");
        } else {
            if (OperatingSystem.isMac()) {
                names.add("forge.command");
            }
            names.add("forge.sh");
        }
        for (String name : names) {
            File launcher = new File(dir, name);
            if (launcher.isFile()) {
                return launcher;
            }
        }
        return null;
    }

    private static File getJavaExecutable() {
        String home = System.getProperty("java.home");
        if (home == null) {
            return null;
        }
        File java = new File(new File(home, "bin"), OperatingSystem.isWindows() ? "java.exe" : "java");
        return java.canExecute() ? java : null;
    }

    private static String readScript(File installerPackage, File dir) {
        String script = null;
        try (JarFile jar = new JarFile(installerPackage)) {
            ZipEntry entry = jar.getEntry(SCRIPT_RESOURCE);
            if (entry != null) {
                try (InputStream in = jar.getInputStream(entry)) {
                    script = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (script == null || !script.contains(INSTALL_PATH_TOKEN)) {
            script = FALLBACK_SCRIPT;
        }
        // IzPack's properties parsing swallows backslashes, so the path has to use forward ones
        String path = dir.getAbsolutePath().replace('\\', '/')
                .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        return script.replace(INSTALL_PATH_TOKEN, path);
    }

    private static File writeHelper(File workDir, File installerPackage, File script, File log, File dir)
            throws IOException {
        long pid = ProcessHandle.current().pid();
        String java = getJavaExecutable().getAbsolutePath();
        String installer = installerPackage.getAbsolutePath();
        File helper;
        String contents;
        if (OperatingSystem.isWindows()) {
            helper = new File(workDir, "install.cmd");
            contents = "@echo off\r\n"
                    + "title Forge Update\r\n"
                    + "echo Waiting for Forge to close...\r\n"
                    + ":wait\r\n"
                    + "tasklist /FI \"PID eq " + pid + "\" /NH 2>nul | find \"" + pid + "\" >nul\r\n"
                    + "if not errorlevel 1 (\r\n"
                    + "  ping -n 2 127.0.0.1 >nul\r\n"
                    + "  goto wait\r\n"
                    + ")\r\n"
                    + "echo Installing the update, this takes a minute...\r\n"
                    + "\"" + java + "\" -jar \"" + installer + "\" \"" + script.getAbsolutePath() + "\""
                    + " > \"" + log.getAbsolutePath() + "\" 2>&1\r\n"
                    + "findstr /C:\"" + SUCCESS_MARKER + "\" \"" + log.getAbsolutePath() + "\" >nul\r\n"
                    + "if errorlevel 1 goto failed\r\n"
                    + "echo Done, starting Forge...\r\n"
                    + "start \"Forge\" /D \"" + dir.getAbsolutePath() + "\" \"" + getLauncher(dir).getAbsolutePath() + "\"\r\n"
                    + "exit /b 0\r\n"
                    + ":failed\r\n"
                    + "echo The update could not be installed automatically.\r\n"
                    + "echo Opening the log and the installer so you can finish it by hand.\r\n"
                    + "start \"\" \"" + log.getAbsolutePath() + "\"\r\n"
                    + "start \"\" \"" + installer + "\"\r\n"
                    + "pause\r\n"
                    + "exit /b 1\r\n";
        } else {
            helper = new File(workDir, "install.sh");
            contents = "#!/bin/sh\n"
                    + "while kill -0 " + pid + " 2>/dev/null; do sleep 1; done\n"
                    + "\"" + java + "\" -jar \"" + installer + "\" \"" + script.getAbsolutePath() + "\""
                    + " > \"" + log.getAbsolutePath() + "\" 2>&1\n"
                    + "if grep -q \"" + SUCCESS_MARKER + "\" \"" + log.getAbsolutePath() + "\"; then\n"
                    + "  cd \"" + dir.getAbsolutePath() + "\" && exec \"" + getLauncher(dir).getAbsolutePath() + "\"\n"
                    + "fi\n"
                    + "echo \"Forge could not install the update automatically, see " + log.getAbsolutePath() + "\" >&2\n"
                    + "exit 1\n";
        }
        Files.write(helper.toPath(), contents.getBytes(StandardCharsets.UTF_8));
        helper.setExecutable(true, false);
        return helper;
    }

    private static void startDetached(File helper, File dir) throws IOException {
        ProcessBuilder pb;
        if (OperatingSystem.isWindows()) {
            // start gives the helper a console of its own, so closing Forge's doesn't kill it
            pb = new ProcessBuilder("cmd", "/c", "start", "Forge Update",
                    "/D", dir.getAbsolutePath(), helper.getAbsolutePath());
        } else {
            pb = new ProcessBuilder(helper.getAbsolutePath());
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(new File(helper.getParentFile(), "helper.log")));
        }
        pb.directory(dir);
        pb.start();
    }
}
