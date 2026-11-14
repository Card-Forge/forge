package forge.ai.simulation;

import java.util.List;

import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilMana;
import forge.game.GameObject;
import forge.game.ability.ApiType;
import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CounterEnumType;
import forge.game.cost.CostPart;
import forge.game.cost.CostRemoveCounter;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * The X of a spell or ability in the rollouts. The simulation always paid the most X it could ("TODO iterate over all
 * possible values"); this picks a better one where the most is wasteful or wrong:
 * <ul>
 *   <li><b>Chalice of the Void</b> (X X): the number of charge counters counters every spell of that mana value, the AI's
 *       own too. X is the value of 0, 1 or 2 that hits the fewest of the AI's own spells in hand and library (ties: 1,
 *       then 2, then 0), never the most mana it could pay.</li>
 *   <li><b>X damage</b> (Fireball, Blaze, Banefire): use a useful single lethal target; do not add unrelated creatures and
 *       player life totals together, so Channel does not spend life to pay for damage that cannot all hit one target.</li>
 *   <li><b>X damage to each player</b> (Earthquake, Hurricane, Crypt Rats): never so much that it kills the AI.</li>
 *   <li><b>Discard X</b> (Mind Twist): never more than the cards in the biggest opposing hand.</li>
 * </ul>
 * Any other cost keeps the value {@code ComputerUtilCost.setMaxXValue} found.
 */
public final class XChoice {
    private XChoice() {
    }

    public static boolean isChalice(final SpellAbility sa) {
        final Card host = sa == null ? null : sa.getHostCard();
        return host != null && "Chalice of the Void".equals(host.getName()) && sa.isSpell();
    }

    /** X damage dealt by the spell, paid with the X of its cost. */
    static boolean isXDamage(final SpellAbility sa) {
        return sa != null && sa.getApi() == ApiType.DealDamage && "X".equals(sa.getParamOrDefault("NumDmg", ""))
                && "Count$xPaid".equals(sa.getSVar("X"));
    }

    /** X damage to each player (Earthquake, Hurricane, Crypt Rats). */
    static boolean isXDamageAll(final SpellAbility sa) {
        return sa != null && sa.getApi() == ApiType.DamageAll && "X".equals(sa.getParamOrDefault("NumDmg", ""))
                && "Count$xPaid".equals(sa.getSVar("X")) && sa.hasParam("ValidPlayers");
    }

    /** Target player discards X cards (Mind Twist). */
    static boolean isXDiscard(final SpellAbility sa) {
        return sa != null && sa.getApi() == ApiType.Discard && "X".equals(sa.getParamOrDefault("NumCards", ""))
                && "Count$xPaid".equals(sa.getSVar("X"));
    }

    static int maxOpponentLife(final Player ai) {
        int life = 0;
        for (final Player opp : ai.getOpponents()) {
            life = Math.max(life, opp.getLife());
        }
        return life;
    }

    static int maxOpponentHand(final Player ai) {
        int cards = 0;
        for (final Player opp : ai.getOpponents()) {
            cards = Math.max(cards, opp.getCardsIn(ZoneType.Hand).size());
        }
        return cards;
    }

    /** The X to pay, never above {@code max}. */
    public static int adjust(final Player ai, final SpellAbility sa, final int max) {
        if (ai == null || sa == null || max <= 0) {
            return Math.max(0, max);
        }
        try {
            if (isChalice(sa)) {
                return chalice(ai, max);
            }
            if (isXDamage(sa)) {
                return Math.min(max, Math.max(1, damageNeeded(ai)));
            }
            if (isXDamageAll(sa)) {
                // each player takes it too: never the AI's own life, and no more than the opponent with the most life has
                return Math.max(0, Math.min(max, Math.min(ai.getLife() - 1, maxOpponentLife(ai))));
            }
            if (isXDiscard(sa)) {
                return Math.min(max, maxOpponentHand(ai)); // more cards than the hand has are mana for nothing
            }
        } catch (final RuntimeException e) {
            // a refinement only
        }
        return max;
    }

    static int chalice(final Player ai, final int max) {
        final int[] order = { 1, 2, 0 };
        int best = -1;
        int bestHits = Integer.MAX_VALUE;
        for (final int x : order) {
            if (x > max) {
                continue;
            }
            int hits = 0;
            for (final ZoneType zone : new ZoneType[] { ZoneType.Hand, ZoneType.Library }) {
                for (final Card c : ai.getCardsIn(zone)) {
                    if (!c.isLand() && c.getCMC() == x) {
                        hits++;
                    }
                }
            }
            if (hits < bestHits) {
                bestHits = hits;
                best = x;
            }
        }
        return best < 0 ? max : best;
    }

    /**
     * A useful single lethal damage amount. A single-target spell cannot deal its X to a player's life and every
     * creature at once, so do not add unrelated targets together. For divided damage, target selection may use the
     * amount to distribute damage, but the AI should not automatically pay the sum of the whole opposing board.
     */
    static int damageNeeded(final Player ai) {
        int need = 0;
        for (final Player opp : ai.getOpponents()) {
            need = Math.max(need, Math.max(0, opp.getLife()));
            for (final Card c : opp.getCreaturesInPlay()) {
                need = Math.max(need, Math.max(0, c.getNetToughness() - c.getDamage()));
            }
        }
        return need;
    }

    // ------------------------------------------------------------------------------------------ loyalty X

    /** The X and the target of a Planeswalker's "-X" ability, chosen the careful way (see {@link #loyaltyPlan}). */
    public static final class LoyaltyPlan {
        public final int x;
        /** The single target, or null for an ability without one. */
        public final GameObject target;

        LoyaltyPlan(final int x, final GameObject target) {
            this.x = x;
            this.target = target;
        }
    }

    /** A "-X" loyalty ability: the cost removes X loyalty counters and has no X of mana. */
    static boolean isLoyaltyX(final SpellAbility sa) {
        if (sa == null || !sa.isPwAbility() || sa.getPayCosts() == null || sa.costHasManaX()) {
            return false;
        }
        for (final CostPart part : sa.getPayCosts().getCostParts()) {
            if (part instanceof CostRemoveCounter && "X".equals(part.getAmount())
                    && ((CostRemoveCounter) part).counter.is(CounterEnumType.LOYALTY)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Flat Monte Carlo cannot try every X of a "-X" Planeswalker ability, and an unset X would be paid as zero (a free
     * ability that does nothing). So it is chosen here, and only when the ability is plainly worth it:
     * <ul>
     *   <li>the Planeswalker always survives: X is at most its loyalty minus one;</li>
     *   <li><b>X damage to one target</b> (Chandra Nalaar, Huatli, Sorin, Grim Nemesis): the opposing creature or Planeswalker
     *   (the best one) that X kills, or an opponent whose life X takes: X is exactly what kills it, no more;</li>
     *   <li><b>X damage to each creature</b> (Chandra, Flamecaller): the X that kills the most of the opponents' creatures
     *   for the least of the AI's own (what it kills of its own is worth at most half of what it kills of theirs), or none;</li>
     *   <li>any other "-X" ability: no plan, so nothing is discovered (the heuristics may still play it).</li>
     * </ul>
     *
     * @return the plan, or null when there is no X worth paying
     */
    public static LoyaltyPlan loyaltyPlan(final Player ai, final SpellAbility sa) {
        if (ai == null || !isLoyaltyX(sa)) {
            return null;
        }
        try {
            final int max = sa.getHostCard().getCounters(CounterEnumType.LOYALTY) - 1;
            if (max < 1) {
                return null;
            }
            sa.setActivatingPlayer(ai);
            if (sa.getApi() == ApiType.DealDamage && "X".equals(sa.getParamOrDefault("NumDmg", "")) && sa.usesTargeting()) {
                return damagePlan(ai, sa, max);
            }
            if (sa.getApi() == ApiType.DamageAll && "X".equals(sa.getParamOrDefault("NumDmg", ""))
                    && sa.hasParam("ValidCards") && !sa.hasParam("ValidPlayers")) {
                return sweepPlan(ai, sa, max);
            }
        } catch (final RuntimeException e) {
            // a refinement only: no plan, nothing discovered
        }
        return null;
    }

    // ------------------------------------------------------------------------------------------ mana X

    /** An activated ability of a permanent in play whose cost has an X of mana: "{X}{W}, {T}: X damage to target attacking creature". */
    public static boolean isManaX(final SpellAbility sa) {
        final Card host = sa == null ? null : sa.getHostCard();
        return sa != null && sa.isActivatedAbility() && !sa.isSpell() && !sa.isPwAbility() && !sa.isManaAbility()
                && host != null && host.isInZone(ZoneType.Battlefield) && sa.getPayCosts() != null && sa.costHasManaX();
    }

    /** How many X the mana cost has ({X}{X} is two): the mana paid for one point of X. */
    public static int xCount(final SpellAbility sa) {
        return sa == null || sa.getPayCosts() == null || sa.getPayCosts().getCostMana() == null ? 1
                : Math.max(1, sa.getPayCosts().getCostMana().getAmountOfX());
    }

    /**
     * The X and the target of an activated ability with an X of mana, chosen the careful way and only when it is plainly
     * worth it: the same plans as the loyalty "-X" (the damage that kills the best creature or planeswalker it can reach, or
     * the opponent; the smallest X that kills more of theirs than of the AI's own for damage to each creature), within the mana
     * left after the rest of the cost. Any other X ability gets no plan: nothing is discovered, the heuristics may still play it.
     */
    public static LoyaltyPlan manaXPlan(final Player ai, final SpellAbility sa) {
        if (ai == null || !isManaX(sa)) {
            return null;
        }
        try {
            final int fixed = sa.getPayCosts().getTotalMana() == null ? 0 : sa.getPayCosts().getTotalMana().getCMC();
            final int max = (ComputerUtilMana.getAvailableManaEstimate(ai) - fixed) / xCount(sa);
            if (max < 1) {
                return null;
            }
            sa.setActivatingPlayer(ai);
            if (sa.getApi() == ApiType.DealDamage && "X".equals(sa.getParamOrDefault("NumDmg", "")) && sa.usesTargeting()) {
                return damagePlan(ai, sa, max);
            }
            if (sa.getApi() == ApiType.DamageAll && "X".equals(sa.getParamOrDefault("NumDmg", ""))
                    && sa.hasParam("ValidCards") && !sa.hasParam("ValidPlayers")) {
                return sweepPlan(ai, sa, max);
            }
        } catch (final RuntimeException e) {
            // a refinement only: no plan, nothing discovered
        }
        return null;
    }

    private static int worth(final Card c) {
        return c.isCreature() ? ComputerUtilCard.evaluateCreature(c) : 250;
    }

    private static int toKill(final Card c) {
        return c.isPlaneswalker() ? c.getCounters(CounterEnumType.LOYALTY) : c.getNetToughness() - c.getDamage();
    }

    private static LoyaltyPlan damagePlan(final Player ai, final SpellAbility sa, final int max) {
        if (sa.getTargetRestrictions() == null || sa.getTargetRestrictions().getMaxTargets(sa.getHostCard(), sa) != 1) {
            return null;
        }
        GameEntity best = null;
        int bestX = 0;
        long bestScore = Long.MIN_VALUE;
        for (final GameEntity e : sa.getTargetRestrictions().getAllCandidates(sa)) {
            final int need;
            final long score;
            if (e instanceof Player) {
                final Player p = (Player) e;
                if (!p.isOpponentOf(ai)) {
                    continue;
                }
                need = p.getLife();
                score = 1_000_000L;
            } else if (e instanceof Card) {
                final Card c = (Card) e;
                if (c.getController() == null || !c.getController().isOpponentOf(ai) || c.hasKeyword(Keyword.INDESTRUCTIBLE)
                        || !(c.isCreature() || c.isPlaneswalker())) {
                    continue;
                }
                need = toKill(c);
                score = worth(c);
            } else {
                continue;
            }
            if (need >= 1 && need <= max && score > bestScore) {
                best = e;
                bestX = need;
                bestScore = score;
            }
        }
        return best == null ? null : new LoyaltyPlan(bestX, best);
    }

    private static LoyaltyPlan sweepPlan(final Player ai, final SpellAbility sa, final int max) {
        final String valid = sa.getParam("ValidCards");
        if (valid.contains("X") || valid.contains("Targeted") || valid.contains("Remembered")) {
            return null;
        }
        final List<Card> hit = CardLists.getValidCards(ai.getGame().getCardsIn(ZoneType.Battlefield), valid, ai,
                sa.getHostCard(), sa);
        int bestX = 0;
        int bestNet = 0;
        for (int x = 1; x <= max; x++) {
            int theirs = 0;
            int mine = 0;
            for (final Card c : hit) {
                if (!c.isCreature() || c.hasKeyword(Keyword.INDESTRUCTIBLE) || toKill(c) > x) {
                    continue;
                }
                if (c.getController().isOpponentOf(ai)) {
                    theirs += worth(c);
                } else {
                    mine += worth(c);
                }
            }
            if (mine * 2 <= theirs && theirs - mine > bestNet) {
                bestNet = theirs - mine;
                bestX = x;
            }
        }
        return bestX > 0 ? new LoyaltyPlan(bestX, null) : null;
    }
}
