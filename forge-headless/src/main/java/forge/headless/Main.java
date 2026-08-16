package forge.headless;

import forge.gui.GuiBase;

/**
 * Main entry point for Forge headless mode.
 */
public final class Main {
    /**
     * Main entry point for Forge headless commands.
     */
    public static void main(final String[] args) {
        System.exit(run(args));
    }

    static int run(final String[] args) {
        if (args.length == 0) {
            printHelp();
            return 1;
        }
        if (isHelpArgument(args[0])) {
            printHelp();
            return 0;
        }

        // HACK - temporary solution to "Comparison method violates it's general contract!" crash
        System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");

        // Setup headless GUI interface (minimal implementation, no actual GUI)
        GuiBase.setInterface(new HeadlessGuiBase());

        // Command line startup
        String mode = args[0].toLowerCase();

        switch (mode) {
            case "sim":
                SimulateMatch.simulate(args);
                return 0;

            case "tui":
                TextUIGame.run(args);
                return 0;

            default:
                System.err.println("Unknown command: " + args[0]);
                System.err.println("Run './headless.sh --help' to see the available commands.");
                return 2;
        }
    }

    private static boolean isHelpArgument(final String argument) {
        return "--help".equals(argument) || "-h".equals(argument) || "help".equalsIgnoreCase(argument);
    }

    private static void printHelp() {
        System.out.println("Forge Headless");
        System.out.println();
        System.out.println("Run Forge games without starting the desktop or mobile application.");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  ./headless.sh <command> [options]");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  sim    Run automated games between Forge AI players.");
        System.out.println("  tui    Play or observe a game in an interactive terminal.");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  ./headless.sh sim -d deck1.dck deck2.dck -n 10");
        System.out.println("  ./headless.sh tui deck1.dck deck2.dck --p1 tui --p2 ai");
        System.out.println();
        System.out.println("Run './headless.sh <command> --help' for command-specific options.");
    }

    // disallow instantiation
    private Main() {
    }
}
