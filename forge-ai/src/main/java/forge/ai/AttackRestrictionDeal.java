package forge.ai;

import java.util.ArrayList;
import java.util.List;

import forge.game.card.Card;
import forge.game.combat.CombatUtil;
import forge.game.player.Player;

/**
 * "Each player may draw a card / put counters / make a land; each opponent who does can't attack you next turn"
 * (The Second Doctor, Orzhov Advokist, Noble Heritage, Zagorka): a bribe the controller offers. The AI takes the card
 * or the counters when the attack it gives up is not worth much, and refuses when that attack is what it wants to do.
 * Taking it always means a board that never attacks.
 */
public final class AttackRestrictionDeal {
    private AttackRestrictionDeal() { }

    /** A possible attacker and the defenders that can legally block it next turn. */
    private static final class Attack {
        private final int damage;
        private final int minBlockers;
        private final List<Card> compatibleBlockers = new ArrayList<>();
        private final boolean canBeBlocked;

        private Attack(final Card attacker, final Player defender) {
            damage = attacker.getNetCombatDamage();
            minBlockers = CombatUtil.getMinNumBlockersForAttacker(attacker, defender);
            for (final Card blocker : defender.getCreaturesInPlay()) {
                // A tapped blocker only matters if it can untap during its controller's next untap step.
                if ((!blocker.isTapped() || blocker.canUntap(defender, true))
                        && CombatUtil.canBlock(attacker, blocker, true)) {
                    compatibleBlockers.add(blocker);
                }
            }
            canBeBlocked = minBlockers > 0 && compatibleBlockers.size() >= minBlockers
                    && CombatUtil.canAttackerBeBlockedWithAmount(attacker, minBlockers, defender);
        }

        /** Prefer assigning scarce blockers to the attackers with the fewest alternatives. */
        private int blockSlack() {
            return canBeBlocked ? compatibleBlockers.size() - minBlockers : Integer.MAX_VALUE;
        }
    }

    /** How many not-yet-processed attackers could use this blocker instead. */
    private static int futureBlockOptions(final Card blocker, final List<Attack> attacks, final int firstIndex,
            final List<Card> availableBlockers) {
        int options = 0;
        for (int i = firstIndex; i < attacks.size(); i++) {
            final Attack attack = attacks.get(i);
            if (!attack.canBeBlocked || !attack.compatibleBlockers.contains(blocker)) {
                continue;
            }
            int available = 0;
            for (final Card candidate : attack.compatibleBlockers) {
                if (availableBlockers.contains(candidate)) {
                    available++;
                }
            }
            if (available >= attack.minBlockers) {
                options++;
            }
        }
        return options;
    }

    /**
     * @param ai the player who may take the deal
     * @param offerer the player whose permanents ai could not attack after taking it
     * @return whether the deal is worth taking: the attack it gives up is small
     */
    public static boolean worthTaking(final Player ai, final Player offerer) {
        if (offerer == null || ai.equals(offerer) || !ai.isOpponentOf(offerer)) {
            return true; // nothing is given up
        }
        if (ai.getOpponents().size() > 1 && !offerer.equals(AiAttackController.choosePreferredDefenderPlayer(ai))) {
            return true; // another opponent is the better target anyway
        }
        // next turn it is untapped and no longer sick: what can attack then is what would stay at home
        final List<Attack> attacks = new ArrayList<>();
        for (final Card c : ai.getCreaturesInPlay()) {
            if (c.getNetCombatDamage() > 0 && CombatUtil.canAttackNextTurn(c, offerer)) {
                attacks.add(new Attack(c, offerer));
            }
        }
        if (attacks.isEmpty()) {
            return true;
        }

        // Process attackers with the fewest legal blocking options first, breaking ties by damage. This avoids
        // spending a flexible blocker on an attacker that has no alternative blocker.
        attacks.sort((left, right) -> {
            final int slack = Integer.compare(left.blockSlack(), right.blockSlack());
            return slack != 0 ? slack : Integer.compare(right.damage, left.damage);
        });

        final List<Card> availableBlockers = new ArrayList<>(offerer.getCreaturesInPlay());
        int through = 0;
        for (int i = 0; i < attacks.size(); i++) {
            final Attack attack = attacks.get(i);
            if (!attack.canBeBlocked) {
                through += attack.damage;
                continue;
            }

            final List<Card> candidates = new ArrayList<>();
            for (final Card blocker : attack.compatibleBlockers) {
                if (availableBlockers.contains(blocker)) {
                    candidates.add(blocker);
                }
            }
            if (candidates.size() < attack.minBlockers) {
                through += attack.damage;
                continue;
            }

            // Of this attacker's legal blockers, use the ones least useful for the remaining attackers first.
            final int firstUnassignedAttack = i + 1;
            candidates.sort((left, right) -> Integer.compare(
                    futureBlockOptions(left, attacks, firstUnassignedAttack, availableBlockers),
                    futureBlockOptions(right, attacks, firstUnassignedAttack, availableBlockers)));
            for (int j = 0; j < attack.minBlockers; j++) {
                availableBlockers.remove(candidates.get(j));
            }
        }

        if (through >= offerer.getLife()) {
            return false; // the damage that gets through could kill, after legal blocks are assigned
        }
        return through < Math.max(3, offerer.getLife() / 4);
    }
}
