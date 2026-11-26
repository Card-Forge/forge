package forge.ai.simulation;

import forge.ai.RunawayLoop;
import forge.game.card.Card;
import forge.game.cost.CostDiscard;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.List;
import java.util.function.Predicate;

/**
 * Underworld Breach with Brain Freeze. Breach gives every nonland card in the graveyard escape (its mana cost and three
 * other cards of the graveyard exiled), and it is sacrificed at the end step, so it all happens in one turn: Breach
 * first, the Brain Freeze from the hand, the hand into the graveyard (Lion's Eye Diamond discards it, for three mana
 * as well), and then the Brain Freeze again from the graveyard for every three other cards there. Every cast is a copy
 * more through storm and three cards more milled; it ends when the opponent's library is empty or the graveyard has
 * too little left to exile.
 * <ul>
 * <li><b>Start.</b> Breach in the hand, Brain Freeze in the hand, and fuel for an escape (three cards of the hand and
 * the graveyard besides the Freeze and the Breach) with the Diamond to turn the hand into fuel, or fuel in the graveyard.</li>
 * <li><b>Order.</b> Breach, the Freeze from the hand, the Diamond, then the Freeze from the graveyard.</li>
 * </ul>
 * Only against an opponent that is not out of cards already.
 */
public final class BreachPlan {
    private static final String BREACH = "Underworld Breach";
    private static final String FREEZE = "Brain Freeze";
    private static final String DIAMOND = "Lion's Eye Diamond";
    private static final int LIMIT = 60;

    private BreachPlan() {
    }

    private static Player target(final Player ai) {
        for (final Player opp : ai.getOpponents()) {
            if (opp.getLife() > 0 && !opp.getCardsIn(ZoneType.Library).isEmpty()) {
                return opp;
            }
        }
        return null;
    }

    private static int others(final Player ai, final ZoneType zone) {
        int n = 0;
        for (final Card c : ai.getCardsIn(zone)) {
            if (!c.getName().equals(FREEZE) && !c.getName().equals(BREACH)) {
                n++;
            }
        }
        return n;
    }

    private static boolean has(final Player ai, final ZoneType zone, final String name) {
        for (final Card c : ai.getCardsIn(zone)) {
            if (c.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static SpellAbility aimed(final Player ai, final SpellAbility sa, final Player opp, final Predicate<SpellAbility> approve) {
        sa.setActivatingPlayer(ai);
        sa.resetTargets();
        if (!sa.canTarget(opp)) {
            return null;
        }
        sa.getTargets().add(opp);
        return approve.test(sa) ? sa : null;
    }

    static SpellAbility next(final Player ai, final List<SpellAbility> candidates, final Predicate<SpellAbility> approve) {
        final forge.game.phase.PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (!ph.isPlayerTurn(ai) || !ai.getGame().getStack().isEmpty()
                || !(ph.is(PhaseType.MAIN1) || ph.is(PhaseType.MAIN2))) {
            return null;
        }
        final Player opp = target(ai);
        final boolean breachInPlay = has(ai, ZoneType.Battlefield, BREACH);
        if (opp == null || !(breachInPlay || LoopRunner.inHand(ai, BREACH))
                || !(LoopRunner.inHand(ai, FREEZE) || has(ai, ZoneType.Graveyard, FREEZE))) {
            return null;
        }
        final boolean diamond = has(ai, ZoneType.Battlefield, DIAMOND);
        final int fuel = others(ai, ZoneType.Graveyard) + (diamond ? others(ai, ZoneType.Hand) : 0);
        if (!breachInPlay) {
            final SpellAbility breach = LoopRunner.castFromHand(ai, candidates, BREACH);
            if (fuel < 3 || breach == null || !forge.ai.ComputerUtilMana.canPayManaCost(breach, ai, 0, false)) {
                return null;
            }
            breach.setActivatingPlayer(ai);
            if (approve.test(breach)) {
                VetoLog.note(ai, "breach-plan | Breach, fuel " + fuel);
                return breach;
            }
            return null;
        }
        final SpellAbility fromHand = LoopRunner.castFromHand(ai, candidates, FREEZE);
        if (fromHand != null && forge.ai.ComputerUtilMana.canPayManaCost(fromHand, ai, 0, false)) {
            final SpellAbility cast = aimed(ai, fromHand, opp, approve);
            if (cast != null) {
                return cast;
            }
        }
        if (diamond && others(ai, ZoneType.Hand) > 0) {
            for (final SpellAbility sa : candidates) {
                if (sa != null && sa.isManaAbility() && sa.getHostCard() != null && sa.getHostCard().getName().equals(DIAMOND)
                        && sa.getPayCosts() != null && sa.getPayCosts().hasSpecificCostType(CostDiscard.class)) {
                    sa.setActivatingPlayer(ai);
                    if (approve.test(sa)) {
                        VetoLog.note(ai, "breach-plan | the Diamond turns the hand into fuel");
                        return sa;
                    }
                }
            }
        }
        if (others(ai, ZoneType.Graveyard) >= 3) {
            for (final SpellAbility sa : candidates) {
                final Card host = sa == null ? null : sa.getHostCard();
                if (host != null && sa.isSpell() && host.getName().equals(FREEZE) && host.isInZone(ZoneType.Graveyard)
                        && !RunawayLoop.exceeded(sa, LIMIT) && forge.ai.ComputerUtilMana.canPayManaCost(sa, ai, 0, false)) {
                    final SpellAbility cast = aimed(ai, sa, opp, approve);
                    if (cast != null) {
                        VetoLog.note(ai, "breach-plan | Brain Freeze escapes, fuel " + others(ai, ZoneType.Graveyard));
                        return cast;
                    }
                }
            }
        }
        return null;
    }
}
