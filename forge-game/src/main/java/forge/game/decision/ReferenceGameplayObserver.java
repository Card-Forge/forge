package forge.game.decision;

import com.google.common.eventbus.Subscribe;
import forge.game.Game;
import forge.game.event.GameEvent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Small audit observer installed identically on both sides of collector differential tests. */
public final class ReferenceGameplayObserver {
    public static final String TRACE_VERSION = "REFERENCE_GAMEPLAY_V1";
    public static final String OUTPUT_DIRECTORY_PROPERTY = "forge.determinism.referenceDir";

    private final Game game;
    private final int gameIndex;
    private final Path outputDirectory;
    private final List<String> records = new ArrayList<>();
    private boolean finished;

    private ReferenceGameplayObserver(final Game game, final int gameIndex, final Path outputDirectory)
            throws IOException {
        this.game = game;
        this.gameIndex = gameIndex;
        this.outputDirectory = outputDirectory;
        Files.createDirectories(outputDirectory);
        game.subscribeToEvents(this);
        record("ATTACH");
    }

    public static ReferenceGameplayObserver attach(final Game game, final int gameIndex,
            final Path outputDirectory) throws IOException {
        return new ReferenceGameplayObserver(game, gameIndex, outputDirectory);
    }

    @Subscribe
    public void receive(final GameEvent event) {
        record(event.getClass().getSimpleName());
    }

    public synchronized void finish() throws IOException {
        if (finished) {
            return;
        }
        record("FINAL");
        final String prefix = String.format(Locale.ROOT, "game-%03d", gameIndex + 1);
        Files.write(outputDirectory.resolve(prefix + ".reference.trace"), records, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        Files.write(outputDirectory.resolve(prefix + ".reference-summary.properties"), List.of(
                "referenceGameplayVersion=" + TRACE_VERSION,
                "referenceGameplayHash=" + DeterminismTraceHasher.sha256(records),
                "finalStateHash=" + DeterminismTraceHasher.sha256(
                        List.of(ForgeStateFingerprint.canonical(game)))), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        finished = true;
    }

    private synchronized void record(final String trigger) {
        if (!finished) {
            records.add(TRACE_VERSION + '|' + records.size() + '|' + canonicalText(trigger) + '|'
                    + ForgeStateFingerprint.canonical(game));
        }
    }

    private static String canonicalText(final String value) {
        return value.replace("%", "%25").replace("|", "%7C")
                .replace("\r", "%0D").replace("\n", "%0A");
    }
}
