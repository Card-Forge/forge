package forge.ai.simulation;

import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
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

    /**
     * What had already happened when the pieces of a combo were all in play, per game and turn: the first time the planner sees
     * both of them. Whatever the opponent lost or Niv-Mizzet dealt before that is not the combo, and the turn's totals cannot tell
     * the two apart. Only a change after this number means that the chain has begun.
     */
    private static final Map<Game, Map<String, Integer>> AT_COMPLETION = new WeakHashMap<>();

    private static int sinceCompletion(final Player ai, final String key, final int total) {
        final Game game = ai.getGame();
        final String turnKey = game.getPhaseHandler().getTurn() + ":" + key;
        synchronized (AT_COMPLETION) {
            final Map<String, Integer> turns = AT_COMPLETION.computeIfAbsent(game, g -> new java.util.HashMap<>());
            final Integer before = turns.putIfAbsent(turnKey, total);
            return total - (before == null ? total : before);
        }
    }

    static SpellAbility next(final Player ai, final List<SpellAbility> candidates, final Predicate<SpellAbility> approve) {
        final forge.game.phase.PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (!ph.isPlayerTurn(ai) || !ai.getGame().getStack().isEmpty()
                || !(ph.is(PhaseType.MAIN1) || ph.is(PhaseType.MAIN2))) {
            return null;
        }
        final SpellAbility niv = nivMizzet(ai, candidates, approve);
        if (niv != null) {
            return niv;
        }
        final SpellAbility bond = bloodAndBond(ai, candidates, approve);
        return bond != null ? bond : guildmageAndMindcrank(ai, candidates, approve);
    }

    /**
     * Duskmantle Guildmage with Mindcrank: with the Guildmage's first ability on, a card that goes to the opponent's
     * graveyard costs one life, and Mindcrank mills that many cards for the life: a card is a life and a life is a card, to
     * the end. The mill ({2}{U}{B}) starts it, after the first ability ({1}{U}{B}), so seven mana go in, and the AI starts
     * only when it has them. The first ability is told by its count this turn.
     */
    private static SpellAbility guildmageAndMindcrank(final Player ai, final List<SpellAbility> candidates,
                                                      final Predicate<SpellAbility> approve) {
        final Card guildmage = inPlayAny(ai, "Duskmantle Guildmage");
        final Card crank = inPlayAny(ai, "Mindcrank");
        if (guildmage == null || crank == null) {
            return null;
        }
        Player opp = null;
        for (final Player p : ai.getOpponents()) {
            if (p.getLife() > 0 && !p.getCardsIn(ZoneType.Library).isEmpty()) {
                opp = p;
                break;
            }
        }
        final SpellAbility effect = LoopRunner.activated(candidates, guildmage, ApiType.Effect);
        final SpellAbility mill = LoopRunner.activated(candidates, guildmage, ApiType.Mill);
        if (opp == null || effect == null || mill == null
                || forge.ai.ComputerUtilMana.getAvailableManaEstimate(ai) < 7) {
            return null;
        }
        if (effect.getActivationsThisTurn() == 0) {
            effect.setActivatingPlayer(ai);
            effect.resetTargets();
            if (!approve.test(effect)) {
                return null;
            }
            VetoLog.note(ai, "guildmage-loop | the Guildmage's cards-to-the-graveyard ability first");
            return effect;
        }
        if (mill.getActivationsThisTurn() > 0) {
            return null; // the chain is running (or has run its course): one mill starts it, not two
        }
        final SpellAbility aimed = LoopRunner.aimedAtPlayer(ai, mill, opp);
        if (aimed == null || !approve.test(aimed)) {
            return null;
        }
        VetoLog.note(ai, "guildmage-loop | the Guildmage mills the opponent, Mindcrank takes it from there");
        return aimed;
    }

    private static SpellAbility nivMizzet(final Player ai, final List<SpellAbility> candidates, final Predicate<SpellAbility> approve) {
        final Card niv = inPlayAny(ai, "Niv-Mizzet, Parun", "Niv-Mizzet, the Firemind");
        if (niv == null) {
            return null;
        }
        final boolean enchanted = niv.getEnchantedBy().stream()
                .anyMatch(a -> a.getName().equals("Curiosity") || a.getName().equals("Ophidian Eye"));
        // seen as soon as the aura is on, so that what Niv-Mizzet did before it is not counted as the chain
        final boolean running = enchanted && started(ai, niv);
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
        if (cheapest != null && !running) {
            cheapest.setActivatingPlayer(ai);
            if (approve.test(cheapest)) {
                VetoLog.note(ai, "trigger-loop | " + cheapest.getHostCard().getName() + " starts " + niv.getName());
                return cheapest;
            }
        }
        return null;
    }

    /**
     * Whether Niv-Mizzet has hit an opponent since the aura is on it: the chain is going. Damage that it dealt before the aura came
     * (an instant that pinged, then the Curiosity) is not the chain, and neither is life that was lost some other way.
     */
    private static boolean started(final Player ai, final Card niv) {
        final int dealt = niv.getDamageHistory().getDamageDoneThisTurn(null, false, null, "Player.Opponent", niv, ai, null);
        return sinceCompletion(ai, "niv:" + niv.getId(), dealt) > 0;
    }

    /**
     * Whether this part of an ability makes the AI gain life or an opponent lose it, which is what Exquisite Blood and
     * Sanguine Bond feed on. Life that an opponent gains (Devour Flesh) or that the AI loses does not start them.
     */
    public static boolean makesTheAiGainOrAnOpponentLose(final SpellAbility part) {
        final String who = part.getParamOrDefault("Defined", "");
        if (part.getApi() == ApiType.GainLife) {
            return !part.usesTargeting() && (who.isEmpty() || "You".equals(who));
        }
        if (part.getApi() == ApiType.LoseLife) {
            return part.usesTargeting() || who.contains("Opponent") || who.startsWith("Targeted");
        }
        return false;
    }

    private static SpellAbility bloodAndBond(final Player ai, final List<SpellAbility> candidates, final Predicate<SpellAbility> approve) {
        if (inPlayAny(ai, "Exquisite Blood") == null || inPlayAny(ai, "Sanguine Bond") == null) {
            return null;
        }
        final Card blood = inPlayAny(ai, "Exquisite Blood");
        final Card bond = inPlayAny(ai, "Sanguine Bond");
        int drained = ai.getLifeGainedThisTurn();
        for (final Player opp : ai.getOpponents()) {
            drained += opp.getLifeLostThisTurn();
        }
        // life lost by the opponent (or gained by the AI) after both pieces are in play has started it; before, it did not
        if (sinceCompletion(ai, "blood:" + blood.getId() + ":" + bond.getId(), drained) > 0) {
            return null; // it has started
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
                drains |= makesTheAiGainOrAnOpponentLose(part);
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
