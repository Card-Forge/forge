package forge.headless;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/** Command-line configuration for JSON-RPC bridge mode. */
@Command(name = "bridge", mixinStandardHelpOptions = true,
        description = "Run the Forge foreign-bot JSON-RPC bridge over stdin/stdout")
final class BridgeOptions {
    @Option(names = {"-d", "--decks"}, arity = "2..*", required = true,
            description = "Forge deck files, in protocol seat order")
    private List<File> decks = new ArrayList<>();

    @Option(names = "--seat", required = true, description = "One-based seat controlled by Forge AI")
    private int seat;

    @Option(names = "--seed", required = true, description = "Deterministic Forge RNG seed")
    private long seed;

    @Option(names = "--log", defaultValue = "forge-bridge.jsonl",
            description = "Full-duplex JSONL message log")
    private Path logPath;

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
