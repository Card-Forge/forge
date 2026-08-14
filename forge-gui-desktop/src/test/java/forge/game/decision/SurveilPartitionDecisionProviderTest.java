package forge.game.decision;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

public class SurveilPartitionDecisionProviderTest extends AITest {
    @Test
    public void everyNonEmptyRequestHasExactlyGraveyardAndRetainCandidates() {
        for (final int count : new int[] {1, 2, 4}) {
            final Fixture fixture = fixture(count);
            final SurveilPartitionDecisionProvider provider = new SurveilPartitionDecisionProvider();
            final SurveilPartitionSession session = provider.admit(fixture.chooser(), fixture.cards());
            recordAllRetain(session, fixture.cards().size());
            final DecisionRequest request = provider.createMembershipRequest(session);

            assertNotNull(request);
            assertEquals(request.getDecisionType(), DecisionType.CARD_SELECTION);
            assertFalse(request.isForced());
            assertNotNull(request.getSurveilPartitionContext());
            assertNull(request.getCardSelectionContext());
            assertEquals(request.getCandidates().size(), 2);
            assertEquals(request.getCandidates().stream()
                    .map(LegalCandidate::getSurveilPartitionCandidateKind)
                    .collect(Collectors.toSet()),
                    Set.of(SurveilPartitionCandidateKind.CLASSIFY_GRAVEYARD,
                            SurveilPartitionCandidateKind.CLASSIFY_RETAIN));
            assertEquals(request.getCandidates().stream()
                    .map(candidate -> candidate.getSurveilPartitionCard().getItemId())
                    .collect(Collectors.toSet()),
                    Set.of(request.getSurveilPartitionContext().getCurrentItemId()));
            assertTrue(request.getCandidates().stream()
                    .noneMatch(candidate -> "DONE".equals(candidate.getSemanticKey())));
            assertTrue(request.getCandidates().stream()
                    .allMatch(candidate -> candidate.getCardSelectionKind() == null
                            && candidate.getCardSelectionCard() == null));

            provider.closeSession(session);
        }
    }

    @Test
    public void nOneIsNotForced() {
        final Fixture fixture = fixture(1);
        final SurveilPartitionDecisionProvider provider = new SurveilPartitionDecisionProvider();
        final SurveilPartitionSession session = provider.admit(fixture.chooser(), fixture.cards());
        recordAllRetain(session, fixture.cards().size());
        final DecisionRequest request = provider.createMembershipRequest(session);

        assertFalse(request.isForced());
        assertEquals(request.getCandidates().size(), 2);
        assertEquals(request.getCandidates().stream()
                .map(LegalCandidate::getSurveilPartitionCandidateKind)
                .collect(Collectors.toSet()),
                Set.of(SurveilPartitionCandidateKind.CLASSIFY_GRAVEYARD,
                        SurveilPartitionCandidateKind.CLASSIFY_RETAIN));

        provider.closeSession(session);
    }

    @Test
    public void nonEmptySessionRequiresNativeVectorBeforeRequestOrApply() {
        final Fixture fixture = fixture(2);
        final SurveilPartitionDecisionProvider provider = new SurveilPartitionDecisionProvider();
        final SurveilPartitionSession session = provider.admit(fixture.chooser(), fixture.cards());

        final IllegalStateException requestFailure = expectThrows(IllegalStateException.class,
                () -> provider.createMembershipRequest(session));
        assertTrue(requestFailure.getMessage().contains("native membership vector"));
        assertEquals(provider.activeSessionCount(), 1);

        final IllegalStateException applyFailure = expectThrows(IllegalStateException.class,
                () -> provider.applyMembershipCandidate(session, null));
        assertTrue(applyFailure.getMessage().contains("native membership vector"));

        provider.closeSession(session);
    }

    @Test
    public void nTwoAndNFourUseCanonicalProjectionOrder() {
        for (final String[] names : new String[][] {
                {"Island", "Forest"},
                {"Swamp", "Forest", "Island", "Mountain"}
        }) {
            final Fixture fixture = fixture(names);
            final SurveilPartitionDecisionProvider provider = new SurveilPartitionDecisionProvider();
            final SurveilPartitionSession session = provider.admit(fixture.chooser(), fixture.cards());
            recordAllRetain(session, fixture.cards().size());
            final List<String> observedNames = new java.util.ArrayList<>();

            for (int step = 0; step < names.length; step++) {
                final DecisionRequest request = provider.createMembershipRequest(session);
                assertNotNull(request);
                final SurveilPartitionContext context = request.getSurveilPartitionContext();
                observedNames.add(context.getVisibleItems().stream()
                        .filter(item -> item.getItemId() == context.getCurrentItemId())
                        .findFirst()
                        .orElseThrow()
                        .getVisibleName());
                final LegalCandidate retain = request.getCandidates().stream()
                        .filter(candidate -> candidate.getSurveilPartitionCandidateKind()
                                == SurveilPartitionCandidateKind.CLASSIFY_RETAIN)
                        .findFirst()
                        .orElseThrow();
                provider.applyMembershipCandidate(session, retain);
            }

            assertEquals(observedNames, Arrays.stream(names).sorted().collect(Collectors.toList()));
            assertTrue(provider.isComplete(session));
            assertEquals(provider.activeSessionCount(), 1);
            provider.closeSession(session);
            assertEquals(provider.activeSessionCount(), 0);
        }
    }

    @Test
    public void completedProviderSessionRemainsOwnedUntilExplicitTerminalClose() {
        final Fixture fixture = fixture(2);
        final SurveilPartitionDecisionProvider provider = new SurveilPartitionDecisionProvider();
        final SurveilPartitionSession session = provider.admit(fixture.chooser(), fixture.cards());
        session.recordNativeMembershipVector(Collections.nCopies(fixture.cards().size(),
                SurveilPartitionCandidateKind.CLASSIFY_RETAIN));

        for (int step = 0; step < fixture.cards().size(); step++) {
            final DecisionRequest request = provider.createMembershipRequest(session);
            provider.applyMembershipCandidate(session, request.getCandidates().get(1));
        }

        assertTrue(provider.isComplete(session));
        assertEquals(provider.activeSessionCount(), 1);
        provider.closeSession(session);
        assertEquals(provider.activeSessionCount(), 0);
    }

    @Test
    public void everySubsetMapsToExactlyOneMembershipVector() {
        final Fixture fixture = fixture("Swamp", "Forest", "Island", "Mountain");
        final int itemCount = fixture.cards().size();

        for (int mask = 0; mask < (1 << itemCount); mask++) {
            final SurveilPartitionDecisionProvider provider = new SurveilPartitionDecisionProvider();
            final SurveilPartitionSession session = provider.admit(fixture.chooser(), fixture.cards());
            final List<SurveilPartitionCandidateKind> nativeVector = new java.util.ArrayList<>();
            for (int step = 0; step < itemCount; step++) {
                nativeVector.add((mask & (1 << step)) == 0
                        ? SurveilPartitionCandidateKind.CLASSIFY_GRAVEYARD
                        : SurveilPartitionCandidateKind.CLASSIFY_RETAIN);
            }
            session.recordNativeMembershipVector(nativeVector);
            for (int step = 0; step < itemCount; step++) {
                assertEquals(session.nativeMembershipKindAt(step), nativeVector.get(step));
            }
            int requestCount = 0;

            for (int step = 0; step < itemCount; step++) {
                final DecisionRequest request = provider.createMembershipRequest(session);
                requestCount++;
                assertEquals(request.getCandidates().size(), 2);
                assertTrue(request.getCandidates().stream()
                        .noneMatch(candidate -> "DONE".equals(candidate.getSemanticKey())));
                assertTrue(request.getCandidates().stream()
                        .allMatch(candidate -> candidate.getSurveilPartitionCard().getItemId()
                                == request.getSurveilPartitionContext().getCurrentItemId()));
                final SurveilPartitionCandidateKind requiredKind =
                        (mask & (1 << step)) == 0
                                ? SurveilPartitionCandidateKind.CLASSIFY_GRAVEYARD
                                : SurveilPartitionCandidateKind.CLASSIFY_RETAIN;
                final LegalCandidate selected = request.getCandidates().stream()
                        .filter(candidate -> candidate.getSurveilPartitionCandidateKind() == requiredKind)
                        .findFirst()
                        .orElseThrow();
                provider.applyMembershipCandidate(session, selected);
            }

            assertEquals(requestCount, itemCount);
            assertTrue(provider.isComplete(session));
            assertEquals(session.nativeMembershipKindAt(0), nativeVector.get(0));
            assertEquals(provider.activeSessionCount(), 1);
            provider.closeSession(session);
            assertEquals(provider.activeSessionCount(), 0);
            expectThrows(IllegalStateException.class, () -> session.nativeMembershipKindAt(0));
            expectThrows(IllegalStateException.class, () -> provider.createMembershipRequest(session));
        }
    }

    private Fixture fixture(final int count) {
        return fixture(Arrays.copyOf(new String[] {"Island", "Forest", "Mountain", "Swamp"}, count));
    }

    private Fixture fixture(final String... names) {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final List<Card> cards = Arrays.stream(names)
                .map(name -> addCardToZone(name, chooser, ZoneType.Hand))
                .collect(Collectors.toList());
        return new Fixture(game, chooser, cards);
    }

    private static void recordAllRetain(final SurveilPartitionSession session, final int itemCount) {
        session.recordNativeMembershipVector(Collections.nCopies(itemCount,
                SurveilPartitionCandidateKind.CLASSIFY_RETAIN));
    }

    private record Fixture(Game game, Player chooser, List<Card> cards) {
    }
}
