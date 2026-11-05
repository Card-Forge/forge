package forge.ai.simulation;

import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Doomsday: five cards are kept, everything else is exiled, and half the life is lost. The deck wins by emptying its
 * own library with the cards of that pile (draws, free cantrips) and then casting Thassa's Oracle (or Laboratory
 * Maniac) with the library at or below the devotion: the Oracle is held in hand and the pile is five cards that draw.
 * This decides when Doomsday may be cast, which five cards it keeps, and how far a Doomsday deck has come.
 */
public final class DoomsdayPile {
    /** The pile is five cards: a library of at most this many cards after Doomsday has been cast. */
    static final int PILE = 5;
    /** The life Doomsday takes is half, rounded up: with less than this it is not cast. */
    static final int MIN_LIFE = 4;

    private DoomsdayPile() {
    }

    public static boolean isDoomsday(final Card c) {
        return c != null && AiCardLists.has(AiCardLists.Role.DOOMSDAY, c.getName());
    }

    private static boolean isWinner(final Card c) {
        return AiCardLists.has(AiCardLists.Role.LIBRARY_OUT_PAYOFF, c.getName());
    }

    /** Whether a card that wins with a short library is in hand (or already in play). */
    static boolean winnerAvailable(final Player ai) {
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            if (isWinner(c)) {
                return true;
            }
        }
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (isWinner(c)) {
                return true;
            }
        }
        return false;
    }

    /** Doomsday is only cast with the winner in hand, life to lose, and a library worth cutting down. */
    public static boolean ready(final Player ai) {
        return winnerAvailable(ai) && ai.getLife() >= MIN_LIFE
                && ai.getCardsIn(ZoneType.Library).size() > 2 * PILE;
    }

    private static int draws(final Card c) {
        for (final SpellAbility sa : c.getSpellAbilities()) {
            for (SpellAbility a = sa; a != null; a = a.getSubAbility()) {
                if (a.getApi() == ApiType.Draw) {
                    try {
                        return Integer.parseInt(a.getParamOrDefault("NumCards", "1").trim());
                    } catch (final NumberFormatException e) {
                        return 1;
                    }
                }
            }
        }
        return 1; // cycling (Street Wraith), Probe's reveal and draw
    }

    /** What a card is worth in the pile: the cards it draws first, then mana, the cheaper the better. 0 for the rest. */
    static int score(final Card c) {
        if (AiCardLists.has(AiCardLists.Role.DOOMSDAY_DRAW, c.getName())) {
            return 100 + 20 * draws(c) - 3 * c.getCMC();
        }
        if (AiCardLists.has(AiCardLists.Role.DOOMSDAY_MANA, c.getName())) {
            return 50 - 3 * c.getCMC();
        }
        return 0;
    }

    /**
     * The card Doomsday should keep next, or null for no opinion. The winner goes into the pile when none is in hand,
     * the rest of it is the cards that draw the most for the least.
     */
    public static Card pick(final Player ai, final CardCollectionView options) {
        if (ai == null || options == null || options.isEmpty()) {
            return null;
        }
        if (!winnerAvailable(ai)) {
            for (final Card c : options) {
                if (isWinner(c)) {
                    return c;
                }
            }
        }
        Card best = null;
        int bestScore = 0;
        for (final Card c : options) {
            final int s = score(c);
            if (s > bestScore || s == bestScore && best != null && s > 0 && c.getName().compareTo(best.getName()) < 0) {
                best = c;
                bestScore = s;
            }
        }
        return best;
    }

    /** Progress of a Doomsday deck in evaluator points: a cut library with the winner at hand is almost the game. */
    public static int term(final Player ai) {
        final int library = ai.getCardsIn(ZoneType.Library).size();
        if (library > 2 * PILE) {
            return 0;
        }
        if (!winnerAvailable(ai)) {
            return -100; // a cut library and nothing to win with
        }
        return library <= PILE ? 200 : 60;
    }
}
