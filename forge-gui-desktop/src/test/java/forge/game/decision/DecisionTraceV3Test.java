package forge.game.decision;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.player.Player;
import forge.util.DeterminismAuditRandom;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class DecisionTraceV3Test extends AITest {
    @Test
    public void legacyV2OrderRequestDefaultsToEligibleAndNonOrderDefaultsToNotApplicable() {
        final DecisionTraceRequestRecord order = DecisionTraceRequestRecord.fromSerializedRequest(
                "DECISION_TRACE_V2|REQUEST|0|1|MAIN|0|ORDER|SIMULTANEOUS_TRIGGER_ORDER|0|false|"
                        + "[RESOLVE_FIRST%7C1,RESOLVE_FIRST%7C2]|hash");
        assertEquals(order.getProfile(), DecisionTraceRequestRecord.Profile.SIMULTANEOUS_TRIGGER_ORDER);
        assertEquals(order.getTeacherLabelEligibility(), DecisionTraceTeacherLabelEligibility.BC_ELIGIBLE);

        final DecisionTraceRequestRecord nonOrder = DecisionTraceRequestRecord.fromSerializedRequest(
                "DECISION_TRACE_V2|REQUEST|1|1|MAIN|0|MULLIGAN|KEEP_OR_REDRAW|0|false|"
                        + "[MULLIGAN%7CKEEP,MULLIGAN%7CREDRAW]|hash");
        assertEquals(nonOrder.getProfile(), DecisionTraceRequestRecord.Profile.OTHER);
        assertEquals(nonOrder.getTeacherLabelEligibility(),
                DecisionTraceTeacherLabelEligibility.NOT_APPLICABLE);
    }

    @Test
    public void legacyV2CopySpellStageCanNeverBecomeBcEligible() {
        final DecisionTraceRequestRecord request = DecisionTraceRequestRecord.fromSerializedRequest(
                "DECISION_TRACE_V2|REQUEST|0|1|MAIN|0|ORDER|COPY_SPELL_RESOLVE_FIRST_ORDER|0|false|"
                        + "[RESOLVE_FIRST%7C1,RESOLVE_FIRST%7C2]|hash");

        assertEquals(request.getProfile(), DecisionTraceRequestRecord.Profile.OTHER);
        assertEquals(request.getTeacherLabelEligibility(),
                DecisionTraceTeacherLabelEligibility.NOT_APPLICABLE);
        assertFalse(DecisionTraceTrainingValidator.isBCPolicySample(request, chosen(request)));
    }

    @Test
    public void nonSymmetricL1CNativeRequestPersistsEligibilityAndRemainsBcEligible() throws Exception {
        final Fixture fixture = fixture();
        final DeterminismTrace trace = DeterminismTrace.attach(fixture.game, 0,
                new DeterminismAuditRandom(20260810L), fixture.directory);
        try {
            final DecisionRequest request = request(fixture.player, 1L,
                    new CopySpellResolveFirstOrderItem(1L,
                            new CopySpellResolveFirstOrderSourceProjection("A"), ApiType.DealDamage,
                            CopySpellResolveFirstOrderItemKind.COPIED_SPELL),
                    new CopySpellResolveFirstOrderItem(2L,
                            new CopySpellResolveFirstOrderSourceProjection("B"), ApiType.DealDamage,
                            CopySpellResolveFirstOrderItemKind.COPIED_SPELL));
            final DeterminismTrace.RequestHandle handle = DeterminismTrace.recordRequest(fixture.game,
                    fixture.player.getId(), request, "COPY_SPELL_RESOLVE_FIRST_ORDER", 0,
                    DecisionTraceRequestRecord.Profile.COPY_SPELL_RESOLVE_FIRST_ORDER,
                    DecisionTraceTeacherLabelEligibility.BC_ELIGIBLE);
            handle.recordNativeMappedResult(request.getCandidates().get(0));
            trace.finish();

            final List<String> records = Files.readAllLines(fixture.directory.resolve("game-001.decision.trace"),
                    StandardCharsets.UTF_8);
            final DecisionTraceRequestRecord parsed =
                    DecisionTraceRequestRecord.fromSerializedRequest(records.get(0));
            assertEquals(parsed.getTeacherLabelEligibility(), DecisionTraceTeacherLabelEligibility.BC_ELIGIBLE);
            assertTrue(DecisionTraceTrainingValidator.isBCPolicySample(parsed, result(records.get(1))));
            assertTrue(Files.readAllLines(fixture.directory.resolve("game-001.summary.properties"),
                    StandardCharsets.UTF_8).contains("decisionTraceVersion=DECISION_TRACE_V3"));
        } finally {
            trace.finish();
            delete(fixture.directory);
        }
    }

    @Test
    public void missingUnknownAndMalformedV3L1CEligibilityFailClosed() {
        final String valid = "DECISION_TRACE_V3|REQUEST|0|1|MAIN|0|ORDER|"
                + "COPY_SPELL_RESOLVE_FIRST_ORDER|0|false|[RESOLVE_FIRST%7C1,RESOLVE_FIRST%7C2]|hash|"
                + "COPY_SPELL_RESOLVE_FIRST_ORDER|BC_ELIGIBLE";
        final List<String> malformed = List.of(
                valid.substring(0, valid.lastIndexOf('|')),
                valid.replace("BC_ELIGIBLE", "UNKNOWN_LABEL"),
                valid.replace("BC_ELIGIBLE", ""),
                valid.replace("|COPY_SPELL_RESOLVE_FIRST_ORDER|0|false|",
                        "||0|false|"));
        for (final String serialized : malformed) {
            final DecisionTraceRequestRecord request =
                    DecisionTraceRequestRecord.fromSerializedRequest(serialized);
            if (!serialized.endsWith("|BC_ELIGIBLE")) {
                assertNull(request.getTeacherLabelEligibility());
            }
            final DecisionTraceResultRecord result = chosen(request);
            assertTrue(DecisionTraceTrainingValidator.isHistoryValid(request, result));
            assertFalse(DecisionTraceTrainingValidator.isBCPolicySample(request, result));
        }
    }

    @Test
    public void l1ThenL1CBeforeFinishWritesOneWholeV3FileWithoutV2Prefix() throws Exception {
        final Fixture fixture = fixture();
        final DeterminismTrace trace = DeterminismTrace.attach(fixture.game, 0,
                new DeterminismAuditRandom(20260810L), fixture.directory);
        try {
            final DecisionRequest l1Request = simultaneousTriggerOrderRequest(fixture.player, 1L);
            final DeterminismTrace.RequestHandle l1 = DeterminismTrace.recordRequest(fixture.game,
                    fixture.player.getId(), l1Request, "SIMULTANEOUS_TRIGGER_ORDER", 0,
                    DecisionTraceRequestRecord.Profile.SIMULTANEOUS_TRIGGER_ORDER,
                    DecisionTraceTeacherLabelEligibility.BC_ELIGIBLE);
            l1.recordMappedResult(l1Request.getCandidates().get(0));

            final DecisionRequest l1cRequest = request(fixture.player, 2L,
                    new CopySpellResolveFirstOrderItem(1L,
                            new CopySpellResolveFirstOrderSourceProjection("A"), ApiType.DealDamage,
                            CopySpellResolveFirstOrderItemKind.COPIED_SPELL),
                    new CopySpellResolveFirstOrderItem(2L,
                            new CopySpellResolveFirstOrderSourceProjection("B"), ApiType.DealDamage,
                            CopySpellResolveFirstOrderItemKind.COPIED_SPELL));
            final DeterminismTrace.RequestHandle l1c = DeterminismTrace.recordRequest(fixture.game,
                    fixture.player.getId(), l1cRequest, "COPY_SPELL_RESOLVE_FIRST_ORDER", 0,
                    DecisionTraceRequestRecord.Profile.COPY_SPELL_RESOLVE_FIRST_ORDER,
                    DecisionTraceTeacherLabelEligibility.BC_ELIGIBLE);
            l1c.recordNativeMappedResult(l1cRequest.getCandidates().get(0));
            trace.finish();

            final List<String> records = Files.readAllLines(fixture.directory.resolve("game-001.decision.trace"),
                    StandardCharsets.UTF_8);
            assertEquals(records.size(), 4);
            assertTrue(records.stream().allMatch(record -> record.startsWith("DECISION_TRACE_V3|")));
            assertFalse(records.stream().anyMatch(record -> record.startsWith("DECISION_TRACE_V2|")));
            assertEquals(records.get(0).split("\\|", -1).length, 14);
            final DecisionTraceRequestRecord parsedL1 =
                    DecisionTraceRequestRecord.fromSerializedRequest(records.get(0));
            assertEquals(parsedL1.getDecisionType(), DecisionType.ORDER);
            assertEquals(parsedL1.getProfile(), DecisionTraceRequestRecord.Profile.SIMULTANEOUS_TRIGGER_ORDER);
            assertEquals(parsedL1.getTeacherLabelEligibility(),
                    DecisionTraceTeacherLabelEligibility.BC_ELIGIBLE);
            assertTrue(Files.readAllLines(fixture.directory.resolve("game-001.summary.properties"),
                    StandardCharsets.UTF_8).contains("decisionTraceVersion=DECISION_TRACE_V3"));
        } finally {
            trace.finish();
            delete(fixture.directory);
        }
    }

    private static DecisionRequest request(final Player player, final long sessionId,
            final CopySpellResolveFirstOrderItem first, final CopySpellResolveFirstOrderItem second) {
        final CopySpellResolveFirstOrderContext context = new CopySpellResolveFirstOrderContext(
                CopySpellResolveFirstOrderProfile.COPY_SPELL_RESOLVE_FIRST_ORDER,
                OrderDirection.RESOLVE_FIRST, sessionId, 0, 2, player.getId());
        return new DecisionRequest(sessionId, DecisionType.ORDER,
                List.of(LegalCandidate.copySpellResolveFirstOrder(0,
                                CopySpellResolveFirstOrderItemKind.COPIED_SPELL, first),
                        LegalCandidate.copySpellResolveFirstOrder(1,
                                CopySpellResolveFirstOrderItemKind.COPIED_SPELL, second)), context);
    }

    private DecisionRequest simultaneousTriggerOrderRequest(final Player player, final long sessionId) {
        final SimultaneousTriggerOrderContext context = new SimultaneousTriggerOrderContext(
                SimultaneousTriggerOrderProfile.SIMULTANEOUS_TRIGGER_ORDER,
                OrderDirection.RESOLVE_FIRST, sessionId, 0, 2, player.getId());
        return new DecisionRequest(sessionId, DecisionType.ORDER,
                List.of(LegalCandidate.order(0, OrderCandidateKind.SELECT_RESOLVE_FIRST,
                                new SimultaneousTriggerOrderItem(1L,
                                        new CardSelectionCard(addCard("Island", player)),
                                        forge.game.trigger.TriggerType.AbilityCast, ApiType.Effect)),
                        LegalCandidate.order(1, OrderCandidateKind.SELECT_RESOLVE_FIRST,
                                new SimultaneousTriggerOrderItem(2L,
                                        new CardSelectionCard(addCard("Mountain", player)),
                                        forge.game.trigger.TriggerType.AbilityCast, ApiType.Effect))),
                context);
    }

    private static DecisionTraceResultRecord chosen(final DecisionTraceRequestRecord request) {
        return new DecisionTraceResultRecord(request.getTraceRequestIndex(), DecisionTraceResultKind.CHOSEN,
                request.getLegalCandidates().get(0), true, true, false, false, false);
    }

    private static DecisionTraceResultRecord result(final String serialized) {
        final String[] fields = serialized.split("\\|", -1);
        return new DecisionTraceResultRecord(Long.parseLong(fields[2]),
                DecisionTraceResultKind.valueOf(fields[3]), fields[4].replace("%7C", "|"),
                Boolean.parseBoolean(fields[5]), Boolean.parseBoolean(fields[6]),
                Boolean.parseBoolean(fields[7]), Boolean.parseBoolean(fields[8]),
                Boolean.parseBoolean(fields[9]));
    }

    private Fixture fixture() throws Exception {
        final Game game = initAndCreateGame();
        return new Fixture(game, game.getPlayers().get(0), Files.createTempDirectory("frl02l1c-v3-"));
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
        private final Path directory;

        private Fixture(final Game game, final Player player, final Path directory) {
            this.game = game;
            this.player = player;
            this.directory = directory;
        }
    }
}
