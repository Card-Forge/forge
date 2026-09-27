package forge.game.staticability;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.zone.ZoneType;

import java.util.ArrayList;
import java.util.List;

public class StaticAbilityAttackRestrict {

    static public Integer globalAttackRestrict(Game game) {
        Integer max = null;
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(StaticAbilityMode.AttackRestrict)
                        || stAb.hasParam("ValidDefender")) {
                    continue;
                }
                int stMax = AbilityUtils.calculateAmount(stAb.getHostCard(),
                        stAb.getParamOrDefault("MaxAttackers", "1"), stAb);
                if (null == max || stMax < max) {
                    max = stMax;
                }
            }
        }
        return max;
    }

    static public Integer attackRestrictNum(GameEntity defender) {
        final Game game = defender.getGame();
        Integer num = null;
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(StaticAbilityMode.AttackRestrict)
                        || !stAb.hasParam("ValidDefender")) {
                    continue;
                }
                if (attackRestrict(stAb, defender)) {
                    int stNum = AbilityUtils.calculateAmount(stAb.getHostCard(),
                            stAb.getParamOrDefault("MaxAttackers", "1"), stAb);
                    if (null == num || stNum < num) {
                        num = stNum;
                    }
                }
            }
        }
        return num;
    }

    /**
     * @return the AttackRestrict static abilities limiting the number of attackers,
     *         either overall (defender == null) or against the given defender
     */
    static public List<StaticAbility> attackRestrictSources(Game game, GameEntity defender) {
        final List<StaticAbility> result = new ArrayList<>();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(StaticAbilityMode.AttackRestrict)
                        || stAb.hasParam("ValidDefender") != (defender != null)) {
                    continue;
                }
                if (defender == null || attackRestrict(stAb, defender)) {
                    result.add(stAb);
                }
            }
        }
        return result;
    }

    static public boolean attackRestrict(StaticAbility stAb, GameEntity defender) {
        if (!stAb.matchesValidParam("ValidDefender", defender)) {
            return false;
        }
        return true;
    }
}
