package forge.view;

import forge.ai.AITest;
import forge.ai.LobbyPlayerAi;
import forge.ai.PlayerControllerAi;
import forge.game.Game;
import forge.game.ability.AbilityApiBased;
import forge.game.ability.AbilityFactory;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardFactory;
import forge.card.CardStateName;
import forge.game.decision.DecisionRequest;
import forge.game.decision.DeterminismTrace;
import forge.game.decision.LegalCandidate;
import forge.game.decision.TargetCandidateKind;
import forge.game.decision.TargetDecisionProvider;
import forge.game.decision.TriggeredTargetDecisionCoordinator;
import forge.game.decision.TriggeredTargetIntegrityException;
import forge.game.cost.Cost;
import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.trigger.WrappedAbility;
import forge.game.zone.ZoneType;
import forge.util.DeterminismAuditRandom;
import forge.util.MyRandom;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

/** Focused FRL-02K-C2A proof that external TARGET ownership is transferred exactly once. */
public class FRL02KTriggeredTargetExternalOwnershipAuditTest extends AITest {
    private static final long DETERMINISTIC_SEED = 20260811L;
    private static final String BLOOD_OPERATIVE = "Blood Operative";
    private static final List<String> TARGET_NAMES = List.of("Runeclaw Bear", "Llanowar Elves");
    private static final Map<String, String> BLOOD_TRIGGER_PARAMS = Map.of(
            "Mode", "ChangesZone",
            "Origin", "Any",
            "Destination", "Battlefield",
            "ValidCard", "Card.Self",
            "OptionalDecider", "You",
            "Execute", "TrigChangeZone");
    private static final Map<String, String> BLOOD_EFFECT_PARAMS = Map.of(
            "DB", "ChangeZone",
            "Origin", "Graveyard",
            "Destination", "Exile",
            "ValidTgts", "Card");

    @Test
    public void nativeControlAndExternalOwnershipRunsTransferOneBloodTarget() throws Exception {
        final BloodRun nativeRun = openBloodRun("frl02k-c2a-native-");
        final String nativeTargetName;
        try {
            assertBloodFixture(nativeRun.fixture());
            final AuditedTargetController nativeController = installController(nativeRun.fixture());
            assertNull(nativeController.getTargetDecisionResolver());
            final int stackBefore = nativeRun.fixture().game().getStack().size();

            assertTrue(nativeController.playTrigger(nativeRun.fixture().source(), nativeRun.fixture().wrapper(), true));
            assertEquals(nativeController.nativeCallbackCount(), 1,
                    "native control must invoke the protected native target adapter once");
            assertEquals(nativeController.resolverCalls(), 0);
            assertEquals(nativeRun.fixture().game().getStack().size(), stackBefore,
                    "Blood ETB preparation must retain the existing no-stack route");
            assertNotNull(nativeController.nativeTarget());
            nativeTargetName = nativeController.nativeTarget().getName();
            assertExactlyOneTargetApplied(nativeRun.fixture(), nativeTargetName);

            final TraceEvidence nativeTrace = readTrace(nativeRun.finishAndReadDecisionTrace());
            assertEquals(nativeTrace.requestDecisionType(), "TARGET");
            assertEquals(nativeTrace.requestAdapter(), "TRIGGERED_TARGET");
            assertEquals(nativeTrace.resultKind(), "CHOSEN");
            assertEquals(nativeTrace.nativeCallbackCompleted(), "true");
            assertEquals(nativeTrace.mappingAttempted(), "true");
        } finally {
            nativeRun.close();
        }

        final BloodRun externalRun = openBloodRun("frl02k-c2a-external-");
        try {
            assertBloodFixture(externalRun.fixture());
            assertEquals(nativeRunFixtureProjection(nativeRun),
                    externalRunFixtureProjection(externalRun.fixture()),
                    "native and external runs must be built from the same public deterministic fixture");
            final AuditedTargetController externalController = installController(externalRun.fixture());
            assertNotSame(nativeRun.fixture().game(), externalRun.fixture().game());
            assertNotSame(nativeRun.fixture().provider(), externalController.getTargetDecisionProvider());

            final AtomicReference<DecisionRequest> requestSeen = new AtomicReference<>();
            final AtomicReference<LegalCandidate> selectedSeen = new AtomicReference<>();
            externalController.setTargetDecisionResolver(request -> {
                externalController.incrementResolverCalls();
                requestSeen.set(request);
                final LegalCandidate selected = request.getCandidates().stream()
                        .filter(candidate -> candidate.getTargetKind() == TargetCandidateKind.TARGET_CARD)
                        .max(Comparator.comparing(LegalCandidate::getSemanticKey))
                        .orElseThrow();
                selectedSeen.set(selected);
                return selected;
            });
            final int stackBefore = externalRun.fixture().game().getStack().size();

            assertTrue(externalController.playTrigger(externalRun.fixture().source(), externalRun.fixture().wrapper(), true));
            assertEquals(externalController.resolverCalls(), 1,
                    "external ownership must invoke the request-local resolver exactly once");
            assertEquals(externalController.nativeCallbackCount(), 0,
                    "external ownership must not invoke the native target adapter");
            assertEquals(externalRun.fixture().game().getStack().size(), stackBefore,
                    "external Blood ETB preparation must retain the existing no-stack route");

            final DecisionRequest request = requestSeen.get();
            final LegalCandidate selected = selectedSeen.get();
            assertNotNull(request);
            assertNotNull(selected);
            assertEquals(request.getDecisionType().name(), "TARGET");
            assertFalse(request.isForced());
            assertEquals(request.getCandidates().size(), 2);
            assertTrue(request.getCandidates().stream()
                    .allMatch(candidate -> candidate.getTargetKind() == TargetCandidateKind.TARGET_CARD));
            assertEquals(request.getCandidates().stream()
                    .filter(candidate -> candidate.getTargetKind() == TargetCandidateKind.TARGET_CARD)
                    .map(LegalCandidate::getTargetName)
                    .sorted()
                    .toList(), TARGET_NAMES.stream().sorted().toList());
            assertFalse(request.getTargetContext().hasActionContinuation());
            assertNull(request.getTargetContext().getDecisionSequenceId());
            assertNull(request.getTargetContext().getSubdecisionIndex());
            assertTrue(request.getCandidates().contains(selected),
                    "resolver must return a candidate from its exact request");
            assertEquals(selected.getTargetKind(), TargetCandidateKind.TARGET_CARD);
            assertEquals(selected.getTargetZone(), ZoneType.Graveyard);
            assertTrue(TARGET_NAMES.contains(selected.getTargetName()));
            assertFalse(selected.getTargetName().equals(nativeTargetName),
                    "the two legal candidates must prove a distinct external ownership choice");
            assertExactlyOneTargetApplied(externalRun.fixture(), selected.getTargetName());

            final TraceEvidence externalTrace = readTrace(externalRun.finishAndReadDecisionTrace());
            assertEquals(externalTrace.requestCount(), 1);
            assertEquals(externalTrace.requestDecisionType(), "TARGET");
            assertEquals(externalTrace.requestAdapter(), "TRIGGERED_TARGET");
            assertEquals(externalTrace.requestForced(), "false");
            assertEquals(externalTrace.resultKind(), "CHOSEN");
            assertEquals(externalTrace.resultSelectedCandidate(), traceText(selected.getSemanticKey()));
            assertEquals(externalTrace.nativeCallbackCompleted(), "false");
            assertEquals(externalTrace.mappingAttempted(), "false");
            assertFalse(externalTrace.isBcPolicySample(),
                    "an externally owned TARGET result must not be a native BC sample");
            assertFalse(externalTrace.hasMappingFailedProvenance());
        } finally {
            externalRun.close();
        }
    }

    @DataProvider(name = "invalidExternalCandidates")
    public Object[][] invalidExternalCandidates() {
        return new Object[][] {
                {InvalidExternalCandidate.NULL},
                {InvalidExternalCandidate.OTHER_REQUEST_PROVIDER},
                {InvalidExternalCandidate.STALE_AFTER_ZONE_CHANGE},
                {InvalidExternalCandidate.FOREIGN_GAME},
                {InvalidExternalCandidate.LIVE_STATE_ILLEGAL}
        };
    }

    @Test(dataProvider = "invalidExternalCandidates")
    public void invalidExternalCandidatesFailClosedWithoutNativeFallback(
            final InvalidExternalCandidate invalidCandidate) throws Exception {
        final BloodRun run = openBloodRun("frl02k-c2a-invalid-");
        try {
            assertBloodFixture(run.fixture());
            final AuditedTargetController controller = installController(run.fixture());
            final AtomicInteger resolverCalls = new AtomicInteger();
            final AtomicReference<DecisionRequest> requestSeen = new AtomicReference<>();
            final LegalCandidate otherRequestCandidate = invalidCandidate == InvalidExternalCandidate.OTHER_REQUEST_PROVIDER
                    ? candidateFromAnotherProvider(run.fixture()) : null;
            final LegalCandidate foreignCandidate = invalidCandidate == InvalidExternalCandidate.FOREIGN_GAME
                    ? candidateFromForeignGame() : null;
            controller.setTargetDecisionResolver(request -> {
                resolverCalls.incrementAndGet();
                requestSeen.set(request);
                if (invalidCandidate == InvalidExternalCandidate.NULL) {
                    return null;
                }
                if (invalidCandidate == InvalidExternalCandidate.OTHER_REQUEST_PROVIDER) {
                    return otherRequestCandidate;
                }
                if (invalidCandidate == InvalidExternalCandidate.FOREIGN_GAME) {
                    return foreignCandidate;
                }

                final LegalCandidate selected = firstTargetCandidate(request);
                final Card selectedCard = run.fixture().targetByName(selected.getTargetName());
                if (invalidCandidate == InvalidExternalCandidate.STALE_AFTER_ZONE_CHANGE) {
                    run.fixture().game().getAction().moveTo(ZoneType.Exile, selectedCard, null, null);
                } else if (invalidCandidate == InvalidExternalCandidate.LIVE_STATE_ILLEGAL) {
                    run.fixture().ability().getTargetRestrictions().setZone(ZoneType.Exile);
                }
                return selected;
            });
            final int stackBefore = run.fixture().game().getStack().size();

            final TriggeredTargetIntegrityException exception = expectThrows(
                    TriggeredTargetIntegrityException.class,
                    () -> controller.playTrigger(run.fixture().source(), run.fixture().wrapper(), true));

            assertEquals(exception.getReason(), "INVALID_EXTERNAL_CANDIDATE");
            assertEquals(resolverCalls.get(), 1);
            assertNotNull(requestSeen.get());
            assertEquals(requestSeen.get().getDecisionType().name(), "TARGET");
            assertEquals(controller.nativeCallbackCount(), 0,
                    "invalid external ownership must not fall back to the native callback");
            assertEquals(run.fixture().game().getStack().size(), stackBefore,
                    "invalid external ownership must not push the trigger");

            final TraceEvidence trace = readTrace(run.finishAndReadDecisionTrace());
            assertEquals(trace.requestCount(), 1);
            assertEquals(trace.resultKind(), "TRACE_INCOMPLETE");
            assertFalse(trace.hasMappingFailedProvenance(),
                    "invalid external candidates must not be mislabeled as native mapping failures");
        } finally {
            run.close();
        }
    }

    @Test
    public void throwingResolverFailsClosedWithoutNativeFallbackOrMappingFailure() throws Exception {
        final BloodRun run = openBloodRun("frl02k-c2a-throwing-resolver-");
        try {
            assertBloodFixture(run.fixture());
            final AuditedTargetController controller = installController(run.fixture());
            controller.setTargetDecisionResolver(request -> {
                controller.incrementResolverCalls();
                throw new IllegalStateException("resolver-private-details");
            });
            final int stackBefore = run.fixture().game().getStack().size();

            final TriggeredTargetIntegrityException exception = expectThrows(
                    TriggeredTargetIntegrityException.class,
                    () -> controller.playTrigger(run.fixture().source(), run.fixture().wrapper(), true));

            assertEquals(exception.getReason(), "INVALID_EXTERNAL_CANDIDATE");
            assertEquals(controller.resolverCalls(), 1);
            assertEquals(controller.nativeCallbackCount(), 0,
                    "throwing external ownership must not fall back to the native callback");
            assertEquals(run.fixture().game().getStack().size(), stackBefore,
                    "throwing external ownership must not push the trigger");

            final TraceEvidence trace = readTrace(run.finishAndReadDecisionTrace());
            assertEquals(trace.requestCount(), 1);
            assertEquals(trace.resultKind(), "TRACE_INCOMPLETE");
            assertFalse(trace.hasMappingFailedProvenance(),
                    "throwing external ownership must not be mislabeled as native mapping failure");
        } finally {
            run.close();
        }
    }

    @Test(timeOut = 5000)
    public void externalOwnershipThroughOrderAndPlayUsesLiveTargetBeforeResolution() throws Exception {
        final BloodRun run = openBloodRun("frl02k-c2a-order-");
        try {
            final BloodFixture fixture = run.fixture();
            assertBloodFixture(fixture);
            final AuditedTargetController controller = installController(fixture);
            final AtomicReference<LegalCandidate> selectedSeen = new AtomicReference<>();
            controller.setTargetDecisionResolver(request -> {
                controller.incrementResolverCalls();
                final LegalCandidate selected = request.getCandidates().stream()
                        .filter(candidate -> candidate.getTargetKind() == TargetCandidateKind.TARGET_CARD)
                        .filter(candidate -> fixture.firstTarget().getName().equals(candidate.getTargetName()))
                        .findFirst()
                        .orElseThrow();
                selectedSeen.set(selected);
                return selected;
            });
            final int stackBefore = fixture.game().getStack().size();

            controller.orderAndPlaySimultaneousSa(List.of(fixture.wrapper()));

            assertEquals(controller.orderSimultaneousSaCalls(), 1,
                    "the production order route must invoke the deterministic ordering hook once");
            assertEquals(controller.resolverCalls(), 1,
                    "external ownership must invoke the resolver exactly once before stack insertion");
            assertEquals(controller.nativeCallbackCount(), 0,
                    "external ownership must not invoke the native target adapter");
            assertEquals(fixture.game().getStack().size(), stackBefore + 1,
                    "the inherited order route must insert one ability on the live stack");
            assertEquals(fixture.ability().getTargets().size(), 1);
            assertEquals(fixture.ability().getTargets().get(0), fixture.firstTarget(),
                    "the selected target must be present on the live underlying ability before resolution");

            fixture.game().getStack().resolveStack();

            assertEquals(controller.resolverCalls(), 1,
                    "resolving the queued trigger must not re-enter the external resolver");
            assertEquals(controller.nativeCallbackCount(), 0,
                    "resolving the queued trigger must not invoke the native target adapter");
            assertEquals(fixture.game().getStack().size(), stackBefore);
            assertExactlyOneTargetApplied(fixture, fixture.firstTarget().getName());

            final TraceEvidence trace = readTrace(run.finishAndReadDecisionTrace());
            assertEquals(trace.requestDecisionType(), "TARGET");
            assertEquals(trace.requestAdapter(), "TRIGGERED_TARGET");
            assertEquals(trace.resultKind(), "CHOSEN");
            assertEquals(trace.resultSelectedCandidate(), traceText(selectedSeen.get().getSemanticKey()));
            assertEquals(trace.nativeCallbackCompleted(), "false");
            assertEquals(trace.mappingAttempted(), "false");
        } finally {
            run.close();
        }
    }

    @Test
    public void copiedTargetedTriggerIsRejectedBeforeOrderBranchStackInsertion() {
        final BloodFixture fixture = bloodFixture();
        final AuditedTargetController controller = installController(fixture);
        final AtomicInteger resolverCalls = new AtomicInteger();
        controller.setTargetDecisionResolver(request -> {
            resolverCalls.incrementAndGet();
            return firstTargetCandidate(request);
        });
        final WrappedAbility copied = (WrappedAbility) CardFactory.copySpellAbilityAndPossiblyHost(
                fixture.wrapper(), fixture.wrapper(), fixture.chooser());
        assertTrue(copied.isCopied());
        final int stackBefore = fixture.game().getStack().size();

        final TriggeredTargetIntegrityException exception = expectThrows(
                TriggeredTargetIntegrityException.class,
                () -> controller.orderAndPlaySimultaneousSa(List.of(copied)));

        assertEquals(exception.getReason(), "UNSUPPORTED_PROFILE");
        assertEquals(resolverCalls.get(), 0);
        assertEquals(controller.orderSimultaneousSaCalls(), 0,
                "unsupported copied targeted triggers must fail before AI ordering");
        assertEquals(controller.nativeCallbackCount(), 0);
        assertEquals(controller.chooseTargetsForCalls(), 0);
        assertEquals(fixture.game().getStack().size(), stackBefore,
                "unsupported copied targeted triggers must not be inserted into the stack");
    }

    @Test(timeOut = 5000)
    public void ordinaryCopiedAbilityWithTargetedChildKeepsNativeOrderAndStackRoute() {
        final OrdinaryCopiedChildFixture fixture = ordinaryCopiedChildFixture();
        final AuditedTargetController controller = installController(fixture.game(), fixture.chooser());
        controller.setTargetDecisionResolver(request -> {
            controller.incrementResolverCalls();
            return firstTargetCandidate(request);
        });
        final long providerRequestsBefore = providerRequestSequence(controller.getTargetDecisionProvider());
        final int stackBefore = fixture.game().getStack().size();

        controller.orderAndPlaySimultaneousSa(List.of(fixture.copiedRoot()));

        assertTrue(fixture.copiedRoot().isCopied());
        assertFalse(fixture.copiedRoot().isTrigger(),
                "the ordinary copied ability must not be treated as a triggered root");
        assertTrue(fixture.targetedChild().usesTargeting());
        assertNull(fixture.targetedChild().getTrigger());
        assertEquals(providerRequestSequence(controller.getTargetDecisionProvider()) - providerRequestsBefore, 0L,
                "ordinary copied children must not enter the C2A provider seam");
        assertEquals(controller.resolverCalls(), 0,
                "ordinary copied children must not invoke the external resolver");
        assertEquals(controller.nativeCallbackCount(), 0,
                "ordinary copied children must retain the native non-trigger route");
        assertEquals(controller.chooseTargetsForCalls(), 0,
                "ordinary copied children must not invoke target-choice fallback");
        assertEquals(controller.orderSimultaneousSaCalls(), 1,
                "ordinary copied abilities must still reach native ordering");
        assertEquals(fixture.game().getStack().size(), stackBefore + 1,
                "ordinary copied abilities must retain native stack insertion");
    }

    @Test
    public void nonWrappedTargetedTriggerIsRejectedBeforeControllerFallbackOrStackInsertion() {
        final BloodFixture fixture = bloodFixture();
        final AuditedTargetController controller = installController(fixture);
        final AtomicInteger resolverCalls = new AtomicInteger();
        controller.setTargetDecisionResolver(request -> {
            resolverCalls.incrementAndGet();
            return firstTargetCandidate(request);
        });
        final SpellAbility nonWrapped = fixture.ability();
        assertFalse(nonWrapped instanceof WrappedAbility);
        nonWrapped.putParam("TargetingPlayer", "You");
        nonWrapped.setTargetingPlayer(fixture.chooser());
        final int stackBefore = fixture.game().getStack().size();

        final TriggeredTargetIntegrityException exception = expectThrows(
                TriggeredTargetIntegrityException.class,
                () -> controller.orderAndPlaySimultaneousSa(List.of(nonWrapped)));

        assertEquals(exception.getReason(), "UNSUPPORTED_PROFILE");
        assertEquals(resolverCalls.get(), 0);
        assertEquals(controller.orderSimultaneousSaCalls(), 0,
                "unsupported non-wrapped targeted triggers must fail before AI ordering");
        assertEquals(controller.nativeCallbackCount(), 0);
        assertEquals(controller.chooseTargetsForCalls(), 0);
        assertEquals(fixture.game().getStack().size(), stackBefore,
                "unsupported non-wrapped targeted triggers must not be inserted into the stack");
    }

    @Test
    public void targetedCharmModeIsRejectedBeforeCharmChoice() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final Card source = addCardToZone("Aven Surveyor", chooser, ZoneType.Battlefield);
        final Trigger trigger = source.getTriggers().stream()
                .filter(candidate -> candidate.getMode() == TriggerType.ChangesZone)
                .filter(candidate -> "TrigCharm".equals(candidate.getParam("Execute")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Aven Surveyor Charm trigger fixture is unavailable"));
        final SpellAbility ability = trigger.ensureAbility();
        ability.setActivatingPlayer(chooser);
        final WrappedAbility wrapper = new WrappedAbility(trigger, ability, chooser);
        assertEquals(wrapper.getApi(), ApiType.Charm);
        assertTrue(wrapper.getAdditionalAbilityList("Choices").stream()
                .anyMatch(SpellAbility::usesTargeting),
                "the Charm fixture must expose a target-bearing mode before choice");

        final AuditedTargetController controller = installController(game, chooser);
        controller.setTargetDecisionResolver(request -> {
            controller.incrementResolverCalls();
            return null;
        });
        final int stackBefore = game.getStack().size();

        final TriggeredTargetIntegrityException exception = expectThrows(
                TriggeredTargetIntegrityException.class,
                () -> controller.playTrigger(source, wrapper, true));

        assertEquals(exception.getReason(), "UNSUPPORTED_PROFILE");
        assertEquals(controller.resolverCalls(), 0,
                "unsupported targeted Charm modes must fail before external target generation");
        assertEquals(controller.chooseModeForAbilityCalls(), 0,
                "unsupported targeted Charm modes must fail before Charm mode choice");
        assertEquals(controller.nativeCallbackCount(), 0);
        assertEquals(game.getStack().size(), stackBefore);
    }

    @Test
    public void nestedTargetedCharmBranchIsRejectedBeforeChoiceOrFallback() {
        final NestedCharmFixture fixture = nestedCharmFixture();
        final AuditedTargetController controller = installController(fixture.game(), fixture.chooser());
        controller.setTargetDecisionResolver(request -> {
            controller.incrementResolverCalls();
            return null;
        });
        final int stackBefore = fixture.game().getStack().size();

        final TriggeredTargetIntegrityException exception = expectThrows(
                TriggeredTargetIntegrityException.class,
                () -> controller.playTrigger(fixture.source(), fixture.wrapper(), true));

        assertEquals(exception.getReason(), "UNSUPPORTED_PROFILE");
        assertFalse(fixture.nonTargetingHead().usesTargeting());
        assertTrue(fixture.nestedTarget().usesTargeting());
        assertEquals(controller.resolverCalls(), 0,
                "nested targeted Charm branches must fail before external target generation");
        assertEquals(controller.nativeCallbackCount(), 0,
                "nested targeted Charm branches must fail before native target fallback");
        assertEquals(controller.chooseTargetsForCalls(), 0,
                "nested targeted Charm branches must fail before chooser target fallback");
        assertEquals(controller.chooseModeForAbilityCalls(), 0,
                "nested targeted Charm branches must fail before Charm mode selection");
        assertEquals(fixture.game().getStack().size(), stackBefore,
                "nested targeted Charm branches must not insert a stack item");
    }

    @DataProvider(name = "targetedAdditionalChildRoutes")
    public Object[][] targetedAdditionalChildRoutes() {
        return new Object[][] {
                {"TrueSubAbility", false},
                {"FalseSubAbility", false},
                {"FallbackAbility", false},
                {"AlternateAbilityList", true}
        };
    }

    @Test(dataProvider = "targetedAdditionalChildRoutes", timeOut = 5000)
    public void targetedAdditionalChildRouteIsRejectedBeforeAnyFallback(final String routeName,
            final boolean listRoute) {
        final AdditionalChildFixture fixture = targetedAdditionalChildFixture(routeName, listRoute);
        final AuditedTargetController controller = installController(fixture.game(), fixture.chooser());
        controller.setTargetDecisionResolver(request -> {
            controller.incrementResolverCalls();
            return null;
        });
        final long providerRequestsBefore = providerRequestSequence(controller.getTargetDecisionProvider());
        final int stackBefore = fixture.game().getStack().size();

        final TriggeredTargetIntegrityException exception = expectThrows(
                TriggeredTargetIntegrityException.class,
                () -> controller.orderAndPlaySimultaneousSa(List.of(fixture.wrapper())));

        assertEquals(exception.getReason(), "UNSUPPORTED_PROFILE");
        assertFalse(fixture.wrapper().usesTargeting(), "the root fixture must be non-targeting");
        assertTrue(fixture.targetedChild().usesTargeting(), "the additional child must be targeted");
        assertEquals(providerRequestSequence(controller.getTargetDecisionProvider()) - providerRequestsBefore, 0L,
                "additional-child rejection must precede target request generation");
        assertEquals(controller.resolverCalls(), 0,
                "additional-child rejection must precede external target resolution");
        assertEquals(controller.nativeCallbackCount(), 0,
                "additional-child rejection must precede native target fallback");
        assertEquals(controller.chooseTargetsForCalls(), 0,
                "additional-child rejection must precede chooser target fallback");
        assertEquals(controller.chooseModeForAbilityCalls(), 0,
                "additional-child rejection must precede mode selection");
        assertEquals(controller.orderSimultaneousSaCalls(), 0,
                "additional-child rejection must precede AI ordering");
        assertEquals(fixture.game().getStack().size(), stackBefore,
                "additional-child rejection must not insert a stack item");
    }

    @Test(timeOut = 5000)
    public void cyclicAbilitySubParentChainFailsClosedBeforeAnyFallback() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final Card source = addCardToZone("Island", chooser, ZoneType.Battlefield);
        final AbilitySub first = new AbilitySub(ApiType.Draw, source, null, Map.of());
        final AbilitySub second = new AbilitySub(ApiType.Draw, source, null, Map.of());
        first.setParent(second);
        second.setParent(first);

        final AuditedTargetController controller = installController(game, chooser);
        controller.setTargetDecisionResolver(request -> {
            controller.incrementResolverCalls();
            return null;
        });
        final long providerRequestsBefore = providerRequestSequence(controller.getTargetDecisionProvider());
        final int stackBefore = game.getStack().size();

        final TriggeredTargetIntegrityException exception = expectThrows(
                TriggeredTargetIntegrityException.class,
                () -> controller.orderAndPlaySimultaneousSa(List.of(first)));

        assertEquals(exception.getReason(), "UNSUPPORTED_PROFILE");
        assertEquals(providerRequestSequence(controller.getTargetDecisionProvider()) - providerRequestsBefore, 0L,
                "cyclic parent rejection must precede target request generation");
        assertEquals(controller.resolverCalls(), 0,
                "cyclic parent rejection must precede external target resolution");
        assertEquals(controller.nativeCallbackCount(), 0,
                "cyclic parent rejection must precede native target fallback");
        assertEquals(controller.chooseTargetsForCalls(), 0,
                "cyclic parent rejection must precede chooser target fallback");
        assertEquals(controller.chooseModeForAbilityCalls(), 0);
        assertEquals(controller.orderSimultaneousSaCalls(), 0,
                "cyclic parent rejection must precede AI ordering");
        assertEquals(game.getStack().size(), stackBefore,
                "cyclic parent rejection must not insert a stack item");
    }

    @Test(timeOut = 5000)
    public void cyclicAbilitySubOrderedOutputIsDroppedWithoutResolver() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final Card source = addCardToZone("Island", chooser, ZoneType.Battlefield);
        final AbilitySub first = new AbilitySub(ApiType.Draw, source, null, Map.of());
        final AbilitySub second = new AbilitySub(ApiType.Draw, source, null, Map.of());
        first.setParent(second);
        second.setParent(first);

        final PreflightOnlyTargetController controller = installPreflightController(
                game, chooser, first);
        assertNull(controller.getTargetDecisionResolver());
        final long providerRequestsBefore = providerRequestSequence(controller.getTargetDecisionProvider());
        final int stackBefore = game.getStack().size();

        controller.orderAndPlaySimultaneousSa(List.of(first));

        assertEquals(providerRequestSequence(controller.getTargetDecisionProvider()) - providerRequestsBefore, 0L,
                "resolver-null cyclic handling must not enter the target provider seam");
        assertEquals(controller.resolverCalls(), 0,
                "resolver-null cyclic handling must remain native-owned without resolver calls");
        assertEquals(controller.nativeCallbackCount(), 0,
                "an unclassifiable cycle must not invoke native target fallback");
        assertEquals(controller.chooseTargetsForCalls(), 0,
                "an unclassifiable cycle must not invoke chooser target fallback");
        assertEquals(controller.chooseModeForAbilityCalls(), 0,
                "an unclassifiable cycle must not invoke mode selection");
        assertEquals(controller.orderCalls(), 1,
                "resolver-null cyclic handling must still reach the controller ordering hook");
        assertEquals(game.getStack().size(), stackBefore,
                "an unclassifiable malformed cycle must not fall through to generic stack insertion");
    }

    @Test
    public void ordinaryNonWrappedTriggerKeepsNativePreparationRouteWithoutResolver() {
        final BloodFixture fixture = bloodFixture();
        final AuditedTargetController controller = installController(fixture);
        final SpellAbility nonWrappedTrigger = fixture.ability();
        nonWrappedTrigger.putParam("TargetingPlayer", "You");
        nonWrappedTrigger.setTargetingPlayer(fixture.chooser());

        assertTrue(nonWrappedTrigger.isTrigger());
        controller.orderAndPlaySimultaneousSa(List.of(nonWrappedTrigger));

        assertEquals(controller.chooseTargetsForCalls(), 1,
                "an ordinary non-wrapped trigger must use native preparation target routing");
        assertEquals(controller.nativeCallbackCount(), 0);
        assertEquals(controller.orderSimultaneousSaCalls(), 1);
    }

    @Test(timeOut = 5000)
    public void cyclicWrappedAbilityReturnsNativeUnsupportedPreparationWithoutResolver() {
        final BloodFixture base = bloodFixture();
        final WrappedAbility wrapper = cyclicWrappedAbility(base);

        final TriggeredTargetDecisionCoordinator.Preparation preparation =
                new TriggeredTargetDecisionCoordinator().prepare(
                        wrapper, base.chooser(), new TargetDecisionProvider(), null);

        assertEquals(preparation.getStatus(),
                TriggeredTargetDecisionCoordinator.PreparationStatus.NATIVE_UNSUPPORTED_TARGETED_TRIGGER);
        assertEquals(preparation.getReason(), "UNSUPPORTED_PROFILE");
    }

    @Test(timeOut = 5000)
    public void cyclicWrappedAbilityDirectPlayTriggerDropsUnclassifiableRouteWithoutResolver() {
        final BloodFixture base = bloodFixture();
        final WrappedAbility wrapper = cyclicWrappedAbility(base);
        final AuditedTargetController controller = installController(base);
        assertNull(controller.getTargetDecisionResolver());
        final long providerRequestsBefore = providerRequestSequence(controller.getTargetDecisionProvider());
        final int stackBefore = base.game().getStack().size();

        assertFalse(controller.playTrigger(base.source(), wrapper, true));

        assertEquals(providerRequestSequence(controller.getTargetDecisionProvider()) - providerRequestsBefore, 0L,
                "an unclassifiable direct trigger must not enter the target provider seam");
        assertEquals(controller.resolverCalls(), 0);
        assertEquals(controller.nativeCallbackCount(), 0,
                "an unclassifiable direct trigger must not invoke native target fallback");
        assertEquals(controller.chooseTargetsForCalls(), 0,
                "an unclassifiable direct trigger must not invoke chooser target fallback");
        assertEquals(controller.chooseModeForAbilityCalls(), 0,
                "an unclassifiable direct trigger must not invoke mode selection");
        assertEquals(controller.playSpellAbilityNoStackCalls(), 0,
                "an unclassifiable direct trigger must not resolve an ability");
        assertNull(controller.nativeTarget());
        assertTrue(wrapper.getWrappedAbility().getTargets().isEmpty());
        assertEquals(base.game().getStack().size(), stackBefore,
                "an unclassifiable direct trigger must not insert a stack item");
    }

    @Test(timeOut = 5000)
    public void targetedNonAbilityAdditionalChildFailsClosedUnderTriggeredAncestor() {
        final NonAbilityAdditionalChildFixture fixture = targetedNonAbilityAdditionalChildFixture();
        final AuditedTargetController controller = installController(fixture.game(), fixture.chooser());
        controller.setTargetDecisionResolver(request -> {
            controller.incrementResolverCalls();
            return null;
        });
        final long providerRequestsBefore = providerRequestSequence(controller.getTargetDecisionProvider());
        final int stackBefore = fixture.game().getStack().size();

        final TriggeredTargetIntegrityException exception = expectThrows(
                TriggeredTargetIntegrityException.class,
                () -> controller.orderAndPlaySimultaneousSa(List.of(fixture.wrapper())));

        assertEquals(exception.getReason(), "UNSUPPORTED_PROFILE");
        assertEquals(exception.getMessage(), "UNSUPPORTED_PROFILE");
        assertFalse(fixture.wrapper().usesTargeting(), "the root fixture must be non-targeting");
        assertTrue(fixture.targetedChild().usesTargeting(), "the additional child must be targeted");
        assertNull(fixture.targetedChild().getTrigger(),
                "the generic additional child must not inherit a trigger through its parent edge");
        assertEquals(providerRequestSequence(controller.getTargetDecisionProvider()) - providerRequestsBefore, 0L,
                "additional-child rejection must precede target request generation");
        assertEquals(controller.resolverCalls(), 0,
                "additional-child rejection must precede external target resolution");
        assertEquals(controller.nativeCallbackCount(), 0,
                "additional-child rejection must precede native target fallback");
        assertEquals(controller.chooseTargetsForCalls(), 0,
                "additional-child rejection must precede chooser target fallback");
        assertEquals(controller.chooseModeForAbilityCalls(), 0,
                "additional-child rejection must precede mode selection");
        assertEquals(controller.orderSimultaneousSaCalls(), 0,
                "additional-child rejection must precede AI ordering");
        assertEquals(fixture.game().getStack().size(), stackBefore,
                "additional-child rejection must not insert a stack item");
    }

    private AdditionalChildFixture targetedAdditionalChildFixture(final String routeName,
            final boolean listRoute) {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final Card source = addCardToZone("Aven Surveyor", chooser, ZoneType.Battlefield);
        final Trigger trigger = source.getTriggers().stream()
                .filter(candidate -> candidate.getMode() == TriggerType.ChangesZone)
                .filter(candidate -> "TrigCharm".equals(candidate.getParam("Execute")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Aven Surveyor Charm trigger fixture is unavailable"));
        final SpellAbility ability = trigger.ensureAbility();
        ability.setActivatingPlayer(chooser);
        final WrappedAbility wrapper = new WrappedAbility(trigger, ability, chooser);
        final AbilitySub nonTargetingHead = wrapper.getAdditionalAbilityList("Choices").stream()
                .filter(choice -> !choice.usesTargeting())
                .filter(choice -> choice.getSubAbility() == null)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Aven Surveyor lacks a usable non-targeting Charm head"));
        final AbilitySub targetBranch = wrapper.getAdditionalAbilityList("Choices").stream()
                .filter(SpellAbility::usesTargeting)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Aven Surveyor lacks a target-bearing Charm branch"));
        ability.setAdditionalAbilityList("Choices", List.of(nonTargetingHead));
        final AbilitySub targetedChild = (AbilitySub) targetBranch.copy(chooser);
        if (listRoute) {
            ability.setAdditionalAbilityList(routeName, List.of(targetedChild));
        } else {
            ability.setAdditionalAbility(routeName, targetedChild);
        }
        return new AdditionalChildFixture(game, chooser, wrapper, targetedChild);
    }

    private NonAbilityAdditionalChildFixture targetedNonAbilityAdditionalChildFixture() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final Card source = addCardToZone("Aven Surveyor", chooser, ZoneType.Battlefield);
        final Trigger trigger = source.getTriggers().stream()
                .filter(candidate -> candidate.getMode() == TriggerType.ChangesZone)
                .filter(candidate -> "TrigCharm".equals(candidate.getParam("Execute")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Aven Surveyor Charm trigger fixture is unavailable"));
        final SpellAbility ability = trigger.ensureAbility();
        ability.setActivatingPlayer(chooser);
        final WrappedAbility wrapper = new WrappedAbility(trigger, ability, chooser);
        final AbilitySub nonTargetingHead = wrapper.getAdditionalAbilityList("Choices").stream()
                .filter(choice -> !choice.usesTargeting())
                .filter(choice -> choice.getSubAbility() == null)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Aven Surveyor lacks a usable non-targeting Charm head"));
        ability.setAdditionalAbilityList("Choices", List.of(nonTargetingHead));

        final Map<String, String> targetParams = Map.of(
                "ValidTgts", "Card",
                "TargetMin", "1",
                "TargetMax", "1");
        final AbilityApiBased targetedChild = new AbilityApiBased(
                ApiType.Draw, source, Cost.Zero, new TargetRestrictions(targetParams), Map.of("NumCards", "1"));
        targetedChild.setActivatingPlayer(chooser);
        ability.setAdditionalAbility("GenericTargetedChild", targetedChild);
        return new NonAbilityAdditionalChildFixture(game, chooser, wrapper, targetedChild);
    }

    private OrdinaryCopiedChildFixture ordinaryCopiedChildFixture() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final Player opponent = game.getPlayers().get(0);
        final Card source = addCardToZone("Island", chooser, ZoneType.Battlefield);
        addCardToZone("Runeclaw Bear", opponent, ZoneType.Battlefield);
        final AbilityApiBased original = new AbilityApiBased(
                ApiType.Draw, source, Cost.Zero, null, Map.of("NumCards", "1"));
        final Map<String, String> targetParams = Map.of(
                "ValidTgts", "Card",
                "TargetMin", "1",
                "TargetMax", "1");
        final AbilitySub targetedChild = new AbilitySub(
                ApiType.Draw, source, new TargetRestrictions(targetParams), Map.of("NumCards", "1"));
        original.setSubAbility(targetedChild);
        original.setActivatingPlayer(chooser);
        final SpellAbility copiedRoot = CardFactory.copySpellAbilityAndPossiblyHost(
                original, original, chooser);
        return new OrdinaryCopiedChildFixture(game, chooser, copiedRoot, copiedRoot.getSubAbility());
    }

    private static WrappedAbility cyclicWrappedAbility(final BloodFixture base) {
        final Map<String, String> targetParams = Map.of("ValidTgts", "Card");
        final AbilitySub first = new AbilitySub(
                ApiType.Draw, base.source(), new TargetRestrictions(targetParams), Map.of());
        final AbilitySub second = new AbilitySub(ApiType.Draw, base.source(), null, Map.of());
        first.setParent(second);
        second.setParent(first);
        first.setActivatingPlayer(base.chooser());
        return new WrappedAbility(base.trigger(), first, base.chooser());
    }

    private NestedCharmFixture nestedCharmFixture() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final Card source = addCardToZone("Aven Surveyor", chooser, ZoneType.Battlefield);
        final Trigger trigger = source.getTriggers().stream()
                .filter(candidate -> candidate.getMode() == TriggerType.ChangesZone)
                .filter(candidate -> "TrigCharm".equals(candidate.getParam("Execute")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Aven Surveyor Charm trigger fixture is unavailable"));
        final SpellAbility ability = trigger.ensureAbility();
        ability.setActivatingPlayer(chooser);
        final WrappedAbility wrapper = new WrappedAbility(trigger, ability, chooser);
        assertEquals(wrapper.getApi(), ApiType.Charm);

        final AbilitySub nonTargetingHead = wrapper.getAdditionalAbilityList("Choices").stream()
                .filter(choice -> !choice.usesTargeting())
                .filter(choice -> choice.getSubAbility() == null)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Aven Surveyor lacks a usable non-targeting Charm head"));
        final AbilitySub targetBranch = wrapper.getAdditionalAbilityList("Choices").stream()
                .filter(SpellAbility::usesTargeting)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Aven Surveyor lacks a target-bearing Charm branch"));
        final AbilitySub nestedTarget = (AbilitySub) targetBranch.copy(chooser);
        nonTargetingHead.setSubAbility(nestedTarget);
        wrapper.setAdditionalAbilityList("Choices", List.of(nonTargetingHead));

        return new NestedCharmFixture(game, chooser, source, wrapper, nonTargetingHead, nestedTarget);
    }

    private BloodRun openBloodRun(final String directoryPrefix) throws Exception {
        final Random previousRandom = MyRandom.getRandom();
        final DeterminismAuditRandom auditRandom = new DeterminismAuditRandom(DETERMINISTIC_SEED);
        MyRandom.setRandom(auditRandom);
        Path traceDirectory = null;
        try {
            final BloodFixture fixture = bloodFixture();
            traceDirectory = Files.createTempDirectory(directoryPrefix);
            final DeterminismTrace trace = DeterminismTrace.attach(fixture.game(), 0, auditRandom, traceDirectory);
            return new BloodRun(fixture, trace, traceDirectory, previousRandom);
        } catch (final Exception ex) {
            try {
                if (traceDirectory != null) {
                    deleteTree(traceDirectory);
                }
            } catch (final IOException ignored) {
                // Preserve the original trace setup failure.
            } finally {
                MyRandom.setRandom(previousRandom);
            }
            throw ex;
        }
    }

    private BloodFixture bloodFixture() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final Player opponent = game.getPlayers().get(0);
        final Card source = addCardToZone(BLOOD_OPERATIVE, chooser, ZoneType.Battlefield);
        final Card firstTarget = addCardToZone("Runeclaw Bear", opponent, ZoneType.Graveyard);
        final Card secondTarget = addCardToZone("Llanowar Elves", opponent, ZoneType.Graveyard);
        final Trigger trigger = source.getTriggers().stream()
                .filter(candidate -> candidate.getMode() == TriggerType.ChangesZone)
                .filter(candidate -> "TrigChangeZone".equals(candidate.getParam("Execute")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("canonical Blood trigger fixture is unavailable"));
        final SpellAbility ability = trigger.ensureAbility();
        ability.setActivatingPlayer(chooser);
        return new BloodFixture(game, chooser, opponent, source, firstTarget, secondTarget, trigger, ability,
                new WrappedAbility(trigger, ability, chooser));
    }

    private static AuditedTargetController installController(final BloodFixture fixture) {
        return installController(fixture.game(), fixture.chooser());
    }

    private static AuditedTargetController installController(final Game game, final Player chooser) {
        final AuditedTargetController controller = new AuditedTargetController(game, chooser);
        chooser.dangerouslySetController(controller);
        return controller;
    }

    private static PreflightOnlyTargetController installPreflightController(final Game game,
            final Player chooser, final SpellAbility orderedAbility) {
        final PreflightOnlyTargetController controller = new PreflightOnlyTargetController(
                game, chooser, orderedAbility);
        chooser.dangerouslySetController(controller);
        return controller;
    }

    private static void assertBloodFixture(final BloodFixture fixture) {
        assertEquals(fixture.source().getName(), BLOOD_OPERATIVE);
        assertEquals(fixture.source().getCurrentStateName(), CardStateName.Original);
        assertFalse(fixture.source().isCloned());
        assertFalse(fixture.source().isFaceDown());
        assertTrue(fixture.source().getView().canBeShownTo(fixture.chooser().getView()));
        assertEquals(fixture.source().getZone().getZoneType(), ZoneType.Battlefield);
        assertEquals(fixture.source().getController(), fixture.chooser());
        assertEquals(fixture.trigger().getMode(), TriggerType.ChangesZone);
        assertTrue(fixture.trigger().isIntrinsic());
        assertFalse(fixture.trigger().isStatic());
        assertNull(fixture.trigger().getSpawningAbility());
        assertEquals(normalize(fixture.trigger().getOriginalMapParams(), "TriggerDescription"),
                BLOOD_TRIGGER_PARAMS);
        assertEquals(normalize(fixture.trigger().getMapParams(), "TriggerDescription"), BLOOD_TRIGGER_PARAMS);
        assertEquals(normalize(AbilityFactory.getMapParams(fixture.source().getSVar("TrigChangeZone")),
                "TgtPrompt", "ValidTgtsDesc"), BLOOD_EFFECT_PARAMS);
        assertEquals(normalize(fixture.ability().getMapParams(), "TgtPrompt", "ValidTgtsDesc", "TgtZone",
                "TargetMin", "TargetMax"), BLOOD_EFFECT_PARAMS);
        assertTrue(fixture.ability().getTargetRestrictions().getZone().contains(ZoneType.Graveyard));
        assertEquals(fixture.ability().getMinTargets(), 1);
        assertEquals(fixture.ability().getMaxTargets(), 1);
        assertFalse(fixture.ability().hasParam("Optional"));
        assertFalse(fixture.ability().hasParam("TargetingPlayer"));
        assertNull(fixture.ability().getTargetingPlayer());
        assertTrue(fixture.ability().getTargets().isEmpty(), "Blood initial targets must be empty");
        assertEquals(fixture.wrapper().getDecider(), fixture.chooser());
        assertEquals(fixture.ability().getActivatingPlayer(), fixture.chooser());
        assertFalse(fixture.wrapper().isCopied());
        assertFalse(fixture.ability().isCopied());
        assertEquals(fixture.firstTarget().getOwner(), fixture.opponent());
        assertEquals(fixture.secondTarget().getOwner(), fixture.opponent());
        assertEquals(fixture.targetNames(), TARGET_NAMES);
        assertTrue(fixture.game().getCardsIn(ZoneType.Graveyard).contains(fixture.targetByName("Runeclaw Bear")));
        assertTrue(fixture.game().getCardsIn(ZoneType.Graveyard).contains(fixture.targetByName("Llanowar Elves")));
    }

    private static void assertExactlyOneTargetApplied(final BloodFixture fixture, final String selectedName) {
        assertEquals(fixture.source().getZone().getZoneType(), ZoneType.Battlefield);
        assertEquals(countInZone(fixture, selectedName, ZoneType.Exile), 1,
                "exactly the selected Blood target must be exiled");
        final String otherName = TARGET_NAMES.stream()
                .filter(name -> !name.equals(selectedName))
                .findFirst()
                .orElseThrow();
        assertEquals(countInZone(fixture, otherName, ZoneType.Graveyard), 1,
                "the unselected Blood candidate must remain in the graveyard");
    }

    private static int countInZone(final BloodFixture fixture, final String name, final ZoneType zone) {
        return (int) fixture.game().getCardsIn(zone).stream()
                .filter(card -> name.equals(card.getName()))
                .count();
    }

    private static LegalCandidate firstTargetCandidate(final DecisionRequest request) {
        return request.getCandidates().stream()
                .filter(candidate -> candidate.getTargetKind() == TargetCandidateKind.TARGET_CARD)
                .findFirst()
                .orElseThrow();
    }

    private static LegalCandidate candidateFromAnotherProvider(final BloodFixture fixture) {
        final DecisionRequest otherRequest = new TargetDecisionProvider()
                .generateTargetRequest(fixture.ability(), fixture.chooser(), null)
                .getRequest();
        return firstTargetCandidate(otherRequest);
    }

    private static LegalCandidate candidateFromForeignGame() {
        final Random previousRandom = MyRandom.getRandom();
        MyRandom.setRandom(new DeterminismAuditRandom(DETERMINISTIC_SEED));
        try {
            final BloodFixture foreignFixture = new FRL02KTriggeredTargetExternalOwnershipAuditTest().bloodFixture();
            return firstTargetCandidate(new TargetDecisionProvider()
                    .generateTargetRequest(foreignFixture.ability(), foreignFixture.chooser(), null)
                    .getRequest());
        } finally {
            MyRandom.setRandom(previousRandom);
        }
    }

    private static Map<String, String> normalize(final Map<String, String> params, final String... ignoredKeys) {
        final Map<String, String> normalized = new HashMap<>(params);
        for (final String ignoredKey : ignoredKeys) {
            normalized.remove(ignoredKey);
        }
        return normalized;
    }

    private static List<String> nativeRunFixtureProjection(final BloodRun run) {
        return List.of(run.fixture().source().getName(), run.fixture().source().getCurrentStateName().name(),
                run.fixture().source().getZone().getZoneType().name(), run.fixture().trigger().getMode().name(),
                run.fixture().ability().getApi().name(), String.join(",", run.fixture().targetNames()));
    }

    private static List<String> externalRunFixtureProjection(final BloodFixture fixture) {
        return List.of(fixture.source().getName(), fixture.source().getCurrentStateName().name(),
                fixture.source().getZone().getZoneType().name(), fixture.trigger().getMode().name(),
                fixture.ability().getApi().name(), String.join(",", fixture.targetNames()));
    }

    private static TraceEvidence readTrace(final List<String> records) {
        final List<String> requestRecords = records.stream()
                .filter(record -> record.startsWith("DECISION_TRACE_V2|REQUEST|"))
                .toList();
        final List<String> resultRecords = records.stream()
                .filter(record -> record.startsWith("DECISION_TRACE_V2|RESULT|"))
                .toList();
        assertEquals(requestRecords.size(), 1, "exactly one TARGET request must be traced");
        assertEquals(resultRecords.size(), 1, "exactly one TARGET result must be traced");
        final String[] requestFields = requestRecords.get(0).split("\\|", -1);
        final String[] resultFields = resultRecords.get(0).split("\\|", -1);
        assertEquals(requestFields[0], "DECISION_TRACE_V2");
        assertEquals(requestFields[1], "REQUEST");
        assertEquals(resultFields[0], "DECISION_TRACE_V2");
        assertEquals(resultFields[1], "RESULT");
        return new TraceEvidence(requestRecords.get(0), resultRecords.get(0), requestFields, resultFields,
                requestRecords.size());
    }

    private static long providerRequestSequence(final TargetDecisionProvider provider) {
        try {
            final Field requestSequence = TargetDecisionProvider.class.getDeclaredField("nextRequestId");
            requestSequence.setAccessible(true);
            return requestSequence.getLong(provider);
        } catch (final ReflectiveOperationException | SecurityException exception) {
            throw new IllegalStateException("target provider request sequence unavailable", exception);
        }
    }

    private static String traceText(final String value) {
        return value.replace("%", "%25").replace("|", "%7C")
                .replace("\r", "%0D").replace("\n", "%0A");
    }

    private enum InvalidExternalCandidate {
        NULL,
        OTHER_REQUEST_PROVIDER,
        STALE_AFTER_ZONE_CHANGE,
        FOREIGN_GAME,
        LIVE_STATE_ILLEGAL
    }

    private static class AuditedTargetController extends PlayerControllerAi {
        private int nativeCallbackCount;
        private int chooseTargetsForCalls;
        private int chooseModeForAbilityCalls;
        private int playSpellAbilityNoStackCalls;
        private int orderSimultaneousSaCalls;
        private int resolverCalls;
        private Card nativeTarget;

        private AuditedTargetController(final Game game, final Player player) {
            super(game, player, new LobbyPlayerAi(player.getName() + "-frl02k-c2a-task9", null));
        }

        @Override
        protected boolean invokeNativeTriggeredTarget(final SpellAbility underlying, final boolean mandatory) {
            nativeCallbackCount++;
            final boolean result = super.invokeNativeTriggeredTarget(underlying, mandatory);
            if (result && underlying.getTargets().size() == 1 && underlying.getTargets().get(0) instanceof Card card) {
                nativeTarget = card;
            }
            return result;
        }

        @Override
        public List<SpellAbility> orderSimultaneousSa(final List<SpellAbility> activePlayerSAs) {
            orderSimultaneousSaCalls++;
            return activePlayerSAs;
        }

        @Override
        public List<AbilitySub> chooseModeForAbility(final SpellAbility sa, final List<AbilitySub> possible,
                final int min, final int num, final boolean allowRepeat) {
            chooseModeForAbilityCalls++;
            return super.chooseModeForAbility(sa, possible, min, num, allowRepeat);
        }

        @Override
        public boolean chooseTargetsFor(final SpellAbility currentAbility) {
            chooseTargetsForCalls++;
            return true;
        }

        @Override
        public void playSpellAbilityNoStack(final SpellAbility effectSA, final boolean canSetupTargets) {
            playSpellAbilityNoStackCalls++;
            super.playSpellAbilityNoStack(effectSA, canSetupTargets);
        }

        @Override
        public boolean confirmTrigger(final WrappedAbility wrapper) {
            return true;
        }

        protected int nativeCallbackCount() {
            return nativeCallbackCount;
        }

        protected int chooseTargetsForCalls() {
            return chooseTargetsForCalls;
        }

        protected int chooseModeForAbilityCalls() {
            return chooseModeForAbilityCalls;
        }

        protected int playSpellAbilityNoStackCalls() {
            return playSpellAbilityNoStackCalls;
        }

        private int orderSimultaneousSaCalls() {
            return orderSimultaneousSaCalls;
        }

        private Card nativeTarget() {
            return nativeTarget;
        }

        protected int resolverCalls() {
            return resolverCalls;
        }

        private void incrementResolverCalls() {
            resolverCalls++;
        }
    }

    private static final class PreflightOnlyTargetController extends AuditedTargetController {
        private int orderCalls;
        private final SpellAbility orderedAbility;

        private PreflightOnlyTargetController(final Game game, final Player player,
                final SpellAbility orderedAbility) {
            super(game, player);
            this.orderedAbility = orderedAbility;
        }

        @Override
        public List<SpellAbility> orderSimultaneousSa(final List<SpellAbility> activePlayerSAs) {
            orderCalls++;
            return List.of(orderedAbility);
        }

        private int orderCalls() {
            return orderCalls;
        }
    }

    private record BloodFixture(Game game, Player chooser, Player opponent, Card source, Card firstTarget,
            Card secondTarget, Trigger trigger, SpellAbility ability, WrappedAbility wrapper) {
        private List<String> targetNames() {
            return List.of(firstTarget.getName(), secondTarget.getName());
        }

        private Card targetByName(final String name) {
            return List.of(firstTarget, secondTarget).stream()
                    .filter(card -> name.equals(card.getName()))
                    .findFirst()
                    .orElseThrow();
        }

        private TargetDecisionProvider provider() {
            return chooser.getController().getTargetDecisionProvider();
        }
    }

    private record NestedCharmFixture(Game game, Player chooser, Card source, WrappedAbility wrapper,
            AbilitySub nonTargetingHead, AbilitySub nestedTarget) {
    }

    private record AdditionalChildFixture(Game game, Player chooser, WrappedAbility wrapper,
            AbilitySub targetedChild) {
    }

    private record NonAbilityAdditionalChildFixture(Game game, Player chooser, WrappedAbility wrapper,
            SpellAbility targetedChild) {
    }

    private record OrdinaryCopiedChildFixture(Game game, Player chooser, SpellAbility copiedRoot,
            SpellAbility targetedChild) {
    }

    private static final class BloodRun implements AutoCloseable {
        private final BloodFixture fixture;
        private final DeterminismTrace trace;
        private final Path traceDirectory;
        private final Random previousRandom;
        private boolean traceFinished;

        private BloodRun(final BloodFixture fixture, final DeterminismTrace trace,
                final Path traceDirectory, final Random previousRandom) {
            this.fixture = fixture;
            this.trace = trace;
            this.traceDirectory = traceDirectory;
            this.previousRandom = previousRandom;
        }

        private BloodFixture fixture() {
            return fixture;
        }

        private List<String> finishAndReadDecisionTrace() throws IOException {
            if (!traceFinished) {
                trace.finish();
                traceFinished = true;
            }
            return Files.readAllLines(traceDirectory.resolve("game-001.decision.trace"), StandardCharsets.UTF_8);
        }

        @Override
        public void close() throws Exception {
            try {
                if (!traceFinished) {
                    trace.finish();
                    traceFinished = true;
                }
            } finally {
                try {
                    deleteTree(traceDirectory);
                } finally {
                    MyRandom.setRandom(previousRandom);
                }
            }
        }
    }

    private record TraceEvidence(String requestLine, String resultLine,
            String[] requestFields, String[] resultFields, int requestCount) {
        private String requestDecisionType() {
            return requestFields[6];
        }

        private String requestAdapter() {
            return requestFields[7];
        }

        private String requestForced() {
            return requestFields[9];
        }

        private String resultKind() {
            return resultFields[3];
        }

        private String resultSelectedCandidate() {
            return resultFields[4];
        }

        private String nativeCallbackCompleted() {
            return resultFields[5];
        }

        private String mappingAttempted() {
            return resultFields[6];
        }

        private boolean isBcPolicySample() {
            return "CHOSEN".equals(resultKind())
                    && "false".equals(requestFields[9])
                    && "true".equals(nativeCallbackCompleted())
                    && "true".equals(mappingAttempted());
        }

        private boolean hasMappingFailedProvenance() {
            return requestLine.contains("|MAPPING_FAILED|") || resultLine.contains("|MAPPING_FAILED|");
        }
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
}
