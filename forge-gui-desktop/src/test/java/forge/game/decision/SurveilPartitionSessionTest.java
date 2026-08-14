package forge.game.decision;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardFactory;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

public class SurveilPartitionSessionTest extends AITest {
    @Test
    public void nativeSnapshotPermutationDoesNotChangeCanonicalPublicOrderOrItemIds() {
        final Fixture fixture = fixture("Island", "Forest");
        final SurveilPartitionDecisionProvider provider = new SurveilPartitionDecisionProvider();

        final SurveilPartitionSession first = provider.admit(fixture.chooser(), fixture.cards());
        final SurveilPartitionSession reversed = provider.admit(fixture.chooser(),
                List.of(fixture.cards().get(1), fixture.cards().get(0)));
        recordAllRetain(first, fixture.cards().size());
        recordAllRetain(reversed, fixture.cards().size());

        assertEquals(publicPairs(provider.createMembershipRequest(first)),
                publicPairs(provider.createMembershipRequest(reversed)));

        provider.closeSession(first);
        provider.closeSession(reversed);
    }

    @Test
    public void privateTieBreakStabilizesExactPublicTiesWithoutExposingTieData() {
        final Fixture fixture = customFixture(
                new CardSpec("Island", 9002, 700002L),
                new CardSpec("Island", 9001, 700001L));
        final Card higherTupleFirst = fixture.cards().get(0);
        final Card lowerTupleFirst = fixture.cards().get(1);
        final SurveilPartitionDecisionProvider provider = new SurveilPartitionDecisionProvider();

        final SurveilPartitionSession first = provider.admit(fixture.chooser(),
                List.of(higherTupleFirst, lowerTupleFirst));
        final SurveilPartitionSession reversed = provider.admit(fixture.chooser(),
                List.of(lowerTupleFirst, higherTupleFirst));
        recordAllRetain(first, fixture.cards().size());
        recordAllRetain(reversed, fixture.cards().size());
        final DecisionRequest firstRequest = provider.createMembershipRequest(first);
        final DecisionRequest reversedRequest = provider.createMembershipRequest(reversed);

        assertEquals(publicPairs(firstRequest), publicPairs(reversedRequest));
        assertEquals(itemIdForNative(first, lowerTupleFirst), SurveilPartitionItemId.opaqueItemId(1));
        assertEquals(itemIdForNative(first, higherTupleFirst), SurveilPartitionItemId.opaqueItemId(2));
        assertEquals(itemIdForNative(reversed, lowerTupleFirst), SurveilPartitionItemId.opaqueItemId(1));
        assertEquals(itemIdForNative(reversed, higherTupleFirst), SurveilPartitionItemId.opaqueItemId(2));

        for (final DecisionRequest request : List.of(firstRequest, reversedRequest)) {
            for (final SurveilPartitionCard item : request.getSurveilPartitionContext().getVisibleItems()) {
                assertFalse(item.getVisibleName().contains("9001"));
                assertFalse(item.getVisibleName().contains("700001"));
                assertFalse(item.getVisibleName().contains("9002"));
                assertFalse(item.getVisibleName().contains("700002"));
            }
            for (final LegalCandidate candidate : request.getCandidates()) {
                assertFalse(candidate.getSemanticKey().contains("9001"));
                assertFalse(candidate.getSemanticKey().contains("700001"));
                assertFalse(candidate.getSemanticKey().contains("9002"));
                assertFalse(candidate.getSemanticKey().contains("700002"));
            }
        }

        provider.closeSession(first);
        provider.closeSession(reversed);
    }

    @Test
    public void itemIdsAreStableAcrossFreshEquivalentSessions() {
        final Fixture firstFixture = customFixture(
                new CardSpec("Island", 9102, 710002L),
                new CardSpec("Forest", 9101, 710001L));
        final Fixture secondFixture = customFixture(
                new CardSpec("Island", 9102, 710002L),
                new CardSpec("Forest", 9101, 710001L));
        final SurveilPartitionDecisionProvider provider = new SurveilPartitionDecisionProvider();

        final SurveilPartitionSession first = provider.admit(firstFixture.chooser(), firstFixture.cards());
        final SurveilPartitionSession second = provider.admit(secondFixture.chooser(), secondFixture.cards());
        recordAllRetain(first, firstFixture.cards().size());
        recordAllRetain(second, secondFixture.cards().size());

        assertEquals(publicPairs(provider.createMembershipRequest(first)),
                publicPairs(provider.createMembershipRequest(second)));

        provider.closeSession(first);
        provider.closeSession(second);
    }

    @Test
    public void itemIdIsNotDerivedFromNativeSnapshotPosition() {
        final Fixture fixture = customFixture(
                new CardSpec("Island", 9202, 720002L),
                new CardSpec("Forest", 9201, 720001L));
        final SurveilPartitionDecisionProvider provider = new SurveilPartitionDecisionProvider();

        final SurveilPartitionSession nativeIslandFirst = provider.admit(fixture.chooser(), fixture.cards());
        final SurveilPartitionSession nativeForestFirst = provider.admit(fixture.chooser(),
                List.of(fixture.cards().get(1), fixture.cards().get(0)));
        recordAllRetain(nativeIslandFirst, fixture.cards().size());
        recordAllRetain(nativeForestFirst, fixture.cards().size());

        final List<String> expected = List.of(
                "Forest=" + SurveilPartitionItemId.opaqueItemId(1),
                "Island=" + SurveilPartitionItemId.opaqueItemId(2));
        assertEquals(publicNameAndIds(provider.createMembershipRequest(nativeIslandFirst)), expected);
        assertEquals(publicNameAndIds(provider.createMembershipRequest(nativeForestFirst)), expected);

        provider.closeSession(nativeIslandFirst);
        provider.closeSession(nativeForestFirst);
    }

    @Test
    public void closedSessionIsRemovedAndCannotBeReused() {
        final Fixture fixture = fixture("Island", "Forest");
        final SurveilPartitionDecisionProvider provider = new SurveilPartitionDecisionProvider();
        final SurveilPartitionSession session = provider.admit(fixture.chooser(), fixture.cards());
        recordAllRetain(session, fixture.cards().size());
        final DecisionRequest request = provider.createMembershipRequest(session);
        final LegalCandidate candidate = request.getCandidates().get(0);

        provider.closeSession(session);

        assertEquals(provider.activeSessionCount(), 0);
        expectThrows(IllegalStateException.class, () -> provider.createMembershipRequest(session));
        expectThrows(IllegalStateException.class, () -> provider.applyMembershipCandidate(session, candidate));
    }

    @Test
    public void newSessionAfterCloseUsesTheSameControllerProviderWithoutRegistryGrowth() {
        final Fixture fixture = fixture("Island");
        final SurveilPartitionDecisionProvider provider = new SurveilPartitionDecisionProvider();

        final SurveilPartitionSession first = provider.admit(fixture.chooser(), fixture.cards());
        recordAllRetain(first, fixture.cards().size());
        final long firstId = provider.createMembershipRequest(first)
                .getSurveilPartitionContext().getSurveilSessionId();
        provider.closeSession(first);

        final SurveilPartitionSession second = provider.admit(fixture.chooser(), fixture.cards());
        recordAllRetain(second, fixture.cards().size());
        final long secondId = provider.createMembershipRequest(second)
                .getSurveilPartitionContext().getSurveilSessionId();

        assertTrue(secondId > firstId);
        assertEquals(provider.activeSessionCount(), 1);
        provider.closeSession(second);
        assertEquals(provider.activeSessionCount(), 0);
    }

    @Test
    public void nativeMembershipVectorIsReadableBeforeInteractiveTraceApplication() {
        final Fixture fixture = fixture("Island", "Forest");
        final SurveilPartitionDecisionProvider provider = new SurveilPartitionDecisionProvider();
        final SurveilPartitionSession session = provider.admit(fixture.chooser(), fixture.cards());
        final List<SurveilPartitionCandidateKind> expected = List.of(
                SurveilPartitionCandidateKind.CLASSIFY_RETAIN,
                SurveilPartitionCandidateKind.CLASSIFY_GRAVEYARD);

        session.recordNativeMembershipVector(expected);

        assertEquals(session.nativeMembershipKindAt(0), expected.get(0));
        assertEquals(session.nativeMembershipKindAt(1), expected.get(1));
        for (final SurveilPartitionCandidateKind kind : expected) {
            final DecisionRequest request = provider.createMembershipRequest(session);
            final LegalCandidate candidate = request.getCandidates().stream()
                    .filter(item -> item.getSurveilPartitionCandidateKind() == kind)
                    .findFirst()
                    .orElseThrow();
            provider.applyMembershipCandidate(session, candidate);
        }

        assertTrue(provider.isComplete(session));
        assertEquals(provider.activeSessionCount(), 1);
        assertEquals(session.nativeMembershipKindAt(0), expected.get(0));
        provider.closeSession(session);
        expectThrows(IllegalStateException.class, () -> session.nativeMembershipKindAt(0));
    }

    @Test
    public void nativeMembershipVectorValidatesCardinalityAndEntries() {
        final Fixture fixture = fixture("Island", "Forest");
        final SurveilPartitionDecisionProvider provider = new SurveilPartitionDecisionProvider();
        final SurveilPartitionSession session = provider.admit(fixture.chooser(), fixture.cards());

        expectThrows(NullPointerException.class,
                () -> session.recordNativeMembershipVector(null));
        expectThrows(IllegalArgumentException.class,
                () -> session.recordNativeMembershipVector(List.of(
                        SurveilPartitionCandidateKind.CLASSIFY_RETAIN)));
        expectThrows(NullPointerException.class,
                () -> session.recordNativeMembershipVector(Arrays.asList(
                        SurveilPartitionCandidateKind.CLASSIFY_RETAIN, null)));

        final List<SurveilPartitionCandidateKind> valid = List.of(
                SurveilPartitionCandidateKind.CLASSIFY_RETAIN,
                SurveilPartitionCandidateKind.CLASSIFY_GRAVEYARD);
        session.recordNativeMembershipVector(valid);
        expectThrows(IllegalStateException.class,
                () -> session.recordNativeMembershipVector(valid));

        provider.closeSession(session);
        expectThrows(IllegalStateException.class,
                () -> session.recordNativeMembershipVector(valid));
    }

    @Test
    public void nativeMembershipKindDoesNotFallBackToInteractiveLabels() {
        final Fixture fixture = fixture("Island", "Forest");
        final SurveilPartitionDecisionProvider provider = new SurveilPartitionDecisionProvider();
        final SurveilPartitionSession session = provider.admit(fixture.chooser(), fixture.cards());

        replaceField(session, "labels", new SurveilPartitionCandidateKind[] {
                SurveilPartitionCandidateKind.CLASSIFY_RETAIN, null});

        expectThrows(IllegalStateException.class, () -> session.nativeMembershipKindAt(0));
        provider.closeSession(session);
    }

    @Test
    public void closedSessionRejectsNativeMembershipRead() {
        final Fixture fixture = fixture("Island");
        final SurveilPartitionDecisionProvider provider = new SurveilPartitionDecisionProvider();
        final SurveilPartitionSession session = provider.admit(fixture.chooser(), fixture.cards());
        session.recordNativeMembershipVector(List.of(SurveilPartitionCandidateKind.CLASSIFY_RETAIN));

        assertEquals(session.nativeMembershipKindAt(0), SurveilPartitionCandidateKind.CLASSIFY_RETAIN);
        provider.closeSession(session);

        expectThrows(IllegalStateException.class, () -> session.nativeMembershipKindAt(0));
    }

    @Test
    public void nullSnapshotCardIsRejectedBySessionAdmissionAfterDefensiveCopy() {
        final Fixture fixture = fixture("Island");
        final SurveilPartitionDecisionProvider provider = new SurveilPartitionDecisionProvider();

        final NullPointerException failure = expectThrows(NullPointerException.class,
                () -> provider.admit(fixture.chooser(), Arrays.asList(fixture.cards().get(0), null)));

        assertEquals(failure.getMessage(), "privateSnapshot card");
        assertEquals(provider.activeSessionCount(), 0);
    }

    @Test
    public void zeroSnapshotCreatesNoPublicRequest() {
        final Fixture fixture = fixture();
        final SurveilPartitionDecisionProvider provider = new SurveilPartitionDecisionProvider();
        final SurveilPartitionSession session = provider.admit(fixture.chooser(), fixture.cards());

        assertNull(provider.createMembershipRequest(session));
        assertTrue(provider.isComplete(session));
        assertEquals(provider.activeSessionCount(), 0);
    }

    @Test
    public void duplicateNativeObjectIsCaptureIntegrityFailure() {
        final Fixture fixture = fixture("Island");
        final SurveilPartitionDecisionProvider provider = new SurveilPartitionDecisionProvider();
        final Card sameCard = fixture.cards().get(0);

        expectThrows(IllegalArgumentException.class,
                () -> provider.admit(fixture.chooser(), List.of(sameCard, sameCard)));
        assertEquals(provider.activeSessionCount(), 0);
    }

    @Test
    public void duplicatePrivateStableIdentityIsCaptureIntegrityFailure() {
        final Fixture fixture = customFixture(new CardSpec("Island", 9301, 730001L));
        final Card first = fixture.cards().get(0);
        final Card duplicateStableIdentity = copyWithStableIdentity(fixture.chooser(), first);
        final SurveilPartitionDecisionProvider provider = new SurveilPartitionDecisionProvider();

        expectThrows(IllegalArgumentException.class,
                () -> provider.admit(fixture.chooser(), List.of(first, duplicateStableIdentity)));
        assertEquals(provider.activeSessionCount(), 0);
    }

    @Test
    public void wrongProfileWrongStepForeignOrAlreadyClassifiedCandidateIsRejected() {
        final Fixture wrongProfileFixture = fixture("Island", "Forest");
        final SurveilPartitionDecisionProvider wrongProfileProvider = new SurveilPartitionDecisionProvider();
        final SurveilPartitionSession wrongProfileSession = wrongProfileProvider.admit(
                wrongProfileFixture.chooser(), wrongProfileFixture.cards());
        recordAllRetain(wrongProfileSession, wrongProfileFixture.cards().size());
        final DecisionRequest wrongProfileRequest = wrongProfileProvider.createMembershipRequest(wrongProfileSession);
        replaceField(wrongProfileRequest.getSurveilPartitionContext(), "profile", null);
        expectThrows(IllegalArgumentException.class,
                () -> wrongProfileProvider.applyMembershipCandidate(wrongProfileSession,
                        wrongProfileRequest.getCandidates().get(0)));
        assertEquals(wrongProfileRequest.getSurveilPartitionContext().getDecisionStepIndex(), 0);

        final Fixture wrongStepFixture = fixture("Island", "Forest");
        final SurveilPartitionDecisionProvider wrongStepProvider = new SurveilPartitionDecisionProvider();
        final SurveilPartitionSession wrongStepSession = wrongStepProvider.admit(
                wrongStepFixture.chooser(), wrongStepFixture.cards());
        recordAllRetain(wrongStepSession, wrongStepFixture.cards().size());
        final DecisionRequest wrongStepRequest = wrongStepProvider.createMembershipRequest(wrongStepSession);
        replaceField(wrongStepRequest.getSurveilPartitionContext(), "decisionStepIndex", 1);
        expectThrows(IllegalArgumentException.class,
                () -> wrongStepProvider.applyMembershipCandidate(wrongStepSession,
                        wrongStepRequest.getCandidates().get(0)));
        assertEquals(wrongStepRequest.getSurveilPartitionContext().getDecisionStepIndex(), 1);

        final Fixture foreignFixture = fixture("Island", "Forest");
        final SurveilPartitionDecisionProvider foreignProvider = new SurveilPartitionDecisionProvider();
        final SurveilPartitionSession foreignTarget = foreignProvider.admit(
                foreignFixture.chooser(), foreignFixture.cards());
        final SurveilPartitionSession foreignSource = foreignProvider.admit(
                foreignFixture.chooser(), foreignFixture.cards());
        recordAllRetain(foreignTarget, foreignFixture.cards().size());
        recordAllRetain(foreignSource, foreignFixture.cards().size());
        final DecisionRequest foreignRequest = foreignProvider.createMembershipRequest(foreignSource);
        final DecisionRequest foreignTargetRequest = foreignProvider.createMembershipRequest(foreignTarget);
        expectThrows(IllegalArgumentException.class,
                () -> foreignProvider.applyMembershipCandidate(foreignTarget,
                        foreignRequest.getCandidates().get(0)));
        assertEquals(foreignTargetRequest.getSurveilPartitionContext().getDecisionStepIndex(), 0);

        final Fixture alreadyClassifiedFixture = fixture("Island", "Forest");
        final SurveilPartitionDecisionProvider alreadyClassifiedProvider =
                new SurveilPartitionDecisionProvider();
        final SurveilPartitionSession alreadyClassifiedSession = alreadyClassifiedProvider.admit(
                alreadyClassifiedFixture.chooser(), alreadyClassifiedFixture.cards());
        recordAllRetain(alreadyClassifiedSession, alreadyClassifiedFixture.cards().size());
        final DecisionRequest firstRequest = alreadyClassifiedProvider
                .createMembershipRequest(alreadyClassifiedSession);
        alreadyClassifiedProvider.applyMembershipCandidate(alreadyClassifiedSession,
                firstRequest.getCandidates().get(0));
        expectThrows(IllegalArgumentException.class,
                () -> alreadyClassifiedProvider.applyMembershipCandidate(alreadyClassifiedSession,
                        firstRequest.getCandidates().get(0)));
        assertEquals(alreadyClassifiedProvider.createMembershipRequest(alreadyClassifiedSession)
                .getSurveilPartitionContext().getDecisionStepIndex(), 1);

        wrongProfileProvider.closeSession(wrongProfileSession);
        wrongStepProvider.closeSession(wrongStepSession);
        foreignProvider.closeSession(foreignTarget);
        foreignProvider.closeSession(foreignSource);
        alreadyClassifiedProvider.closeSession(alreadyClassifiedSession);
    }

    @Test
    public void publicLabelsDoNotUseItemIdOrPrivateNativeIdentity() {
        final Fixture fixture = customFixture(new CardSpec("Island", 9401, 740001L));
        final SurveilPartitionDecisionProvider provider = new SurveilPartitionDecisionProvider();
        final SurveilPartitionSession session = provider.admit(fixture.chooser(), fixture.cards());
        recordAllRetain(session, fixture.cards().size());
        final DecisionRequest request = provider.createMembershipRequest(session);
        final long itemId = request.getSurveilPartitionContext().getCurrentItemId();

        for (final LegalCandidate candidate : request.getCandidates()) {
            final SurveilPartitionCandidateKind kind = candidate.getSurveilPartitionCandidateKind();
            assertNotNull(kind);
            assertEquals(candidate.getSemanticKey(),
                    "SURVEIL_PARTITION|" + kind.name() + "|" + itemId);
            assertTrue(candidate.getSemanticKey().contains(Long.toString(itemId)));
            assertFalse(candidate.getSemanticKey().contains("9401"));
            assertFalse(candidate.getSemanticKey().contains("740001"));
            assertFalse(candidate.getSemanticKey().contains(Integer.toHexString(System.identityHashCode(
                    fixture.cards().get(0)))));
            assertEquals(candidate.getSourceName(), "");
        }

        provider.closeSession(session);
    }

    private Fixture fixture(final String... names) {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final List<Card> cards = Arrays.stream(names)
                .map(name -> addCardToZone(name, chooser, ZoneType.Hand))
                .collect(Collectors.toList());
        return new Fixture(game, chooser, cards);
    }

    private Fixture customFixture(final CardSpec... specs) {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final List<Card> cards = Arrays.stream(specs)
                .map(spec -> customCard(chooser, spec))
                .collect(Collectors.toList());
        return new Fixture(game, chooser, cards);
    }

    private Card customCard(final Player chooser, final CardSpec spec) {
        final Card template = createCard(spec.name(), chooser);
        final Card card = CardFactory.getCard(template.getPaperCard(), chooser, spec.cardId(), chooser.getGame());
        card.setGameTimestamp(spec.gameTimestamp());
        chooser.getZone(ZoneType.Hand).add(card);
        return card;
    }

    private Card copyWithStableIdentity(final Player chooser, final Card source) {
        final Card copy = CardFactory.getCard(source.getPaperCard(), chooser, source.getId(), chooser.getGame());
        copy.setGameTimestamp(source.getGameTimestamp());
        chooser.getZone(ZoneType.Hand).add(copy);
        return copy;
    }

    private static List<String> publicPairs(final DecisionRequest request) {
        return request.getSurveilPartitionContext().getVisibleItems().stream()
                .map(item -> item.getVisibleName() + "|" + item.getItemId())
                .collect(Collectors.toList());
    }

    private static List<String> publicNameAndIds(final DecisionRequest request) {
        return request.getSurveilPartitionContext().getVisibleItems().stream()
                .map(item -> item.getVisibleName() + "=" + item.getItemId())
                .collect(Collectors.toList());
    }

    private static long itemIdForNative(final SurveilPartitionSession session, final Card card) {
        try {
            final Field mapField = Arrays.stream(session.getClass().getDeclaredFields())
                    .filter(field -> IdentityHashMap.class.isAssignableFrom(field.getType()))
                    .findFirst()
                    .orElseThrow();
            mapField.setAccessible(true);
            final Map<?, ?> nativeItems = (Map<?, ?>) mapField.get(session);
            final Object item = nativeItems.get(card);
            assertNotNull(item);
            final Field itemIdField = item.getClass().getDeclaredField("itemId");
            itemIdField.setAccessible(true);
            return itemIdField.getLong(item);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private static void recordAllRetain(final SurveilPartitionSession session, final int itemCount) {
        session.recordNativeMembershipVector(Collections.nCopies(itemCount,
                SurveilPartitionCandidateKind.CLASSIFY_RETAIN));
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

    private record CardSpec(String name, int cardId, long gameTimestamp) {
    }

    private record Fixture(Game game, Player chooser, List<Card> cards) {
    }
}
