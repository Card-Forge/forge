package forge.game.decision;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.player.Player;
import forge.util.DeterminismAuditRandom;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class DeterminismTraceTest extends AITest {

    @Test
    public void attachedSessionWritesCanonicalGameplayDecisionAndRngArtifacts() throws Exception {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(0);
        final DeterminismAuditRandom random = new DeterminismAuditRandom(20260810L);
        final Path directory = Files.createTempDirectory("frl02k0-trace-");
        try {
            final DeterminismTrace trace = DeterminismTrace.attach(game, 0, random, directory);
            DeterminismTrace.recordDecision(game, player.getId(), DecisionType.PRIORITY_ACTION,
                    "PRIORITY", 0, false, "PRIORITY_ACTION|PASS");
            random.nextBoolean();
            trace.recordGameplayCheckpoint("MANUAL");
            trace.finish();

            final Path gameplay = directory.resolve("game-001.gameplay.trace");
            final Path decision = directory.resolve("game-001.decision.trace");
            final Path rng = directory.resolve("game-001.rng.trace");
            final Path rngDiagnostic = directory.resolve("game-001.rng-diagnostic.trace");
            final Path summary = directory.resolve("game-001.summary.properties");
            assertTrue(Files.exists(gameplay));
            assertTrue(Files.exists(decision));
            assertTrue(Files.exists(rng));
            assertTrue(Files.exists(rngDiagnostic));
            assertTrue(Files.exists(summary));
            assertTrue(Files.readString(gameplay, StandardCharsets.UTF_8).startsWith("GAMEPLAY_TRACE_V1|0|"));
            assertTrue(Files.readString(decision, StandardCharsets.UTF_8).startsWith("DECISION_TRACE_V1|0|"));
            assertTrue(Files.readString(rng, StandardCharsets.UTF_8).startsWith("RNG_TRACE_V1|0|"));
            final List<String> summaryLines = Files.readAllLines(summary, StandardCharsets.UTF_8);
            assertTrue(summaryLines.stream().anyMatch(line -> line.startsWith("gameplayHash=")));
            assertTrue(summaryLines.stream().anyMatch(line -> line.startsWith("decisionHash=")));
            assertTrue(summaryLines.stream().anyMatch(line -> line.startsWith("rngHash=")));
        } finally {
            deleteTraceDirectory(directory);
        }
    }

    @Test
    public void decisionArtifactIsAbsentWhenNoNeutralDecisionWasRecorded() throws Exception {
        final Game game = initAndCreateGame();
        final DeterminismAuditRandom random = new DeterminismAuditRandom(7L);
        final Path directory = Files.createTempDirectory("frl02k0-trace-off-");
        try {
            final long drawsBefore = random.getDrawCount();
            final DeterminismTrace trace = DeterminismTrace.attach(game, 0, random, directory);
            trace.finish();

            assertEquals(random.getDrawCount(), drawsBefore,
                    "attaching, snapshotting, hashing, and writing the collector must consume zero RNG");
            assertFalse(Files.exists(directory.resolve("game-001.decision.trace")));
            final List<String> summary = Files.readAllLines(directory.resolve("game-001.summary.properties"),
                    StandardCharsets.UTF_8);
            assertEquals(summary.stream().filter(line -> line.equals("decisionHash=ABSENT")).count(), 1L);
        } finally {
            deleteTraceDirectory(directory);
        }
    }

    private static void deleteTraceDirectory(final Path directory) throws Exception {
        if (!Files.exists(directory)) {
            return;
        }
        try (var files = Files.list(directory)) {
            for (final Path file : files.toList()) {
                Files.deleteIfExists(file);
            }
        }
        Files.deleteIfExists(directory);
    }
}
