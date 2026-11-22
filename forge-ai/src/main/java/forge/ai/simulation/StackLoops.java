package forge.ai.simulation;

import forge.ai.RunawayLoop;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.cost.CostTap;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.List;
import java.util.function.Predicate;

/**
 * Loops that are played with something on the stack.
 * <ul>
 * <li><b>Basalt Monolith with Rings of Brighthearth.</b> Tap the Monolith for {C}{C}{C}, pay {3} to untap it, and Rings of
 * Brighthearth copies the untap for {2}. The copy resolves first and untaps the Monolith: with the original untap still on the
 * stack it is tapped for {C}{C}{C} again, and the original untaps it once more. A round is six mana in and five out: one over.
 * The steps are two at an empty stack (the tap; the untap, with the pool and the lands to pay both the {3} and the {2}) and
 * one with the original untap on the stack and the Monolith untapped: {@link #response}.</li>
 * </ul>
 */
public final class StackLoops {
    private static final int LIMIT = 80;
    private static final int RINGS = 2;

    private StackLoops() {
    }

    private static Card ringsOf(final Player ai) {
        return LoopRunner.inPlay(ai, "Rings of Brighthearth");
    }

    /** The paid untap of the card itself, with no target. */
    private static SpellAbility untapOf(final Iterable<SpellAbility> candidates, final Card host) {
        for (final SpellAbility sa : candidates) {
            if (sa != null && sa.getHostCard() == host && sa.isActivatedAbility() && !sa.isManaAbility()
                    && sa.getApi() == ApiType.Untap && !sa.usesTargeting()
                    && "Self".equals(sa.getParamOrDefault("Defined", "Self"))) {
                return sa;
            }
        }
        return null;
    }

    /** The tap for mana of the card, which costs only the tap. */
    private static SpellAbility tapOf(final Iterable<SpellAbility> candidates, final Card host) {
        for (final SpellAbility sa : candidates) {
            if (sa != null && sa.getHostCard() == host && sa.isManaAbility() && sa.getApi() == ApiType.Mana
                    && sa.getPayCosts() != null && sa.getPayCosts().hasSpecificCostType(CostTap.class)
                    && sa.getPayCosts().getCostMana() == null) {
                return sa;
            }
        }
        return null;
    }

    /** A round makes this many mana over, with the Rings: two taps, the untap and the copy. Zero or less goes nowhere. */
    private static int roundNet(final Player ai, final Card host, final SpellAbility tap, final SpellAbility untap) {
        return 2 * ManaLoopPlan.produced(ai, host, tap) - ManaLoopPlan.untapCost(ai, untap) - RINGS;
    }

    /**
     * Whether the untap that is copied is the loop's: a permanent that taps for more than its untap costs
     * is the loop of {@link ManaLoopPlan}, and one that does not is this loop's (with the Rings it pays).
     */
    public static boolean copyIsTheLoop(final Player ai, final SpellAbility copied) {
        final Card host = copied == null ? null : copied.getHostCard();
        if (host == null || copied.getApi() != ApiType.Untap || ringsOf(ai) == null || !host.isTapped()
                || ManaLoopPlan.goal(ai) <= 0) {
            return false;
        }
        final SpellAbility tap = tapOf(host.getSpellAbilities(), host);
        return tap != null && roundNet(ai, host, tap, copied) > 0;
    }

    /** The tap and the untap at an empty stack. */
    static SpellAbility next(final Player ai, final List<SpellAbility> candidates, final Predicate<SpellAbility> approve) {
        final forge.game.phase.PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (!ph.isPlayerTurn(ai) || !ai.getGame().getStack().isEmpty()
                || !(ph.is(forge.game.phase.PhaseType.MAIN1) || ph.is(forge.game.phase.PhaseType.MAIN2))) {
            return null;
        }
        final int goal = ManaLoopPlan.goal(ai);
        if (ringsOf(ai) == null || goal <= 0 || ai.getManaPool().totalMana() >= goal) {
            return null;
        }
        for (final Card host : ai.getCardsIn(ZoneType.Battlefield)) {
            if (host.isLand() || host.isCreature() && host.isSick()) {
                continue;
            }
            final SpellAbility tap = tapOf(candidates, host);
            final SpellAbility untap = untapOf(candidates, host);
            if (tap == null || untap == null || roundNet(ai, host, tap, untap) <= 0) {
                continue;
            }
            final int cost = ManaLoopPlan.untapCost(ai, untap);
            if (ManaLoopPlan.produced(ai, host, tap) > cost) {
                continue; // it makes mana on its own: the plan of the untap loops has it
            }
            if (!host.isTapped()) {
                tap.setActivatingPlayer(ai);
                if (!RunawayLoop.exceeded(tap, LIMIT) && approve.test(tap)) {
                    VetoLog.note(ai, "rings-loop | " + host.getName() + " taps, pool " + ai.getManaPool().totalMana()
                            + " of " + goal);
                    return tap;
                }
                continue;
            }
            int lands = 0;
            for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
                lands += c.isLand() && !c.isTapped() && !c.getManaAbilities().isEmpty() ? 1 : 0;
            }
            if (ai.getManaPool().totalMana() + lands >= cost + RINGS && !RunawayLoop.exceeded(untap, LIMIT)) {
                untap.setActivatingPlayer(ai);
                untap.resetTargets();
                if (approve.test(untap)) {
                    VetoLog.note(ai, "rings-loop | " + host.getName() + " untaps, copied by the Rings");
                    return untap;
                }
            }
        }
        return null;
    }

    /**
     * With the AI's own untap of a Monolith on the stack, the Monolith untapped (the copy has resolved): the tap for mana,
     * before the original untap resolves.
     */
    public static SpellAbility response(final Player ai, final SpellAbility top, final List<SpellAbility> candidates,
                                        final Predicate<SpellAbility> approve) {
        if (top == null || !top.isActivatedAbility() || top.getApi() != ApiType.Untap || !ai.equals(top.getActivatingPlayer())
                || ringsOf(ai) == null) {
            return null;
        }
        final Card host = top.getHostCard();
        if (host == null || host.isTapped() || !ai.equals(host.getController()) || ManaLoopPlan.goal(ai) <= 0) {
            return null;
        }
        final SpellAbility tap = tapOf(candidates, host);
        if (tap == null || roundNet(ai, host, tap, top) <= 0 || RunawayLoop.exceeded(tap, LIMIT)) {
            return null;
        }
        tap.setActivatingPlayer(ai);
        if (!approve.test(tap)) {
            return null;
        }
        VetoLog.note(ai, "rings-loop | " + host.getName() + " taps again with its untap on the stack");
        return tap;
    }
}
