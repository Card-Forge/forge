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
import forge.util.JVMOptions;
import io.sentry.Sentry;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.List;

/**
 * Main class for Forge's swing application view.
 */
public final class Main {
    /**
     * Main entry point for Forge
     */
    public static void main(final String[] args) {
        String javaVersion = System.getProperty("java.version");
        checkJVMArgs(javaVersion, args);
    }
    static void checkJVMArgs(String javaVersion, String[] args) {
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        List<String> arguments = runtimeMxBean.getInputArguments();

        List<Object> options = new ArrayList<>();
        JButton ok = new JButton("OK");
        options.add(ok);
        JVMOptions.getStringBuilder().append("Java Version: ").append(javaVersion).append("\nArguments: \n");
        for (String a : arguments) {
            if (a.startsWith("-agent") || a.startsWith("-javaagent"))
                continue;
            JVMOptions.getStringBuilder().append(a).append("\n");
        }
        JOptionPane pane = new JOptionPane(JVMOptions.getStringBuilder(), JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION, null, options.toArray());
        JDialog dlg = pane.createDialog(JOptionPane.getRootFrame(), "Error");
        ok.addActionListener(e -> {
            dlg.setVisible(false);
            System.exit(0);
        });
        dlg.setResizable(false);

        if (!JVMOptions.checkRuntime(arguments)) {
            dlg.setVisible(true);
        } else {
            start(args);
        }
    }
    static void start(final String[] args) {
        Sentry.init(options -> {
            options.setEnableExternalConfiguration(true);
            options.setRelease(BuildInfo.getVersionString());
            options.setEnvironment(System.getProperty("os.name"));
            options.setTag("Java Version", System.getProperty("java.version"));
            options.setShutdownTimeoutMillis(5000);
            if (options.getDsn() == null)
                options.setDsn("https://87bc8d329e49441895502737c069067b@sentry.cardforge.org//3");
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

        // Start splash screen first, then data models, then controller.
        if (args.length == 0) {
            Singletons.initializeOnce(true);

            // Controller can now step in and take over.
            Singletons.getControl().initialize();
            return;
        }

        // command line startup here
        String mode = args[0].toLowerCase();

        switch(mode) {
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
                System.out.println("Unknown mode.\nKnown mode is 'sim', 'parse' ");
                break;
        }

        System.exit(0);
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
    private Main() { }
}
