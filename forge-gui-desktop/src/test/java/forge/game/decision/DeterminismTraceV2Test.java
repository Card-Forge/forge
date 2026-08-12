package forge.game.decision;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.DeterminismAuditRandom;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;

public class DeterminismTraceV2Test extends AITest {

    @Test
    public void requestCarriesOrderedUniqueSemanticCandidatesAndMappedResultClosesIt() throws Exception {
        final Fixture fixture = attachTrace();
        try {
            final DecisionRequest request = mulliganRequest(1L);
            final DeterminismTrace.RequestHandle handle = DeterminismTrace.recordRequest(fixture.game,
                    fixture.player.getId(), request, "KEEP_OR_REDRAW", 0);

            assertEquals(handle.getRequestRecord().getLegalCandidates(),
                    List.of("MULLIGAN|KEEP", "MULLIGAN|REDRAW"));
            assertFalse(handle.getRequestRecord().isForced());
            assertFalse(handle.getResultRecord().isPresent());

            handle.recordMappedResult(request.getCandidates().get(1));

            assertEquals(handle.getResultRecord().orElseThrow().getKind(), DecisionTraceResultKind.CHOSEN);
            assertEquals(handle.getResultRecord().orElseThrow().getSelectedCandidateSemanticKey(),
                    "MULLIGAN|REDRAW");
            assertTrue(DecisionTraceTrainingValidator.isHistoryValid(handle.getRequestRecord(),
                    handle.getResultRecord().orElseThrow()));
            assertTrue(DecisionTraceTrainingValidator.isBCPolicySample(handle.getRequestRecord(),
                    handle.getResultRecord().orElseThrow()));
        } finally {
            fixture.finishAndDelete();
        }
    }

    @Test
    public void forcedRequestWaitsForNativeMappingAndIsExcludedFromPolicyTraining() throws Exception {
        final Fixture fixture = attachTrace();
        try {
            final DecisionRequest request = forcedMulliganRequest(2L);
            final DeterminismTrace.RequestHandle handle = DeterminismTrace.recordRequest(fixture.game,
                    fixture.player.getId(), request, "KEEP_OR_REDRAW", 0);

            assertFalse(handle.getResultRecord().isPresent(),
                    "recordRequest must not close a forced request before the native callback returns");
            handle.recordMappedResult(request.getCandidates().get(0));

            assertEquals(handle.getResultRecord().orElseThrow().getKind(), DecisionTraceResultKind.FORCED);
            assertTrue(DecisionTraceTrainingValidator.isHistoryValid(handle.getRequestRecord(),
                    handle.getResultRecord().orElseThrow()));
            assertFalse(DecisionTraceTrainingValidator.isBCPolicySample(handle.getRequestRecord(),
                    handle.getResultRecord().orElseThrow()));
        } finally {
            fixture.finishAndDelete();
        }
    }

    @Test
    public void externalConfirmationChoiceIsValidHistoryButNotBcSample() throws Exception {
        final Fixture fixture = attachTrace();
        try {
            final CardSelectionCard source = new CardSelectionCard(addCard("Gelectrode", fixture.player));
            final DecisionRequest request = new DecisionRequest(13L, DecisionType.CONFIRMATION,
                    List.of(LegalCandidate.confirmation(0, ConfirmationCandidateKind.ACCEPT),
                            LegalCandidate.confirmation(1, ConfirmationCandidateKind.DECLINE)),
                    new ConfirmationDecisionContext(ConfirmationTriggerProfile.GELECTRODE_SPELL_CAST_UNTAP_SELF,
                            ConfirmationEventType.SPELL_CAST, source, fixture.player.getId(), fixture.player.getId()));
            final DeterminismTrace.RequestHandle handle = record(fixture, request);

            handle.recordExternalChosenResult(request.getCandidates().get(0));

            final DecisionTraceResultRecord result = handle.getResultRecord().orElseThrow();
            assertTrue(DecisionTraceTrainingValidator.isHistoryValid(handle.getRequestRecord(), result));
            assertFalse(result.isNativeCallbackCompleted());
            assertFalse(result.isMappingAttempted());
            assertFalse(DecisionTraceTrainingValidator.isBCPolicySample(handle.getRequestRecord(), result));
        } finally {
            fixture.finishAndDelete();
        }
    }

    @Test
    public void externalTargetChoiceRequiresFalseNativeAndMappingFlagsAndIsNotBcSample() throws Exception {
        final Fixture fixture = attachTrace();
        try {
            final DecisionRequest request = targetRequest(fixture);
            final DeterminismTrace.RequestHandle handle = recordTarget(fixture, request);
            final LegalCandidate selected = request.getCandidates().get(0);
            final DecisionTraceRequestRecord requestRecord = handle.getRequestRecord();
            final DecisionTraceResultRecord external = chosenResult(requestRecord, selected, false, false);

            assertTrue(DecisionTraceTrainingValidator.isHistoryValid(requestRecord, external));
            assertFalse(DecisionTraceTrainingValidator.isBCPolicySample(requestRecord, external));
            assertFalse(DecisionTraceTrainingValidator.isHistoryValid(requestRecord,
                    chosenResult(requestRecord, selected, true, false)));
            assertFalse(DecisionTraceTrainingValidator.isHistoryValid(requestRecord,
                    chosenResult(requestRecord, selected, false, true)));
        } finally {
            fixture.finishAndDelete();
        }
    }

    @Test
    public void resultTaxonomyKeepsUnobservedMappingFailureAndRollbackDistinct() throws Exception {
        final Fixture fixture = attachTrace();
        try {
            final DeterminismTrace.RequestHandle unobserved = record(fixture, mulliganRequest(3L));
            unobserved.recordUnobserved();
            final DeterminismTrace.RequestHandle mappingFailed = record(fixture, mulliganRequest(4L));
            mappingFailed.recordMappingFailed();
            final DeterminismTrace.RequestHandle rollback = record(fixture, mulliganRequest(5L));
            rollback.recordEngineRollback();

            assertEquals(unobserved.getResultRecord().orElseThrow().getKind(), DecisionTraceResultKind.UNOBSERVED);
            assertEquals(mappingFailed.getResultRecord().orElseThrow().getKind(),
                    DecisionTraceResultKind.MAPPING_FAILED);
            assertEquals(rollback.getResultRecord().orElseThrow().getKind(),
                    DecisionTraceResultKind.ENGINE_ROLLBACK);
            assertFalse(DecisionTraceTrainingValidator.isBCPolicySample(unobserved.getRequestRecord(),
                    unobserved.getResultRecord().orElseThrow()));
            assertFalse(DecisionTraceTrainingValidator.isBCPolicySample(mappingFailed.getRequestRecord(),
                    mappingFailed.getResultRecord().orElseThrow()));
        } finally {
            fixture.finishAndDelete();
        }
    }

    @Test
    public void duplicateAndIllegalTerminalResultsAreRejected() throws Exception {
        final Fixture fixture = attachTrace();
        try {
            final DecisionRequest request = mulliganRequest(6L);
            final DeterminismTrace.RequestHandle handle = record(fixture, request);
            handle.recordMappedResult(request.getCandidates().get(0));
            assertThrows(IllegalStateException.class, handle::recordUnobserved);

            final DecisionRequest other = mulliganRequest(7L);
            final DeterminismTrace.RequestHandle otherHandle = record(fixture, other);
            assertThrows(IllegalArgumentException.class,
                    () -> otherHandle.recordMappedResult(LegalCandidate.pass(9)));
        } finally {
            fixture.finishAndDelete();
        }
    }

    @Test
    public void finishClosesEveryOpenRequestAsTraceIncompleteAndWritesVersionMetadata() throws Exception {
        final Fixture fixture = attachTrace();
        final DeterminismTrace.RequestHandle handle = record(fixture, mulliganRequest(8L));
        fixture.trace.finish();

        assertEquals(handle.getResultRecord().orElseThrow().getKind(), DecisionTraceResultKind.TRACE_INCOMPLETE);
        final List<String> records = Files.readAllLines(fixture.directory.resolve("game-001.decision.trace"),
                StandardCharsets.UTF_8);
        assertTrue(records.get(0).startsWith("DECISION_TRACE_V2|REQUEST|0|"));
        assertTrue(records.get(0).contains("|false|[MULLIGAN%7CKEEP,MULLIGAN%7CREDRAW]|"));
        assertTrue(records.get(1).startsWith("DECISION_TRACE_V2|RESULT|0|TRACE_INCOMPLETE|"));
        final List<String> summary = Files.readAllLines(fixture.directory.resolve("game-001.summary.properties"),
                StandardCharsets.UTF_8);
        assertTrue(summary.contains("decisionTraceVersion=DECISION_TRACE_V2"));
        assertTrue(summary.contains("gameplayTraceVersion=GAMEPLAY_TRACE_V1"));
        assertTrue(summary.contains("rngTraceVersion=RNG_TRACE_V1"));
        fixture.delete();
    }

    @Test
    public void v2HashingAndFirstDivergenceCoverRequestAndResultRecords() throws Exception {
        final Fixture fixture = attachTrace();
        final DecisionRequest request = mulliganRequest(12L);
        record(fixture, request).recordMappedResult(request.getCandidates().get(0));
        fixture.trace.finish();
        final List<String> records = Files.readAllLines(fixture.directory.resolve("game-001.decision.trace"),
                StandardCharsets.UTF_8);

        assertEquals(DeterminismTraceHasher.sha256(records),
                DeterminismTraceHasher.sha256(List.copyOf(records)));
        final List<String> requestChanged = List.of(records.get(0).replace("|MULLIGAN|", "|MODE|"),
                records.get(1));
        final List<String> resultChanged = List.of(records.get(0),
                records.get(1).replace("|CHOSEN|", "|MAPPING_FAILED|"));
        assertEquals(DeterminismTraceHasher.firstDivergence(records, requestChanged), 0);
        assertEquals(DeterminismTraceHasher.firstDivergence(records, resultChanged), 1);
        fixture.delete();
    }

    @Test
    public void trainingValidatorRejectsUnknownResultReferencesAndDuplicateSemanticKeys() throws Exception {
        final Fixture fixture = attachTrace();
        try {
            final DeterminismTrace.RequestHandle first = record(fixture, mulliganRequest(9L));
            final DeterminismTrace.RequestHandle second = record(fixture, mulliganRequest(10L));
            second.recordUnobserved();
            assertThrows(IllegalArgumentException.class, () -> DecisionTraceTrainingValidator.validateRecords(
                    List.of(first.getRequestRecord()), List.of(second.getResultRecord().orElseThrow())));
            assertThrows(IllegalArgumentException.class, () -> DecisionTraceTrainingValidator.validateRecords(
                    List.of(first.getRequestRecord()), List.of()));

            final MulliganContext context = mulliganContext();
            assertThrows(IllegalArgumentException.class, () -> new DecisionRequest(11L, DecisionType.MULLIGAN,
                    List.of(LegalCandidate.mulligan(0, MulliganCandidateKind.KEEP),
                            LegalCandidate.mulligan(1, MulliganCandidateKind.KEEP)), context));
        } finally {
            fixture.finishAndDelete();
        }
    }

    private DeterminismTrace.RequestHandle record(final Fixture fixture, final DecisionRequest request) {
        return DeterminismTrace.recordRequest(fixture.game, fixture.player.getId(), request, "KEEP_OR_REDRAW", 0);
    }

    private DeterminismTrace.RequestHandle recordTarget(final Fixture fixture, final DecisionRequest request) {
        return DeterminismTrace.recordRequest(fixture.game, fixture.player.getId(), request,
                "TRIGGERED_TARGET", 0);
    }

    private DecisionRequest targetRequest(final Fixture fixture) {
        final Player chooser = fixture.player;
        final Player opponent = fixture.game.getPlayers().stream()
                .filter(player -> player != chooser)
                .findFirst()
                .orElseThrow();
        final Card source = addCardToZone("Dark Banishing", chooser, ZoneType.Hand);
        final SpellAbility ability = source.getSpellAbilities().stream()
                .filter(SpellAbility::usesTargeting)
                .findFirst()
                .orElseThrow();
        ability.setActivatingPlayer(chooser);
        addCard("Runeclaw Bear", opponent);
        addCard("Llanowar Elves", opponent);

        final TargetDecisionProvider.Generation generation = new TargetDecisionProvider()
                .generateTargetRequest(ability, chooser, null);
        assertEquals(generation.getStatus(), TargetDecisionProvider.Status.DECISION);
        assertFalse(generation.getRequest().isForced());
        return generation.getRequest();
    }

    private static DecisionTraceResultRecord chosenResult(final DecisionTraceRequestRecord request,
            final LegalCandidate selected, final boolean nativeCallbackCompleted,
            final boolean mappingAttempted) {
        return new DecisionTraceResultRecord(request.getTraceRequestIndex(), DecisionTraceResultKind.CHOSEN,
                selected.getSemanticKey(), nativeCallbackCompleted, mappingAttempted,
                false, false, false);
    }

    private Fixture attachTrace() throws Exception {
        final Game game = initAndCreateGame();
        final Path directory = Files.createTempDirectory("frl02k0-v2-");
        final DeterminismTrace trace = DeterminismTrace.attach(game, 0,
                new DeterminismAuditRandom(20260810L), directory);
        return new Fixture(game, game.getPlayers().get(0), trace, directory);
    }

    private static DecisionRequest mulliganRequest(final long id) {
        return new DecisionRequest(id, DecisionType.MULLIGAN,
                List.of(LegalCandidate.mulligan(0, MulliganCandidateKind.KEEP),
                        LegalCandidate.mulligan(1, MulliganCandidateKind.REDRAW)), mulliganContext());
    }

    private static DecisionRequest forcedMulliganRequest(final long id) {
        return new DecisionRequest(id, DecisionType.MULLIGAN,
                List.of(LegalCandidate.mulligan(0, MulliganCandidateKind.KEEP)), mulliganContext());
    }

    private static MulliganContext mulliganContext() {
        return new MulliganContext(1, 1L, 0, 0, 0, 1, 0, 7, MulliganStage.KEEP_OR_REDRAW, List.of());
    }

    private static final class Fixture {
        private final Game game;
        private final Player player;
        private final DeterminismTrace trace;
        private final Path directory;

        private Fixture(final Game game, final Player player, final DeterminismTrace trace, final Path directory) {
            this.game = game;
            this.player = player;
            this.trace = trace;
            this.directory = directory;
        }

        private void finishAndDelete() throws Exception {
            trace.finish();
            delete();
        }

        private void delete() throws Exception {
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
}
