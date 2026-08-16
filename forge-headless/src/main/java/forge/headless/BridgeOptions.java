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
            "Examples:",
            "  ./headless.sh bridge",
            "  ./headless.sh bridge --listen 127.0.0.1:17772 --log forge.jsonl",
            "",
            "Decks, the controlled seat, and random seed normally arrive in the game_start request."
        })
final class BridgeOptions {
    @Option(names = {"-d", "--decks"}, arity = "2..*",
            paramLabel = "<deck>",
            description = "Legacy: deck file for every player, ordered by one-based seat number")
    private List<File> decks = new ArrayList<>();

    @Option(names = "--seat", paramLabel = "<number>",
            description = "Legacy: require game_start to select this one-based seat")
    private Integer seat;

    @Option(names = "--seed", paramLabel = "<number>",
            description = "Legacy: require game_start to use this Forge random seed")
    private Long seed;

    @Option(names = "--listen", paramLabel = "<host:port>",
            description = "Accept one JSON-RPC connection over TCP instead of using standard input and output")
    private String listenAddress;

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

    Integer getSeat() {
        return seat;
    }

    Long getSeed() {
        return seed;
    }

    String getListenAddress() {
        return listenAddress;
    }

    Path getLogPath() {
        return logPath;
    }

    boolean isSkeleton() {
        return skeleton;
    }

    void validate() {
        boolean legacyConfigured = !decks.isEmpty() || seat != null || seed != null;
        if (legacyConfigured && (decks.isEmpty() || seat == null || seed == null)) {
            throw new IllegalArgumentException("-d/--decks, --seat, and --seed must be supplied together");
        }
        if (legacyConfigured && (seat < 1 || seat > decks.size())) {
            throw new IllegalArgumentException("--seat must refer to one of the supplied decks");
        }
        for (File deck : decks) {
            if (!deck.isFile()) {
                throw new IllegalArgumentException("Deck file not found: " + deck);
            }
        }
    }
}
