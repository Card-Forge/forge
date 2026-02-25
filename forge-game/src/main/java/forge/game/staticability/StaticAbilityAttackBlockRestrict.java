package forge.game.staticability;

import java.util.Collection;
import java.util.List;

import org.testng.collections.Lists;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardPredicates;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.Expressions;

public class StaticAbilityAttackBlockRestrict {

    static public Integer globalAttackRestrictNum(Game game) {
        Integer max = null;
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(StaticAbilityMode.AttackRestrictNum)
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
                if (!stAb.checkConditions(StaticAbilityMode.AttackRestrictNum)
                        || !stAb.hasParam("ValidDefender")) {
                    continue;
                }
                if (validRestrictNum(stAb, defender)) {
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

    static public int blockRestrictNum(Player defender) {
        final Game game = defender.getGame();
        int num = Integer.MAX_VALUE;
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(StaticAbilityMode.BlockRestrictNum)) {
                    continue;
                }
                if (validRestrictNum(stAb, defender)) {
                    int stNum = AbilityUtils.calculateAmount(stAb.getHostCard(),
                            stAb.getParamOrDefault("MaxBlockers", "1"), stAb);
                    if (stNum < num) {
                        num = stNum;
                    }
                }

            }
        }
        return num;
    }

    static public boolean validRestrictNum(StaticAbility stAb, GameEntity defender) {
        if (!stAb.matchesValidParam("ValidDefender", defender)) {
            return false;
        }
        return true;
    }

    static public List<StaticAbility> attackRestrict(final Card attacker, final Collection<Card> others) {
        final Game game = attacker.getGame();
        List<StaticAbility> result = Lists.newArrayList();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(StaticAbilityMode.AttackRestrict)) {
                    continue;
                }
                if (!stAb.matchesValidParam("ValidCard", attacker)) {
                    continue;
                }
                if (validRestrictCommon(stAb, attacker, others)) {
                    result.add(stAb);
                }
            }
        }
        return result;
    }

    static public boolean validRestrictCommon(StaticAbility stAb, Card card, Collection<Card> others) {
        long size;
        if (stAb.hasParam("ValidOthers")) {
            size = others.stream().filter(CardPredicates.restriction(stAb.getParam("ValidOthers").split(","), card.getController(), card, stAb)).count();
        } else {
            size = others.size();
        }
        String compare = stAb.getParamOrDefault("OthersCompare", "GE1");

        String operator = compare.substring(0, 2);
        String operand = compare.substring(2);

        final int operandValue = AbilityUtils.calculateAmount(card, operand, stAb);
        return !Expressions.compare((int)size, operator, operandValue);
    }
}
