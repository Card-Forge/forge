package forge;

import forge.view.Main;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

/**
 * Guards the command line modes ({@code sim}, {@code parse}, {@code server}) against regaining a
 * hard dependency on a display.
 *
 * <p>The display checks have to run in a forked JVM. {@link java.awt.GraphicsEnvironment} decides
 * whether it is headless once, on first use, and CI runs the suite under Xvfb with a real
 * {@code DISPLAY} — so a check made inside the test JVM would exercise the headful path and prove
 * nothing.
 *
 * <p>The regression being guarded: {@code GuiDesktop} once resolved the screen scale from a static
 * initializer, so merely loading the class threw {@link java.awt.HeadlessException}. Since
 * {@code Main} installs the GUI interface before it parses argv, {@code sim} died before it could
 * read its own arguments.
 *
 * <p>{@code Main} forces {@code java.awt.headless} for console modes so that a desktop run takes the
 * same path a server does, and leaves it alone when the user has set it explicitly. Both are
 * asserted by reading the property back out of the forked JVM
 * ({@link #mainForcesHeadlessUnlessUserSaysOtherwise()}) rather than by observing
 * {@code isHeadless()}, which would be true anyway on a machine with no display and so would prove
 * nothing there.
 */
// The per-method budget has to clear the probe budget below times the most forks any one method
// runs (two), so a hung fork trips its own timeout and reports what it had printed so far, rather
// than TestNG killing the method first with nothing to show.
@Test(timeOut = 240000, enabled = true)
public class HeadlessStartupTest {

    /** Forks take ~2s in practice; this is a hang guard, not a deadline. */
    private static final long PROBE_TIMEOUT_SECONDS = 90;

    /** Loading GuiDesktop and reading the screen scale must work with no display available. */
    public void guiDesktopLoadsHeadless() throws Exception {
        ProbeResult result = runHeadless(ScreenScaleProbe.class.getName(), true);
        assertEquals("probe failed, output was:\n" + result.output, 0, result.exitCode);
        assertTrue("expected a usable screen scale, got:\n" + result.output,
                result.output.contains("screenScale=1.0"));
    }

    /**
     * Dialog entry points must degrade to a return value instead of throwing HeadlessException, and
     * must not answer a confirmation on the user's behalf — {@code SOptionPane.showConfirmDialog}
     * treats index 0 as "Yes", so returning the default option would approve destructive prompts.
     */
    public void dialogsDegradeHeadless() throws Exception {
        ProbeResult result = runHeadless(DialogProbe.class.getName(), true);
        assertEquals("probe failed, output was:\n" + result.output, 0, result.exitCode);
        assertTrue("expected the dialog to report itself dismissed, got:\n" + result.output,
                result.output.contains("optionDialog=-1"));
        assertTrue("expected the initial input back, got:\n" + result.output,
                result.output.contains("inputDialog=keep-me"));
        assertTrue("expected a non-null fallback for a null initial input, got:\n" + result.output,
                result.output.contains("inputDialogNullInitial="));
        assertFalse("showInputDialog must never return null headless:\n" + result.output,
                result.output.contains("inputDialogNullInitial=null"));
    }

    /**
     * The whole of {@code Main}'s startup — including {@code GuiBase.setInterface(new GuiDesktop())},
     * which is where the original bug struck — must survive with no display. {@code server} is used
     * because it is the one console mode that returns without loading the card database.
     */
    public void mainStartsHeadless() throws Exception {
        ProbeResult result = runInThrowawayHome(Main.class.getName(), List.of(), "server");
        assertEquals("Main failed to start headless, output was:\n" + result.output, 0, result.exitCode);
        assertFalse("Main hit a display dependency during startup:\n" + result.output,
                result.output.contains("HeadlessException"));
        assertTrue("expected the server mode banner, got:\n" + result.output,
                result.output.contains("Dedicated server mode"));
    }

    /**
     * Runs a class that starts {@code Main}, with the Forge profile pointed at a throwaway home so a
     * test run leaves nothing in the developer's (or CI user's) real one. {@code user.home} covers
     * Linux and macOS; Windows resolves its profile from APPDATA/LOCALAPPDATA instead, so those are
     * overridden too.
     */
    private static ProbeResult runInThrowawayHome(final String mainClass, final List<String> jvmArgs,
                                                  final String... args) throws Exception {
        File fakeHome = Files.createTempDirectory("headless-probe-home").toFile();
        // Backstop for the JVM being killed outright, matching the deleteOnExit on the temp files.
        // Both calls throw IllegalStateException once shutdown has begun, so each is guarded, for a
        // different reason: unguarded, the add would abandon the method before it ran at all and
        // strand the directory it had just created, while the remove sits in a finally where it
        // would replace the result or failure this method was about to report. An unregistered hook
        // costs nothing more than the backstop — the finally below does the actual cleanup.
        Thread cleanup = new Thread(() -> deleteRecursively(fakeHome));
        try {
            Runtime.getRuntime().addShutdownHook(cleanup);
        } catch (final IllegalStateException alreadyShuttingDown) {
            cleanup = null;
        }
        try {
            List<String> withHome = new ArrayList<>(jvmArgs);
            withHome.add("-Duser.home=" + fakeHome.getAbsolutePath());
            return runHeadless(mainClass, false, withHome, fakeHome, args);
        } finally {
            deleteRecursively(fakeHome);
            if (cleanup != null) {
                try {
                    Runtime.getRuntime().removeShutdownHook(cleanup);
                } catch (final IllegalStateException alreadyShuttingDown) {
                    // The hook will run during shutdown and find nothing left to delete.
                }
            }
        }
    }

    private static void deleteRecursively(final File file) {
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        file.delete();
    }

    /**
     * A console mode must run headless even where a display exists, so a desktop run reproduces what
     * a server does — but an explicit {@code -Djava.awt.headless=false} must still win.
     */
    public void mainForcesHeadlessUnlessUserSaysOtherwise() throws Exception {
        ProbeResult forced = runInThrowawayHome(HeadlessPropertyProbe.class.getName(), List.of());
        assertEquals("probe failed, output was:\n" + forced.output, 0, forced.exitCode);
        assertTrue("Main must force headless for a console mode, got:\n" + forced.output,
                forced.output.contains("java.awt.headless=true"));

        ProbeResult explicit = runInThrowawayHome(HeadlessPropertyProbe.class.getName(),
                List.of("-Djava.awt.headless=false"));
        assertEquals("probe failed, output was:\n" + explicit.output, 0, explicit.exitCode);
        assertTrue("an explicit -Djava.awt.headless=false must not be overridden, got:\n"
                + explicit.output, explicit.output.contains("java.awt.headless=false"));
    }

    /** The console-mode list must stay in sync with the switch in {@code Main.main}. */
    public void commandLineModesAreRecognized() {
        assertTrue("sim must be a console mode", Main.isCommandLineMode("sim"));
        assertTrue("parse must be a console mode", Main.isCommandLineMode("parse"));
        assertTrue("server must be a console mode", Main.isCommandLineMode("server"));
        assertFalse("the zero-argument GUI launch must not be a console mode",
                Main.isCommandLineMode(""));
        assertFalse("an unknown mode must not be a console mode", Main.isCommandLineMode("garbage"));
    }

    /**
     * Runs a class in a forked JVM and returns its exit code and combined output.
     *
     * @param forceHeadless pass {@code -Djava.awt.headless=true}; {@code Main} sets it itself, so
     *                      forcing it there would hide whether {@code Main} still does so
     */
    private static ProbeResult runHeadless(final String mainClass, final boolean forceHeadless,
                                           final String... args) throws Exception {
        return runHeadless(mainClass, forceHeadless, List.of(), null, args);
    }

    private static ProbeResult runHeadless(final String mainClass, final boolean forceHeadless,
                                           final List<String> jvmArgs, final File homeOverride,
                                           final String... args) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
        if (forceHeadless) {
            command.add("-Djava.awt.headless=true");
        }
        command.addAll(jvmArgs);
        command.add("-cp");
        command.add(classPath());
        command.add(mainClass);
        command.addAll(List.of(args));

        // Output goes to a file rather than a pipe this test reads. Draining a pipe to EOF before
        // waiting would make the timeout below unreachable — EOF only arrives when the child exits —
        // and would instead block here forever on a child that hangs holding stdout open.
        File outputFile = File.createTempFile("headless-probe", ".log");
        outputFile.deleteOnExit();
        // An empty file as stdin gives the child immediate EOF, so a probe that ever reads the
        // console can't wedge waiting for input nobody will type. Portable, unlike /dev/null.
        File emptyStdin = File.createTempFile("headless-probe-stdin", "");
        emptyStdin.deleteOnExit();

        ProcessBuilder pb = new ProcessBuilder(command);
        if (homeOverride != null) {
            // ForgeProfileProperties reads these rather than user.home on Windows.
            pb.environment().put("APPDATA", homeOverride.getAbsolutePath());
            pb.environment().put("LOCALAPPDATA", homeOverride.getAbsolutePath());
        }
        pb.redirectErrorStream(true);
        pb.redirectOutput(outputFile);
        pb.redirectInput(emptyStdin);

        Process process = pb.start();
        try {
            if (!process.waitFor(PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new AssertionError("headless probe timed out after " + PROBE_TIMEOUT_SECONDS
                        + "s, output so far:\n" + read(outputFile));
            }
            return new ProbeResult(process.exitValue(), read(outputFile));
        } finally {
            // Covers the timeout above and a TestNG timeout unwinding this thread; without it a
            // hung child is reparented to init and leaks for the life of the machine.
            // Wait for it to actually die: the caller may be about to delete a directory the child
            // is still writing into.
            process.destroyForcibly().waitFor(5, TimeUnit.SECONDS);
            outputFile.delete();
            emptyStdin.delete();
        }
    }

    /**
     * Surefire launches the test JVM from a manifest-only booter jar, then replaces
     * {@code java.class.path} with the expanded test classpath and stashes the original booter jar
     * in {@code surefire.real.class.path} — that property is the one-entry jar, not the real path,
     * despite its name. So {@code java.class.path} is what the child wants.
     */
    private static String classPath() {
        return System.getProperty("java.class.path");
    }

    private static String read(final File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    private static final class ProbeResult {
        private final int exitCode;
        private final String output;

        private ProbeResult(final int exitCode, final String output) {
            this.exitCode = exitCode;
            this.output = output;
        }
    }

    /** Runs in the forked JVM: loads GuiDesktop and reports the screen scale. */
    public static final class ScreenScaleProbe {
        public static void main(final String[] args) {
            System.out.println("screenScale=" + new GuiDesktop().getScreenScale());
        }
    }

    /**
     * Runs in the forked JVM: lets {@code Main} start a console mode, then reports what
     * {@code java.awt.headless} ended up as. The read happens in a shutdown hook because
     * {@code Main.main} ends in {@code System.exit}, and the property is what matters rather than
     * {@code isHeadless()} — the latter is true on a display-less machine no matter what Main does.
     */
    public static final class HeadlessPropertyProbe {
        public static void main(final String[] args) {
            Runtime.getRuntime().addShutdownHook(new Thread(() ->
                    System.out.println("java.awt.headless=" + System.getProperty("java.awt.headless"))));
            // "server" returns immediately; every other console mode loads the card database first.
            Main.main(new String[] {"server"});
        }
    }

    /** Runs in the forked JVM: checks the dialog entry points return instead of throwing. */
    public static final class DialogProbe {
        public static void main(final String[] args) {
            GuiDesktop gui = new GuiDesktop();
            System.out.println("optionDialog=" + gui.showOptionDialog("msg", "title", null, null, 0));
            System.out.println("inputDialog="
                    + gui.showInputDialog("msg", "title", null, "keep-me", null, false));
            System.out.println("inputDialogNullInitial="
                    + gui.showInputDialog("msg", "title", null, null, null, false));
        }
    }
}
