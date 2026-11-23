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

    /** Whether a -1/-1 counter put on the creature is no counter: Melira stops it, a Vizier of Remedies takes one off. */
    public static boolean counterIsFree(final Player ai, final Card host, final int amount) {
        try {
            if (!host.canReceiveCounters(forge.game.card.CounterEnumType.M1M1, null)) {
                return true;
            }
        } catch (final RuntimeException e) {
            return false;
        }
        if (amount > 1) {
            return false;
        }
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (c.getName().equals("Vizier of Remedies") && !host.equals(c)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether the untap costs mana only, or a -1/-1 counter that does not stay on the creature (Devoted Druid with a
     * Melira or a Vizier of Remedies): a cost that is paid for nothing.
     */
    private static boolean payable(final Player ai, final Card host, final SpellAbility untap) {
        if (untap.getPayCosts() == null) {
            return false;
        }
        return untap.getPayCosts().getCostParts().stream().allMatch(p -> {
            if (p instanceof forge.game.cost.CostPartMana) {
                return true;
            }
            if (p instanceof forge.game.cost.CostPutCounter) {
                final forge.game.cost.CostPutCounter c = (forge.game.cost.CostPutCounter) p;
                return c.getCounter().is(forge.game.card.CounterEnumType.M1M1)
                        && counterIsFree(ai, host, Math.max(1, p.convertAmount() == null ? 1 : p.convertAmount()));
            }
            return false;
        });
    }

    /** The colourless mana the loop is run for: what the hand costs, or enough for an X spell to finish the opponent. */
    public static int goal(final Player ai) {
        return goal(ai, null);
    }

    /** The goal without the hand cards of those names (separated by |): a spell that is a part of the loop is not what the mana is for. */
    public static int goal(final Player ai, final String ignore) {
        int sum = 0;
        boolean x = false;
        boolean spell = false;
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            if (c.isLand() || ignore != null && java.util.Arrays.asList(ignore.split("\\|")).contains(c.getName())) {
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
        final SpellAbility chain = foodChain(ai, candidates, approve);
        if (chain != null) {
            return chain;
        }
        final int goal = goal(ai);
        if (goal <= 0 || ai.getManaPool().totalMana() >= goal) {
            return null;
        }
        final SpellAbility tokens = echoes(ai, candidates, approve, goal);
        if (tokens != null) {
            return tokens;
        }
        for (final Card host : ai.getCardsIn(ZoneType.Battlefield)) {
            if (host.isLand() || host.isSick()) {
                continue;
            }
            final SpellAbility tap = own(candidates, host, ApiType.Mana, true);
            final SpellAbility untap = own(candidates, host, ApiType.Untap, false);
            if (tap == null && untap == null) {
                continue;
            }
            final SpellAbility anyUntap = untap != null ? untap : findUntap(host);
            if (anyUntap == null || !payable(ai, host, anyUntap) || !"Self".equals(anyUntap.getParamOrDefault("Defined", "Self"))) {
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

    /**
     * Mana Echoes: a creature that enters adds one colourless mana for each creature of yours that shares a type with it,
     * itself too. A Sliver Queen that makes a 1/1 Sliver for {2} gets two for the first, three for the second, and so on:
     * a cycle that pays for itself from the second token on. The tokens are made while the pool is short of the goal and
     * the mana is there to pay, lands or pool.
     */
    static SpellAbility echoes(final Player ai, final List<SpellAbility> candidates, final Predicate<SpellAbility> approve,
                               final int goal) {
        boolean echoes = false;
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            echoes |= c.getName().equals("Mana Echoes");
        }
        if (!echoes || ai.getManaPool().totalMana() >= goal) {
            return null;
        }
        for (final Card host : ai.getCardsIn(ZoneType.Battlefield)) {
            if (!host.isCreature()) {
                continue;
            }
            for (final SpellAbility sa : candidates) {
                if (sa == null || sa.getHostCard() != host || !sa.isActivatedAbility() || sa.getApi() != ApiType.Token
                        || sa.usesTargeting() || RunawayLoop.exceeded(sa, LIMIT)) {
                    continue;
                }
                final String script = sa.getParamOrDefault("TokenScript", "").toLowerCase();
                boolean shares = false;
                for (final String type : host.getType().getCreatureTypes()) {
                    shares |= script.contains(type.toLowerCase());
                }
                if (!shares) {
                    continue;
                }
                int sharing = 1; // the token itself
                for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
                    sharing += c.isCreature() && c.sharesCreatureTypeWith(host) ? 1 : 0;
                }
                sa.setActivatingPlayer(ai);
                final int cost = sa.getPayCosts() == null ? 0 : sa.getPayCosts().getTotalMana().getCMC();
                if (sharing >= cost && forge.ai.ComputerUtilMana.canPayManaCost(sa, ai, 0, false) && approve.test(sa)) {
                    VetoLog.note(ai, "mana-loop | " + host.getName() + " tokens with Mana Echoes, pool "
                            + ai.getManaPool().totalMana() + " of " + goal);
                    return sa;
                }
            }
        }
        return null;
    }

    /** The creature spells in the hand cost this much: what Food Chain's mana can pay for. */
    static int creatureGoal(final Player ai) {
        int sum = 0;
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            if (c.isCreature()) {
                sum += c.getCMC() + (c.getManaCost() != null && c.getManaCost().countX() > 0 ? 12 : 0);
            }
        }
        return Math.min(40, sum);
    }

    /** A creature that is cast from exile (Squee, Misthollow Griffin, Eternal Scourge): Food Chain loses it for nothing. */
    private static boolean castFromExile(final Card c) {
        for (final forge.game.staticability.StaticAbility st : c.getStaticAbilities()) {
            if (st.hasParam("MayPlay") && st.getParamOrDefault("AffectedZone", "").contains("Exile")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Food Chain: exile a creature, add one more mana than its cost for creature spells only. With a creature that can be
     * cast from exile the loop is a cast, an exile and one mana over, round and round, until the pool holds what the
     * creature spells in the hand cost. Run only when every creature the AI has is such a creature, so that the exile is
     * never the Ballista or the finisher.
     */
    static SpellAbility foodChain(final Player ai, final List<SpellAbility> candidates, final Predicate<SpellAbility> approve) {
        final int goal = creatureGoal(ai);
        if (goal <= 0 || ai.getManaPool().totalMana() >= goal) {
            return null;
        }
        boolean chain = false;
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            chain |= c.getName().equals("Food Chain");
        }
        if (!chain) {
            return null;
        }
        Card fuel = null;
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (c.isCreature()) {
                if (!castFromExile(c)) {
                    return null;
                }
                fuel = c;
            }
        }
        if (fuel != null) {
            for (final SpellAbility sa : candidates) {
                if (sa != null && sa.getApi() == ApiType.Mana && sa.getHostCard() != null
                        && sa.getHostCard().getName().equals("Food Chain") && !RunawayLoop.exceeded(sa, LIMIT)) {
                    sa.setActivatingPlayer(ai);
                    if (approve.test(sa)) {
                        VetoLog.note(ai, "mana-loop | Food Chain exiles " + fuel.getName() + ", pool "
                                + ai.getManaPool().totalMana() + " of " + goal);
                        return sa;
                    }
                }
            }
            return null;
        }
        for (final SpellAbility sa : candidates) {
            final Card host = sa == null ? null : sa.getHostCard();
            if (host != null && sa.isSpell() && castFromExile(host)
                    && (host.isInZone(ZoneType.Exile) || host.isInZone(ZoneType.Graveyard))) {
                sa.setActivatingPlayer(ai);
                if (forge.ai.ComputerUtilMana.canPayManaCost(sa, ai, 0, false) && approve.test(sa)) {
                    return sa;
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
