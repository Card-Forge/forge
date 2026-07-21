package forge.ai;

import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.player.Player;

import java.util.HashMap;
import java.util.Map;

/** Request-local memoization of pairwise combat facts. */
final class AiCombatEvaluationMatrix {
    private final Map<PairKey, Boolean> blockLegality = new HashMap<>();
    private final Map<CombatPairKey, Boolean> combatBlockLegality = new HashMap<>();
    private final Map<OutcomeKey, Boolean> destroysAttacker = new HashMap<>();
    private final Map<OutcomeKey, Boolean> destroysBlocker = new HashMap<>();
    private final Map<PairKey, Integer> blockerDamage = new HashMap<>();
    private final Map<DamageToKillKey, Integer> damageToKill = new HashMap<>();
    private final Map<CombatCardKey, Integer> creatureValues = new HashMap<>();
    private final Map<DefenderCardKey, Integer> unblockedDamage = new HashMap<>();

    boolean canBlockWithoutCombat(final Card attacker, final Card blocker) {
        PairKey key = new PairKey(attacker, blocker);
        return blockLegality.computeIfAbsent(key,
                ignored -> CombatUtil.canBlock(attacker, blocker));
    }

    boolean canBlock(final Card attacker, final Card blocker, final Combat combat) {
        CombatPairKey key = new CombatPairKey(attacker, blocker,
                combat.getBlockers(attacker).size(),
                combat.getAttackersBlockedBy(blocker).size());
        return combatBlockLegality.computeIfAbsent(key,
                ignored -> CombatUtil.canBlock(attacker, blocker, combat,
                        () -> canBlockWithoutCombat(attacker, blocker)));
    }

    boolean canDestroyAttacker(final Player ai, final Card attacker, final Card blocker,
            final Combat combat,
            final boolean withoutAbilities, final boolean withoutAttackerStaticAbilities) {
        return getDestroyOutcome(destroysAttacker, ai, attacker, blocker, combat,
                withoutAbilities, withoutAttackerStaticAbilities, true);
    }

    boolean canDestroyBlocker(final Player ai, final Card attacker, final Card blocker,
            final Combat combat,
            final boolean withoutAbilities, final boolean withoutAttackerStaticAbilities) {
        return getDestroyOutcome(destroysBlocker, ai, attacker, blocker, combat,
                withoutAbilities, withoutAttackerStaticAbilities, false);
    }

    int damageAsBlocker(final Card attacker, final Card blocker) {
        return blockerDamage.computeIfAbsent(
                new PairKey(attacker, blocker),
                ignored -> ComputerUtilCombat.dealsDamageAsBlocker(attacker, blocker));
    }

    int enoughDamageToKill(final Card target, final int maxDamage,
            final Card source, final boolean combat) {
        return damageToKill.computeIfAbsent(new DamageToKillKey(
                        target, source, maxDamage, combat),
                ignored -> ComputerUtilCombat.getEnoughDamageToKill(
                        target, maxDamage, source, combat));
    }

    int evaluateCreature(final Card card, final Combat combat) {
        return evaluateCreature(card, combat, true);
    }

    int evaluateCreature(final Card card, final Combat combat,
            final boolean considerManaValue) {
        CombatCardKey key = new CombatCardKey(card,
                combat.getBlockers(card).size(),
                combat.getAttackersBlockedBy(card).size(), considerManaValue);
        return creatureValues.computeIfAbsent(key,
                ignored -> ComputerUtilCard.evaluateCreature(card, true, considerManaValue));
    }

    int damageIfUnblocked(final Card attacker, final Player defender,
            final Combat combat) {
        return unblockedDamage.computeIfAbsent(
                new DefenderCardKey(defender.getId(), attacker),
                ignored -> ComputerUtilCombat.damageIfUnblocked(
                        attacker, defender, combat, false));
    }

    Card bestCreature(final Iterable<Card> cards, final Combat combat) {
        return creatureWithExtremeValue(cards, combat, true);
    }

    Card worstCreature(final Iterable<Card> cards, final Combat combat) {
        return creatureWithExtremeValue(cards, combat, false);
    }

    /** Only facts that depend on the current block assignment must be discarded. */
    void combatChanged() {
        combatBlockLegality.clear();
        destroysBlocker.clear();
    }

    private boolean getDestroyOutcome(final Map<OutcomeKey, Boolean> cache,
            final Player ai, final Card attacker, final Card blocker, final Combat combat,
            final boolean withoutAbilities, final boolean withoutAttackerStaticAbilities,
            final boolean attackingCard) {
        OutcomeKey key = new OutcomeKey(attacker, blocker, withoutAbilities,
                withoutAttackerStaticAbilities);
        return cache.computeIfAbsent(key, ignored -> calculateDestroyOutcome(ai, attacker,
                blocker, combat, withoutAbilities, withoutAttackerStaticAbilities,
                attackingCard));
    }

    private static boolean calculateDestroyOutcome(final Player ai, final Card attacker,
            final Card blocker, final Combat combat, final boolean withoutAbilities,
            final boolean withoutAttackerStaticAbilities, final boolean attackingCard) {
        return attackingCard
                ? ComputerUtilCombat.canDestroyAttacker(ai, attacker, blocker, combat,
                        withoutAbilities, withoutAttackerStaticAbilities)
                : ComputerUtilCombat.canDestroyBlocker(ai, blocker, attacker, combat,
                        withoutAbilities, withoutAttackerStaticAbilities);
    }

    private Card creatureWithExtremeValue(final Iterable<Card> cards,
            final Combat combat, final boolean best) {
        Card result = null;
        int extreme = best ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        for (Card card : cards) {
            int value = evaluateCreature(card, combat);
            if (best ? value > extreme : value < extreme) {
                result = card;
                extreme = value;
            }
        }
        return result;
    }

    private record PairKey(Card attacker, Card blocker) {}

    private record CombatPairKey(Card attacker, Card blocker,
            int blockerCount, int blockedAttackerCount) {}

    private record CombatCardKey(Card card, int blockerCount,
            int blockedAttackerCount, boolean considerManaValue) {}

    private record DefenderCardKey(int defenderId, Card card) {}

    private record DamageToKillKey(Card target, Card source,
            int maxDamage, boolean combat) {}

    private record OutcomeKey(Card attacker, Card blocker, boolean withoutAbilities,
            boolean withoutAttackerStaticAbilities) {}

}
