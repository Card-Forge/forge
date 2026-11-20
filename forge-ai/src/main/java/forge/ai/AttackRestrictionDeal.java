package forge.ai;

import java.util.ArrayList;
import java.util.List;

import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.combat.CombatUtil;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.staticability.StaticAbilityAssignCombatDamageAsUnblocked;

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
        private final Card attacker;
        private final int damage;
        private final int minBlockers;
        private final List<Card> compatibleBlockers = new ArrayList<>();
        private final List<Card> assignedBlockers = new ArrayList<>();
        private final boolean canBeBlocked;

        private Attack(final Card attacker, final Player defender) {
            this.attacker = attacker;
            // getAttack includes both combat-damage steps for double strike.
            damage = ComputerUtilCombat.getAttack(attacker);
            minBlockers = CombatUtil.getMinNumBlockersForAttacker(attacker, defender);
            final boolean assignsAsUnblocked = StaticAbilityAssignCombatDamageAsUnblocked
                    .assignCombatDamageAsUnblocked(attacker, false)
                    || StaticAbilityAssignCombatDamageAsUnblocked.assignCombatDamageAsUnblocked(attacker, true);
            for (final Card blocker : defender.getCreaturesInPlay()) {
                // A tapped blocker only matters if it can untap during its controller's next untap step.
                if ((!blocker.isTapped() || blocker.canUntap(defender, true))
                        && CombatUtil.canBlock(attacker, blocker, true)) {
                    compatibleBlockers.add(blocker);
                }
            }
            canBeBlocked = !assignsAsUnblocked && minBlockers > 0 && compatibleBlockers.size() >= minBlockers
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

    /** Damage that reaches the defending player after the selected blocks. */
    private static int damageThrough(final Attack attack, final Player defender) {
        if (attack.assignedBlockers.isEmpty()) {
            // Also handles double strike, damage prevention, and effects that assign damage as unblocked.
            return ComputerUtilCombat.damageIfUnblocked(attack.attacker, defender, null, false);
        }
        if (attack.attacker.hasKeyword(Keyword.TRAMPLE) && !attack.attacker.isInfectDamage(defender)
                && defender.canLoseLife()) {
            return Math.max(0, ComputerUtilCombat.getAttack(attack.attacker)
                    - ComputerUtilCombat.totalShieldDamage(attack.attacker, attack.assignedBlockers));
        }
        return 0;
    }

    /** Poison counters that reach the defending player after the selected blocks. */
    private static int poisonThrough(final Attack attack, final Player defender) {
        if (!defender.canReceiveCounters(CounterEnumType.POISON)) {
            return 0;
        }
        if (attack.assignedBlockers.isEmpty()) {
            return ComputerUtilCombat.poisonIfUnblocked(attack.attacker, defender);
        }
        if (!attack.attacker.hasKeyword(Keyword.TRAMPLE)) {
            return 0;
        }

        final int trampleDamage = Math.max(0, ComputerUtilCombat.getAttack(attack.attacker)
                - ComputerUtilCombat.totalShieldDamage(attack.attacker, attack.assignedBlockers));
        if (trampleDamage == 0) {
            return 0;
        }
        int poison = attack.attacker.isInfectDamage(defender) ? trampleDamage : 0;
        poison += ComputerUtilCombat.predictExtraPoisonWithDamage(attack.attacker, defender, trampleDamage);
        return poison;
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
            if (ComputerUtilCombat.getAttack(c) > 0 && CombatUtil.canAttackNextTurn(c, offerer)) {
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
        for (int i = 0; i < attacks.size(); i++) {
            final Attack attack = attacks.get(i);
            if (!attack.canBeBlocked) {
                continue;
            }

            final List<Card> candidates = new ArrayList<>();
            for (final Card blocker : attack.compatibleBlockers) {
                if (availableBlockers.contains(blocker)) {
                    candidates.add(blocker);
                }
            }
            if (candidates.size() < attack.minBlockers) {
                continue;
            }

            // Of this attacker's legal blockers, use the ones least useful for the remaining attackers first.
            final int firstUnassignedAttack = i + 1;
            candidates.sort((left, right) -> Integer.compare(
                    futureBlockOptions(left, attacks, firstUnassignedAttack, availableBlockers),
                    futureBlockOptions(right, attacks, firstUnassignedAttack, availableBlockers)));
            for (int j = 0; j < attack.minBlockers; j++) {
                final Card blocker = candidates.get(j);
                attack.assignedBlockers.add(blocker);
                availableBlockers.remove(blocker);
            }

            // A defender may assign more than the minimum number of blockers to a trampler. Use additional
            // compatible blockers when they reduce damage through; otherwise the estimate treats every
            // trampler as though only one blocker could be assigned.
            if (attack.attacker.hasKeyword(Keyword.TRAMPLE)) {
                int shieldDamage = ComputerUtilCombat.totalShieldDamage(attack.attacker, attack.assignedBlockers);
                for (int j = attack.minBlockers; j < candidates.size() && shieldDamage < attack.damage; j++) {
                    final Card blocker = candidates.get(j);
                    final int extraShield = ComputerUtilCombat.shieldDamage(attack.attacker, blocker);
                    if (extraShield <= 0) {
                        continue;
                    }
                    attack.assignedBlockers.add(blocker);
                    availableBlockers.remove(blocker);
                    shieldDamage += extraShield;
                }
            }
        }

        int through = 0;
        int poisonThrough = 0;
        for (final Attack attack : attacks) {
            through += damageThrough(attack, offerer);
            poisonThrough += poisonThrough(attack, offerer);
        }
        if (through >= offerer.getLife()) {
            return false; // the damage that gets through could kill, after legal blocks are assigned
        }
        final int poisonToLose = offerer.getGame().getRules().getPoisonCountersToLose();
        if (poisonToLose > offerer.getPoisonCounters()
                && poisonThrough >= poisonToLose - offerer.getPoisonCounters()) {
            return false; // the poison that gets through could be lethal
        }
        return through < Math.max(3, offerer.getLife() / 4);
    }
}
