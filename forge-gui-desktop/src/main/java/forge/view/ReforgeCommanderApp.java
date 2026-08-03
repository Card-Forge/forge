/*
 * REFORGE COMMANDER EXTENSION
 *
 * Reforge Commander Main Entry Point
 *
 * Dedicated application entry point for Reforge Commander.
 * Hides/bypasses all legacy non-Commander modes (Quest, Draft, Sealed, Puzzles, Adventure)
 * and boots directly into a streamlined Commander interface.
 */
package forge.view;

import com.formdev.flatlaf.FlatDarkLaf;
import forge.GuiDesktop;
import forge.Singletons;
import forge.error.ExceptionHandler;
import forge.gui.GuiBase;

public final class ReforgeCommanderApp {

    public static void main(final String[] args) {
        System.out.println("==============================================================================");
        System.out.println("                     REFORGE COMMANDER ENGINE STARTUP                         ");
        System.out.println("==============================================================================");

        // Turn off legacy merge sort warnings & disable Direct3D to avoid flickering
        System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
        System.setProperty("sun.java2d.d3d", "false");

        // Flag application as running in Reforge Commander mode
        System.setProperty("reforge.commander.mode", "true");

        // Apply FlatLaf dark theme before any Swing components are created
        FlatDarkLaf.setup();

        // Setup desktop GUI interface
        GuiBase.setInterface(new GuiDesktop());

        // Install error handler
        ExceptionHandler.registerErrorHandling();
        GuiBase.logHWInfo();

        // Initialize singletons and launch control flow
        Singletons.initializeOnce(true);
        Singletons.getControl().initialize();
    }

    private ReforgeCommanderApp() {
    }
}
