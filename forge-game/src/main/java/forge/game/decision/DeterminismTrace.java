package forge.game.decision;

import com.google.common.eventbus.Subscribe;
import forge.game.Game;
import forge.game.event.GameEvent;
import forge.game.phase.PhaseHandler;
import forge.game.player.Player;
import forge.util.DeterminismAuditRandom;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

/** Opt-in per-game canonical trace collector for FRL determinism audits. */
public final class DeterminismTrace {
    public static final String OUTPUT_DIRECTORY_PROPERTY = "forge.determinism.traceDir";
    public static final String DECISION_TRACE_VERSION = "DECISION_TRACE_V1";
    public static final String GAMEPLAY_TRACE_VERSION = "GAMEPLAY_TRACE_V1";

    private static final Map<Game, DeterminismTrace> ACTIVE =
            Collections.synchronizedMap(new WeakHashMap<>());

    private final Game game;
    private final int gameIndex;
    private final DeterminismAuditRandom random;
    private final Path outputDirectory;
    private final long rngStartIndex;
    private final List<String> gameplayRecords = new ArrayList<>();
    private final List<String> decisionRecords = new ArrayList<>();
    private boolean finished;

    private DeterminismTrace(final Game game, final int gameIndex, final DeterminismAuditRandom random,
            final Path outputDirectory, final long rngStartIndex) throws IOException {
        this.game = game;
        this.gameIndex = gameIndex;
        this.random = random;
        this.outputDirectory = outputDirectory;
        this.rngStartIndex = rngStartIndex;
        Files.createDirectories(outputDirectory);
        ACTIVE.put(game, this);
        game.subscribeToEvents(this);
        recordGameplayCheckpoint("ATTACH");
    }

    public static DeterminismTrace attach(final Game game, final int gameIndex,
            final DeterminismAuditRandom random, final Path outputDirectory) throws IOException {
        return attach(game, gameIndex, random, outputDirectory, random.getDrawCount());
    }

    public static DeterminismTrace attach(final Game game, final int gameIndex,
            final DeterminismAuditRandom random, final Path outputDirectory,
            final long rngStartIndex) throws IOException {
        return new DeterminismTrace(game, gameIndex, random, outputDirectory, rngStartIndex);
    }

    public static void recordDecision(final Game game, final int actingPlayerSeat, final DecisionType decisionType,
            final String adapterOrStage, final int decisionStepIndex, final boolean forced,
            final String selectedCandidateSemanticKey) {
        final DeterminismTrace trace = ACTIVE.get(game);
        if (trace != null) {
            trace.addDecision(actingPlayerSeat, decisionType, adapterOrStage, decisionStepIndex, forced,
                    selectedCandidateSemanticKey);
        }
    }

    @Subscribe
    public void receive(final GameEvent event) {
        recordGameplayCheckpoint("EVENT:" + event.getClass().getSimpleName());
    }

    public synchronized void recordGameplayCheckpoint(final String trigger) {
        if (finished) {
            return;
        }
        gameplayRecords.add(GAMEPLAY_TRACE_VERSION + '|' + gameplayRecords.size() + '|'
                + canonicalText(trigger) + '|' + ForgeStateFingerprint.canonical(game));
    }

    public synchronized void finish() throws IOException {
        if (finished) {
            return;
        }
        recordGameplayCheckpoint("FINAL");
        final long rngEndIndex = random.getDrawCount();
        final List<String> rngRecords = random.getCanonicalRecords(rngStartIndex, rngEndIndex);
        final List<String> rngDiagnosticRecords = random.getDiagnosticRecords(rngStartIndex, rngEndIndex);
        final String prefix = String.format(Locale.ROOT, "game-%03d", gameIndex + 1);
        write(prefix + ".gameplay.trace", gameplayRecords);
        if (!decisionRecords.isEmpty()) {
            write(prefix + ".decision.trace", decisionRecords);
        }
        write(prefix + ".rng.trace", rngRecords);
        write(prefix + ".rng-diagnostic.trace", rngDiagnosticRecords);
        write(prefix + ".summary.properties", List.of(
                "gameplayHash=" + DeterminismTraceHasher.sha256(gameplayRecords),
                "decisionHash=" + (decisionRecords.isEmpty() ? "ABSENT"
                        : DeterminismTraceHasher.sha256(decisionRecords)),
                "rngHash=" + DeterminismTraceHasher.sha256(rngRecords),
                "rngDrawStart=" + rngStartIndex,
                "rngDrawEnd=" + rngEndIndex,
                "rngDrawCount=" + (rngEndIndex - rngStartIndex),
                "outcome=" + outcome()));
        finished = true;
        ACTIVE.remove(game);
    }

    private synchronized void addDecision(final int actingPlayerSeat, final DecisionType decisionType,
            final String adapterOrStage, final int decisionStepIndex, final boolean forced,
            final String selectedCandidateSemanticKey) {
        if (finished) {
            return;
        }
        final PhaseHandler phase = game.getPhaseHandler();
        decisionRecords.add(String.join("|", DECISION_TRACE_VERSION,
                Integer.toString(decisionRecords.size()), Integer.toString(phase.getTurn()),
                canonicalText(phase.getPhase()), Integer.toString(actingPlayerSeat),
                decisionType.name(), canonicalText(adapterOrStage), Integer.toString(decisionStepIndex),
                Boolean.toString(forced), canonicalText(selectedCandidateSemanticKey)));
    }

    private String outcome() {
        if (!game.isGameOver() || game.getOutcome() == null) {
            return "IN_PROGRESS";
        }
        if (game.getOutcome().isDraw()) {
            return "DRAW";
        }
        final List<Integer> winningSeats = new ArrayList<>();
        for (final Player player : game.getRegisteredPlayers()) {
            if (player.getOutcome() != null && player.getOutcome().hasWon()) {
                winningSeats.add(player.getId());
            }
        }
        Collections.sort(winningSeats);
        if (winningSeats.size() == 1) {
            return "WINNER_SEAT_" + winningSeats.get(0);
        }
        if (winningSeats.isEmpty()) {
            return "MAPPING_FAILED";
        }
        return "INVALID_WINNER_SEATS_" + winningSeats.toString().replace(" ", "");
    }

    private void write(final String fileName, final List<String> records) throws IOException {
        Files.write(outputDirectory.resolve(fileName), records, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
    }

    private static String canonicalText(final Object value) {
        if (value == null) {
            return "";
        }
        return value.toString().replace("%", "%25").replace("|", "%7C")
                .replace("\r", "%0D").replace("\n", "%0A");
    }
}
