package forge.game.decision;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardFactory;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.DeterminismAuditRandom;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class CopySpellResolveFirstOrderTraceTest extends AITest {
    @Test
    public void symmetricNativeCopyRequestsPersistExclusionButRemainValidHistory() throws Exception {
        final Fixture fixture = fixture(2);
        final DeterminismTrace trace = DeterminismTrace.attach(fixture.game, 0,
                new DeterminismAuditRandom(20260810L), fixture.directory);
        try {
            final List<SpellAbility> nativeInsertion = new ArrayList<>(fixture.copies);
            new CopySpellResolveFirstOrderDecisionCoordinator().order(fixture.copies, fixture.player,
                    new CopySpellResolveFirstOrderDecisionProvider(), input -> nativeInsertion);
            trace.finish();

            final List<String> records = Files.readAllLines(fixture.directory.resolve("game-001.decision.trace"),
                    StandardCharsets.UTF_8);
            assertEquals(records.size(), 2);
            assertTrue(records.stream().allMatch(record -> record.startsWith("DECISION_TRACE_V3|")));
            final DecisionTraceRequestRecord request =
                    DecisionTraceRequestRecord.fromSerializedRequest(records.get(0));
            final DecisionTraceResultRecord result = result(records.get(1));
            assertEquals(request.getProfile(), DecisionTraceRequestRecord.Profile.COPY_SPELL_RESOLVE_FIRST_ORDER);
            assertEquals(request.getTeacherLabelEligibility(),
                    DecisionTraceTeacherLabelEligibility.BC_EXCLUDED_PUBLIC_SYMMETRY);
            assertTrue(DecisionTraceTrainingValidator.isHistoryValid(request, result));
            assertFalse(DecisionTraceTrainingValidator.isBCPolicySample(request, result));
        } finally {
            trace.finish();
            delete(fixture.directory);
        }
    }

    @Test
    public void externalCopyRequestIsHistoryValidButNotNativeBcTraining() throws Exception {
        final Fixture fixture = fixture(2);
        final DeterminismTrace trace = DeterminismTrace.attach(fixture.game, 0,
                new DeterminismAuditRandom(20260810L), fixture.directory);
        try {
            final CopySpellResolveFirstOrderDecisionProvider provider =
                    new CopySpellResolveFirstOrderDecisionProvider();
            provider.setResolver(request -> request.getCandidates().get(0));
            new CopySpellResolveFirstOrderDecisionCoordinator().order(fixture.copies, fixture.player,
                    provider, input -> {
                        throw new AssertionError("external L1C must not call native ordering");
                    });
            trace.finish();

            final List<String> records = Files.readAllLines(fixture.directory.resolve("game-001.decision.trace"),
                    StandardCharsets.UTF_8);
            assertEquals(records.size(), 2);
            assertTrue(records.stream().allMatch(record -> record.startsWith("DECISION_TRACE_V3|")));
            final DecisionTraceRequestRecord request =
                    DecisionTraceRequestRecord.fromSerializedRequest(records.get(0));
            final DecisionTraceResultRecord result = result(records.get(1));
            assertFalse(result.isNativeCallbackCompleted());
            assertFalse(result.isMappingAttempted());
            assertTrue(DecisionTraceTrainingValidator.isHistoryValid(request, result));
            assertFalse(DecisionTraceTrainingValidator.isBCPolicySample(request, result));
        } finally {
            trace.finish();
            delete(fixture.directory);
        }
    }

    private static DecisionTraceResultRecord result(final String serialized) {
        final String[] fields = serialized.split("\\|", -1);
        return new DecisionTraceResultRecord(Long.parseLong(fields[2]),
                DecisionTraceResultKind.valueOf(fields[3]), decode(fields[4]),
                Boolean.parseBoolean(fields[5]), Boolean.parseBoolean(fields[6]),
                Boolean.parseBoolean(fields[7]), Boolean.parseBoolean(fields[8]),
                Boolean.parseBoolean(fields[9]));
    }

    private Fixture fixture(final int count) throws Exception {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(0);
        final Card source = addCardToZone("Pyromatics", player, ZoneType.Battlefield);
        final SpellAbility original = source.getFirstSpellAbility();
        original.setActivatingPlayer(player);
        final List<SpellAbility> copies = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            copies.add(CardFactory.copySpellAbilityAndPossiblyHost(original, original, player));
        }
        return new Fixture(game, player, copies, Files.createTempDirectory("frl02l1c-trace-"));
    }

    private static String decode(final String value) {
        return value.replace("%7C", "|").replace("%25", "%");
    }

    private static void delete(final Path directory) throws Exception {
        if (Files.exists(directory)) {
            try (var paths = Files.walk(directory)) {
                paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (final Exception ex) {
                        throw new IllegalStateException(ex);
                    }
                });
            }
        }
    }

    private static final class Fixture {
        private final Game game;
        private final Player player;
        private final List<SpellAbility> copies;
        private final Path directory;

        private Fixture(final Game game, final Player player, final List<SpellAbility> copies,
                final Path directory) {
            this.game = game;
            this.player = player;
            this.copies = copies;
            this.directory = directory;
        }
    }
}
