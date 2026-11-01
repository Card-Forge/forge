package forge.ai.simulation;

import forge.game.Game;
import forge.game.GameObject;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.player.PlayerView;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

/**
 * Determinization for Monte Carlo rollouts: re-deals, in a <em>copied</em> game, the cards that the AI player
 * cannot know.
 * <p>
 * {@link GameCopier} copies every zone exactly, so without this step every rollout of the same line would be
 * identical (nothing random is left), and the AI would effectively see the opponent's hand and the order of the
 * libraries. For every player, the cards in hand and library that are not visible to the AI player are pooled,
 * shuffled and dealt back into the same slots, so zone sizes never change. Cards the AI is allowed to see
 * (its own hand, revealed cards, cards it may look at) stay exactly where they are.
 */
public final class HiddenInfoShuffler {
    private HiddenInfoShuffler() {
    }

    /**
     * @param simGame    the copied game, which is modified
     * @param copier     the copier that produced {@code simGame}, used to look up the original cards
     * @param viewerOrig the AI player, as a player of the <em>original</em> game; its point of view decides
     *                   which cards count as hidden
     * @param rnd        source of randomness, so that rollouts are reproducible from a seed
     */
    public static void shuffle(final Game simGame, final GameCopier copier, final Player viewerOrig, final Random rnd) {
        shuffle(simGame, copier, viewerOrig, rnd, null);
    }

    /**
     * As above, but the hidden cards of the viewer's own library that satisfy {@code bottom} are dealt to the deepest
     * hidden slots, so that what draws take from the top is never one of them while a search can still find them.
     * A line that depends on drawing a particular card is luck, not a plan: with this, a line found is one that holds.
     */
    public static void shuffle(final Game simGame, final GameCopier copier, final Player viewerOrig, final Random rnd,
                               final Predicate<Card> bottom) {
        final PlayerView viewer = viewerOrig.getView();
        for (final Player simPlayer : simGame.getPlayers()) {
            final Zone hand = simPlayer.getZone(ZoneType.Hand);
            final Zone library = simPlayer.getZone(ZoneType.Library);
            final List<Card> handCards = new ArrayList<>(hand.getCards());
            final List<Card> libraryCards = new ArrayList<>(library.getCards());

            final List<Integer> hiddenHandSlots = hiddenSlots(handCards, copier, viewer);
            final List<Integer> hiddenLibrarySlots = hiddenSlots(libraryCards, copier, viewer);

            final List<Card> pool = new ArrayList<>(hiddenHandSlots.size() + hiddenLibrarySlots.size());
            for (final int slot : hiddenHandSlots) {
                pool.add(handCards.get(slot));
            }
            for (final int slot : hiddenLibrarySlots) {
                pool.add(libraryCards.get(slot));
            }
            if (pool.size() < 2) {
                continue; // nothing to re-deal
            }

            Collections.shuffle(pool, rnd);
            if (bottom != null && copier.reverseFind(simPlayer) == viewerOrig) {
                pool.sort((a, b) -> Boolean.compare(bottom.test(a), bottom.test(b))); // stable: false first
            }
            int next = 0;
            for (final int slot : hiddenHandSlots) {
                handCards.set(slot, pool.get(next++));
            }
            for (final int slot : hiddenLibrarySlots) {
                libraryCards.set(slot, pool.get(next++));
            }
            hand.setCards(handCards);
            library.setCards(libraryCards);
        }
    }

    private static List<Integer> hiddenSlots(final List<Card> cards, final GameCopier copier, final PlayerView viewer) {
        final List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < cards.size(); i++) {
            if (isHiddenFrom(cards.get(i), copier, viewer)) {
                slots.add(i);
            }
        }
        return slots;
    }

    private static boolean isHiddenFrom(final Card simCard, final GameCopier copier, final PlayerView viewer) {
        final GameObject orig = copier.reverseFind(simCard);
        if (!(orig instanceof Card)) {
            return false; // cannot tell what the AI knows about it, so never move it
        }
        return !((Card) orig).getView().canBeShownTo(viewer);
    }
}
