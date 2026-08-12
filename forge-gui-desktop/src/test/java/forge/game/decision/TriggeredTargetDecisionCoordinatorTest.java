package forge.game.decision;

import forge.ai.AITest;
import forge.ai.LobbyPlayerAi;
import forge.ai.PlayerControllerAi;
import forge.game.Game;
import forge.game.GameObject;
import forge.game.ability.AbilityFactory;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardFactory;
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

import java.io.IOException;
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
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class TriggeredTargetDecisionCoordinatorTest extends AITest {
    @Test
    public void exactBloodEtbProfileIsAdmitted() {
        final BloodFixture fixture = bloodFixture();
        final TargetDecisionProvider provider = new TargetDecisionProvider();
        final AtomicInteger resolverCalls = new AtomicInteger();

        final var preparation = new TriggeredTargetDecisionCoordinator().prepare(
                fixture.wrapper(), fixture.chooser(), provider, request -> {
                    resolverCalls.incrementAndGet();
                    return request.getCandidates().get(0);
                });

        assertEquals(preparation.getStatus().name(), "PREPARED");
        assertEquals(normalizedOriginalTriggerProjection(fixture.trigger()), Map.of(
                "Mode", "ChangesZone",
                "Origin", "Any",
                "Destination", "Battlefield",
                "ValidCard", "Card.Self",
                "OptionalDecider", "You",
                "Execute", "TrigChangeZone"));
        final Map<String, String> staticChangeZone =
                AbilityFactory.getMapParams(fixture.source().getSVar("TrigChangeZone"));
        assertEquals(staticChangeZoneSemanticProjection(staticChangeZone), Map.of(
                "DB", "ChangeZone",
                "Origin", "Graveyard",
                "Destination", "Exile",
                "ValidTgts", "Card"));
        assertFalse(staticChangeZone.containsKey("Optional"));
        assertFalse(staticChangeZone.containsKey("TargetingPlayer"));
        assertEquals(fixture.trigger().getMode(), TriggerType.ChangesZone);
        assertEquals(fixture.trigger().getParam("Origin"), "Any");
        assertEquals(fixture.trigger().getParam("Destination"), "Battlefield");
        assertEquals(fixture.trigger().getParam("ValidCard"), "Card.Self");
        assertEquals(fixture.trigger().getParam("OptionalDecider"), "You");
        assertEquals(fixture.trigger().getParam("Execute"), "TrigChangeZone");
        assertEquals(fixture.ability().getApi(), ApiType.ChangeZone);
        assertEquals(fixture.ability().getParam("Origin"), "Graveyard");
        assertEquals(fixture.ability().getParam("Destination"), "Exile");
        assertEquals(fixture.ability().getParam("ValidTgts"), "Card");
        assertTrue(fixture.ability().getTargetRestrictions().getZone().contains(ZoneType.Graveyard));
        assertEquals(fixture.ability().getMinTargets(), 1);
        assertEquals(fixture.ability().getMaxTargets(), 1);
        assertFalse(fixture.ability().hasParam("Optional"));
        assertEquals(fixture.ability().getTargets().size(), 1);
        assertEquals(fixture.wrapper().getDecider().getId(), fixture.chooser().getId());
        assertEquals(fixture.ability().getActivatingPlayer().getId(), fixture.chooser().getId());
        assertEquals(fixture.source().getController().getId(), fixture.chooser().getId());
        assertEquals(resolverCalls.get(), 1,
                "external strategic preparation must invoke the strategic resolver exactly once");
    }

    @Test
    public void triggerDescriptionAndTargetPromptDoNotAffectAdmission() {
        final BloodFixture decorated = bloodFixture();
        decorated.trigger().putParam("TriggerDescription", "decorative trigger text");
        decorated.ability().putParam("TgtPrompt", "decorative target text");

        final var preparation = new TriggeredTargetDecisionCoordinator().prepare(
                decorated.wrapper(), decorated.chooser(), new TargetDecisionProvider(),
                request -> request.getCandidates().get(0));

        assertEquals(preparation.getStatus().name(), "PREPARED");

        final BloodFixture unknownTriggerParam = bloodFixture();
        unknownTriggerParam.trigger().putParam("UnknownSemantic", "True");
        assertUnsupportedTargeted(unknownTriggerParam, "UNSUPPORTED_PROFILE");

        final BloodFixture unknownLiveParam = bloodFixture();
        unknownLiveParam.ability().putParam("UnknownSemantic", "True");
        assertUnsupportedTargeted(unknownLiveParam, "UNSUPPORTED_PROFILE");
    }

    @Test
    public void unknownOriginalTriggerSemanticParameterRejectsAdmission() {
        final BloodFixture fixture = bloodFixture();
        fixture.trigger().getOriginalMapParams().put("UnknownSemantic", "True");

        assertEquals(fixture.trigger().getOriginalMapParams().get("UnknownSemantic"), "True");
        assertFalse(fixture.trigger().getMapParams().containsKey("UnknownSemantic"));
        assertUnsupportedTargeted(fixture, "UNSUPPORTED_PROFILE");
    }

    @Test
    public void liveChangeZoneMismatchRejectsStaticHit() {
        final BloodFixture fixture = bloodFixture();
        fixture.ability().setApi(ApiType.GainLife);

        assertUnsupportedTargeted(fixture, "LIVE_EFFECT_MISMATCH");
    }

    @Test
    public void liveTargetBoundsMismatchRejectsAdmission() {
        final BloodFixture minMismatch = bloodFixture();
        replaceLiveTargetBounds(minMismatch.ability(), "0", "1");
        assertEquals(minMismatch.ability().getMinTargets(), 0);
        assertEquals(minMismatch.ability().getMaxTargets(), 1);
        assertUnsupportedTargeted(minMismatch, "LIVE_EFFECT_MISMATCH");

        final BloodFixture maxMismatch = bloodFixture();
        replaceLiveTargetBounds(maxMismatch.ability(), "1", "2");
        assertEquals(maxMismatch.ability().getMinTargets(), 1);
        assertEquals(maxMismatch.ability().getMaxTargets(), 2);
        assertUnsupportedTargeted(maxMismatch, "LIVE_EFFECT_MISMATCH");
    }

    @Test
    public void runtimeRewriteRejectsStaticDefinition() {
        final BloodFixture triggerRewrite = bloodFixture();
        triggerRewrite.trigger().putParam("Destination", "Graveyard");
        assertUnsupportedTargeted(triggerRewrite, "UNSUPPORTED_PROFILE");

        final BloodFixture effectRewrite = bloodFixture();
        effectRewrite.source().setSVar("TrigChangeZone",
                "DB$ ChangeZone | Origin$ Library | Destination$ Exile | ValidTgts$ Card");
        assertUnsupportedTargeted(effectRewrite, "UNSUPPORTED_PROFILE");

        final BloodFixture optionalityRewrite = bloodFixture();
        optionalityRewrite.trigger().removeParam("OptionalDecider");
        assertUnsupportedTargeted(optionalityRewrite, "UNSUPPORTED_PROFILE");
    }

    @Test
    public void underlyingOptionalParamRejectsDuplicatedOptionality() {
        final BloodFixture fixture = bloodFixture();
        fixture.ability().putParam("Optional", "True");

        assertUnsupportedTargeted(fixture, "UNSUPPORTED_PROFILE");
    }

    @Test
    public void nonEmptyInitialTargetsFailBeforeGeneration() {
        final BloodFixture fixture = bloodFixture();
        fixture.ability().getTargets().add(fixture.firstTarget());

        final TriggeredTargetIntegrityException exception = assertUnsupportedTargeted(
                fixture, "NON_EMPTY_INITIAL_TARGETS");

        assertEquals(exception.getReason(), "NON_EMPTY_INITIAL_TARGETS");
        assertEquals(fixture.ability().getTargets().size(), 1);
    }

    @Test
    public void chooserMustMatchDeciderActivatorAndSourceController() {
        final BloodFixture deciderMismatch = bloodFixture();
        assertUnsupportedTargeted(withWrapper(deciderMismatch,
                new WrappedAbility(deciderMismatch.trigger(), deciderMismatch.ability(),
                        deciderMismatch.opponent())), "UNSUPPORTED_PROFILE");

        final BloodFixture activatorMismatch = bloodFixture();
        activatorMismatch.ability().setActivatingPlayer(activatorMismatch.opponent());
        assertUnsupportedTargeted(activatorMismatch, "UNSUPPORTED_PROFILE");

        final BloodFixture controllerMismatch = bloodFixture();
        controllerMismatch.source().setController(controllerMismatch.opponent(),
                controllerMismatch.game().getNextTimestamp());
        assertUnsupportedTargeted(controllerMismatch, "UNSUPPORTED_PROFILE");
    }

    @Test
    public void copiedWrapperAndClonedSourceAreSeparateProvenanceFailures() {
        final BloodFixture copiedBase = bloodFixture();
        final WrappedAbility copiedWrapper = (WrappedAbility) CardFactory.copySpellAbilityAndPossiblyHost(
                copiedBase.wrapper(), copiedBase.wrapper(), copiedBase.chooser());
        assertTrue(copiedWrapper.isCopied());
        assertUnsupportedTargeted(withWrapper(copiedBase, copiedWrapper), "UNSUPPORTED_PROFILE");

        final BloodFixture cloned = clonedSourceFixture(bloodFixture());
        assertTrue(cloned.source().isCloned());
        assertUnsupportedTargeted(cloned, "UNSUPPORTED_PROFILE");
    }

    @Test
    public void nonTargetedTriggerIsNotApplicableAndNativeWithResolver() {
        final TriggeredFixture fixture = nonTargetedFixture();
        final AtomicInteger resolverCalls = new AtomicInteger();
        final CountingTargetController nativeController = installCountingController(
                fixture.game(), fixture.chooser());

        final var preparation = new TriggeredTargetDecisionCoordinator().prepare(
                fixture.wrapper(), fixture.chooser(), new TargetDecisionProvider(), request -> {
                    resolverCalls.incrementAndGet();
                    return request.getCandidates().get(0);
                });

        assertEquals(preparation.getStatus().name(), "NOT_APPLICABLE");
        assertNull(preparation.getRequest());
        assertFalse(fixture.ability().usesTargeting());
        assertEquals(resolverCalls.get(), 0);
        assertEquals(nativeController.getChooseTargetsForCalls(), 0);
    }

    @Test
    public void unsupportedTargetedProfileFailsClosedOnlyWithResolver() {
        final TriggeredFixture fixture = targetedProfileFixture();

        assertUnsupportedTargeted(fixture, "UNSUPPORTED_PROFILE");
    }

    @Test
    public void copiedGeneratedSpawningAndTargetingPlayerCasesNeverFallbackExternally() {
        final List<Consumer<BloodFixture>> unsupportedCases = List.of(
                fixture -> fixture.wrapper().setCopied(true),
                fixture -> {
                    fixture.trigger().setIntrinsic(false);
                    fixture.ability().setIntrinsic(false);
                    fixture.wrapper().setIntrinsic(false);
                },
                fixture -> fixture.trigger().setSpawningAbility(fixture.ability()),
                fixture -> fixture.trigger().putParam("Static", "True"),
                fixture -> {
                    fixture.ability().putParam("TargetingPlayer", "You");
                    fixture.ability().setTargetingPlayer(fixture.chooser());
                });

        for (final Consumer<BloodFixture> unsupportedCase : unsupportedCases) {
            final BloodFixture fixture = bloodFixture();
            unsupportedCase.accept(fixture);
            assertUnsupportedTargeted(fixture, "UNSUPPORTED_PROFILE");
        }
    }

    @Test
    public void unsupportedTargetedProfileRemainsNativeWithoutResolver() {
        final TriggeredFixture fixture = targetedProfileFixture();

        final var preparation = new TriggeredTargetDecisionCoordinator().prepare(
                fixture.wrapper(), fixture.chooser(), new TargetDecisionProvider(), null);

        assertTrue(preparation.getStatus().name().startsWith("NATIVE"),
                "unsupported targeted profiles remain on the native path without a resolver");
        assertNotEquals(preparation.getStatus().name(), "NOT_APPLICABLE");
        assertNotEquals(preparation.getStatus().name(), "PREPARED");
    }

    @Test
    public void nonWrappedTargetedTriggerFailsClosedWhenExternalOwnershipIsActive() {
        final TriggeredFixture fixture = targetedProfileFixture();
        final TargetDecisionProvider provider = new TargetDecisionProvider();
        final AtomicInteger resolverCalls = new AtomicInteger();
        final CountingTargetController nativeController = installCountingController(
                fixture.game(), fixture.chooser());
        final SpellAbility nonWrapped = fixture.ability();
        assertFalse(nonWrapped instanceof WrappedAbility);

        final TriggeredTargetIntegrityException exception = expectThrows(
                TriggeredTargetIntegrityException.class,
                () -> new TriggeredTargetDecisionCoordinator().prepare(
                        nonWrapped, fixture.chooser(), provider, request -> {
                            resolverCalls.incrementAndGet();
                            return request.getCandidates().get(0);
                        }));

        assertEquals(exception.getReason(), "UNSUPPORTED_PROFILE");
        assertEquals(resolverCalls.get(), 0);
        assertEquals(nativeController.getChooseTargetsForCalls(), 0);
    }

    @Test
    public void bloodWithoutResolverUsesNativePreparationWithTeacherCapture() {
        final BloodFixture fixture = bloodFixture();
        final TargetDecisionProvider provider = new TargetDecisionProvider();

        final var preparation = new TriggeredTargetDecisionCoordinator().prepare(
                fixture.wrapper(), fixture.chooser(), provider, null);

        assertPreparedRequest(preparation::getRequest, fixture);
        assertEquals(preparation.getStatus(),
                TriggeredTargetDecisionCoordinator.PreparationStatus.NATIVE_WITH_TEACHER_CAPTURE);
    }

    @Test
    public void bloodWithResolverUsesPreparedExternalPathForTwoTargetRequest() {
        final BloodFixture fixture = bloodFixture();
        final TargetDecisionProvider provider = new TargetDecisionProvider();
        final CountingTargetController nativeController = installCountingController(
                fixture.game(), fixture.chooser());
        final AtomicInteger resolverCalls = new AtomicInteger();

        final var preparation = new TriggeredTargetDecisionCoordinator().prepare(
                fixture.wrapper(), fixture.chooser(), provider, request -> {
                    resolverCalls.incrementAndGet();
                    return request.getCandidates().get(0);
                });

        assertPreparedRequest(preparation::getRequest, fixture);
        assertEquals(preparation.getStatus(), TriggeredTargetDecisionCoordinator.PreparationStatus.PREPARED);
        assertEquals(fixture.ability().getTargets().size(), 1);
        assertEquals(resolverCalls.get(), 1,
                "external strategic preparation must invoke the strategic resolver exactly once");
        assertEquals(nativeController.getChooseTargetsForCalls(), 0,
                "external preparation must not invoke the native target callback");
    }

    @Test
    public void externalZeroTargetGenerationReturnsNoStackWithoutTeacherCaptureOrFallback() throws Exception {
        final BloodFixture fixture = bloodFixtureWithTargetCount(0);
        final TargetDecisionProvider provider = spy(new TargetDecisionProvider());
        final CountingTargetController nativeController = installCountingController(
                fixture.game(), fixture.chooser());
        final AtomicInteger resolverCalls = new AtomicInteger();
        final int stackSizeBefore = fixture.game().getStack().size();

        try (TraceCapture trace = attachTrace(fixture.game())) {
            final TriggeredTargetDecisionCoordinator.Preparation preparation =
                    new TriggeredTargetDecisionCoordinator().prepare(
                            fixture.wrapper(), fixture.chooser(), provider, request -> {
                                resolverCalls.incrementAndGet();
                                return null;
                            });

            assertEquals(preparation.getStatus(),
                    TriggeredTargetDecisionCoordinator.PreparationStatus.NO_STACK);
            assertEquals(preparation.getReason(), "INVALID_TARGETING");
            assertNull(preparation.getRequest());
            verify(provider, times(1)).generateTargetRequest(any(SpellAbility.class), any(Player.class),
                    isNull(ActionContinuation.class));
            assertEquals(resolverCalls.get(), 0,
                    "provider INVALID_TARGETING must not invoke the external resolver");
            assertEquals(nativeController.getNativeCallbackCalls(), 0,
                    "external zero-target preparation must not fall back to native targeting");
            assertEquals(nativeController.getChooseTargetsForCalls(), 0);
            assertEquals(fixture.ability().getTargets().size(), 0);
            assertEquals(fixture.game().getStack().size(), stackSizeBefore,
                    "external zero-target preparation must not push a stack entry");
            assertTrue(trace.finishAndReadDecisionTrace().isEmpty(),
                    "INVALID_TARGETING must not create a teacher request or result capture");
        }
    }

    @Test
    public void externalForcedOneTargetUsesProviderCompletionWithoutResolverOrNativeFallback() throws Exception {
        final BloodFixture fixture = bloodFixtureWithTargetCount(1);
        final TargetDecisionProvider provider = spy(new TargetDecisionProvider());
        final CountingTargetController nativeController = installCountingController(
                fixture.game(), fixture.chooser());
        final AtomicInteger resolverCalls = new AtomicInteger();
        final int stackSizeBefore = fixture.game().getStack().size();

        doAnswer(invocation -> {
            final TargetDecisionProvider.Generation applied =
                    (TargetDecisionProvider.Generation) invocation.callRealMethod();
            assertEquals(applied.getStatus(), TargetDecisionProvider.Status.COMPLETE,
                    "a forced external target must require provider completion");
            return applied;
        }).when(provider).apply(any(DecisionRequest.class), any(LegalCandidate.class));

        try (TraceCapture trace = attachTrace(fixture.game())) {
            final TriggeredTargetDecisionCoordinator.Preparation preparation =
                    new TriggeredTargetDecisionCoordinator().prepare(
                            fixture.wrapper(), fixture.chooser(), provider, request -> {
                                resolverCalls.incrementAndGet();
                                return null;
                            });

            assertEquals(preparation.getStatus(), TriggeredTargetDecisionCoordinator.PreparationStatus.PREPARED);
            assertNotNull(preparation.getRequest());
            assertTrue(preparation.getRequest().isForced(),
                    "the request's forced flag is authoritative for the one-candidate route");
            assertEquals(preparation.getRequest().getCandidates().size(), 1);
            final LegalCandidate selected = preparation.getRequest().getCandidates().get(0);
            assertEquals(fixture.ability().getTargets().size(), 1);
            assertSame(fixture.ability().getTargets().get(0), selected.getTarget());
            verify(provider, times(1)).apply(any(DecisionRequest.class), any(LegalCandidate.class));
            assertEquals(resolverCalls.get(), 0,
                    "forced external targeting must not invoke the strategic resolver");
            assertEquals(nativeController.getNativeCallbackCalls(), 0,
                    "forced external targeting must not fall back to the native callback");
            assertEquals(nativeController.getChooseTargetsForCalls(), 0);
            assertEquals(fixture.game().getStack().size(), stackSizeBefore,
                    "coordinator preparation must not push a stack entry before normal routing");

            final TraceEvidence evidence = targetTrace(trace.finishAndReadDecisionTrace());
            assertEquals(evidence.requestForced(), "true");
            assertEquals(evidence.resultKind(), "FORCED");
            assertEquals(evidence.nativeCallbackCompleted(), "false");
            assertEquals(evidence.mappingAttempted(), "false");
            assertEquals(evidence.engineForcedBypass(), "true");
            assertTrue(evidence.selectedCandidateIsListed());
        }
    }

    @Test
    public void externalStrategicProviderApplicationFailureIsSanitizedWithoutFallbackOrMappingFailure() throws Exception {
        final BloodFixture fixture = bloodFixture();
        final TargetDecisionProvider provider = spy(new TargetDecisionProvider());
        final String privateReason = "provider-private-strategic-application-reason";
        final CountingTargetController nativeController = installCountingController(
                fixture.game(), fixture.chooser());
        final AtomicInteger resolverCalls = new AtomicInteger();
        final int stackSizeBefore = fixture.game().getStack().size();

        doThrow(new UnsupportedTargetDecisionException(fixture.ability(), privateReason))
                .when(provider)
                .apply(any(DecisionRequest.class), any(LegalCandidate.class));

        try (TraceCapture trace = attachTrace(fixture.game())) {
            final TriggeredTargetIntegrityException exception = expectThrows(
                    TriggeredTargetIntegrityException.class,
                    () -> new TriggeredTargetDecisionCoordinator().prepare(
                            fixture.wrapper(), fixture.chooser(), provider, request -> {
                                resolverCalls.incrementAndGet();
                                return firstCardCandidate(request);
                            }));

            assertEquals(exception.getReason(), "INVALID_EXTERNAL_CANDIDATE");
            assertEquals(exception.getMessage(), "INVALID_EXTERNAL_CANDIDATE");
            assertFalse(exception.getMessage().contains(fixture.source().getName()));
            assertFalse(exception.getMessage().contains(privateReason));
            verify(provider, times(1)).apply(any(DecisionRequest.class), any(LegalCandidate.class));
            assertEquals(resolverCalls.get(), 1);
            assertEquals(nativeController.getNativeCallbackCalls(), 0,
                    "strategic provider application failure must not fall back to native targeting");
            assertEquals(nativeController.getChooseTargetsForCalls(), 0);
            assertEquals(fixture.game().getStack().size(), stackSizeBefore,
                    "strategic provider application failure must not push a stack entry");

            final TraceEvidence evidence = targetTrace(trace.finishAndReadDecisionTrace());
            assertEquals(evidence.resultKind(), "TRACE_INCOMPLETE");
            assertEquals(evidence.nativeCallbackCompleted(), "false");
            assertEquals(evidence.mappingAttempted(), "false");
            assertFalse(evidence.resultLineContains("MAPPING_FAILED"));
        }
    }

    @Test
    public void unsupportedProviderGenerationIsSanitizedWithoutFallbackOrMappingFailure() throws Exception {
        final BloodFixture fixture = bloodFixture();
        final TargetDecisionProvider provider = spy(new TargetDecisionProvider());
        final String privateReason = "provider-private-generation-reason";
        doThrow(new UnsupportedTargetDecisionException(fixture.ability(), privateReason))
                .when(provider)
                .generateTargetRequest(any(SpellAbility.class), any(Player.class),
                        isNull(ActionContinuation.class));
        final AtomicInteger resolverCalls = new AtomicInteger();
        final CountingTargetController nativeController = installCountingController(
                fixture.game(), fixture.chooser());
        final int stackSizeBefore = fixture.game().getStack().size();

        try (TraceCapture trace = attachTrace(fixture.game())) {
            final TriggeredTargetIntegrityException exception = expectThrows(
                    TriggeredTargetIntegrityException.class,
                    () -> new TriggeredTargetDecisionCoordinator().prepare(
                            fixture.wrapper(), fixture.chooser(), provider, request -> {
                                resolverCalls.incrementAndGet();
                                return request.getCandidates().get(0);
                            }));

            assertEquals(exception.getReason(), "TARGET_APPLICATION_INCOMPLETE");
            assertEquals(exception.getMessage(), "TARGET_APPLICATION_INCOMPLETE");
            assertFalse(exception.getMessage().contains(fixture.source().getName()));
            assertFalse(exception.getMessage().contains(privateReason));
            assertEquals(resolverCalls.get(), 0);
            assertEquals(nativeController.getNativeCallbackCalls(), 0);
            assertEquals(nativeController.getChooseTargetsForCalls(), 0);
            assertEquals(fixture.game().getStack().size(), stackSizeBefore);

            final List<String> records = trace.finishAndReadDecisionTrace();
            assertTrue(records.stream().noneMatch(record -> record.contains("|MAPPING_FAILED|")));
            assertTrue(records.stream().noneMatch(record -> record.startsWith("DECISION_TRACE_V2|REQUEST|")));
        }
    }

    @Test
    public void unsupportedForcedProviderApplicationIsSanitizedWithoutFallbackOrMappingFailure() throws Exception {
        final BloodFixture fixture = bloodFixtureWithTargetCount(1);
        final TargetDecisionProvider provider = spy(new TargetDecisionProvider());
        final String privateReason = "provider-private-application-reason";
        doThrow(new UnsupportedTargetDecisionException(fixture.ability(), privateReason))
                .when(provider)
                .apply(any(DecisionRequest.class), any(LegalCandidate.class));
        final AtomicInteger resolverCalls = new AtomicInteger();
        final CountingTargetController nativeController = installCountingController(
                fixture.game(), fixture.chooser());
        final int stackSizeBefore = fixture.game().getStack().size();

        try (TraceCapture trace = attachTrace(fixture.game())) {
            final TriggeredTargetIntegrityException exception = expectThrows(
                    TriggeredTargetIntegrityException.class,
                    () -> new TriggeredTargetDecisionCoordinator().prepare(
                            fixture.wrapper(), fixture.chooser(), provider, request -> {
                                resolverCalls.incrementAndGet();
                                return request.getCandidates().get(0);
                            }));

            assertEquals(exception.getReason(), "TARGET_APPLICATION_INCOMPLETE");
            assertEquals(exception.getMessage(), "TARGET_APPLICATION_INCOMPLETE");
            assertFalse(exception.getMessage().contains(fixture.source().getName()));
            assertFalse(exception.getMessage().contains(privateReason));
            assertEquals(resolverCalls.get(), 0,
                    "forced application must not invoke the strategic resolver");
            assertEquals(nativeController.getNativeCallbackCalls(), 0);
            assertEquals(nativeController.getChooseTargetsForCalls(), 0);
            assertEquals(fixture.game().getStack().size(), stackSizeBefore);

            final List<String> records = trace.finishAndReadDecisionTrace();
            assertTrue(records.stream().anyMatch(record -> record.startsWith("DECISION_TRACE_V2|REQUEST|")));
            assertTrue(records.stream().noneMatch(record -> record.contains("|MAPPING_FAILED|")));
        }
    }

    @Test
    public void nativeZeroTargetBloodUsesForgeNoStackPathWithoutDecisionRequest() throws Exception {
        final BloodFixture fixture = bloodFixtureWithTargetCount(0);
        final TargetDecisionProvider provider = new TargetDecisionProvider();
        final TargetDecisionProvider.Generation generation = provider.generateTargetRequest(
                fixture.ability(), fixture.chooser(), null);
        assertEquals(generation.getStatus(), TargetDecisionProvider.Status.INVALID_TARGETING);
        assertNull(generation.getRequest(),
                "zero legal Blood targets must not produce an exported DecisionRequest");
        assertEquals(new TriggeredTargetDecisionCoordinator().classify(fixture.wrapper()),
                TriggeredTargetDecisionCoordinator.Classification.ADMITTED);

        final CountingTargetController nativeController = installCountingController(
                fixture.game(), fixture.chooser());
        try (TraceCapture trace = attachTrace(fixture.game())) {
            final int stackSizeBefore = fixture.game().getStack().size();

            assertFalse(nativeController.playTrigger(fixture.source(), fixture.wrapper(), true));
            assertEquals(nativeController.getNativeCallbackCalls(), 1,
                    "native Blood preparation must retain Forge's failed no-target callback");
            assertNull(nativeController.getTargetDecisionResolver());
            assertEquals(nativeController.getConfirmTriggerCalls(), 0);
            assertEquals(fixture.game().getStack().size(), stackSizeBefore,
                    "failed mandatory native targeting must not push a stack entry");
            assertTrue(fixture.ability().getTargets().isEmpty());

            final List<String> records = trace.finishAndReadDecisionTrace();
            assertTrue(records.stream().noneMatch(record -> record.startsWith("DECISION_TRACE_V2|REQUEST|")),
                    "zero-target native Blood must not create a C2A request or teacher capture");
            assertTrue(records.stream().noneMatch(record -> record.startsWith("DECISION_TRACE_V2|RESULT|")));
        }
    }

    @Test
    public void nativeForcedOneTargetBloodMapsSoleCandidateOnce() throws Exception {
        final BloodFixture fixture = bloodFixtureWithTargetCount(1);
        final CountingTargetController nativeController = installCountingController(
                fixture.game(), fixture.chooser());
        try (TraceCapture trace = attachTrace(fixture.game())) {
            final int stackSizeBefore = fixture.game().getStack().size();

            assertTrue(nativeController.playTrigger(fixture.source(), fixture.wrapper(), true));
            assertEquals(nativeController.getNativeCallbackCalls(), 1,
                    "the native adapter must be called exactly once for a forced Blood target");
            assertNull(nativeController.getTargetDecisionResolver());
            assertEquals(nativeController.getConfirmTriggerCalls(), 1);
            assertSame(nativeController.getNativeTarget(), fixture.firstTarget());
            assertEquals(fixture.game().getStack().size(), stackSizeBefore,
                    "Blood's native no-stack route must remain unchanged");
            assertTrue(containsCardIdInZone(fixture.game(), fixture.firstTarget().getId(), ZoneType.Exile));

            final TraceEvidence evidence = targetTrace(trace.finishAndReadDecisionTrace());
            assertEquals(evidence.requestDecisionType(), "TARGET");
            assertEquals(evidence.requestForced(), "true");
            assertEquals(evidence.resultKind(), "FORCED");
            assertEquals(evidence.nativeCallbackCompleted(), "true");
            assertEquals(evidence.mappingAttempted(), "true");
            assertEquals(evidence.engineForcedBypass(), "false");
            assertTrue(evidence.selectedCandidateIsListed());
        }
    }

    @Test
    public void nativeStrategicMultiTargetBloodMapsExactlyOneNewTargetOnce() throws Exception {
        final BloodFixture fixture = bloodFixture();
        final CountingTargetController nativeController = installCountingController(
                fixture.game(), fixture.chooser());
        try (TraceCapture trace = attachTrace(fixture.game())) {
            final int stackSizeBefore = fixture.game().getStack().size();

            assertTrue(nativeController.playTrigger(fixture.source(), fixture.wrapper(), true));
            assertEquals(nativeController.getNativeCallbackCalls(), 1,
                    "the native adapter must be called exactly once for a strategic Blood target");
            assertNull(nativeController.getTargetDecisionResolver());
            assertEquals(nativeController.getConfirmTriggerCalls(), 1);
            assertNotNull(nativeController.getNativeTarget());
            assertTrue(fixture.targetIds().contains(nativeController.getNativeTarget().getId()));
            assertEquals(countTargetIdsInZone(fixture, ZoneType.Exile), 1,
                    "native targeting must apply exactly one legal candidate");
            assertEquals(countTargetIdsInZone(fixture, ZoneType.Graveyard), 1);
            assertEquals(fixture.game().getStack().size(), stackSizeBefore);

            final TraceEvidence evidence = targetTrace(trace.finishAndReadDecisionTrace());
            assertEquals(evidence.requestDecisionType(), "TARGET");
            assertEquals(evidence.requestForced(), "false");
            assertEquals(evidence.resultKind(), "CHOSEN");
            assertEquals(evidence.nativeCallbackCompleted(), "true");
            assertEquals(evidence.mappingAttempted(), "true");
            assertTrue(evidence.selectedCandidateIsListed(),
                    "the native target must map to one candidate from the existing request");
        }
    }

    @Test
    public void nativeResultFalseSanitizesToMappingFailedWithoutExternalRoute() throws Exception {
        final BloodFixture fixture = bloodFixture();

        assertNativeMappingFailed(fixture, false, preparation -> {
            assertTrue(fixture.ability().getTargets().isEmpty());
            assertNotNull(preparation.getRequest());
        });
    }

    @Test
    public void nativeResultTrueWithZeroNewTargetsSanitizesToMappingFailed() throws Exception {
        final BloodFixture fixture = bloodFixture();

        assertNativeMappingFailed(fixture, true, preparation -> {
            assertTrue(fixture.ability().getTargets().isEmpty());
            assertNotNull(preparation.getRequest());
        });
    }

    @Test
    public void nativeResultTrueWithMultipleNewTargetsSanitizesToMappingFailed() throws Exception {
        final BloodFixture fixture = bloodFixture();

        assertNativeMappingFailed(fixture, true, preparation -> {
            final List<LegalCandidate> candidates = targetCardCandidates(preparation.getRequest());
            assertEquals(candidates.size(), 2);
            assertTrue(fixture.ability().getTargets().add(candidates.get(0).getTarget()));
            assertTrue(fixture.ability().getTargets().add(candidates.get(1).getTarget()));
            assertEquals(fixture.ability().getTargets().size(), 2);
        });
    }

    @Test
    public void nativeResultTrueWithForeignTargetSanitizesToMappingFailed() throws Exception {
        final BloodFixture fixture = bloodFixture();

        assertNativeMappingFailed(fixture, true, preparation -> {
            assertTrue(preparation.getRequest().getCandidates().stream()
                    .noneMatch(candidate -> candidate.getTarget() == fixture.source()));
            assertTrue(fixture.ability().getTargets().add(fixture.source()));
            assertEquals(fixture.ability().getTargets().size(), 1);
        });
    }

    @Test
    public void nativeDuplicateTargetStateCoversUnconstructibleAmbiguousIdentityMapping() throws Exception {
        final BloodFixture fixture = bloodFixture();

        assertNativeMappingFailed(fixture, true, preparation -> {
            final List<LegalCandidate> candidates = targetCardCandidates(preparation.getRequest());
            assertEquals(candidates.size(), 2);
            assertTrue(candidates.get(0).getTarget() != candidates.get(1).getTarget(),
                    "exact Blood request candidates have distinct target identities");

            // TargetDecisionProvider creates one immutable candidate per exact Blood target identity,
            // and DecisionRequest copies that list. The public Forge API cannot inject two request
            // candidates for one identity here, while Forge TargetChoices rejects a duplicate live
            // identity. The nearest authoritative ambiguity is therefore the >1-new-target state,
            // which must fail the exact-new-target count before identity mapping.
            final GameObject duplicateTarget = candidates.get(0).getTarget();
            assertTrue(fixture.ability().getTargets().add(duplicateTarget));
            assertFalse(fixture.ability().getTargets().add(duplicateTarget),
                    "Forge TargetChoices rejects duplicate target identity");
            assertTrue(fixture.ability().getTargets().add(candidates.get(1).getTarget()));
            assertEquals(fixture.ability().getTargets().size(), 2);
        });
    }

    @Test
    public void externalBloodTargetAStaysAuthoritativeThroughConfirmationAndNoStackResolution() throws Exception {
        final BloodFixture fixture = bloodFixture();
        final CountingTargetController controller = installCountingController(
                fixture.game(), fixture.chooser());
        final AtomicInteger resolverCalls = new AtomicInteger();
        final AtomicReference<LegalCandidate> selectedCandidate = new AtomicReference<>();
        controller.setTargetDecisionResolver(request -> {
            resolverCalls.incrementAndGet();
            final LegalCandidate selected = firstCardCandidate(request);
            selectedCandidate.set(selected);
            return selected;
        });

        try (TraceCapture trace = attachTrace(fixture.game())) {
            final int stackSizeBefore = fixture.game().getStack().size();

            assertTrue(controller.playTrigger(fixture.source(), fixture.wrapper(), true));
            final Card targetA = (Card) selectedCandidate.get().getTarget();
            assertSame(controller.getTargetAtConfirmation(), targetA,
                    "external preparation must leave target A on the live underlying ability");
            assertEquals(resolverCalls.get(), 1);
            assertEquals(controller.getNativeCallbackCalls(), 0,
                    "confirmTrigger's temporary target evaluation must not re-enter C2A");
            assertEquals(controller.getConfirmTriggerCalls(), 1);
            assertEquals(fixture.game().getStack().size(), stackSizeBefore,
                    "the existing external no-stack route must remain intact");
            assertTrue(containsCardIdInZone(fixture.game(), targetA.getId(), ZoneType.Exile),
                    "the effect must consume stack/no-stack target A rather than temporary target B");
            assertEquals(countTargetIdsInZone(fixture, ZoneType.Exile), 1);
            assertEquals(countTargetIdsInZone(fixture, ZoneType.Graveyard), 1);

            final TraceEvidence evidence = targetTrace(trace.finishAndReadDecisionTrace());
            assertEquals(evidence.resultKind(), "CHOSEN");
            assertEquals(evidence.nativeCallbackCompleted(), "false");
            assertEquals(evidence.mappingAttempted(), "false");
            assertTrue(evidence.selectedCandidateIsListed());
        }
    }

    @Test
    public void stackTimeBloodTargetAStaysAuthoritativeThroughTemporaryConfirmationTarget() throws Exception {
        final BloodFixture fixture = bloodFixture();
        final CountingTargetController controller = installCountingController(
                fixture.game(), fixture.chooser());
        final AtomicInteger resolverCalls = new AtomicInteger();
        final AtomicReference<LegalCandidate> selectedCandidate = new AtomicReference<>();

        try (TraceCapture trace = attachTrace(fixture.game())) {
            final TriggeredTargetDecisionCoordinator.Preparation preparation =
                    new TriggeredTargetDecisionCoordinator().prepare(
                            fixture.wrapper(), fixture.chooser(), controller.getTargetDecisionProvider(), request -> {
                                resolverCalls.incrementAndGet();
                                final LegalCandidate selected = firstCardCandidate(request);
                                selectedCandidate.set(selected);
                                return selected;
                            });
            assertEquals(preparation.getStatus(), TriggeredTargetDecisionCoordinator.PreparationStatus.PREPARED);

            final Card targetA = (Card) selectedCandidate.get().getTarget();
            assertSame(fixture.ability().getTargets().get(0), targetA);
            final int stackSizeBefore = fixture.game().getStack().size();
            fixture.game().getStack().add(fixture.wrapper());
            assertEquals(fixture.game().getStack().size(), stackSizeBefore + 1);

            fixture.game().getStack().resolveStack();

            assertEquals(resolverCalls.get(), 1);
            assertEquals(controller.getNativeCallbackCalls(), 0,
                    "the later temporary B evaluation must not invoke the C2A native adapter");
            assertEquals(controller.getConfirmTriggerCalls(), 1);
            assertSame(controller.getTargetAtConfirmation(), targetA);
            assertEquals(fixture.game().getStack().size(), stackSizeBefore);
            assertTrue(containsCardIdInZone(fixture.game(), targetA.getId(), ZoneType.Exile),
                    "the ChangeZone effect must consume stack-time target A");
            assertEquals(countTargetIdsInZone(fixture, ZoneType.Exile), 1);
            assertEquals(countTargetIdsInZone(fixture, ZoneType.Graveyard), 1);

            final TraceEvidence evidence = targetTrace(trace.finishAndReadDecisionTrace());
            assertEquals(evidence.resultKind(), "CHOSEN");
            assertEquals(evidence.nativeCallbackCompleted(), "false");
            assertEquals(evidence.mappingAttempted(), "false");
            assertTrue(evidence.selectedCandidateIsListed());
        }
    }

    @Test
    public void externalBloodFizzleDoesNotRetargetAfterStackPreparation() throws Exception {
        final BloodFixture fixture = bloodFixture();
        final CountingTargetController controller = installCountingController(
                fixture.game(), fixture.chooser());
        final AtomicInteger resolverCalls = new AtomicInteger();
        final AtomicReference<LegalCandidate> selectedCandidate = new AtomicReference<>();

        try (TraceCapture trace = attachTrace(fixture.game())) {
            final TriggeredTargetDecisionCoordinator.Preparation preparation =
                    new TriggeredTargetDecisionCoordinator().prepare(
                            fixture.wrapper(), fixture.chooser(), controller.getTargetDecisionProvider(), request -> {
                                resolverCalls.incrementAndGet();
                                final LegalCandidate selected = firstCardCandidate(request);
                                selectedCandidate.set(selected);
                                return selected;
                            });
            assertEquals(preparation.getStatus(), TriggeredTargetDecisionCoordinator.PreparationStatus.PREPARED);

            final Card targetA = (Card) selectedCandidate.get().getTarget();
            assertSame(fixture.ability().getTargets().get(0), targetA);
            final int stackSizeBefore = fixture.game().getStack().size();
            fixture.game().getStack().add(fixture.wrapper());
            assertEquals(fixture.game().getStack().size(), stackSizeBefore + 1);
            assertSame(fixture.ability().getTargets().get(0), targetA);

            fixture.game().getAction().moveTo(ZoneType.Exile, targetA, null, null);
            fixture.game().getStack().resolveStack();

            assertEquals(resolverCalls.get(), 1,
                    "an illegal stack-time target must not trigger a second TARGET request");
            assertEquals(controller.getNativeCallbackCalls(), 0);
            assertEquals(controller.getConfirmTriggerCalls(), 0,
                    "a normal Forge fizzle must stop before confirmTrigger");
            assertEquals(fixture.game().getStack().size(), stackSizeBefore);
            assertTrue(fixture.ability().getTargets().isEmpty(),
                    "fizzle may clear the stale target but must not replace it");
            assertEquals(countTargetIdsInZone(fixture, ZoneType.Exile), 1,
                    "the original A must remain the only moved card");
            assertEquals(countTargetIdsInZone(fixture, ZoneType.Graveyard), 1,
                    "fizzle must not apply Blood to another candidate");

            final TraceEvidence evidence = targetTrace(trace.finishAndReadDecisionTrace());
            assertEquals(evidence.resultKind(), "CHOSEN");
            assertEquals(evidence.nativeCallbackCompleted(), "false");
            assertEquals(evidence.mappingAttempted(), "false");
            assertTrue(evidence.selectedCandidateIsListed());
        }
    }

    private BloodFixture bloodFixture() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final Player opponent = game.getPlayers().get(0);
        final Card source = addCardToZone("Blood Operative", chooser, ZoneType.Battlefield);
        final Card firstTarget = addCardToZone("Runeclaw Bear", opponent, ZoneType.Graveyard);
        final Card secondTarget = addCardToZone("Llanowar Elves", opponent, ZoneType.Graveyard);
        final Trigger trigger = bloodTrigger(source);
        final SpellAbility ability = trigger.ensureAbility();
        ability.setActivatingPlayer(chooser);
        return new BloodFixture(game, chooser, opponent, source, trigger, ability, firstTarget,
                List.of(firstTarget.getId(), secondTarget.getId()).stream().sorted().toList(),
                new WrappedAbility(trigger, ability, chooser));
    }

    private BloodFixture bloodFixtureWithTargetCount(final int targetCount) {
        if (targetCount == 2) {
            return bloodFixture();
        }
        if (targetCount < 0 || targetCount > 1) {
            throw new IllegalArgumentException("test fixture supports zero or one target here");
        }

        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final Player opponent = game.getPlayers().get(0);
        final Card source = addCardToZone("Blood Operative", chooser, ZoneType.Battlefield);
        final Card firstTarget = targetCount == 0
                ? null : addCardToZone("Runeclaw Bear", opponent, ZoneType.Graveyard);
        final Trigger trigger = bloodTrigger(source);
        final SpellAbility ability = trigger.ensureAbility();
        ability.setActivatingPlayer(chooser);
        return new BloodFixture(game, chooser, opponent, source, trigger, ability, firstTarget,
                firstTarget == null ? List.of() : List.of(firstTarget.getId()),
                new WrappedAbility(trigger, ability, chooser));
    }

    private TriggeredFixture nonTargetedFixture() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final Card source = addCardToZone("Quirion Sentinel", chooser, ZoneType.Battlefield);
        final Trigger trigger = triggerFor(source, TriggerType.ChangesZone, "TrigMana");
        final SpellAbility ability = trigger.ensureAbility();
        ability.setActivatingPlayer(chooser);
        return new TriggeredFixture(game, chooser, source, trigger, ability,
                new WrappedAbility(trigger, ability, chooser));
    }

    private TriggeredFixture targetedProfileFixture() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final Card source = addCardToZone("Quill-Slinger Boggart", chooser, ZoneType.Battlefield);
        final Trigger trigger = triggerFor(source, TriggerType.SpellCast, "TrigLoseLife");
        final SpellAbility ability = trigger.ensureAbility();
        ability.setActivatingPlayer(chooser);
        return new TriggeredFixture(game, chooser, source, trigger, ability,
                new WrappedAbility(trigger, ability, chooser));
    }

    private BloodFixture clonedSourceFixture(final BloodFixture base) {
        final Card clonedSource = addCardToZone("Blood Operative", base.chooser(), ZoneType.Battlefield);
        clonedSource.addCloneState(CardFactory.getCloneStates(base.source(), clonedSource, base.ability()),
                base.game().getNextTimestamp());
        final Trigger trigger = bloodTrigger(clonedSource);
        final SpellAbility ability = trigger.ensureAbility();
        ability.setActivatingPlayer(base.chooser());
        return new BloodFixture(base.game(), base.chooser(), base.opponent(), clonedSource, trigger, ability,
                base.firstTarget(), base.targetIds(), new WrappedAbility(trigger, ability, base.chooser()));
    }

    private static Trigger bloodTrigger(final Card source) {
        return triggerFor(source, TriggerType.ChangesZone, "TrigChangeZone");
    }

    private static Trigger triggerFor(final Card source, final TriggerType mode, final String execute) {
        return source.getTriggers().stream()
                .filter(candidate -> candidate.getMode() == mode)
                .filter(candidate -> execute.equals(candidate.getParam("Execute")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("expected trigger fixture is unavailable"));
    }

    private static Map<String, String> normalizedOriginalTriggerProjection(final Trigger trigger) {
        final Map<String, String> original = trigger.getOriginalMapParams();
        return Map.of(
                "Mode", original.get("Mode"),
                "Origin", original.get("Origin"),
                "Destination", original.get("Destination"),
                "ValidCard", original.get("ValidCard"),
                "OptionalDecider", original.get("OptionalDecider"),
                "Execute", original.get("Execute"));
    }

    private static Map<String, String> staticChangeZoneSemanticProjection(
            final Map<String, String> staticChangeZone) {
        return Map.of(
                "DB", staticChangeZone.get("DB"),
                "Origin", staticChangeZone.get("Origin"),
                "Destination", staticChangeZone.get("Destination"),
                "ValidTgts", staticChangeZone.get("ValidTgts"));
    }

    private static void replaceLiveTargetBounds(final SpellAbility ability,
            final String minTargets, final String maxTargets) {
        final Map<String, String> liveParams = new HashMap<>(ability.getMapParams());
        liveParams.put("TargetMin", minTargets);
        liveParams.put("TargetMax", maxTargets);
        ability.setTargetRestrictions(new TargetRestrictions(liveParams));
    }

    private static BloodFixture withWrapper(final BloodFixture fixture, final WrappedAbility wrapper) {
        return new BloodFixture(fixture.game(), fixture.chooser(), fixture.opponent(), fixture.source(),
                fixture.trigger(), fixture.ability(), fixture.firstTarget(), fixture.targetIds(), wrapper);
    }

    private static TriggeredTargetIntegrityException assertUnsupportedTargeted(
            final Fixture fixture, final String reason) {
        final TargetDecisionProvider provider = new TargetDecisionProvider();
        final AtomicInteger resolverCalls = new AtomicInteger();
        final CountingTargetController nativeController = installCountingController(
                fixture.game(), fixture.chooser());

        final TriggeredTargetIntegrityException exception = expectThrows(
                TriggeredTargetIntegrityException.class,
                () -> new TriggeredTargetDecisionCoordinator().prepare(
                        fixture.wrapper(), fixture.chooser(), provider, request -> {
                            resolverCalls.incrementAndGet();
                            return request.getCandidates().get(0);
                        }));

        assertEquals(exception.getReason(), reason);
        assertEquals(resolverCalls.get(), 0,
                "unsupported external ownership must not invoke the resolver");
        assertEquals(nativeController.getChooseTargetsForCalls(), 0,
                "unsupported external ownership must not fall back to Forge AI");
        return exception;
    }

    private static void assertNativeMappingFailed(final BloodFixture fixture, final boolean nativeResult,
            final Consumer<TriggeredTargetDecisionCoordinator.Preparation> nativeStateMutation) throws Exception {
        try (TraceCapture trace = attachTrace(fixture.game())) {
            final TriggeredTargetDecisionCoordinator coordinator = new TriggeredTargetDecisionCoordinator();
            final TriggeredTargetDecisionCoordinator.Preparation preparation = coordinator.prepare(
                    fixture.wrapper(), fixture.chooser(), new TargetDecisionProvider(), null);
            assertEquals(preparation.getStatus(),
                    TriggeredTargetDecisionCoordinator.PreparationStatus.NATIVE_WITH_TEACHER_CAPTURE,
                    "native mapping failures must use the native-only teacher-capture preparation");
            assertNotNull(preparation.getRequest());

            nativeStateMutation.accept(preparation);
            final TriggeredTargetIntegrityException exception = expectThrows(
                    TriggeredTargetIntegrityException.class,
                    () -> coordinator.completeNative(preparation, nativeResult));
            assertEquals(exception.getReason(), "MAPPING_FAILED");

            final TraceEvidence evidence = targetTrace(trace.finishAndReadDecisionTrace());
            assertEquals(evidence.requestDecisionType(), "TARGET");
            assertEquals(evidence.resultKind(), "MAPPING_FAILED");
            assertEquals(evidence.nativeCallbackCompleted(), "true",
                    "native mapping failure must not be recorded as external ownership");
            assertEquals(evidence.mappingAttempted(), "true");
            assertEquals(evidence.engineForcedBypass(), "false");
        }
    }

    private static List<LegalCandidate> targetCardCandidates(final DecisionRequest request) {
        return request.getCandidates().stream()
                .filter(candidate -> candidate.getTargetKind() == TargetCandidateKind.TARGET_CARD)
                .toList();
    }

    private static void assertPreparedRequest(final Supplier<DecisionRequest> requestSupplier,
            final BloodFixture fixture) {
        final DecisionRequest request = requestSupplier.get();
        assertNotNull(request);
        assertEquals(request.getRequestId(), 0L);
        assertEquals(request.getDecisionType(), DecisionType.TARGET);
        assertFalse(request.isForced());
        assertSame(request.getTargetContext().getAbility(), fixture.ability());
        assertEquals(request.getTargetContext().getChoosingPlayerId(), fixture.chooser().getId());
        assertFalse(request.getTargetContext().hasActionContinuation());
        assertEquals(request.getCandidates().stream()
                .filter(candidate -> candidate.getTargetKind() == TargetCandidateKind.TARGET_CARD)
                .map(LegalCandidate::getTargetEntityId)
                .sorted()
                .toList(), fixture.targetIds());
    }

    private static CountingTargetController installCountingController(final Game game, final Player player) {
        final CountingTargetController controller = new CountingTargetController(
                game, player);
        player.dangerouslySetController(controller);
        return controller;
    }

    private static LegalCandidate firstCardCandidate(final DecisionRequest request) {
        return request.getCandidates().stream()
                .filter(candidate -> candidate.getTargetKind() == TargetCandidateKind.TARGET_CARD)
                .findFirst()
                .orElseThrow(() -> new AssertionError("expected a Blood card candidate"));
    }

    private static long countTargetIdsInZone(final BloodFixture fixture, final ZoneType zone) {
        return fixture.game().getCardsIn(zone).stream()
                .filter(card -> fixture.targetIds().contains(card.getId()))
                .count();
    }

    private static boolean containsCardIdInZone(final Game game, final int cardId, final ZoneType zone) {
        return game.getCardsIn(zone).stream().anyMatch(card -> card.getId() == cardId);
    }

    private static TraceCapture attachTrace(final Game game) throws IOException {
        final Random previousRandom = MyRandom.getRandom();
        final DeterminismAuditRandom auditRandom = new DeterminismAuditRandom(20260811L);
        MyRandom.setRandom(auditRandom);
        Path directory = null;
        try {
            directory = Files.createTempDirectory("frl02k-c2a-coordinator-");
            return new TraceCapture(DeterminismTrace.attach(game, 0, auditRandom, directory), directory,
                    previousRandom);
        } catch (final IOException | RuntimeException ex) {
            MyRandom.setRandom(previousRandom);
            if (directory != null) {
                try {
                    deleteTree(directory);
                } catch (final IOException ignored) {
                    // Preserve the original trace setup failure.
                }
            }
            throw ex;
        }
    }

    private static TraceEvidence targetTrace(final List<String> records) {
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
        return new TraceEvidence(requestFields, resultFields);
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

    private static final class CountingTargetController extends PlayerControllerAi {
        private int nativeCallbackCalls;
        private int chooseTargetsForCalls;
        private int confirmTriggerCalls;
        private Card nativeTarget;
        private GameObject targetAtConfirmation;

        private CountingTargetController(final Game game, final Player player) {
            super(game, player, new LobbyPlayerAi(player.getName() + "-frl02k-c2a", null));
        }

        @Override
        protected boolean invokeNativeTriggeredTarget(final SpellAbility underlying, final boolean mandatory) {
            nativeCallbackCalls++;
            final boolean result = super.invokeNativeTriggeredTarget(underlying, mandatory);
            if (result && underlying.getTargets().size() == 1
                    && underlying.getTargets().get(0) instanceof Card card) {
                nativeTarget = card;
            }
            return result;
        }

        @Override
        public boolean confirmTrigger(final WrappedAbility wrapper) {
            confirmTriggerCalls++;
            if (wrapper.getWrappedAbility().getTargets().size() == 1
                    && wrapper.getWrappedAbility().getTargets().get(0) != null) {
                targetAtConfirmation = wrapper.getWrappedAbility().getTargets().get(0);
            }
            return super.confirmTrigger(wrapper);
        }

        @Override
        public boolean chooseTargetsFor(final SpellAbility currentAbility) {
            chooseTargetsForCalls++;
            return true;
        }

        private int getNativeCallbackCalls() {
            return nativeCallbackCalls;
        }

        private int getChooseTargetsForCalls() {
            return chooseTargetsForCalls;
        }

        private int getConfirmTriggerCalls() {
            return confirmTriggerCalls;
        }

        private Card getNativeTarget() {
            return nativeTarget;
        }

        private GameObject getTargetAtConfirmation() {
            return targetAtConfirmation;
        }
    }

    private static final class TraceCapture implements AutoCloseable {
        private final DeterminismTrace trace;
        private final Path directory;
        private final Random previousRandom;
        private boolean finished;

        private TraceCapture(final DeterminismTrace trace0, final Path directory0,
                final Random previousRandom0) {
            trace = trace0;
            directory = directory0;
            previousRandom = previousRandom0;
        }

        private List<String> finishAndReadDecisionTrace() throws IOException {
            if (!finished) {
                trace.finish();
                finished = true;
            }
            final Path decisionTrace = directory.resolve("game-001.decision.trace");
            return Files.exists(decisionTrace)
                    ? Files.readAllLines(decisionTrace, StandardCharsets.UTF_8) : List.of();
        }

        @Override
        public void close() throws Exception {
            try {
                if (!finished) {
                    trace.finish();
                    finished = true;
                }
            } finally {
                try {
                    deleteTree(directory);
                } finally {
                    MyRandom.setRandom(previousRandom);
                }
            }
        }
    }

    private record TraceEvidence(String[] requestFields, String[] resultFields) {
        private String requestDecisionType() {
            return requestFields[6];
        }

        private String requestForced() {
            return requestFields[9];
        }

        private String resultKind() {
            return resultFields[3];
        }

        private String nativeCallbackCompleted() {
            return resultFields[5];
        }

        private String mappingAttempted() {
            return resultFields[6];
        }

        private String engineForcedBypass() {
            return resultFields[8];
        }

        private boolean selectedCandidateIsListed() {
            return requestFields[10].contains(resultFields[4]);
        }

        private boolean resultLineContains(final String value) {
            return String.join("|", resultFields).contains(value);
        }
    }

    private interface Fixture {
        Game game();
        Player chooser();
        Card source();
        Trigger trigger();
        SpellAbility ability();
        WrappedAbility wrapper();
    }

    private record BloodFixture(Game game, Player chooser, Player opponent, Card source,
            Trigger trigger, SpellAbility ability, Card firstTarget, List<Integer> targetIds,
            WrappedAbility wrapper) implements Fixture {
    }

    private record TriggeredFixture(Game game, Player chooser, Card source, Trigger trigger,
            SpellAbility ability, WrappedAbility wrapper) implements Fixture {
    }
}
