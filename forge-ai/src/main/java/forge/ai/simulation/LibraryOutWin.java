package forge.ai.simulation;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import forge.ai.ComputerUtilMana;
import forge.game.card.Card;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Oops! All Spells: the win that follows the Balustrade Spy has to be certain. With the Oracle (or Jace, Wielder of
 * Mysteries) in hand it only is when the mana for it is there after the Spy, in the same turn: the library is empty for
 * good, and the next draw step loses the game before the Oracle can be cast. The other way, the flashback of Dread
 * Return with three creatures, costs no mana and is judged by {@link ComboPlan#libraryOutSelfMillSafe}.
 */
final class LibraryOutWin {
    private LibraryOutWin() {
    }

    /** Cards that win on the spot when they enter with an empty library. */
    private static final Set<String> WIN_CARDS = new HashSet<>(Arrays.asList("Thassa's Oracle",
            "Jace, Wielder of Mysteries"));
    /** Creatures the flashback of Dread Return sacrifices. */
    private static final int FLASHBACK_CREATURES = 3;

    /**
     * Whether the win can still be made after this play (the cast of the Spy or an activated outlet). True when no winner
     * is in hand (the other route is judged elsewhere), when one can be paid for together with the play, or when the
     * flashback of Dread Return is there to be paid with creatures.
     */
    static boolean castable(final Player ai, final ComboPlan plan, final SpellAbility play, final int extraCreatures) {
        if (ai == null || plan == null || play == null || play.getPayCosts() == null
                || !plan.has(ComboPlan.Kind.LIBRARY_OUT)) {
            return true;
        }
        boolean winnerInHand = false;
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            if (c == play.getHostCard() || c.isLand() || !WIN_CARDS.contains(c.getName())) {
                continue;
            }
            winnerInHand = true;
            try {
                final ManaCostBeingPaid total = new ManaCostBeingPaid(play.getPayCosts().getTotalMana());
                total.addManaCost(c.getManaCost());
                play.setActivatingPlayer(ai);
                if (ComputerUtilMana.canPayManaCost(total, play, ai, false)) {
                    return true;
                }
            } catch (final RuntimeException e) {
                return true; // cannot tell: the old rule stands
            }
        }
        if (!winnerInHand) {
            return true;
        }
        boolean recursion = false;
        for (final Card c : ai.getCardsIn(ZoneType.Graveyard)) {
            recursion |= AiCardLists.has(AiCardLists.Role.LIBRARY_OUT_RECURSION, c.getName());
        }
        return recursion && ai.getCreaturesInPlay().size() + extraCreatures >= FLASHBACK_CREATURES;
    }
}
