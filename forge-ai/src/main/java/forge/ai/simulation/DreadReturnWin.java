package forge.ai.simulation;

import java.util.List;

import forge.ai.ComputerUtilCost;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.cost.CostSacrifice;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Oops! All Spells: the library is gone (the Balustrade Spy milled it) and Thassa's Oracle is in the graveyard. The win
 * is the flashback of Dread Return, paid with three creatures and aimed at the Oracle: it enters with an empty library
 * and wins. The rollouts do not find it by themselves (three sacrifices look like a loss of board), so it is played
 * as soon as it can be, with the target set by hand.
 */
public final class DreadReturnWin {
    /** The Oracle wins while the library is no bigger than the devotion to blue it counts (two for its own {U}{U}). */
    private static final int LIBRARY_MAX = 2;

    private DreadReturnWin() {
    }

    /** A creature that wins on the spot when it enters with an empty library. */
    static boolean isWinner(final Card c) {
        return c != null && c.isCreature() && AiCardLists.has(AiCardLists.Role.LIBRARY_OUT_PAYOFF, c.getName());
    }

    /** The flashback ability to play now, aimed at the winner in the graveyard, or null. */
    public static SpellAbility sureCast(final Player ai, final List<SpellAbility> candidates) {
        if (ai == null || candidates == null || candidates.isEmpty()) {
            return null;
        }
        final Game game = ai.getGame();
        final PhaseHandler ph = game.getPhaseHandler();
        if (!ph.isPlayerTurn(ai) || !game.getStack().isEmpty() || !(ph.is(PhaseType.MAIN1) || ph.is(PhaseType.MAIN2))
                || ai.getCardsIn(ZoneType.Library).size() > LIBRARY_MAX) {
            return null;
        }
        Card winner = null;
        final CardCollectionView yard = ai.getCardsIn(ZoneType.Graveyard);
        for (final Card c : yard) {
            if (isWinner(c)) {
                winner = c;
                break;
            }
        }
        if (winner == null) {
            return null;
        }
        for (final SpellAbility sa : candidates) {
            final Card host = sa == null ? null : sa.getHostCard();
            if (host == null || !sa.isSpell() || !host.isInZone(ZoneType.Graveyard) || !ai.equals(host.getOwner())
                    || sa.getApi() != ApiType.ChangeZone || !"Graveyard".equals(sa.getParamOrDefault("Origin", ""))
                    || !"Battlefield".equals(sa.getParamOrDefault("Destination", "")) || !sa.usesTargeting()
                    || sa.getPayCosts() == null || !sa.getPayCosts().hasSpecificCostType(CostSacrifice.class)
                    || !AiCardLists.has(AiCardLists.Role.LIBRARY_OUT_RECURSION, host.getName())) {
                continue;
            }
            sa.setActivatingPlayer(ai);
            sa.resetTargets();
            if (sa.canTarget(winner) && sa.getTargets().add(winner) && sa.canPlay()
                    && ComputerUtilCost.canPayCost(sa, ai, false)) {
                return sa;
            }
            sa.resetTargets();
        }
        return null;
    }
}
