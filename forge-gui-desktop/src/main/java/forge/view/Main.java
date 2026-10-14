/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.view;

import forge.GuiDesktop;
import forge.Singletons;
import forge.error.ExceptionHandler;
import forge.gui.GuiBase;
import forge.gui.card.CardReaderExperiments;
import forge.util.BuildInfo;
import io.sentry.Sentry;

import java.awt.GraphicsEnvironment;
import java.io.File;

/**
 * Main class for Forge's swing application view.
 */
public final class Main {
    /**
     * Main entry point for Forge
     */
    public static void main(final String[] args) {
        // Command line modes must never touch the display: they run on servers, in CI, and over ssh.
        // This has to happen before any AWT class loads, so it also has to happen before Sentry.init.
        final String mode = args.length > 0 ? args[0].toLowerCase() : "";
        if (isCommandLineMode(mode) && System.getProperty("java.awt.headless") == null) {
            // Only when the user hasn't decided: setting this here would otherwise beat an explicit
            // -Djava.awt.headless=false on the command line, since it runs before GraphicsEnvironment
            // caches its answer.
            System.setProperty("java.awt.headless", "true");
        }

        // The GUI needs a display and a Java runtime that can drive one. Linux distributions ship a
        // "headless" runtime package that lacks the AWT/Swing libraries, and it is often the default
        // java, so check up front and say what is wrong: the alternative is a HeadlessException from
        // the first Swing call (and, in releases before #11761, no output at all).
        if (args.length == 0 && GraphicsEnvironment.isHeadless()) {
            explainMissingDisplay();
            System.exit(1);
        }

        // Sentry chains to whatever default handler is already installed, but if none is, an uncaught
        // exception is reported and then discarded without ever reaching stderr. Install a printing
        // handler first so failures during the startup below are visible rather than a bare exit code.
        //
        // This cannot just be registerErrorHandling() moved up: that reads ForgeConstants.LOG_FILE,
        // whose class initializer resolves ASSETS_DIR through GuiBase.getInterface() and so needs the
        // GUI interface already set. The window we need covered includes setting it, which is where
        // the headless crash this fix is about used to happen — so the real handler cannot be
        // installed early enough to see it, and something simpler has to cover the gap.
        //
        // Scope: registerErrorHandling() then replaces the default handler outright, so this one
        // covers only the window up to that call. After it, visibility depends on BugReporter, which
        // prints before it reports.
        Thread.setDefaultUncaughtExceptionHandler((t, ex) -> {
            System.err.println("Uncaught exception in thread " + t.getName() + ":");
            ex.printStackTrace();
            System.err.flush();
        });

        Sentry.init(options -> {
            options.setEnableExternalConfiguration(true);
            options.setRelease(BuildInfo.getVersionString());
            options.setEnvironment(System.getProperty("os.name"));
            options.setTag("Java Version", System.getProperty("java.version"));
            options.setShutdownTimeoutMillis(5000);
            // these belong to sentry.properties, but somehow some OS/Zip tool discards it?
            if (options.getDsn() == null || options.getDsn().isEmpty())
                options.setDsn("https://87bc8d329e49441895502737c069067b@sentry.asgardsrealm.net/3");
        }, true);

        // HACK - temporary solution to "Comparison method violates it's general contract!" crash
        System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");

        //Turn off the Java 2D system's use of Direct3D to improve rendering speed (particularly when Full Screen)
        System.setProperty("sun.java2d.d3d", "false");

        //Turn on OpenGl acceleration to improve performance
        //System.setProperty("sun.java2d.opengl", "True");

        //setup GUI interface
        GuiBase.setInterface(new GuiDesktop());

        //install our error handler
        ExceptionHandler.registerErrorHandling();
        GuiBase.logHWInfo();

        // Start splash screen first, then data models, then controller.
        if (args.length == 0) {
            Singletons.initializeOnce(true);

            // Controller can now step in and take over.
            Singletons.getControl().initialize();
            return;
        }

        // command line startup here
        switch (mode) {
            case "sim":
                SimulateMatch.simulate(args);
                break;

            case "parse":
                CardReaderExperiments.parseAllCards(args);
                break;

            case "server":
                System.out.println("Dedicated server mode.\nNot implemented.");
                break;

            default:
                System.out.println("Unknown mode.\nKnown modes are 'sim', 'parse', 'server'");
                break;
        }

        System.exit(0);
    }

    /**
     * Tell the user why the GUI cannot start on this runtime. Written to stderr since there is no
     * display to put a dialog on.
     */
    private static void explainMissingDisplay() {
        System.err.print(describeMissingDisplay(System.getProperty("os.name", ""), System.getProperty("java.home"),
                System.getProperty("java.vendor") + " " + System.getProperty("java.version"),
                System.getProperty("java.awt.headless"), System.getenv("DISPLAY")));
        System.err.flush();
    }

    /**
     * Build the explanation for a runtime that reports itself headless, naming the likely cause.
     * On Linux the runtime is headless when DISPLAY is unset or when the install is a "headless"
     * package, which the JDK detects by the absence of lib/libawt_xawt.so.
     */
    public static String describeMissingDisplay(final String osName, final String javaHome, final String javaVersion,
            final String headlessProperty, final String display) {
        final boolean windowsOrMac = osName.matches("(?i).*(windows|mac).*");
        final StringBuilder sb = new StringBuilder();
        sb.append("Forge cannot start: this Java runtime has no display to open a window on.\n");
        sb.append("  Java: ").append(javaHome).append(" (").append(javaVersion).append(")\n");
        if ("true".equalsIgnoreCase(headlessProperty)) {
            sb.append("  -Djava.awt.headless=true was passed on the command line; drop it to run the GUI.\n");
        } else if (!windowsOrMac && (display == null || display.trim().isEmpty())) {
            sb.append("  The DISPLAY environment variable is not set, so there is no X11/Wayland session to use.\n");
        } else if (!windowsOrMac && !new File(javaHome, "lib/libawt_xawt.so").exists()) {
            sb.append("  This is a \"headless\" Java package (no lib/libawt_xawt.so), which cannot show windows.\n");
            sb.append("  Install the full runtime instead, e.g. \"sudo apt install openjdk-21-jre\" or\n");
            sb.append("  \"sudo dnf install java-21-openjdk\", or point JAVA_HOME at a full JDK.\n");
        } else {
            sb.append("  Check that a graphical session is available and that the Java install is not a headless package.\n");
        }
        return sb.toString();
    }

    /**
     * Modes that run to completion on the console and never open a window, and so should force
     * headless AWT. Keep in sync with the named cases of the switch in {@link #main(String[])}.
     * The switch's {@code default} arm also stays on the console, but an unrecognised argument is
     * not a mode and only prints usage, so it is deliberately not listed here.
     */
    public static boolean isCommandLineMode(final String mode) {
        return "sim".equals(mode) || "parse".equals(mode) || "server".equals(mode);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void finalize() throws Throwable {
        try {
            ExceptionHandler.unregisterErrorHandling();
        } finally {
            super.finalize();
        }
    }

    // disallow instantiation
    private Main() {
    }
}
