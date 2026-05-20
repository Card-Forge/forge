package forge.game.combat;

import java.util.Map;
import java.util.Objects;

import com.google.common.collect.Maps;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.staticability.StaticAbilityAttackRestrict;
import forge.util.collect.FCollectionView;

public class GlobalAttackRestrictions {

    private final Integer max;
    private final Map<GameEntity, Integer> defenderMax;
    private GlobalAttackRestrictions(final Integer max, final Map<GameEntity, Integer> defenderMax) {
        this.max = max;
        this.defenderMax = defenderMax;
    }

    public Integer getMax() {
        return max;
    }
    public Map<GameEntity, Integer> getDefenderMax() {
        return defenderMax;
    }

    public boolean isLegal(final Map<Card, GameEntity> attackers) {
        if (max != null && attackers.size() > max) {
            return false;
        }

        return attackers.values().stream().distinct().noneMatch(defender -> {
            final Integer max = defenderMax.get(defender);
            if (max == null) {
                return false;
            }
            if (max == 0) {
                // there's at least one creature attacking this defender
                return true;
            }
            return attackers.values().stream().filter(attDef -> attDef == defender).count() > max;
        });
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
        final Map<GameEntity, Integer> defenderMax = Maps.newHashMapWithExpectedSize(possibleDefenders.size());
        final Game game = attackingPlayer.getGame();

        Integer max = StaticAbilityAttackRestrict.globalAttackRestrict(game);

        for (final GameEntity defender : possibleDefenders) {
            final Integer defMax = StaticAbilityAttackRestrict.attackRestrictNum(defender);
            if (defMax != null) {
                defenderMax.put(defender, defMax);
            }
        }
        if (defenderMax.size() == possibleDefenders.size()) {
            // maximum on each defender, global maximum is sum of these
            max = Math.min(Objects.requireNonNullElse(max, Integer.MAX_VALUE), defenderMax.values().stream().mapToInt(Integer::intValue).sum());
        }

        return new GlobalAttackRestrictions(max, defenderMax);
    }
}
