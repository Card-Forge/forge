package forge.ai.simulation;

import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.List;
import java.util.function.Predicate;

/**
 * Trigger chains that go on by themselves once they are started: what is left to the AI is to put the pieces in the
 * right place and to start them.
 * <ul>
 * <li><b>Niv-Mizzet with Curiosity.</b> Niv-Mizzet pings when its controller draws, and Curiosity (or Ophidian Eye) on it draws a
 * card when it damages an opponent: each ping is a draw and each draw a ping. The aura is cast on Niv-Mizzet, not on any
 * other creature; with it on, an instant or a sorcery from the hand starts the chain (Niv-Mizzet draws a card for it).</li>
 * <li><b>Exquisite Blood with Sanguine Bond.</b> The opponent's lost life is gained by the AI, and the life it gains is lost by
 * the opponent, and so on. One life lost by the opponent, or gained by the AI, starts it: a spell or ability of the AI's with
 * that effect (a drain) goes first.</li>
 * </ul>
 */
public final class TriggerLoopPlan {
    private TriggerLoopPlan() {
    }

    private static Card inPlayAny(final Player ai, final String... names) {
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            for (final String n : names) {
                if (c.getName().equals(n)) {
                    return c;
                }
            }
        }
        return null;
    }

    static SpellAbility next(final Player ai, final List<SpellAbility> candidates, final Predicate<SpellAbility> approve) {
        final forge.game.phase.PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (!ph.isPlayerTurn(ai) || !ai.getGame().getStack().isEmpty()
                || !(ph.is(PhaseType.MAIN1) || ph.is(PhaseType.MAIN2))) {
            return null;
        }
        final SpellAbility niv = nivMizzet(ai, candidates, approve);
        return niv != null ? niv : bloodAndBond(ai, candidates, approve);
    }

    private static SpellAbility nivMizzet(final Player ai, final List<SpellAbility> candidates, final Predicate<SpellAbility> approve) {
        final Card niv = inPlayAny(ai, "Niv-Mizzet, Parun", "Niv-Mizzet, the Firemind");
        if (niv == null) {
            return null;
        }
        final boolean enchanted = niv.getEnchantedBy().stream()
                .anyMatch(a -> a.getName().equals("Curiosity") || a.getName().equals("Ophidian Eye"));
        if (!enchanted) {
            for (final String aura : new String[] {"Curiosity", "Ophidian Eye"}) {
                final SpellAbility sa = LoopRunner.castFromHand(ai, candidates, aura);
                if (sa == null || !forge.ai.ComputerUtilMana.canPayManaCost(sa, ai, 0, false)) {
                    continue;
                }
                sa.setActivatingPlayer(ai);
                sa.resetTargets();
                if (!sa.canTarget(niv)) {
                    continue;
                }
                sa.getTargets().add(niv);
                if (approve.test(sa)) {
                    VetoLog.note(ai, "trigger-loop | " + aura + " goes on " + niv.getName());
                    return sa;
                }
            }
            return null;
        }
        // started by any instant or sorcery: the cheapest one the mana pays for
        SpellAbility cheapest = null;
        for (final SpellAbility sa : candidates) {
            final Card host = sa == null ? null : sa.getHostCard();
            if (host == null || !sa.isSpell() || !host.isInZone(ZoneType.Hand) || !(host.isInstant() || host.isSorcery())
                    || !ai.equals(host.getController()) || sa.usesTargeting() || !forge.ai.ComputerUtilMana.canPayManaCost(sa, ai, 0, false)) {
                continue;
            }
            if (cheapest == null || host.getCMC() < cheapest.getHostCard().getCMC()) {
                cheapest = sa;
            }
        }
        if (cheapest != null && !started(ai)) {
            cheapest.setActivatingPlayer(ai);
            if (approve.test(cheapest)) {
                VetoLog.note(ai, "trigger-loop | " + cheapest.getHostCard().getName() + " starts " + niv.getName());
                return cheapest;
            }
        }
        return null;
    }

    /** Whether an opponent has lost life this turn: the chain is going, or the AI has hit already. */
    private static boolean started(final Player ai) {
        for (final Player opp : ai.getOpponents()) {
            if (opp.getLifeLostThisTurn() > 0) {
                return true;
            }
        }
        return false;
    }

    private static SpellAbility bloodAndBond(final Player ai, final List<SpellAbility> candidates, final Predicate<SpellAbility> approve) {
        if (inPlayAny(ai, "Exquisite Blood") == null || inPlayAny(ai, "Sanguine Bond") == null) {
            return null;
        }
        for (final Player opp : ai.getOpponents()) {
            if (opp.getLifeLostThisTurn() > 0) {
                return null; // it has started
            }
        }
        SpellAbility best = null;
        for (final SpellAbility sa : candidates) {
            final Card host = sa == null ? null : sa.getHostCard();
            if (host == null || host.isLand() || !ai.equals(host.getController()) || sa.isManaAbility()
                    || !(sa.isSpell() ? host.isInZone(ZoneType.Hand) : host.isInZone(ZoneType.Battlefield))) {
                continue;
            }
            boolean drains = false;
            SpellAbility part = sa;
            while (part != null) {
                drains |= part.getApi() == ApiType.GainLife || part.getApi() == ApiType.LoseLife;
                part = part.getSubAbility();
            }
            if (!drains || !forge.ai.ComputerUtilMana.canPayManaCost(sa, ai, 0, false)) {
                continue;
            }
            if (best == null || host.getCMC() < best.getHostCard().getCMC()) {
                sa.setActivatingPlayer(ai);
                sa.resetTargets();
                boolean aimed = !sa.usesTargeting();
                for (final Player opp : ai.getOpponents()) {
                    if (sa.usesTargeting() && sa.canTarget(opp)) {
                        sa.getTargets().add(opp);
                        aimed = true;
                        break;
                    }
                }
                if (aimed) {
                    best = sa;
                }
            }
        }
        if (best != null && approve.test(best)) {
            VetoLog.note(ai, "trigger-loop | " + best.getHostCard().getName() + " starts the Blood and Bond");
            return best;
        }
        return null;
    }
}
