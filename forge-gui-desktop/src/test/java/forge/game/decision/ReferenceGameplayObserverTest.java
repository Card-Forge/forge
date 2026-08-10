package forge.game.decision;

import forge.ai.AITest;
import forge.game.Game;
import forge.util.DeterminismAuditRandom;
import forge.util.MyRandom;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class ReferenceGameplayObserverTest extends AITest {

    @Test
    public void observerIsStructurallyReadOnlyAndConsumesNoRandomDraws() throws Exception {
        final Game game = initAndCreateGame();
        final DeterminismAuditRandom random = new DeterminismAuditRandom(20260810L);
        MyRandom.setRandom(random);
        final String stateBefore = ForgeStateFingerprint.canonical(game);
        final long drawsBefore = random.getDrawCount();
        final Path directory = Files.createTempDirectory("frl02k0-reference-");
        try {
            final ReferenceGameplayObserver observer = ReferenceGameplayObserver.attach(game, 0, directory);
            observer.finish();

            assertEquals(ForgeStateFingerprint.canonical(game), stateBefore);
            assertEquals(random.getDrawCount(), drawsBefore);
            final List<String> records = Files.readAllLines(directory.resolve("game-001.reference.trace"),
                    StandardCharsets.UTF_8);
            assertTrue(records.get(0).startsWith("REFERENCE_GAMEPLAY_V1|0|ATTACH|FORGE_STATE_V1|"));
            assertTrue(records.get(records.size() - 1).contains("|FINAL|FORGE_STATE_V1|"));
        } finally {
            delete(directory);
        }
    }

    private static void delete(final Path directory) throws Exception {
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
