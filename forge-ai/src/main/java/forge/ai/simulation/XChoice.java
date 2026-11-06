package forge.ai.simulation;

import forge.game.ability.ApiType;
import forge.game.card.Card;
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
 *   <li><b>X damage</b> (Fireball, Blaze, Banefire): never more than the life of the opponent with the most life, plus the
 *       toughness of its creatures (the damage may be divided), so the mana left over is kept (Channel and Fireball: X is 20, not more).</li>
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
     * The damage that kills everything the spell could be aimed at (it may be divided among targets): the life of an
     * opponent and the toughness of its creatures, for the opponent with the most.
     */
    static int damageNeeded(final Player ai) {
        int need = 0;
        for (final Player opp : ai.getOpponents()) {
            int total = Math.max(0, opp.getLife());
            for (final Card c : opp.getCreaturesInPlay()) {
                total += Math.max(0, c.getNetToughness() - c.getDamage());
            }
            need = Math.max(need, total);
        }
        return need;
    }
}
