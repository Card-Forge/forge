package forge.headless;

import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
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
            MyRandom.setRandom(new Random(options.getSeed() == null ? 0L : options.getSeed()));
            FModel.initialize(null, null);
            // Card and model initialization may consume gameplay RNG, including from worker threads.
            // BridgeSession resets it from game_start once the coordinator supplies the match seed.
            if (options.getListenAddress() == null) {
                try (BridgeTransport transport = new BridgeTransport(protocolInput, protocolOutput,
                        options.getLogPath())) {
                    new BridgeSession(options, transport, diagnosticOutput).run();
                }
            } else {
                InetSocketAddress address = parseListenAddress(options.getListenAddress());
                try (ServerSocket server = new ServerSocket()) {
                    server.bind(address);
                    diagnosticOutput.println("Forge bridge listening on " + server.getLocalSocketAddress());
                    try (Socket socket = server.accept();
                            PrintStream socketOutput = new PrintStream(socket.getOutputStream(), true,
                                    StandardCharsets.UTF_8);
                            BridgeTransport transport = new BridgeTransport(socket.getInputStream(), socketOutput,
                                    options.getLogPath())) {
                        new BridgeSession(options, transport, diagnosticOutput).run();
                    }
                }
            }
            return 0;
        } catch (Exception e) {
            diagnosticOutput.println("Forge bridge failed: " + e.getMessage());
            e.printStackTrace(diagnosticOutput);
            return 1;
        }
    }

    private static InetSocketAddress parseListenAddress(String value) {
        int separator = value.lastIndexOf(':');
        if (separator <= 0 || separator == value.length() - 1) {
            throw new IllegalArgumentException("--listen must use HOST:PORT syntax");
        }
        String host = value.substring(0, separator);
        if (host.startsWith("[") && host.endsWith("]")) {
            host = host.substring(1, host.length() - 1);
        }
        int port;
        try {
            port = Integer.parseInt(value.substring(separator + 1));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("--listen port must be a number", e);
        }
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("--listen port must be between 0 and 65535");
        }
        return new InetSocketAddress(host, port);
    }
}
