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
    private final Map<PairKey, Boolean> combatBlockLegality = new HashMap<>();
    private final Map<OutcomeKey, Boolean> destroysAttacker = new HashMap<>();
    private final Map<OutcomeKey, Boolean> destroysBlocker = new HashMap<>();
    private final Map<PairKey, Integer> blockerDamage = new HashMap<>();
    private final Map<DamageToKillKey, Integer> damageToKill = new HashMap<>();
    private final Map<DefenderCardKey, Integer> unblockedDamage = new HashMap<>();

    boolean canBlockWithoutCombat(final Card attacker, final Card blocker) {
        return blockLegality.computeIfAbsent(new PairKey(attacker, blocker),
                ignored -> CombatUtil.canBlock(attacker, blocker));
    }

    boolean canBlock(final Card attacker, final Card blocker, final Combat combat) {
        return combatBlockLegality.computeIfAbsent(new PairKey(attacker, blocker),
                ignored -> CombatUtil.canBlock(attacker, blocker, combat,
                        () -> canBlockWithoutCombat(attacker, blocker)));
    }

    boolean canDestroyAttacker(final Player ai, final Card attacker, final Card blocker,
            final Combat combat,
            final boolean withoutAbilities, final boolean withoutAttackerStaticAbilities) {
        return destroysAttacker.computeIfAbsent(new OutcomeKey(attacker, blocker,
                        withoutAbilities, withoutAttackerStaticAbilities),
                ignored -> ComputerUtilCombat.canDestroyAttacker(ai, attacker, blocker,
                        combat, withoutAbilities, withoutAttackerStaticAbilities));
    }

    boolean canDestroyBlocker(final Player ai, final Card attacker, final Card blocker,
            final Combat combat,
            final boolean withoutAbilities, final boolean withoutAttackerStaticAbilities) {
        return destroysBlocker.computeIfAbsent(new OutcomeKey(attacker, blocker,
                        withoutAbilities, withoutAttackerStaticAbilities),
                ignored -> ComputerUtilCombat.canDestroyBlocker(ai, blocker, attacker,
                        combat, withoutAbilities, withoutAttackerStaticAbilities));
    }

    int damageAsBlocker(final Card attacker, final Card blocker) {
        return blockerDamage.computeIfAbsent(
                new PairKey(attacker, blocker),
                ignored -> ComputerUtilCombat.dealsDamageAsBlocker(attacker, blocker));
    }

    int enoughDamageToKill(final Card target, final int maxDamage, final Card source) {
        return damageToKill.computeIfAbsent(new DamageToKillKey(
                        target, source, maxDamage),
                ignored -> ComputerUtilCombat.getEnoughDamageToKill(
                        target, maxDamage, source, true));
    }

    int damageIfUnblocked(final Card attacker, final Player defender,
            final Combat combat) {
        return unblockedDamage.computeIfAbsent(
                new DefenderCardKey(defender.getId(), attacker),
                ignored -> ComputerUtilCombat.damageIfUnblocked(
                        attacker, defender, combat, false));
    }

    /** Only facts that depend on the current block assignment must be discarded. */
    void combatChanged() {
        combatBlockLegality.clear();
        destroysBlocker.clear();
    }

    private record PairKey(Card attacker, Card blocker) {}

    private record DefenderCardKey(int defenderId, Card card) {}

    private record DamageToKillKey(Card target, Card source, int maxDamage) {}

    private record OutcomeKey(Card attacker, Card blocker, boolean withoutAbilities,
            boolean withoutAttackerStaticAbilities) {}

}
