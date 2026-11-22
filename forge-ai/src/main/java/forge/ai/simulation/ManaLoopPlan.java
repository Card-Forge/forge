package forge.ai.simulation;

import forge.ai.RunawayLoop;
import forge.card.mana.ManaCost;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.cost.CostAdjustment;
import forge.game.cost.CostTap;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.List;
import java.util.function.Predicate;

/**
 * Untap loops that make mana: a permanent that taps itself for mana and has a paid ability that untaps itself (Basalt
 * Monolith, Grim Monolith) gives more than it costs once something cuts the price of the untap or adds to the mana.
 * <ul>
 * <li><b>Price.</b> The untap is priced as the game prices it: Power Artifact ({2} less) and Zirda ({2} less) take the
 * cost down, to one mana at least.</li>
 * <li><b>Yield.</b> What the tap makes, and one more for each Kinnan, Bonder Prodigy (a nonland permanent that is tapped
 * for mana adds one mana of the type that it made).</li>
 * <li><b>Net.</b> A cycle is a tap and an untap; it is gone round only when the net is positive. Basalt Monolith alone
 * (three for three) and Grim Monolith alone (three for four) make nothing, and are left alone.</li>
 * <li><b>Goal.</b> Only with something to spend the mana on (a spell in the hand that costs more than the AI has, an X
 * spell, a Ballista in play), and only until the pool holds what the hand costs: the loop is not run for its own sake.
 * Colourless mana only; a spell that needs a colour is paid by the lands, the loop pays the rest.</li>
 * </ul>
 * One step at a time, like the other plans: the tap while the pool is short and the permanent is untapped, the untap
 * once the pool can pay it.
 */
public final class ManaLoopPlan {
    private static final int MIN_GOAL = 12;
    private static final int LIMIT = 60;

    private ManaLoopPlan() {
    }

    /** The ability of the card of that API that has no target and untaps or taps the card itself, or null. */
    private static SpellAbility own(final List<SpellAbility> candidates, final Card host, final ApiType api, final boolean mana) {
        for (final SpellAbility sa : candidates) {
            if (sa == null || sa.getHostCard() != host || !sa.isActivatedAbility() || sa.isManaAbility() != mana) {
                continue;
            }
            if (mana ? sa.getApi() == ApiType.Mana : sa.getApi() == api && !sa.usesTargeting()) {
                return sa;
            }
        }
        return null;
    }

    private static int kinnans(final Player ai) {
        int n = 0;
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (c.getName().equals("Kinnan, Bonder Prodigy")) {
                n++;
            }
        }
        return n;
    }

    /** The mana of the tap, with Kinnan's extra mana for a nonland permanent. */
    public static int produced(final Player ai, final Card host, final SpellAbility tap) {
        int amount;
        try {
            amount = AbilityUtils.calculateAmount(host, tap.getParamOrDefault("Amount", "1"), tap);
        } catch (final RuntimeException e) {
            return 0;
        }
        return amount + (host.isLand() ? 0 : kinnans(ai));
    }

    /** What the untap costs the AI now, reductions in. */
    public static int untapCost(final Player ai, final SpellAbility untap) {
        final ManaCost base = untap.getPayCosts() == null ? ManaCost.ZERO : untap.getPayCosts().getTotalMana();
        if (base == null) {
            return 0;
        }
        final ManaCostBeingPaid paid = new ManaCostBeingPaid(base);
        untap.setActivatingPlayer(ai);
        try {
            CostAdjustment.adjust(paid, untap, ai, null, true, false);
        } catch (final RuntimeException e) {
            return base.getCMC();
        }
        return paid.getConvertedManaCost();
    }

    /** Whether the untap pays for something other than mana (a sacrifice, life): then it is no loop of ours. */
    private static boolean manaOnly(final SpellAbility untap) {
        return untap.getPayCosts() != null && untap.getPayCosts().getCostParts().stream()
                .allMatch(p -> p instanceof forge.game.cost.CostPartMana);
    }

    /** The colourless mana the loop is run for: what the hand costs, or enough for an X spell to finish the opponent. */
    public static int goal(final Player ai) {
        int sum = 0;
        boolean x = false;
        boolean spell = false;
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            if (c.isLand()) {
                continue;
            }
            spell = true;
            sum += c.getCMC();
            x |= c.getManaCost() != null && c.getManaCost().countX() > 0;
        }
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            x |= c.getName().equals("Walking Ballista");
        }
        if (!spell && !x) {
            return 0;
        }
        int goal = Math.min(40, sum);
        if (x) {
            int life = 0;
            for (final Player opp : ai.getOpponents()) {
                life = Math.max(life, opp.getLife());
            }
            goal = Math.max(goal, Math.max(MIN_GOAL, life + 2));
        }
        return goal;
    }

    /** The next step of a loop that is in play and positive, or null. */
    static SpellAbility next(final Player ai, final List<SpellAbility> candidates, final Predicate<SpellAbility> approve) {
        final forge.game.phase.PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (!ph.isPlayerTurn(ai) || !ai.getGame().getStack().isEmpty()
                || !(ph.is(PhaseType.MAIN1) || ph.is(PhaseType.MAIN2))) {
            return null;
        }
        final int goal = goal(ai);
        if (goal <= 0 || ai.getManaPool().totalMana() >= goal) {
            return null;
        }
        for (final Card host : ai.getCardsIn(ZoneType.Battlefield)) {
            if (host.isLand()) {
                continue;
            }
            final SpellAbility tap = own(candidates, host, ApiType.Mana, true);
            final SpellAbility untap = own(candidates, host, ApiType.Untap, false);
            if (tap == null && untap == null) {
                continue;
            }
            final SpellAbility anyUntap = untap != null ? untap : findUntap(host);
            if (anyUntap == null || !manaOnly(anyUntap) || !"Self".equals(anyUntap.getParamOrDefault("Defined", "Self"))) {
                continue;
            }
            final SpellAbility anyTap = tap != null ? tap : findTap(host);
            if (anyTap == null || !anyTap.getPayCosts().hasSpecificCostType(CostTap.class)) {
                continue;
            }
            final int cost = untapCost(ai, anyUntap);
            if (produced(ai, host, anyTap) <= cost) {
                continue; // no net mana: a cycle makes nothing
            }
            if (host.isTapped()) {
                if (untap != null && ai.getManaPool().totalMana() >= cost && !RunawayLoop.exceeded(untap, LIMIT)) {
                    untap.setActivatingPlayer(ai);
                    untap.resetTargets();
                    if (approve.test(untap)) {
                        return untap;
                    }
                }
            } else if (tap != null && !RunawayLoop.exceeded(tap, LIMIT)) {
                tap.setActivatingPlayer(ai);
                if (approve.test(tap)) {
                    VetoLog.note(ai, "mana-loop | " + host.getName() + " pool " + ai.getManaPool().totalMana() + " of " + goal);
                    return tap;
                }
            }
        }
        return null;
    }

    private static SpellAbility findUntap(final Card host) {
        for (final SpellAbility sa : host.getSpellAbilities()) {
            if (sa.isActivatedAbility() && sa.getApi() == ApiType.Untap && !sa.usesTargeting()) {
                return sa;
            }
        }
        return null;
    }

    private static SpellAbility findTap(final Card host) {
        for (final SpellAbility sa : host.getSpellAbilities()) {
            if (sa.isActivatedAbility() && sa.getApi() == ApiType.Mana && sa.getPayCosts() != null) {
                return sa;
            }
        }
        return null;
    }
}
