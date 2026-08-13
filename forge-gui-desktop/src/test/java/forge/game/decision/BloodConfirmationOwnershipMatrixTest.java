package forge.game.decision;

import forge.ai.AITest;
import forge.ai.LobbyPlayerAi;
import forge.ai.PlayerControllerAi;
import forge.game.Game;
import forge.game.GameObject;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.trigger.WrappedAbility;
import forge.game.zone.ZoneType;
import forge.util.DeterminismAuditRandom;
import forge.util.MyRandom;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/** First bounded FRL-02K-D1 ownership-matrix RED slice. */
public class BloodConfirmationOwnershipMatrixTest extends AITest {
    private static final long DETERMINISTIC_SEED = 20260813L;

    @Test
    public void externalTargetAndExternalConfirmationUseOneBloodEffectRoute() throws Exception {
        final Random previousRandom = MyRandom.getRandom();
        final DeterminismAuditRandom auditRandom = new DeterminismAuditRandom(DETERMINISTIC_SEED);
        Path traceDirectory = null;
        DeterminismTrace trace = null;
        MyRandom.setRandom(auditRandom);

        try {
            final BloodFixture fixture = bloodFixture();
            assertBloodFixture(fixture);

            final CountingController controller = installController(fixture);
            final AtomicInteger targetResolverCalls = new AtomicInteger();
            final AtomicReference<DecisionRequest> targetRequest = new AtomicReference<>();
            final AtomicReference<LegalCandidate> selectedTarget = new AtomicReference<>();
            controller.setTargetDecisionResolver(request -> {
                targetResolverCalls.incrementAndGet();
                targetRequest.set(request);
                final LegalCandidate selected = request.getCandidates().stream()
                        .filter(candidate -> candidate.getTargetKind() == TargetCandidateKind.TARGET_CARD)
                        .filter(candidate -> candidate.getTarget() == fixture.targetA())
                        .findFirst()
                        .orElseThrow(() -> new AssertionError("Blood target A is not in the legal request"));
                selectedTarget.set(selected);
                return selected;
            });

            final AtomicInteger confirmationResolverCalls = new AtomicInteger();
            final AtomicReference<DecisionRequest> confirmationRequest = new AtomicReference<>();
            final AtomicReference<LegalCandidate> selectedConfirmation = new AtomicReference<>();
            controller.getConfirmationDecisionProvider().setResolver(request -> {
                confirmationResolverCalls.incrementAndGet();
                confirmationRequest.set(request);
                final LegalCandidate selected = request.getCandidates().stream()
                        .filter(candidate -> candidate.getConfirmationKind() == ConfirmationCandidateKind.ACCEPT)
                        .findFirst()
                        .orElseThrow(() -> new AssertionError("ACCEPT is not in the confirmation request"));
                selectedConfirmation.set(selected);
                return selected;
            });

            traceDirectory = Files.createTempDirectory("frl02k-d1-blood-confirmation-");
            trace = DeterminismTrace.attach(fixture.game(), 0, auditRandom, traceDirectory);

            UnsupportedConfirmationDecisionException unsupportedBaseline = null;
            final int stackSizeBefore = fixture.game().getStack().size();
            GameObject liveTargetAtStackInsertion = null;
            try {
                controller.orderAndPlaySimultaneousSa(List.of(fixture.wrapper()));
                if (fixture.ability().getTargets().size() == 1) {
                    liveTargetAtStackInsertion = fixture.ability().getTargets().get(0);
                }
                fixture.game().getStack().resolveStack();
            } catch (final UnsupportedConfirmationDecisionException ex) {
                // Current B1 is intentionally narrower than Blood. Keep the RED at assertions.
                unsupportedBaseline = ex;
            }

            trace.finish();
            final Path decisionTrace = traceDirectory.resolve("game-001.decision.trace");
            final List<String> records = Files.exists(decisionTrace)
                    ? Files.readAllLines(decisionTrace, StandardCharsets.UTF_8) : List.of();
            final List<String> requestRecords = records.stream()
                    .filter(record -> record.startsWith("DECISION_TRACE_V2|REQUEST|"))
                    .toList();
            final List<String> resultRecords = records.stream()
                    .filter(record -> record.startsWith("DECISION_TRACE_V2|RESULT|"))
                    .toList();
            final List<String> targetRequestRecords = requestRecords.stream()
                    .filter(record -> "TARGET".equals(traceField(record, 6)))
                    .toList();
            final List<String> confirmationRequestRecords = requestRecords.stream()
                    .filter(record -> "CONFIRMATION".equals(traceField(record, 6)))
                    .toList();

            final SoftAssert assertions = new SoftAssert();
            assertions.assertNull(unsupportedBaseline,
                    "external Blood confirmation must complete without an unsupported-profile exception");
            assertions.assertEquals(controller.orderSimultaneousSaCalls(), 1,
                    "the production order route must invoke deterministic ordering once");
            assertions.assertEquals(targetResolverCalls.get(), 1,
                    "the external TARGET resolver must be called exactly once");
            assertions.assertEquals(confirmationResolverCalls.get(), 1,
                    "the external CONFIRMATION resolver must be called exactly once");
            final LegalCandidate selectedTargetCandidate = selectedTarget.get();
            assertions.assertNotNull(selectedTargetCandidate,
                    "the external TARGET resolver must select a candidate");
            if (selectedTargetCandidate != null) {
                assertions.assertSame(selectedTargetCandidate.getTarget(), fixture.targetA(),
                        "the TARGET resolver must select card A");
            }
            assertions.assertEquals(fixture.game().getStack().size(), stackSizeBefore,
                    "the queued trigger must resolve without leaving a stack entry");
            assertions.assertEquals(fixture.ability().getTargets().size(), 1,
                    "the live underlying ability must retain exactly one target");
            assertions.assertTrue(fixture.ability().getTargets().contains(fixture.targetA()),
                    "the live underlying ability must retain target A");
            assertions.assertFalse(fixture.ability().getTargets().contains(fixture.targetB()),
                    "the live underlying ability must never contain temporary target B");
            assertions.assertSame(liveTargetAtStackInsertion, fixture.targetA(),
                    "the live ChangeZone target C must remain exactly target A");
            assertions.assertEquals(controller.nativeTargetCallbackCalls(), 0,
                    "external TARGET ownership must not invoke the native target callback");
            assertions.assertEquals(controller.nativeConfirmationCallbackCalls(), 0,
                    "external CONFIRMATION ownership must not invoke the native confirmation callback");
            assertions.assertNull(controller.targetAtNativeConfirmation(),
                    "native confirmation must not create a temporary target B");
            final Card currentTargetA = fixture.game().getCardState(fixture.targetA(), null);
            final Card currentTargetB = fixture.game().getCardState(fixture.targetB(), null);
            assertions.assertEquals(fixture.game().getCardsIn(ZoneType.Exile).stream()
                    .filter(card -> samePublicCardIdentity(card, currentTargetA)).count(), 1L,
                    "the existing ChangeZone effect must consume card A");
            assertions.assertEquals(fixture.game().getCardsIn(ZoneType.Graveyard).stream()
                    .filter(card -> samePublicCardIdentity(card, currentTargetA)).count(), 0L,
                    "card A must leave the graveyard exactly once");
            assertions.assertEquals(fixture.game().getCardsIn(ZoneType.Exile).stream()
                    .filter(card -> samePublicCardIdentity(card, currentTargetB)).count(), 0L,
                    "card B must not be consumed by the ChangeZone effect");
            assertions.assertEquals(fixture.game().getCardsIn(ZoneType.Graveyard).stream()
                    .filter(card -> samePublicCardIdentity(card, currentTargetB)).count(), 1L,
                    "card B must remain the untouched alternative");
            assertions.assertEquals(targetRequestRecords.size(), 1,
                    "the route must trace exactly one TARGET request");
            assertions.assertEquals(confirmationRequestRecords.size(), 1,
                    "the route must trace exactly one CONFIRMATION request");
            assertions.assertEquals(resultRecords.size(), 2,
                    "the route must trace one terminal RESULT for each request");

            final DecisionRequest observedTargetRequest = targetRequest.get();
            assertions.assertNotNull(observedTargetRequest, "the external TARGET request must be observable");
            if (observedTargetRequest != null) {
                assertions.assertEquals(observedTargetRequest.getDecisionType(), DecisionType.TARGET);
                assertions.assertFalse(observedTargetRequest.isForced(),
                        "two legal Blood cards must keep TARGET external and non-forced");
                final List<LegalCandidate> targetCandidates = observedTargetRequest.getCandidates().stream()
                        .filter(candidate -> candidate.getTargetKind() == TargetCandidateKind.TARGET_CARD)
                        .toList();
                assertions.assertEquals(targetCandidates.size(), 2,
                        "the TARGET request must expose both A and B");
                assertions.assertTrue(targetCandidates.stream()
                        .anyMatch(candidate -> candidate.getTarget() == fixture.targetA()));
                assertions.assertTrue(targetCandidates.stream()
                        .anyMatch(candidate -> candidate.getTarget() == fixture.targetB()));
            }

            final DecisionRequest observedConfirmationRequest = confirmationRequest.get();
            assertions.assertNotNull(observedConfirmationRequest,
                    "the external CONFIRMATION request must be observable");
            assertions.assertNotNull(selectedConfirmation.get(),
                    "the external CONFIRMATION resolver must select ACCEPT");
            if (observedConfirmationRequest != null) {
                assertions.assertEquals(observedConfirmationRequest.getDecisionType(), DecisionType.CONFIRMATION);
                assertions.assertFalse(observedConfirmationRequest.isForced(),
                        "Blood confirmation must remain an explicit non-forced request");
                assertions.assertEquals(observedConfirmationRequest.getCandidates().stream()
                        .map(LegalCandidate::getConfirmationKind).toList(),
                        List.of(ConfirmationCandidateKind.ACCEPT, ConfirmationCandidateKind.DECLINE),
                        "Blood confirmation candidates must be exactly ACCEPT then DECLINE");
            }
            if (selectedConfirmation.get() != null) {
                assertions.assertEquals(selectedConfirmation.get().getConfirmationKind(),
                        ConfirmationCandidateKind.ACCEPT);
            }
            assertTerminalExternalResult(assertions, targetRequestRecords, resultRecords,
                    selectedTargetCandidate, "TARGET");
            assertTerminalExternalResult(assertions, confirmationRequestRecords, resultRecords,
                    selectedConfirmation.get(), "CONFIRMATION");
            assertions.assertAll();
        } finally {
            if (trace != null) {
                trace.finish();
            }
            if (traceDirectory != null) {
                deleteTree(traceDirectory);
            }
            MyRandom.setRandom(previousRandom);
        }
    }

    @Test
    public void targetAndConfirmationResolversRemainIndependentAcrossAllFourCells() throws Exception {
        runOwnershipCell(false, false);
        runOwnershipCell(true, false);
        runOwnershipCell(false, true);
        runOwnershipCell(true, true);
    }

    @Test
    public void wrapperExternalBloodUsesProfileTraceAndCapturedExternalOwnership() throws Exception {
        final Random previousRandom = MyRandom.getRandom();
        final DeterminismAuditRandom auditRandom = new DeterminismAuditRandom(DETERMINISTIC_SEED);
        Path traceDirectory = null;
        DeterminismTrace trace = null;
        final SoftAssert assertions = new SoftAssert();
        MyRandom.setRandom(auditRandom);

        try {
            final BloodFixture fixture = bloodFixture();
            assertBloodFixture(fixture);
            fixture.ability().getTargets().add(fixture.targetA());
            final CountingController controller = installController(fixture);
            final Card targetAIdentity = fixture.targetA();
            final AtomicInteger confirmationResolverCalls = new AtomicInteger();
            controller.getConfirmationDecisionProvider().setResolver(request -> {
                confirmationResolverCalls.incrementAndGet();
                controller.getConfirmationDecisionProvider().setResolver(null);
                return request.getCandidates().stream()
                        .filter(candidate -> candidate.getConfirmationKind() == ConfirmationCandidateKind.ACCEPT)
                        .findFirst()
                        .orElse(null);
            });

            traceDirectory = Files.createTempDirectory("frl02k-d1-blood-wrapper-external-");
            trace = DeterminismTrace.attach(fixture.game(), 0, auditRandom, traceDirectory);
            RuntimeException unexpectedBaseline = null;
            try {
                fixture.wrapper().resolve();
            } catch (final RuntimeException ex) {
                unexpectedBaseline = ex;
            }

            final Exception traceFinishFailure = finishTrace(trace);
            final List<String> records = readTraceRecords(traceDirectory, assertions);
            final List<String> requestRecords = records.stream()
                    .filter(record -> record.startsWith("DECISION_TRACE_V2|REQUEST|"))
                    .toList();
            final List<String> resultRecords = records.stream()
                    .filter(record -> record.startsWith("DECISION_TRACE_V2|RESULT|"))
                    .toList();
            final List<String> confirmationRequestRecords = requestRecords.stream()
                    .filter(record -> "CONFIRMATION".equals(traceField(record, 6)))
                    .toList();

            assertions.assertNull(unexpectedBaseline,
                    "external Blood wrapper resolution must not throw on the admitted profile");
            assertions.assertNull(traceFinishFailure, "the attached decision trace must finish cleanly");
            assertions.assertEquals(confirmationResolverCalls.get(), 1,
                    "external Blood confirmation must invoke the resolver exactly once");
            assertions.assertEquals(controller.nativeConfirmationCallbackCalls(), 0,
                    "captured external ownership must not invoke native confirmation");
            assertions.assertEquals(confirmationRequestRecords.size(), 1,
                    "the wrapper must trace exactly one Blood CONFIRMATION request");
            assertions.assertEquals(resultRecords.size(), 1,
                    "the wrapper must trace exactly one terminal Blood result");
            if (confirmationRequestRecords.size() == 1) {
                final String request = confirmationRequestRecords.get(0);
                assertions.assertEquals(traceField(request, 7), "BLOOD_ETB_CONFIRMATION",
                        "the request stage must identify the Blood ETB profile");
                assertions.assertEquals(traceField(request, 9), "false",
                        "Blood confirmation must not be forced");
                assertions.assertEquals(traceField(request, 10), "[ACCEPT,DECLINE]",
                        "Blood confirmation candidates must remain ACCEPT then DECLINE");
            }
            if (resultRecords.size() == 1 && confirmationRequestRecords.size() == 1) {
                assertions.assertEquals(traceField(resultRecords.get(0), 2),
                        traceField(confirmationRequestRecords.get(0), 2));
                assertions.assertEquals(traceField(resultRecords.get(0), 3), "CHOSEN",
                        "external Blood confirmation must close as CHOSEN");
                assertions.assertEquals(traceField(resultRecords.get(0), 4), "ACCEPT");
                assertions.assertEquals(traceField(resultRecords.get(0), 5), "false",
                        "external Blood confirmation must record nativeCallbackCompleted=false");
                assertions.assertEquals(traceField(resultRecords.get(0), 6), "false",
                        "external Blood confirmation must record mappingAttempted=false");
            }
            assertions.assertAll();
        } finally {
            finishTrace(trace);
            if (traceDirectory != null) {
                deleteTree(traceDirectory);
            }
            MyRandom.setRandom(previousRandom);
        }
    }

    @Test
    public void wrapperNativeBloodAIntegrityFailureRecordsMappingFailed() throws Exception {
        final Random previousRandom = MyRandom.getRandom();
        final DeterminismAuditRandom auditRandom = new DeterminismAuditRandom(DETERMINISTIC_SEED);
        Path traceDirectory = null;
        DeterminismTrace trace = null;
        final SoftAssert assertions = new SoftAssert();
        MyRandom.setRandom(auditRandom);

        try {
            final BloodFixture fixture = bloodFixture();
            assertBloodFixture(fixture);
            fixture.ability().getTargets().add(fixture.targetA());
            final Card targetAIdentity = fixture.targetA();
            final CountingController controller = installController(fixture);
            controller.configureNativeAIntegrityFailure();
            controller.getConfirmationDecisionProvider().setResolver(null);

            traceDirectory = Files.createTempDirectory("frl02k-d1-blood-wrapper-native-");
            trace = DeterminismTrace.attach(fixture.game(), 0, auditRandom, traceDirectory);
            UnsupportedConfirmationDecisionException observed = null;
            RuntimeException unexpectedBaseline = null;
            try {
                fixture.wrapper().resolve();
            } catch (final UnsupportedConfirmationDecisionException ex) {
                observed = ex;
            } catch (final RuntimeException ex) {
                unexpectedBaseline = ex;
            }

            final Exception traceFinishFailure = finishTrace(trace);
            final List<String> records = readTraceRecords(traceDirectory, assertions);
            final List<String> requestRecords = records.stream()
                    .filter(record -> record.startsWith("DECISION_TRACE_V2|REQUEST|"))
                    .toList();
            final List<String> resultRecords = records.stream()
                    .filter(record -> record.startsWith("DECISION_TRACE_V2|RESULT|"))
                    .toList();

            assertions.assertNull(unexpectedBaseline,
                    "native Blood A-integrity failure must use the controlled mapping exception");
            assertions.assertNotNull(observed,
                    "native Blood A-integrity failure must throw UnsupportedConfirmationDecisionException");
            if (observed != null) {
                assertions.assertEquals(observed.getStatus(), ConfirmationDecisionProvider.Status.NATIVE_MAPPING_FAILED);
            }
            assertions.assertEquals(controller.nativeConfirmationCallbackCalls(), 1,
                    "native Blood confirmation must invoke the callback exactly once");
            assertions.assertEquals(fixture.game().getCardsIn(ZoneType.Exile).stream()
                    .filter(card -> samePublicCardIdentity(card, targetAIdentity)).count(), 0L,
                    "A-integrity failure must not consume target A");
            assertions.assertEquals(fixture.game().getCardsIn(ZoneType.Graveyard).stream()
                    .filter(card -> samePublicCardIdentity(card, targetAIdentity)).count(), 1L,
                    "target A must remain in the graveyard after mapping failure");
            assertions.assertEquals(requestRecords.size(), 1,
                    "native Blood confirmation must trace exactly one request");
            assertions.assertEquals(resultRecords.size(), 1,
                    "native Blood A-integrity failure must close exactly one result");
            if (resultRecords.size() == 1) {
                final String result = resultRecords.get(0);
                assertions.assertEquals(traceField(result, 3), "MAPPING_FAILED");
                assertions.assertEquals(traceField(result, 4), "");
                assertions.assertEquals(traceField(result, 5), "true",
                        "mapping failure must record nativeCallbackCompleted=true");
                assertions.assertEquals(traceField(result, 6), "true",
                        "mapping failure must record mappingAttempted=true");
            }
            assertions.assertNull(traceFinishFailure, "the attached decision trace must finish cleanly");
            assertions.assertAll();
        } finally {
            finishTrace(trace);
            if (traceDirectory != null) {
                deleteTree(traceDirectory);
            }
            MyRandom.setRandom(previousRandom);
        }
    }

    private static boolean samePublicCardIdentity(final Card actual, final Card expected) {
        return actual != null && expected != null
                && actual.getId() == expected.getId()
                && actual.getGameTimestamp() == expected.getGameTimestamp();
    }

    private void runOwnershipCell(final boolean externalTarget, final boolean externalConfirmation)
            throws Exception {
        final Random previousRandom = MyRandom.getRandom();
        final DeterminismAuditRandom auditRandom = new DeterminismAuditRandom(DETERMINISTIC_SEED);
        Path traceDirectory = null;
        DeterminismTrace trace = null;
        MyRandom.setRandom(auditRandom);
        try {
            final BloodFixture fixture = bloodFixture();
            assertBloodFixture(fixture);
            final CountingController controller = installController(fixture);
            controller.configureNativeTarget(fixture.targetA());

            final AtomicInteger targetResolverCalls = new AtomicInteger();
            if (externalTarget) {
                controller.setTargetDecisionResolver(request -> {
                    targetResolverCalls.incrementAndGet();
                    return request.getCandidates().stream()
                            .filter(candidate -> candidate.getTargetKind() == TargetCandidateKind.TARGET_CARD)
                            .filter(candidate -> candidate.getTarget() == fixture.targetA())
                            .findFirst()
                            .orElseThrow(() -> new AssertionError("Blood target A is not in the legal request"));
                });
            }

            final AtomicInteger confirmationResolverCalls = new AtomicInteger();
            if (externalConfirmation) {
                controller.getConfirmationDecisionProvider().setResolver(request -> {
                    confirmationResolverCalls.incrementAndGet();
                    return request.getCandidates().stream()
                            .filter(candidate -> candidate.getConfirmationKind() == ConfirmationCandidateKind.ACCEPT)
                            .findFirst()
                            .orElseThrow(() -> new AssertionError("ACCEPT is not in the confirmation request"));
                });
            } else {
                controller.getConfirmationDecisionProvider().setResolver(null);
            }

            traceDirectory = Files.createTempDirectory("frl02k-d1-blood-matrix-");
            trace = DeterminismTrace.attach(fixture.game(), 0, auditRandom, traceDirectory);
            controller.orderAndPlaySimultaneousSa(List.of(fixture.wrapper()));
            fixture.game().getStack().resolveStack();
            trace.finish();

            final List<String> records = readTraceRecords(traceDirectory, new SoftAssert());
            final List<String> requestRecords = records.stream()
                    .filter(record -> record.startsWith("DECISION_TRACE_V2|REQUEST|"))
                    .toList();
            final List<String> resultRecords = records.stream()
                    .filter(record -> record.startsWith("DECISION_TRACE_V2|RESULT|"))
                    .toList();
            final List<String> targetRequests = requestRecords.stream()
                    .filter(record -> "TARGET".equals(traceField(record, 6)))
                    .toList();
            final List<String> confirmationRequests = requestRecords.stream()
                    .filter(record -> "CONFIRMATION".equals(traceField(record, 6)))
                    .toList();

            assertEquals(targetResolverCalls.get(), externalTarget ? 1 : 0,
                    "TARGET resolver ownership mismatch for matrix cell");
            assertEquals(confirmationResolverCalls.get(), externalConfirmation ? 1 : 0,
                    "CONFIRMATION resolver ownership mismatch for matrix cell");
            assertEquals(controller.nativeTargetCallbackCalls(), externalTarget ? 0 : 1,
                    "native TARGET callback ownership mismatch for matrix cell");
            assertEquals(controller.nativeConfirmationCallbackCalls(), externalConfirmation ? 0 : 1,
                    "native CONFIRMATION callback ownership mismatch for matrix cell");
            assertEquals(targetRequests.size(), 1, "every matrix cell must create one TARGET request");
            assertEquals(confirmationRequests.size(), 1,
                    "every matrix cell must create one CONFIRMATION request");
            assertEquals(resultRecords.size(), 2, "every matrix cell must close both requests");
            assertEquals(fixture.ability().getTargets().size(), 1,
                    "every matrix cell must retain exactly one target after confirmation");
            assertTrue(fixture.ability().getTargets().contains(fixture.targetA()),
                    "every matrix cell must restore target A");
            assertFalse(fixture.ability().getTargets().contains(fixture.targetB()),
                    "every matrix cell must not leave target B selected");
            if (externalConfirmation) {
                assertNull(controller.targetAtNativeConfirmation(),
                        "external confirmation must not create temporary target B");
            }
            final List<String> targetResults = resultsFor(targetRequests, resultRecords);
            final List<String> confirmationResults = resultsFor(confirmationRequests, resultRecords);
            assertEquals(traceField(targetResults.get(0), 3), "CHOSEN");
            assertEquals(traceField(confirmationResults.get(0), 3), "CHOSEN");
            assertEquals(traceField(targetResults.get(0), 5), Boolean.toString(!externalTarget));
            assertEquals(traceField(targetResults.get(0), 6), Boolean.toString(!externalTarget));
            assertEquals(traceField(confirmationResults.get(0), 5), Boolean.toString(!externalConfirmation));
            assertEquals(traceField(confirmationResults.get(0), 6), Boolean.toString(!externalConfirmation));
        } finally {
            finishTrace(trace);
            if (traceDirectory != null) {
                deleteTree(traceDirectory);
            }
            MyRandom.setRandom(previousRandom);
        }
    }

    private static List<String> resultsFor(final List<String> requestRecords, final List<String> resultRecords) {
        assertEquals(requestRecords.size(), 1);
        final String requestId = traceField(requestRecords.get(0), 2);
        final List<String> matching = resultRecords.stream()
                .filter(record -> requestId.equals(traceField(record, 2)))
                .toList();
        assertEquals(matching.size(), 1);
        return matching;
    }

    private static Exception finishTrace(final DeterminismTrace trace) {
        if (trace == null) {
            return null;
        }
        try {
            trace.finish();
            return null;
        } catch (final Exception ex) {
            return ex;
        }
    }

    private static List<String> readTraceRecords(final Path traceDirectory, final SoftAssert assertions) {
        if (traceDirectory == null) {
            assertions.assertTrue(false, "decision trace directory was not created");
            return List.of();
        }
        final Path decisionTrace = traceDirectory.resolve("game-001.decision.trace");
        try {
            return Files.exists(decisionTrace)
                    ? Files.readAllLines(decisionTrace, StandardCharsets.UTF_8) : List.of();
        } catch (final IOException ex) {
            assertions.assertTrue(false, "decision trace could not be read: " + ex.getMessage());
            return List.of();
        }
    }

    private BloodFixture bloodFixture() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final Player opponent = game.getPlayers().get(0);
        final Card source = addCardToZone("Blood Operative", chooser, ZoneType.Battlefield);
        final Card targetA = addCardToZone("Runeclaw Bear", opponent, ZoneType.Graveyard);
        final Card targetB = addCardToZone("Llanowar Elves", opponent, ZoneType.Graveyard);
        final Trigger trigger = source.getTriggers().stream()
                .filter(candidate -> candidate.getMode() == TriggerType.ChangesZone)
                .filter(candidate -> "Any".equals(candidate.getParam("Origin")))
                .filter(candidate -> "Battlefield".equals(candidate.getParam("Destination")))
                .filter(candidate -> "Card.Self".equals(candidate.getParam("ValidCard")))
                .filter(candidate -> "You".equals(candidate.getParam("OptionalDecider")))
                .filter(candidate -> "TrigChangeZone".equals(candidate.getParam("Execute")))
                .filter(Trigger::isIntrinsic)
                .filter(candidate -> !candidate.isStatic())
                .filter(candidate -> candidate.getSpawningAbility() == null)
                .findFirst()
                .orElseThrow(() -> new AssertionError("expected Blood Operative ETB trigger is unavailable"));
        final SpellAbility ability = trigger.ensureAbility();
        ability.setActivatingPlayer(chooser);
        ability.setOptionalTrigger(true);
        return new BloodFixture(game, chooser, source, targetA, targetB, trigger, ability,
                new WrappedAbility(trigger, ability, chooser));
    }

    private static void assertBloodFixture(final BloodFixture fixture) {
        assertEquals(fixture.source().getName(), "Blood Operative");
        assertEquals(fixture.source().getZone().getZoneType(), ZoneType.Battlefield);
        assertEquals(fixture.source().getController(), fixture.chooser());
        assertEquals(fixture.game().getCardsIn(ZoneType.Graveyard).size(), 2);
        assertNotSame(fixture.targetA(), fixture.targetB());
        assertTrue(fixture.game().getCardsIn(ZoneType.Graveyard).contains(fixture.targetA()));
        assertTrue(fixture.game().getCardsIn(ZoneType.Graveyard).contains(fixture.targetB()));
        assertEquals(fixture.trigger().getMode(), TriggerType.ChangesZone);
        assertTrue(fixture.trigger().isIntrinsic());
        assertFalse(fixture.trigger().isStatic());
        assertNull(fixture.trigger().getSpawningAbility());
        assertEquals(fixture.ability().getApi(), ApiType.ChangeZone);
        assertEquals(fixture.ability().getParam("Origin"), "Graveyard");
        assertEquals(fixture.ability().getParam("Destination"), "Exile");
        assertEquals(fixture.ability().getParam("ValidTgts"), "Card");
        final TargetRestrictions restrictions = fixture.ability().getTargetRestrictions();
        assertNotNull(restrictions);
        assertTrue(restrictions.getZone().contains(ZoneType.Graveyard));
        assertEquals(fixture.ability().getMinTargets(), 1);
        assertEquals(fixture.ability().getMaxTargets(), 1);
        assertTrue(fixture.ability().canTarget(fixture.targetA()));
        assertTrue(fixture.ability().canTarget(fixture.targetB()));
        assertTrue(fixture.ability().getTargets().isEmpty());
        assertEquals(fixture.wrapper().getDecider(), fixture.chooser());
        assertTrue(fixture.wrapper().isOptionalTrigger());
    }

    private static CountingController installController(final BloodFixture fixture) {
        final CountingController controller = new CountingController(fixture.game(), fixture.chooser());
        fixture.chooser().dangerouslySetController(controller);
        return controller;
    }

    private static void deleteTree(final Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (var paths = Files.walk(root)) {
            for (final Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    private static void assertTerminalExternalResult(final SoftAssert assertions,
            final List<String> requestRecords, final List<String> resultRecords,
            final LegalCandidate selectedCandidate, final String decisionType) {
        if (requestRecords.size() != 1) {
            return;
        }
        final String requestId = traceField(requestRecords.get(0), 2);
        final List<String> matchingResults = resultRecords.stream()
                .filter(record -> requestId.equals(traceField(record, 2)))
                .toList();
        assertions.assertEquals(matchingResults.size(), 1,
                "exactly one terminal RESULT must close the " + decisionType + " request");
        if (matchingResults.size() != 1) {
            return;
        }
        final String result = matchingResults.get(0);
        assertions.assertEquals(traceField(result, 3), "CHOSEN",
                decisionType + " must finish with CHOSEN, not TRACE_INCOMPLETE");
        assertions.assertEquals(traceField(result, 5), "false",
                decisionType + " must record external native-callback=false");
        assertions.assertEquals(traceField(result, 6), "false",
                decisionType + " must record external mapping-attempted=false");
        assertions.assertEquals(traceField(result, 8), "false",
                decisionType + " must not be engine-forced");
        if (selectedCandidate != null) {
            assertions.assertEquals(traceField(result, 4), traceText(selectedCandidate.getSemanticKey()),
                    decisionType + " RESULT must name the selected candidate");
        }
    }

    private static String traceField(final String record, final int index) {
        final String[] fields = record.split("\\|", -1);
        return index < fields.length ? fields[index] : "";
    }

    private static String traceText(final String value) {
        return value.replace("%", "%25").replace("|", "%7C")
                .replace("\r", "%0D").replace("\n", "%0A");
    }

    private static final class CountingController extends PlayerControllerAi {
        private int nativeTargetCallbackCalls;
        private int nativeConfirmationCallbackCalls;
        private int orderSimultaneousSaCalls;
        private GameObject targetAtNativeConfirmation;
        private boolean nativeAIntegrityFailure;
        private Card nativeTarget;

        private CountingController(final Game game, final Player player) {
            super(game, player, new LobbyPlayerAi(player.getName() + "-frl02k-d1", null));
        }

        @Override
        protected boolean invokeNativeTriggeredTarget(final SpellAbility underlying, final boolean mandatory) {
            nativeTargetCallbackCalls++;
            if (nativeTarget != null) {
                underlying.getTargets().add(nativeTarget);
                return true;
            }
            return super.invokeNativeTriggeredTarget(underlying, mandatory);
        }

        @Override
        public List<SpellAbility> orderSimultaneousSa(final List<SpellAbility> activePlayerSAs) {
            orderSimultaneousSaCalls++;
            return activePlayerSAs;
        }

        @Override
        public boolean confirmTrigger(final WrappedAbility wrapper) {
            nativeConfirmationCallbackCalls++;
            if (wrapper.getWrappedAbility().getTargets().size() == 1) {
                targetAtNativeConfirmation = wrapper.getWrappedAbility().getTargets().get(0);
            }
            if (nativeAIntegrityFailure) {
                wrapper.getWrappedAbility().resetTargets();
                return true;
            }
            return super.confirmTrigger(wrapper);
        }

        private void configureNativeAIntegrityFailure() {
            nativeAIntegrityFailure = true;
        }

        private void configureNativeTarget(final Card target) {
            nativeTarget = target;
        }

        private int nativeTargetCallbackCalls() {
            return nativeTargetCallbackCalls;
        }

        private int nativeConfirmationCallbackCalls() {
            return nativeConfirmationCallbackCalls;
        }

        private int orderSimultaneousSaCalls() {
            return orderSimultaneousSaCalls;
        }

        private GameObject targetAtNativeConfirmation() {
            return targetAtNativeConfirmation;
        }
    }

    private record BloodFixture(Game game, Player chooser, Card source, Card targetA, Card targetB,
            Trigger trigger, SpellAbility ability, WrappedAbility wrapper) {
    }
}
