package forge.headless;

import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Random;

import forge.model.FModel;
import forge.util.MyRandom;
import picocli.CommandLine;
import picocli.CommandLine.ParameterException;

/** Entry point for the newline-delimited JSON-RPC bridge. */
public final class BridgeMain {
    private BridgeMain() {
    }

    public static int run(String[] args, InputStream protocolInput, PrintStream protocolOutput,
            PrintStream diagnosticOutput) {
        // Forge has many legacy System.out diagnostics. Reserve the original stdout exclusively
        // for JSON-RPC before initializing any Forge subsystem.
        System.setOut(diagnosticOutput);

        String[] bridgeArgs = Arrays.copyOfRange(args, 1, args.length);
        BridgeOptions options = new BridgeOptions();
        CommandLine command = new CommandLine(options);
        command.setOut(new PrintWriter(diagnosticOutput, true));
        command.setErr(new PrintWriter(diagnosticOutput, true));
        try {
            command.parseArgs(bridgeArgs);
        } catch (ParameterException e) {
            diagnosticOutput.println(e.getMessage());
            command.usage(diagnosticOutput);
            return 2;
        }
        if (command.isUsageHelpRequested() || command.isVersionHelpRequested()) {
            command.usage(diagnosticOutput);
            return 0;
        }

        try {
            options.validate();
            MyRandom.setRandom(new Random(options.getSeed()));
            FModel.initialize(null, null);
            // Card and model initialization may consume gameplay RNG, including from worker threads.
            // Give every bridge game the requested initial RNG state after initialization has completed.
            MyRandom.setRandom(new Random(options.getSeed()));
            try (BridgeTransport transport = new BridgeTransport(protocolInput, protocolOutput,
                    options.getLogPath())) {
                new BridgeSession(options, transport, diagnosticOutput).run();
            }
            return 0;
        } catch (Exception e) {
            diagnosticOutput.println("Forge bridge failed: " + e.getMessage());
            e.printStackTrace(diagnosticOutput);
            return 1;
        }
    }
}
