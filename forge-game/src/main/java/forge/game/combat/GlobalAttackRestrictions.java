package forge.game.combat;

import java.util.Map;
import java.util.Map.Entry;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.staticability.StaticAbilityAttackRestrict;
import forge.util.collect.FCollectionView;
import forge.util.maps.LinkedHashMapToAmount;
import forge.util.maps.MapToAmount;
import forge.util.maps.MapToAmountUtil;

public class GlobalAttackRestrictions {

    private final int max;
    private final MapToAmount<GameEntity> defenderMax;
    private GlobalAttackRestrictions(final int max, final MapToAmount<GameEntity> defenderMax) {
        this.max = max;
        this.defenderMax = defenderMax;
    }

    public int getMax() {
        return max;
    }
    public MapToAmount<GameEntity> getDefenderMax() {
        return defenderMax;
    }

    public boolean isLegal(final Map<Card, GameEntity> attackers) {
        return !getViolations(attackers, true).isViolated();
    }

    private GlobalAttackRestrictionViolations getViolations(final Map<Card, GameEntity> attackers, final boolean returnQuickly) {
        final int nTooMany = max < 0 ? 0 : attackers.size() - max;
        if (returnQuickly && nTooMany > 0) {
            return new GlobalAttackRestrictionViolations(nTooMany, MapToAmountUtil.emptyMap());
        }

        final MapToAmount<GameEntity> defenderTooMany = new LinkedHashMapToAmount<>(defenderMax.size());
        outer: for (final GameEntity defender : attackers.values()) {
            final Integer max = defenderMax.get(defender);
            if (max == null) {
                continue;
            }
            if (returnQuickly && max == 0) {
                // there's at least one creature attacking this defender
                defenderTooMany.put(defender, 1);
                break;
            }
            int count = 0;
            for (final Entry<Card, GameEntity> attDef : attackers.entrySet()) {
                if (attDef.getValue() == defender) {
                    count++;
                    if (returnQuickly && count > max) {
                        defenderTooMany.put(defender, count - max);
                        break outer;
                    }
                }
            }
            final int nDefTooMany = count - max;
            if (nDefTooMany > 0) {
                // Too many attackers to one defender!
                defenderTooMany.put(defender, nDefTooMany);
            }
        }

        return new GlobalAttackRestrictionViolations(nTooMany, defenderTooMany);
    }

    final class GlobalAttackRestrictionViolations {
        private final boolean isViolated;

        public GlobalAttackRestrictionViolations(final int globalTooMany, final MapToAmount<GameEntity> defenderTooMany) {
            this.isViolated = globalTooMany > 0 || !defenderTooMany.isEmpty();
        }
        public boolean isViolated() {
            return isViolated;
        }

    }

    /**
     * <p>
     * Get all global restrictions (applying to all creatures).
     * </p>
     * 
     * @param attackingPlayer
     *            the {@link Player} declaring attack.
     * @return a {@link GlobalAttackRestrictions} object.
     */
    public static GlobalAttackRestrictions getGlobalRestrictions(final Player attackingPlayer, final FCollectionView<GameEntity> possibleDefenders) {
        final MapToAmount<GameEntity> defenderMax = new LinkedHashMapToAmount<>(possibleDefenders.size());
        final Game game = attackingPlayer.getGame();

        int max = StaticAbilityAttackRestrict.globalAttackRestrict(game);

        for (final GameEntity defender : possibleDefenders) {
            final int defMax = StaticAbilityAttackRestrict.attackRestrictNum(defender);
            if (defMax != -1) {
                defenderMax.add(defender, defMax);
            }
        }
        if (defenderMax.size() == possibleDefenders.size()) {
            // maximum on each defender, global maximum is sum of these
            max = Math.min(max, defenderMax.countAll());
        }

        return new GlobalAttackRestrictions(max, defenderMax);
    }
}
