package forge.game.decision;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Iterator;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class MulliganBottomAdapterTest extends AITest {
    private final MulliganBottomAdapter adapter = new MulliganBottomAdapter();

    @Test
    public void exactOneReplaysOneAtomicSelectionWithoutZoneMutation() {
        final Game game = initAndCreateGame();
        final Player actingPlayer = game.getPlayers().get(1);
        final Card first = addCardToZone("Island", actingPlayer, ZoneType.Hand);
        final Card second = addCardToZone("Mountain", actingPlayer, ZoneType.Hand);
        final CardCollection callbackHand = new CardCollection(List.of(first, second));
        final int handSizeBefore = actingPlayer.getCardsIn(ZoneType.Hand).size();

        final MulliganBottomAdapter.Capture capture = adapter.begin(actingPlayer, callbackHand, 1);
        final MulliganBottomAdapter.Replay replay = adapter.replay(capture, new CardCollection(second));

        assertEquals(capture.getStatus(), MulliganBottomAdapter.Status.SUPPORTED);
        assertEquals(replay.getStatus(), MulliganBottomAdapter.ReplayStatus.COMPLETE);
        assertEquals(replay.getSteps().size(), 1);
        assertEquals(replay.getCompletedCards().size(), 1);
        assertSame(replay.getCompletedCards().get(0), second);
        assertEquals(actingPlayer.getCardsIn(ZoneType.Hand).size(), handSizeBefore);
    }

    @Test
    public void exactTwoReplaysControllerOrderAndHasNoEarlyDoneCandidate() {
        final Game game = initAndCreateGame();
        final Player actingPlayer = game.getPlayers().get(1);
        final Card first = addCardToZone("Island", actingPlayer, ZoneType.Hand);
        final Card second = addCardToZone("Mountain", actingPlayer, ZoneType.Hand);
        final Card third = addCardToZone("Forest", actingPlayer, ZoneType.Hand);
        final CardCollection callbackHand = new CardCollection(List.of(first, second, third));

        final MulliganBottomAdapter.Capture capture = adapter.begin(actingPlayer, callbackHand, 2);
        final MulliganBottomAdapter.Replay replay = adapter.replay(capture,
                new CardCollection(List.of(second, first)));

        assertEquals(replay.getStatus(), MulliganBottomAdapter.ReplayStatus.COMPLETE);
        assertEquals(replay.getSteps().size(), 2);
        assertEquals(replay.getSteps().get(0).getRequest().getCandidates().size(), 3);
        assertTrue(replay.getSteps().get(0).getRequest().getCandidates().stream()
                .noneMatch(candidate -> candidate.getCardSelectionKind() == CardSelectionCandidateKind.DONE));
        assertEquals(replay.getSteps().get(1).getRequest().getCandidates().size(), 2);
        assertTrue(replay.getSteps().get(1).getRequest().getCandidates().stream()
                .noneMatch(candidate -> candidate.getCardSelectionKind() == CardSelectionCandidateKind.DONE));
        assertSame(replay.getCompletedCards().get(0), second);
        assertSame(replay.getCompletedCards().get(1), first);
    }

    @Test
    public void zeroCardsCompletesWithoutSyntheticPolicyRequest() {
        final Game game = initAndCreateGame();
        final Player actingPlayer = game.getPlayers().get(1);
        final Card first = addCardToZone("Island", actingPlayer, ZoneType.Hand);
        final CardCollection callbackHand = new CardCollection(first);

        final MulliganBottomAdapter.Capture capture = adapter.begin(actingPlayer, callbackHand, 0);
        final MulliganBottomAdapter.Replay replay = adapter.replay(capture, new CardCollection());

        assertEquals(replay.getStatus(), MulliganBottomAdapter.ReplayStatus.COMPLETE);
        assertTrue(replay.getSteps().isEmpty());
        assertTrue(replay.getCompletedCards().isEmpty());
    }

    @Test
    public void callbackHandIsTheSelectableDomainNotTheFullLiveHand() {
        final Game game = initAndCreateGame();
        final Player actingPlayer = game.getPlayers().get(1);
        final Card callbackCard = addCardToZone("Island", actingPlayer, ZoneType.Hand);
        final Card hiddenFromCallback = addCardToZone("Mountain", actingPlayer, ZoneType.Hand);
        final CardCollection callbackHand = new CardCollection(callbackCard);

        final MulliganBottomAdapter.Capture capture = adapter.begin(actingPlayer, callbackHand, 1);
        final MulliganBottomAdapter.Replay replay = adapter.replay(capture, new CardCollection(callbackCard));

        assertEquals(replay.getStatus(), MulliganBottomAdapter.ReplayStatus.COMPLETE);
        assertEquals(replay.getSteps().get(0).getRequest().getCandidates().size(), 1);
        assertFalse(replay.getSteps().get(0).getRequest().getCandidates().stream()
                .anyMatch(candidate -> candidate.getCardSelectionCard().getCardId() == hiddenFromCallback.getId()));
    }

    @Test
    public void sameNamedCardsRemainDistinctByIdAndTimestamp() {
        final Game game = initAndCreateGame();
        final Player actingPlayer = game.getPlayers().get(1);
        final Card first = addCardToZone("Island", actingPlayer, ZoneType.Hand);
        final Card second = addCardToZone("Island", actingPlayer, ZoneType.Hand);
        final CardCollection callbackHand = new CardCollection(List.of(first, second));

        final MulliganBottomAdapter.Capture capture = adapter.begin(actingPlayer, callbackHand, 1);
        final MulliganBottomAdapter.Replay replay = adapter.replay(capture, new CardCollection(second));

        assertEquals(replay.getStatus(), MulliganBottomAdapter.ReplayStatus.COMPLETE);
        assertSame(replay.getCompletedCards().get(0), second);
        assertFalse(replay.getCompletedCards().contains(first));
    }

    @Test
    public void invalidCountAndControllerMappingsFailBeforeSelection() {
        final Game game = initAndCreateGame();
        final Player actingPlayer = game.getPlayers().get(1);
        final Card first = addCardToZone("Island", actingPlayer, ZoneType.Hand);
        final Card second = addCardToZone("Mountain", actingPlayer, ZoneType.Hand);
        final Card unknown = addCardToZone("Forest", actingPlayer, ZoneType.Hand);
        final CardCollection callbackHand = new CardCollection(List.of(first, second));

        assertEquals(adapter.begin(actingPlayer, callbackHand, 3).getStatus(),
                MulliganBottomAdapter.Status.INVALID_DOMAIN);

        final MulliganBottomAdapter.Capture capture = adapter.begin(actingPlayer, callbackHand, 1);
        assertEquals(adapter.replay(capture, new CardCollection()).getStatus(),
                MulliganBottomAdapter.ReplayStatus.MAPPING_FAILED);
        assertEquals(adapter.replay(capture, duplicateView(first)).getStatus(),
                MulliganBottomAdapter.ReplayStatus.MAPPING_FAILED);
        assertEquals(adapter.replay(capture, new CardCollection(unknown)).getStatus(),
                MulliganBottomAdapter.ReplayStatus.MAPPING_FAILED);
    }

    @Test
    public void callbackIdentityChangeIsMappingFailureAndLeavesZonesUntouched() {
        final Game game = initAndCreateGame();
        final Player actingPlayer = game.getPlayers().get(1);
        final Card selected = addCardToZone("Island", actingPlayer, ZoneType.Hand);
        final CardCollection callbackHand = new CardCollection(selected);
        final int handSizeBefore = actingPlayer.getCardsIn(ZoneType.Hand).size();
        final long originalTimestamp = selected.getGameTimestamp();

        final MulliganBottomAdapter.Capture capture = adapter.begin(actingPlayer, callbackHand, 1);
        final Card moved = game.getAction().moveToGraveyard(selected, null);
        final Card returned = game.getAction().moveTo(ZoneType.Hand, moved, null, AbilityKey.newMap());

        assertEquals(returned.getId(), selected.getId());
        assertFalse(returned.getGameTimestamp() == originalTimestamp);
        final MulliganBottomAdapter.Replay replay = adapter.replay(capture, new CardCollection(returned));

        assertEquals(replay.getStatus(), MulliganBottomAdapter.ReplayStatus.MAPPING_FAILED);
        assertEquals(actingPlayer.getCardsIn(ZoneType.Hand).size(), handSizeBefore);
    }

    private static CardCollectionView duplicateView(final Card card) {
        return new DuplicateCardCollectionView(List.of(card, card));
    }

    private static final class DuplicateCardCollectionView implements CardCollectionView {
        private final List<Card> cards;

        private DuplicateCardCollectionView(final List<Card> cards) {
            this.cards = List.copyOf(cards);
        }

        @Override
        public int size() {
            return cards.size();
        }

        @Override
        public boolean isEmpty() {
            return cards.isEmpty();
        }

        @Override
        public boolean contains(final Object object) {
            return cards.contains(object);
        }

        @Override
        public Iterator<Card> iterator() {
            return cards.iterator();
        }

        @Override
        public Object[] toArray() {
            return cards.toArray();
        }

        @Override
        public <T> T[] toArray(final T[] target) {
            return cards.toArray(target);
        }

        @Override
        public boolean add(final Card card) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(final Object object) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsAll(final java.util.Collection<?> collection) {
            return cards.containsAll(collection);
        }

        @Override
        public boolean addAll(final java.util.Collection<? extends Card> collection) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(final java.util.Collection<?> collection) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(final java.util.Collection<?> collection) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Card get(final int index) {
            return cards.get(index);
        }

        @Override
        public Card getFirst() {
            return cards.get(0);
        }

        @Override
        public Card getLast() {
            return cards.get(cards.size() - 1);
        }

        @Override
        public int indexOf(final Object object) {
            return cards.indexOf(object);
        }

        @Override
        public int lastIndexOf(final Object object) {
            return cards.lastIndexOf(object);
        }

        @Override
        public List<Card> subList(final int fromIndex, final int toIndex) {
            return cards.subList(fromIndex, toIndex);
        }

        @Override
        public Iterable<Card> threadSafeIterable() {
            return List.copyOf(cards);
        }

        @Override
        public Card get(final Card object) {
            final int index = cards.indexOf(object);
            return index < 0 ? null : cards.get(index);
        }

        @Override
        public Stream<Card> stream() {
            return cards.stream();
        }

        @Override
        public boolean anyMatch(final Predicate<? super Card> predicate) {
            return cards.stream().anyMatch(predicate);
        }

        @Override
        public boolean allMatch(final Predicate<? super Card> predicate) {
            return cards.stream().allMatch(predicate);
        }
    }
}
