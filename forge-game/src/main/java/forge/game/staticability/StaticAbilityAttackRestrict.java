package forge.game.staticability;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.zone.ZoneType;

public class StaticAbilityAttackRestrict {

    static String MODE = "AttackRestrict";

    static public int globalAttackRestrict(Game game) {
        int max = Integer.MAX_VALUE;
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(MODE)
                        || stAb.hasParam("ValidDefender")) {
                    continue;
                }
                int stMax = AbilityUtils.calculateAmount(stAb.getHostCard(),
                        stAb.getParamOrDefault("MaxAttackers", "1"), stAb);
                if (stMax < max) {
                    max = stMax;
                }
            }
        }
        return max < Integer.MAX_VALUE ? max : -1;
    }

    static public int attackRestrictNum(GameEntity defender) {
        final Game game = defender.getGame();
        int num = Integer.MAX_VALUE;
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(MODE)
                        || !stAb.hasParam("ValidDefender")) {
                    continue;
                }
                if (attackRestrict(stAb, defender)) {
                    int stNum = AbilityUtils.calculateAmount(stAb.getHostCard(),
                            stAb.getParamOrDefault("MaxAttackers", "1"), stAb);
                    if (stNum < num) {
                        num = stNum;
                    }
                }

            }
        }
        return num < Integer.MAX_VALUE ? num : -1;
    }

    static public boolean attackRestrict(StaticAbility stAb, GameEntity defender) {
        if (!stAb.matchesValidParam("ValidDefender", defender)) {
            return false;
        }
        return true;
    }
}
