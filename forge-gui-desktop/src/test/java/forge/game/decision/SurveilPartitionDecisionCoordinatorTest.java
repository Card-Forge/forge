package forge.game.decision;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardFactory;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.DeterminismAuditRandom;
import forge.util.MyRandom;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.testng.annotations.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

public class SurveilPartitionDecisionCoordinatorTest extends AITest {
    @Test
    public void coordinatorDoesNotConstructProviderImplicitly() {
        expectThrows(NoSuchMethodException.class,
                () -> SurveilPartitionDecisionCoordinator.class.getConstructor());
    }

    @Test
    public void validNativeCaptureCallsArrangeForSurveilExactlyOnce() {
        final Player chooser = initAndCreateGame().getPlayers().get(1);
        final CardCollection topN = new CardCollection();
        topN.add(addCardToZone("Island", chooser, forge.game.zone.ZoneType.Hand));
        final Pair<CardCollection, CardCollection> nativePair =
                new ImmutablePair<>(new CardCollection(), new CardCollection(topN));
        final AtomicInteger calls = new AtomicInteger();
        final SurveilPartitionDecisionCoordinator coordinator = coordinator();

        final Pair<CardCollection, CardCollection> result = coordinator.captureNativeSurveil(
                chooser, topN, cards -> {
                    calls.incrementAndGet();
                    return nativePair;
                });

        assertEquals(calls.get(), 1);
        assertSame(result, nativePair);
    }

    @Test
    public void nullTopNStillCallsNativeExactlyOnce() {
        final SurveilPartitionDecisionCoordinator coordinator = coordinator();
        final AtomicInteger calls = new AtomicInteger();
        final AtomicReference<CardCollection> argument = new AtomicReference<>();
        final Pair<CardCollection, CardCollection> nativePair = new ImmutablePair<>(null, null);

        final Pair<CardCollection, CardCollection> result = coordinator.captureNativeSurveil(
                null, null, cards -> {
                    calls.incrementAndGet();
                    argument.set(cards);
                    return nativePair;
                });

        assertEquals(calls.get(), 1);
        assertSame(argument.get(), null);
        assertSame(result, nativePair);
    }

    @Test
    public void nullTopNFallsBackBeforeAdmissionAndPreservesNativeException() {
        final SurveilPartitionDecisionCoordinator coordinator = coordinator();
        final RuntimeException failure = new RuntimeException("native-null-topN");
        final AtomicInteger calls = new AtomicInteger();

        final RuntimeException actual = expectThrows(RuntimeException.class,
                () -> coordinator.captureNativeSurveil(null, null, ignored -> {
                    calls.incrementAndGet();
                    throw failure;
                }));

        assertSame(actual, failure);
        assertEquals(calls.get(), 1);
        assertEquals(coordinator.activeSessionCount(), 0);
    }

    @Test
    public void nativeCallbackReceivesOriginalMutableTopNInstance() {
        final Player chooser = initAndCreateGame().getPlayers().get(1);
        final CardCollection originalTopN = new CardCollection();
        originalTopN.add(addCardToZone("Island", chooser, ZoneType.Hand));
        final AtomicReference<CardCollection> callbackArgument = new AtomicReference<>();
        final SurveilPartitionDecisionCoordinator coordinator = coordinator();

        coordinator.captureNativeSurveil(chooser, originalTopN, argument -> {
            callbackArgument.set(argument);
            argument.clear();
            return new ImmutablePair<>(new CardCollection(), new CardCollection());
        });

        assertSame(callbackArgument.get(), originalTopN);
        assertTrue(originalTopN.isEmpty());
    }

    @Test
    public void nullCardEntryIsClassifiedAsCaptureAdmissionFailure() {
        final Player chooser = initAndCreateGame().getPlayers().get(1);
        final CardCollection topN = new CardCollection();
        topN.add(null);
        final Pair<CardCollection, CardCollection> nativePair = new ImmutablePair<>(null, null);
        final AtomicInteger calls = new AtomicInteger();
        final SurveilPartitionDecisionCoordinator coordinator = coordinator();

        final Pair<CardCollection, CardCollection> result = coordinator.captureNativeSurveil(chooser, topN, argument -> {
            calls.incrementAndGet();
            return nativePair;
        });

        assertEquals(calls.get(), 1);
        assertSame(result, nativePair);
    }

    @Test
    public void captureAdmissionFailureCallsNativeExactlyOnceAndCreatesNoL2ARequest() {
        final Player chooser = initAndCreateGame().getPlayers().get(1);
        final CardCollection topN = new CardCollection();
        topN.add(null);
        final AtomicInteger calls = new AtomicInteger();
        final SurveilPartitionDecisionCoordinator coordinator = coordinator();

        coordinator.captureNativeSurveil(chooser, topN, argument -> {
            calls.incrementAndGet();
            return new ImmutablePair<>(null, null);
        });

        assertEquals(calls.get(), 1);
        assertEquals(coordinator.activeSessionCount(), 0);
    }

    @Test
    public void nativePairMappingFailureReturnsOriginalPairAndCreatesNoMembershipRows() {
        final Player chooser = initAndCreateGame().getPlayers().get(1);
        final Card card = addCardToZone("Island", chooser, ZoneType.Hand);
        final CardCollection topN = new CardCollection(card);
        final CardCollection foreign = new CardCollection(addCardToZone("Forest", chooser, ZoneType.Hand));
        final Pair<CardCollection, CardCollection> nativePair = new ImmutablePair<>(null, foreign);
        final SurveilPartitionDecisionCoordinator coordinator = coordinator();

        assertNoSurveilTraceRows(chooser.getGame(), () -> {
            final Pair<CardCollection, CardCollection> result = coordinator.captureNativeSurveil(
                    chooser, topN, ignored -> nativePair);
            assertSame(result, nativePair);
        });
        assertEquals(coordinator.activeSessionCount(), 0);
    }

    @Test
    public void nativeCallbackFailureRethrowsWithoutSecondInvocationOrTraceRows() {
        final Player chooser = initAndCreateGame().getPlayers().get(1);
        final CardCollection topN = new CardCollection();
        topN.add(addCardToZone("Island", chooser, ZoneType.Hand));
        final RuntimeException failure = new RuntimeException("native");
        final AtomicInteger calls = new AtomicInteger();
        final SurveilPartitionDecisionCoordinator coordinator = coordinator();

        final RuntimeException actual = expectThrows(RuntimeException.class,
                () -> assertNoSurveilTraceRows(chooser.getGame(), () -> coordinator.captureNativeSurveil(
                        chooser, topN, ignored -> {
                    calls.incrementAndGet();
                    throw failure;
                })));

        assertSame(actual, failure);
        assertEquals(calls.get(), 1);
        assertEquals(coordinator.activeSessionCount(), 0);
    }

    @Test
    public void terminalCapturePathsRemoveRegisteredSessions() {
        final Player chooser = initAndCreateGame().getPlayers().get(1);
        final Card card = addCardToZone("Island", chooser, ZoneType.Hand);
        final CardCollection topN = new CardCollection(card);
        final SurveilPartitionDecisionCoordinator coordinator = coordinator();

        coordinator.captureNativeSurveil(chooser, topN, ignored ->
                new ImmutablePair<>(new CardCollection(), new CardCollection(card)));
        assertEquals(coordinator.activeSessionCount(), 0);
    }

    @Test
    public void nullEmptyHumanSideIsNormalizedForValidationButOriginalPairIsReturned() {
        final Player chooser = initAndCreateGame().getPlayers().get(1);
        final Card card = addCardToZone("Island", chooser, ZoneType.Hand);
        final CardCollection topN = new CardCollection(card);
        final SurveilPartitionDecisionCoordinator coordinator = coordinator();

        for (final CardCollection retained : new CardCollection[] {null, new CardCollection()}) {
            final Pair<CardCollection, CardCollection> nativePair = new ImmutablePair<>(retained,
                    new CardCollection(card));
            assertSame(coordinator.captureNativeSurveil(chooser, topN, ignored -> nativePair), nativePair);
        }
    }

    @Test
    public void retainedOrderIsIgnoredForMembershipButReturnedUnchanged() {
        final Player chooser = initAndCreateGame().getPlayers().get(1);
        final Card first = addCardToZone("Island", chooser, ZoneType.Hand);
        final Card second = addCardToZone("Forest", chooser, ZoneType.Hand);
        final CardCollection topN = new CardCollection(List.of(first, second));
        final CardCollection retained = new CardCollection(List.of(second, first));
        final Pair<CardCollection, CardCollection> nativePair = new ImmutablePair<>(retained, null);
        final SurveilPartitionDecisionProvider provider = new SurveilPartitionDecisionProvider();
        final SurveilPartitionDecisionCoordinator coordinator =
                new SurveilPartitionDecisionCoordinator(provider);
        final AtomicReference<SurveilPartitionSession> session = new AtomicReference<>();

        final Pair<CardCollection, CardCollection> result = coordinator.captureNativeSurveil(
                chooser, topN, ignored -> {
                    session.set(soleRegisteredSession(provider));
                    return nativePair;
                });

        assertSame(result, nativePair);
        assertEquals(result.getLeft(), retained);
        assertEquals(readField(session.get(), "retainedNativeList"), List.of(second, first));
    }

    @Test
    public void callbackCardMutationDoesNotChangeCapturedCanonicalMembershipOrder() {
        final Player chooser = initAndCreateGame().getPlayers().get(1);
        final Card retainedCard = addCardToZone("Island", chooser, ZoneType.Hand);
        final Card graveyardCard = addCardToZone("Forest", chooser, ZoneType.Hand);
        final CardCollection topN = new CardCollection(List.of(retainedCard, graveyardCard));
        final Pair<CardCollection, CardCollection> nativePair = new ImmutablePair<>(
                new CardCollection(retainedCard), new CardCollection(graveyardCard));
        final SurveilPartitionDecisionCoordinator coordinator = coordinator();

        final List<String> rows = captureTraceRows(chooser.getGame(), () -> {
            final Pair<CardCollection, CardCollection> result = coordinator.captureNativeSurveil(
                    chooser, topN, ignored -> {
                        graveyardCard.setName("ZZZ");
                        return nativePair;
                    });
            assertSame(result, nativePair);
        });

        final List<String> resultRows = rows.stream()
                .filter(row -> row.contains("|RESULT|"))
                .toList();
        assertTrue(resultRows.get(0).contains("CLASSIFY_GRAVEYARD"));
        assertTrue(resultRows.get(1).contains("CLASSIFY_RETAIN"));
    }

    @Test
    public void diagnosticsReasonNamespaceRejectsCardAndNativeData() throws Exception {
        final Method sanitize = SurveilPartitionDiagnostics.class.getDeclaredMethod("sanitize", String.class);
        sanitize.setAccessible(true);
        assertEquals(sanitize.invoke(null, "native-card=Island|id=42"), "UNKNOWN");
        assertEquals(sanitize.invoke(null, "IDENTITY"), "IDENTITY");

        final Method approvedReasonCounter = SurveilPartitionDiagnostics.class
                .getDeclaredMethod("isApprovedReasonCounter", String.class);
        approvedReasonCounter.setAccessible(true);
        assertTrue((Boolean) approvedReasonCounter.invoke(null, "mapping_failure_IDENTITY"));
        assertFalse((Boolean) approvedReasonCounter.invoke(null, "mapping_failure_native-card_Island"));
    }

    @Test
    public void coordinatorAdmissionMatrixN0ThroughN4PreservesNativeBoundary() {
        for (int itemCount = 0; itemCount <= 4; itemCount++) {
            final Fixture fixture = fixture(itemCount);
            final CardCollection topN = new CardCollection(fixture.cards());
            final CardCollection retained = new CardCollection();
            final CardCollection graveyard = new CardCollection();
            if (!fixture.cards().isEmpty()) {
                graveyard.add(fixture.cards().get(0));
                retained.addAll(fixture.cards().subList(1, fixture.cards().size()));
            }
            final Pair<CardCollection, CardCollection> nativePair =
                    new ImmutablePair<>(retained, graveyard);
            final SurveilPartitionDecisionProvider provider = new SurveilPartitionDecisionProvider();
            final SurveilPartitionDecisionCoordinator coordinator =
                    new SurveilPartitionDecisionCoordinator(provider);
            final AtomicInteger calls = new AtomicInteger();

            final Pair<CardCollection, CardCollection> result = coordinator.captureNativeSurveil(
                    fixture.chooser(), topN, ignored -> {
                        calls.incrementAndGet();
                        return nativePair;
                    });

            assertSame(result, nativePair, "native Pair must remain the engine-owned object for N=" + itemCount);
            assertEquals(calls.get(), 1, "native callback count for N=" + itemCount);
            assertEquals(provider.activeSessionCount(), 0, "registry after N=" + itemCount);
        }
    }

    @Test
    public void admissionFailuresPreserveNativeOutcomeAndEmitNoRows() {
        final Fixture duplicateObjectFixture = fixture(1);
        final Card sameCard = duplicateObjectFixture.cards().get(0);
        assertAdmissionFallback("same native object twice", duplicateObjectFixture.chooser(),
                new CardCollection(Arrays.asList(sameCard, sameCard)),
                new ImmutablePair<>(new CardCollection(), new CardCollection()));

        final Fixture duplicateTupleFixture = customFixture(new CardSpec("Island", 9501, 750001L));
        final Card duplicateTuple = copyWithStableIdentity(duplicateTupleFixture.chooser(),
                duplicateTupleFixture.cards().get(0));
        assertAdmissionFallback("duplicate private stable tuple", duplicateTupleFixture.chooser(),
                new CardCollection(Arrays.asList(duplicateTupleFixture.cards().get(0), duplicateTuple)),
                new ImmutablePair<>(new CardCollection(), new CardCollection()));

        final Fixture visibilityFixture = fixture(1);
        final Card hidden = visibilityFixture.cards().get(0);
        hidden.setFaceDown(true);
        assertAdmissionFallback("chooser visibility failure", visibilityFixture.chooser(),
                new CardCollection(hidden), new ImmutablePair<>(null, null));

        final Fixture nullCardFixture = fixture(1);
        final CardCollection nullCardTopN = new CardCollection();
        nullCardTopN.add(null);
        assertAdmissionFallback("null card", nullCardFixture.chooser(), nullCardTopN,
                new ImmutablePair<>(new CardCollection(), new CardCollection()));
    }

    @Test
    public void admissionFailurePreservesNativeExceptionWithoutRegisteringOrTracing() {
        final Fixture fixture = fixture(1);
        final Card card = fixture.cards().get(0);
        final CardCollection duplicateTopN = new CardCollection(Arrays.asList(card, card));
        final SurveilPartitionDecisionProvider provider = new SurveilPartitionDecisionProvider();
        final SurveilPartitionDecisionCoordinator coordinator =
                new SurveilPartitionDecisionCoordinator(provider);
        final RuntimeException nativeFailure = new RuntimeException("native-admission-fallback");
        final AtomicInteger calls = new AtomicInteger();

        final RuntimeException actual = expectThrows(RuntimeException.class,
                () -> assertNoSurveilTraceRows(fixture.game(), () -> coordinator.captureNativeSurveil(
                        fixture.chooser(), duplicateTopN, ignored -> {
                            calls.incrementAndGet();
                            throw nativeFailure;
                        })));

        assertSame(actual, nativeFailure);
        assertEquals(calls.get(), 1);
        assertEquals(provider.activeSessionCount(), 0);
    }

    @Test
    public void nullTopNCaptureProducesNoTeacherOrBcRowsAndPreservesNativeException() {
        final Fixture fixture = fixture(0);
        final SurveilPartitionDecisionProvider provider = new SurveilPartitionDecisionProvider();
        final SurveilPartitionDecisionCoordinator coordinator =
                new SurveilPartitionDecisionCoordinator(provider);
        final RuntimeException nativeFailure = new RuntimeException("native-null-topN");
        final AtomicInteger calls = new AtomicInteger();

        final RuntimeException actual = expectThrows(RuntimeException.class,
                () -> assertNoSurveilTraceRows(fixture.game(), () -> coordinator.captureNativeSurveil(
                        fixture.chooser(), null, ignored -> {
                            calls.incrementAndGet();
                            throw nativeFailure;
                        })));

        assertSame(actual, nativeFailure);
        assertEquals(calls.get(), 1);
        assertEquals(provider.activeSessionCount(), 0);
    }

    @Test
    public void privateSnapshotRemainsAdmissionAuthorityWhenNativeCollectionDrifts() throws Exception {
        final Fixture fixture = fixture(2);
        final CardCollection originalTopN = new CardCollection(fixture.cards());
        final Pair<CardCollection, CardCollection> nativePair = new ImmutablePair<>(
                new CardCollection(fixture.cards().get(1)), new CardCollection(fixture.cards().get(0)));
        final SurveilPartitionDecisionProvider provider = new SurveilPartitionDecisionProvider();
        final SurveilPartitionDecisionCoordinator coordinator =
                new SurveilPartitionDecisionCoordinator(provider);

        final List<String> rows = captureTraceRows(fixture.game(), () -> {
            final Pair<CardCollection, CardCollection> result = coordinator.captureNativeSurveil(
                    fixture.chooser(), originalTopN, argument -> {
                        assertSame(argument, originalTopN);
                        originalTopN.clear();
                        return nativePair;
                    });
            assertSame(result, nativePair);
        });

        assertEquals(rows.stream().filter(row -> row.contains("|REQUEST|")).count(), 2L);
        assertTrue(originalTopN.isEmpty());
        assertEquals(provider.activeSessionCount(), 0);
    }

    @Test
    public void gameAndPlayerSessionDriftFailsClosedWithoutNativeRetry() {
        final Fixture fixture = fixture(1);
        final Fixture otherFixture = fixture(1);
        final Card card = fixture.cards().get(0);
        final Pair<CardCollection, CardCollection> nativePair =
                new ImmutablePair<>(new CardCollection(), new CardCollection(card));

        for (final String driftField : List.of("game", "chooser")) {
            final SurveilPartitionDecisionProvider provider = new SurveilPartitionDecisionProvider();
            final SurveilPartitionDecisionCoordinator coordinator =
                    new SurveilPartitionDecisionCoordinator(provider);
            final AtomicInteger calls = new AtomicInteger();
            final RuntimeException actual = expectThrows(RuntimeException.class,
                    () -> assertNoSurveilTraceRows(fixture.game(), () -> coordinator.captureNativeSurveil(
                            fixture.chooser(), new CardCollection(card), ignored -> {
                                calls.incrementAndGet();
                                final SurveilPartitionSession session = soleRegisteredSession(provider);
                                replaceField(session, driftField,
                                        "game".equals(driftField) ? otherFixture.game() : otherFixture.chooser());
                                return nativePair;
                            })));

            assertTrue(actual instanceof IllegalStateException);
            assertEquals(calls.get(), 1);
            assertEquals(provider.activeSessionCount(), 0);
        }
    }

    @Test
    public void completeNativePairMatrixPreservesIdentityAndUsesGraveyardOnlyForLabels() {
        final Fixture fixture = fixture("Island", "Forest", "Mountain");
        final CardCollection topN = new CardCollection(fixture.cards());
        final CardCollection retained = new CardCollection(Arrays.asList(
                fixture.cards().get(2), fixture.cards().get(0)));
        final CardCollection graveyard = new CardCollection(fixture.cards().get(1));
        final Pair<CardCollection, CardCollection> nativePair = new ImmutablePair<>(retained, graveyard);
        final SurveilPartitionDecisionCoordinator coordinator = coordinator();

        final List<String> rows = captureTraceRows(fixture.game(), () -> {
            final Pair<CardCollection, CardCollection> result = coordinator.captureNativeSurveil(
                    fixture.chooser(), topN, ignored -> nativePair);
            assertSame(result, nativePair);
            assertSame(result.getLeft(), retained);
            assertSame(result.getRight(), graveyard);
        });

        final List<String> resultRows = rows.stream()
                .filter(row -> row.contains("|RESULT|"))
                .toList();
        assertEquals(resultRows.size(), 3);
        assertTrue(resultRows.get(0).contains("CLASSIFY_GRAVEYARD"));
        assertTrue(resultRows.get(1).contains("CLASSIFY_RETAIN"));
        assertTrue(resultRows.get(2).contains("CLASSIFY_RETAIN"));
    }

    @Test
    public void malformedNativePairMatrixReturnsOriginalPairWithoutMembershipRows() {
        assertMalformedPair("null pair", ignored -> null);
        assertMalformedPair("null entry", fixture -> {
            final CardCollection graveyard = new CardCollection();
            graveyard.add(null);
            return new ImmutablePair<>(new CardCollection(), graveyard);
        });
        assertMalformedPair("foreign card", fixture -> new ImmutablePair<>(new CardCollection(),
                new CardCollection(addCardToZone("Mountain", fixture.chooser(), ZoneType.Hand))));
        assertMalformedPair("omitted card", fixture -> new ImmutablePair<>(new CardCollection(),
                new CardCollection(fixture.cards().get(0))));
        assertMalformedPair("duplicate result", fixture -> new ImmutablePair<>(new CardCollection(),
                new CardCollection(Arrays.asList(fixture.cards().get(0), fixture.cards().get(0)))));
        assertMalformedPair("same card in both sides", fixture -> new ImmutablePair<>(
                new CardCollection(fixture.cards().get(0)), new CardCollection(fixture.cards().get(0))));
        assertMalformedPair("wrong cardinality", 3, fixture -> new ImmutablePair<>(
                new CardCollection(fixture.cards().get(2)),
                new CardCollection(fixture.cards().get(0))));
        assertMalformedPair("stale replaced result", fixture -> {
            final Card replaced = copyWithStableIdentity(fixture.chooser(), fixture.cards().get(0));
            return new ImmutablePair<>(new CardCollection(fixture.cards().get(1)),
                    new CardCollection(replaced));
        });
    }

    @Test
    public void typedFutureOwnershipAcceptsOnlyNativeGraveyardOrRetainCandidates() {
        assertTypedNativeCandidate(SurveilPartitionCandidateKind.CLASSIFY_GRAVEYARD);
        assertTypedNativeCandidate(SurveilPartitionCandidateKind.CLASSIFY_RETAIN);
    }

    @Test
    public void typedFutureOwnershipRejectsStaleForeignWrongContextAndPriorRequestCandidates() {
        final Fixture fixture = fixture(2);
        final SurveilPartitionDecisionProvider provider = new SurveilPartitionDecisionProvider();
        final SurveilPartitionSession target = provider.admit(fixture.chooser(), fixture.cards());
        target.recordNativeMembershipVector(List.of(
                SurveilPartitionCandidateKind.CLASSIFY_RETAIN,
                SurveilPartitionCandidateKind.CLASSIFY_RETAIN));
        final DecisionRequest firstRequest = provider.createMembershipRequest(target);
        final LegalCandidate firstCandidate = firstRequest.getCandidates().get(0);
        provider.applyMembershipCandidate(target, firstCandidate);
        final DecisionRequest secondRequest = provider.createMembershipRequest(target);
        assertTrue(secondRequest.getRequestId() > firstRequest.getRequestId());
        expectThrows(IllegalArgumentException.class,
                () -> provider.applyMembershipCandidate(target, firstCandidate));

        final Fixture foreignFixture = fixture(1);
        final SurveilPartitionSession foreign = provider.admit(foreignFixture.chooser(), foreignFixture.cards());
        foreign.recordNativeMembershipVector(List.of(SurveilPartitionCandidateKind.CLASSIFY_RETAIN));
        final LegalCandidate foreignCandidate = provider.createMembershipRequest(foreign).getCandidates().get(0);
        expectThrows(IllegalArgumentException.class,
                () -> provider.applyMembershipCandidate(target, foreignCandidate));

        replaceField(secondRequest.getSurveilPartitionContext(), "profile", null);
        expectThrows(IllegalArgumentException.class,
                () -> provider.applyMembershipCandidate(target, secondRequest.getCandidates().get(0)));
        replaceField(secondRequest.getSurveilPartitionContext(), "profile", SurveilPartitionProfile.SURVEIL_PARTITION);
        replaceField(secondRequest.getSurveilPartitionContext(), "decisionStepIndex", 0);
        expectThrows(IllegalArgumentException.class,
                () -> provider.applyMembershipCandidate(target, secondRequest.getCandidates().get(0)));
        expectThrows(IllegalArgumentException.class, () -> provider.applyMembershipCandidate(target, null));

        provider.closeSession(target);
        provider.closeSession(foreign);
        expectThrows(IllegalStateException.class, () -> provider.createMembershipRequest(target));
        assertEquals(provider.activeSessionCount(), 0);
    }

    @Test
    public void externalOwnershipIsRejectedBeforeAnySecondNativeCallback() {
        assertTrue(Arrays.stream(SurveilPartitionDecisionCoordinator.class.getDeclaredMethods())
                .noneMatch(method -> method.getName().toLowerCase().contains("resolver")));
        assertTrue(Arrays.stream(SurveilPartitionDecisionProvider.class.getDeclaredMethods())
                .noneMatch(method -> method.getName().toLowerCase().contains("resolver")));
        final Fixture fixture = fixture(1);
        final Card card = fixture.cards().get(0);
        final Pair<CardCollection, CardCollection> nativePair =
                new ImmutablePair<>(new CardCollection(), new CardCollection(card));
        final AtomicInteger calls = new AtomicInteger();
        final Pair<CardCollection, CardCollection> result = coordinator().captureNativeSurveil(
                fixture.chooser(), new CardCollection(card), ignored -> {
                    calls.incrementAndGet();
                    return nativePair;
                });

        assertSame(result, nativePair);
        assertEquals(calls.get(), 1);
    }

    @Test
    public void nullExternalResultFailsClosedWithoutSecondCallbackOrTraceRows() {
        final Fixture fixture = fixture(1);
        final SurveilPartitionDecisionProvider provider = new SurveilPartitionDecisionProvider();
        final SurveilPartitionDecisionCoordinator coordinator =
                new SurveilPartitionDecisionCoordinator(provider);
        final AtomicInteger calls = new AtomicInteger();

        assertNoSurveilTraceRows(fixture.game(), () -> {
            final Pair<CardCollection, CardCollection> result = coordinator.captureNativeSurveil(
                    fixture.chooser(), new CardCollection(fixture.cards()), ignored -> {
                        calls.incrementAndGet();
                        return null;
                    });
            assertNull(result);
        });

        assertEquals(calls.get(), 1);
        assertEquals(provider.activeSessionCount(), 0);
    }

    @Test
    public void externalResolverExceptionIsPreservedWithoutExternalAdmissionOrSecondCallback() {
        assertTrue(Arrays.stream(SurveilPartitionDecisionCoordinator.class.getDeclaredMethods())
                .noneMatch(method -> method.getName().toLowerCase().contains("resolver")));
        assertTrue(Arrays.stream(SurveilPartitionDecisionProvider.class.getDeclaredMethods())
                .noneMatch(method -> method.getName().toLowerCase().contains("resolver")));
        final Fixture fixture = fixture(1);
        final SurveilPartitionDecisionProvider provider = new SurveilPartitionDecisionProvider();
        final SurveilPartitionDecisionCoordinator coordinator =
                new SurveilPartitionDecisionCoordinator(provider);
        final RuntimeException externalResolverFailure =
                new RuntimeException("external-resolver-failure");
        final AtomicInteger calls = new AtomicInteger();

        final RuntimeException actual = expectThrows(RuntimeException.class,
                () -> assertNoSurveilTraceRows(fixture.game(), () -> coordinator.captureNativeSurveil(
                        fixture.chooser(), new CardCollection(fixture.cards()), ignored -> {
                            calls.incrementAndGet();
                            throw externalResolverFailure;
                        })));

        assertSame(actual, externalResolverFailure);
        assertEquals(calls.get(), 1);
        assertEquals(provider.activeSessionCount(), 0);
    }

    private void assertAdmissionFallback(final String label, final Player chooser,
            final CardCollection topN, final Pair<CardCollection, CardCollection> nativePair) {
        final SurveilPartitionDecisionProvider provider = new SurveilPartitionDecisionProvider();
        final SurveilPartitionDecisionCoordinator coordinator =
                new SurveilPartitionDecisionCoordinator(provider);
        final AtomicInteger calls = new AtomicInteger();
        assertNoSurveilTraceRows(chooser.getGame(), () -> {
            final Pair<CardCollection, CardCollection> result = coordinator.captureNativeSurveil(
                    chooser, topN, argument -> {
                        calls.incrementAndGet();
                        assertSame(argument, topN, label);
                        return nativePair;
                    });
            assertSame(result, nativePair, label);
        });
        assertEquals(calls.get(), 1, label);
        assertEquals(provider.activeSessionCount(), 0, label);
    }

    private void assertMalformedPair(final String label,
            final Function<Fixture, Pair<CardCollection, CardCollection>> nativePairFactory) {
        assertMalformedPair(label, 2, nativePairFactory);
    }

    private void assertMalformedPair(final String label, final int itemCount,
            final Function<Fixture, Pair<CardCollection, CardCollection>> nativePairFactory) {
        final Fixture fixture = fixture(itemCount);
        final CardCollection topN = new CardCollection(fixture.cards());
        final SurveilPartitionDecisionProvider provider = new SurveilPartitionDecisionProvider();
        final SurveilPartitionDecisionCoordinator coordinator =
                new SurveilPartitionDecisionCoordinator(provider);
        final AtomicInteger calls = new AtomicInteger();
        final Pair<CardCollection, CardCollection> nativePair = nativePairFactory.apply(fixture);

        assertNoSurveilTraceRows(fixture.game(), () -> {
            final Pair<CardCollection, CardCollection> result = coordinator.captureNativeSurveil(
                    fixture.chooser(), topN, ignored -> {
                        calls.incrementAndGet();
                        return nativePair;
                    });
            assertSame(result, nativePair, label);
        });
        assertEquals(calls.get(), 1, label);
        assertEquals(provider.activeSessionCount(), 0, label);
    }

    private void assertTypedNativeCandidate(final SurveilPartitionCandidateKind expectedKind) {
        final Fixture fixture = fixture(1);
        final SurveilPartitionDecisionProvider provider = new SurveilPartitionDecisionProvider();
        final SurveilPartitionSession session = provider.admit(fixture.chooser(), fixture.cards());
        session.recordNativeMembershipVector(List.of(expectedKind));
        final DecisionRequest request = provider.createMembershipRequest(session);
        final LegalCandidate candidate = request.getCandidates().stream()
                .filter(item -> item.getSurveilPartitionCandidateKind() == expectedKind)
                .findFirst().orElseThrow();

        assertEquals(request.getCandidates().size(), 2);
        assertEquals(candidate.getSurveilPartitionCandidateKind(), expectedKind);
        assertEquals(candidate.getSurveilPartitionCard().getItemId(),
                request.getSurveilPartitionContext().getCurrentItemId());
        provider.applyMembershipCandidate(session, candidate);
        assertTrue(provider.isComplete(session));
        provider.closeSession(session);
        assertEquals(provider.activeSessionCount(), 0);
    }

    private Fixture fixture(final int itemCount) {
        final String[] names = {"Island", "Forest", "Mountain", "Swamp"};
        final String[] selected = Arrays.copyOf(names, itemCount);
        return fixture(selected);
    }

    private Fixture fixture(final String... names) {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final List<Card> cards = new ArrayList<>();
        for (final String name : names) {
            cards.add(addCardToZone(name, chooser, ZoneType.Hand));
        }
        return new Fixture(game, chooser, cards);
    }

    private Fixture customFixture(final CardSpec... specs) {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final List<Card> cards = new ArrayList<>();
        for (final CardSpec spec : specs) {
            final Card template = createCard(spec.name(), chooser);
            final Card card = CardFactory.getCard(template.getPaperCard(), chooser,
                    spec.cardId(), chooser.getGame());
            card.setGameTimestamp(spec.gameTimestamp());
            chooser.getZone(ZoneType.Hand).add(card);
            cards.add(card);
        }
        return new Fixture(game, chooser, cards);
    }

    private Card copyWithStableIdentity(final Player chooser, final Card source) {
        final Card copy = CardFactory.getCard(source.getPaperCard(), chooser, source.getId(), chooser.getGame());
        copy.setGameTimestamp(source.getGameTimestamp());
        chooser.getZone(ZoneType.Hand).add(copy);
        return copy;
    }

    private static SurveilPartitionSession soleRegisteredSession(
            final SurveilPartitionDecisionProvider provider) {
        try {
            final Field field = SurveilPartitionDecisionProvider.class.getDeclaredField("activeSessions");
            field.setAccessible(true);
            final Map<?, ?> sessions = (Map<?, ?>) field.get(provider);
            assertEquals(sessions.size(), 1);
            return (SurveilPartitionSession) sessions.values().iterator().next();
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private static void replaceField(final Object target, final String fieldName, final Object value) {
        try {
            final Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private static Object readField(final Object target, final String fieldName) {
        try {
            final Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private static List<String> captureTraceRows(final Game game, final Runnable action) {
        final Random previousRandom = MyRandom.getRandom();
        final DeterminismAuditRandom auditRandom = new DeterminismAuditRandom(20260814L);
        MyRandom.setRandom(auditRandom);
        Path directory = null;
        DeterminismTrace trace = null;
        try {
            directory = Files.createTempDirectory("frl02l2a-success-trace-");
            trace = DeterminismTrace.attach(game, 0, auditRandom, directory);
            action.run();
            trace.finish();
            final Path decisionTrace = directory.resolve("game-001.decision.trace");
            return Files.exists(decisionTrace)
                    ? Files.readAllLines(decisionTrace, StandardCharsets.UTF_8) : List.of();
        } catch (IOException exception) {
            throw new AssertionError(exception);
        } finally {
            try {
                if (trace != null) {
                    trace.finish();
                }
                if (directory != null) {
                    try (var paths = Files.walk(directory)) {
                        for (final Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                            Files.deleteIfExists(path);
                        }
                    }
                }
            } catch (Exception exception) {
                throw new AssertionError(exception);
            } finally {
                MyRandom.setRandom(previousRandom);
            }
        }
    }

    private static SurveilPartitionDecisionCoordinator coordinator() {
        return new SurveilPartitionDecisionCoordinator(new SurveilPartitionDecisionProvider());
    }

    private static void assertNoSurveilTraceRows(final Game game, final Runnable action) {
        final Random previousRandom = MyRandom.getRandom();
        final DeterminismAuditRandom auditRandom = new DeterminismAuditRandom(20260814L);
        MyRandom.setRandom(auditRandom);
        Path directory = null;
        DeterminismTrace trace = null;
        RuntimeException actionFailure = null;
        try {
            directory = Files.createTempDirectory("frl02l2a-failure-trace-");
            trace = DeterminismTrace.attach(game, 0, auditRandom, directory);
            try {
                action.run();
            } catch (final RuntimeException failure) {
                actionFailure = failure;
            }
            trace.finish();
            final Path decisionTrace = directory.resolve("game-001.decision.trace");
            final List<String> rows = Files.exists(decisionTrace)
                    ? Files.readAllLines(decisionTrace, StandardCharsets.UTF_8) : List.of();
            assertTrue(rows.stream().noneMatch(row -> row.contains("SURVEIL_PARTITION")), rows.toString());
            assertFalse(rows.stream().anyMatch(row -> row.contains("REQUEST") || row.contains("RESULT")),
                    rows.toString());
            if (actionFailure != null) {
                throw actionFailure;
            }
        } catch (final IOException ex) {
            throw new AssertionError(ex);
        } finally {
            try {
                if (trace != null) {
                    trace.finish();
                }
                if (directory != null) {
                    try (var paths = Files.walk(directory)) {
                        for (final Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                            Files.deleteIfExists(path);
                        }
                    }
                }
            } catch (final Exception ex) {
                throw new AssertionError(ex);
            } finally {
                MyRandom.setRandom(previousRandom);
            }
        }
    }

    private record CardSpec(String name, int cardId, long gameTimestamp) {
    }

    private record Fixture(Game game, Player chooser, List<Card> cards) {
    }
}
