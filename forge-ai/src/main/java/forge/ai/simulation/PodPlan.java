package forge.ai.simulation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import forge.ai.ComputerUtilCard;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;

/**
 * What a Birthing Pod deck fetches. Pod sacrifices a creature for one that costs one more, so every use climbs a
 * rung; the best creature at that cost is not always the best fetch. A creature with an enters trigger pays back at
 * once (Kitchen Finks, Restoration Angel, Reveillark, Thragtusk), and while Pod stays in play the next use needs a
 * creature one rung higher in the library: a fetch that leaves a rung to climb to is worth more than one at the end of
 * the ladder. A combo piece that the catalogue is missing is fetched before any of this (see {@link DeckPlan}).
 */
public final class PodPlan {
    private static final String POD = "Birthing Pod";
    private static final int ENTERS_TRIGGER = 100;
    private static final int LADDER = 150;

    private PodPlan() {
    }

    /** A deck with Birthing Pod among the cards the AI owns. */
    public static boolean isPodDeck(final Player ai) {
        for (final Card c : ai.getCardsIn(ZoneType.Hand, ZoneType.Library, ZoneType.Battlefield, ZoneType.Graveyard)) {
            if (c.getName().equals(POD)) {
                return true;
            }
        }
        return false;
    }

    private static boolean entersWithATrigger(final Card c) {
        for (final Trigger t : c.getTriggers()) {
            if (t.getMode() == TriggerType.ChangesZone && "Battlefield".equals(t.getParam("Destination"))
                    && t.getParam("ValidCard") != null && t.getParam("ValidCard").startsWith("Card.Self")) {
                return true;
            }
        }
        return false;
    }

    private static boolean podInPlay(final Player ai) {
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (c.getName().equals(POD)) {
                return true;
            }
        }
        return false;
    }

    /** Whether a creature one rung up is still in the library, for the next Pod. */
    static boolean ladderContinues(final Player ai, final Card c) {
        for (final Card other : ai.getCardsIn(ZoneType.Library)) {
            if (other != c && other.isCreature() && other.getCMC() == c.getCMC() + 1
                    && !(other.getType().isLegendary() && ai.isCardInPlay(other.getName()))) {
                return true;
            }
        }
        return false;
    }

    static int score(final Player ai, final Card c, final boolean ladder) {
        int s = ComputerUtilCard.evaluateCreature(c);
        if (entersWithATrigger(c)) {
            s += ENTERS_TRIGGER;
        }
        if (ladder && ladderContinues(ai, c)) {
            s += LADDER;
        }
        return s;
    }

    /** The creatures to fetch, best first; null for no opinion (not a Pod deck, or no creature in the pool). */
    public static CardCollection picks(final Player ai, final Iterable<Card> pool, final int n) {
        if (ai == null || pool == null || n <= 0 || !isPodDeck(ai)) {
            return null;
        }
        final boolean ladder = podInPlay(ai);
        final List<Card> creatures = new ArrayList<>();
        for (final Card c : pool) {
            if (c.isCreature() && !(c.getType().isLegendary() && ai.isCardInPlay(c.getName()))) {
                creatures.add(c);
            }
        }
        if (creatures.isEmpty()) {
            return null;
        }
        creatures.sort(Comparator.comparingInt((Card c) -> -score(ai, c, ladder)));
        return new CardCollection(creatures.subList(0, Math.min(n, creatures.size())));
    }
}
