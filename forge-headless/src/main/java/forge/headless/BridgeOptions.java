package forge.headless;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/** Command-line configuration for a Forge AI controlled over JSON-RPC. */
@Command(name = "bridge", mixinStandardHelpOptions = true,
        description = {
            "Run one Forge AI player under an external game coordinator.",
            "",
            "The coordinator sends newline-delimited JSON-RPC requests and game events on standard input.",
            "Forge writes only JSON-RPC messages to standard output; diagnostics go to standard error."
        },
        footer = {
            "",
            "This command is normally launched by a coordinator rather than used as an interactive game.",
            "Example:",
            "  ./headless.sh bridge -d player1.dck player2.dck --seat 2 --seed 42 --log forge.jsonl"
        })
final class BridgeOptions {
    @Option(names = {"-d", "--decks"}, arity = "2..*", required = true,
            paramLabel = "<deck>",
            description = "Deck file for every player, ordered by one-based seat number")
    private List<File> decks = new ArrayList<>();

    @Option(names = "--seat", required = true, paramLabel = "<number>",
            description = "One-based player seat controlled by this Forge AI")
    private int seat;

    @Option(names = "--seed", required = true, paramLabel = "<number>",
            description = "Seed Forge's internal random-number generator for a repeatable run")
    private long seed;

    @Option(names = "--log", defaultValue = "forge-bridge.jsonl",
            paramLabel = "<path>",
            description = "Write the full-duplex JSON-RPC transcript as JSON Lines (default: ${DEFAULT-VALUE})")
    private Path logPath;

    // Protocol smoke tests use this mode to exercise transport and session setup without starting
    // a Forge game. It remains accepted for the downstream harness but is not a public CLI mode.
    @Option(names = "--skeleton", hidden = true)
    private boolean skeleton;

    List<File> getDecks() {
        return decks;
    }

    int getSeat() {
        return seat;
    }

    long getSeed() {
        return seed;
    }

    Path getLogPath() {
        return logPath;
    }

    boolean isSkeleton() {
        return skeleton;
    }

    void validate() {
        if (seat < 1 || seat > decks.size()) {
            throw new IllegalArgumentException("--seat must refer to one of the supplied decks");
        }
        for (File deck : decks) {
            if (!deck.isFile()) {
                throw new IllegalArgumentException("Deck file not found: " + deck);
            }
        }
    }
}
