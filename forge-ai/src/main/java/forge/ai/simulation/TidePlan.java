package forge.ai.simulation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * What a High Tide deck fetches with a tutor, whatever the deck: Intuition, Cunning Wish, Merchant Scroll, Impulse.
 * The loop of {@link LoopRunner} needs three things: a finisher (Stroke of Genius or Blue Sun's Zenith to draw the
 * opponent's library, Brain Freeze to mill it), High Tide, and something that untaps the lands (Turnabout, Candelabra
 * of Tawnos, Mind Over Matter). A tutor fetches the first of these that the hand and the table lack, so that the
 * loop can be set up; with nothing missing it has no opinion and the normal choice is made.
 */
public final class TidePlan {
    private static final String HIGH_TIDE = "High Tide";
    private static final List<String> FINISHERS = List.of("Stroke of Genius", "Blue Sun's Zenith", "Brain Freeze");
    private static final List<String> UNTAPPERS = List.of("Turnabout", "Candelabra of Tawnos", "Mind Over Matter");
    private static final List<String> BACKUP = List.of("Force of Will", "Force of Negation", "Pact of Negation");

    private TidePlan() {
    }

    private static boolean isTideCard(final Card c) {
        final String n = c.getName();
        return n.equals(HIGH_TIDE) || FINISHERS.contains(n) || UNTAPPERS.contains(n) || BACKUP.contains(n);
    }

    private static Set<String> names(final Iterable<Card> cards) {
        final Set<String> names = new HashSet<>();
        for (final Card c : cards) {
            names.add(c.getName());
        }
        return names;
    }

    /** A deck with High Tide and a finisher somewhere among the cards the AI owns (sideboard included: Cunning Wish). */
    public static boolean isTideDeck(final Player ai) {
        boolean tide = false;
        boolean finisher = false;
        for (final Card c : ai.getCardsIn(ZoneType.Hand, ZoneType.Library, ZoneType.Graveyard, ZoneType.Battlefield,
                ZoneType.Sideboard)) {
            tide |= c.getName().equals(HIGH_TIDE);
            finisher |= FINISHERS.contains(c.getName());
        }
        return tide && finisher;
    }

    /**
     * The cards to search for: what the loop is missing, the finisher first, then High Tide, then what untaps the
     * lands. Null for no opinion (not a High Tide deck, or nothing is missing).
     */
    public static CardCollection picks(final Player ai, final Iterable<Card> pool, final int n) {
        if (ai == null || pool == null || n <= 0 || !isTideDeck(ai)) {
            return null;
        }
        final Set<String> have = names(ai.getCardsIn(ZoneType.Hand, ZoneType.Battlefield));
        boolean tideCast = false;
        for (final Card c : ai.getGame().getCardsIn(ZoneType.Command)) {
            tideCast |= c.getName().startsWith(HIGH_TIDE);
        }
        final List<String> want = new ArrayList<>();
        if (missing(have, FINISHERS)) {
            want.addAll(FINISHERS);
        }
        if (!have.contains(HIGH_TIDE) && !tideCast) {
            want.add(HIGH_TIDE);
        }
        if (missing(have, UNTAPPERS)) {
            want.addAll(UNTAPPERS);
        }
        if (want.isEmpty()) {
            return null;
        }
        final CardCollection picks = new CardCollection();
        final Set<String> taken = new HashSet<>();
        for (final String name : want) {
            for (final Card c : pool) {
                if (c.getName().equals(name) && taken.add(name)) {
                    picks.add(c);
                    break;
                }
            }
        }
        if (picks.isEmpty()) {
            return null;
        }
        final CardCollection result = new CardCollection();
        for (final Card c : picks) {
            if (result.size() < n) {
                result.add(c);
            }
        }
        // Intuition asks for three cards: the rest of them are other cards of the loop
        for (final Card c : pool) {
            if (result.size() >= n) {
                break;
            }
            if (!result.contains(c) && isTideCard(c) && taken.add(c.getName())) {
                result.add(c);
            }
        }
        return result.size() >= n ? result : null;
    }

    private static boolean missing(final Set<String> have, final List<String> names) {
        for (final String n : names) {
            if (have.contains(n)) {
                return false;
            }
        }
        return true;
    }
}
