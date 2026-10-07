package forge.game.staticability;

import java.util.Collection;
import java.util.List;

import com.google.common.collect.Lists;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
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

    static public List<StaticAbility> attackRestrict(final Card card, final Collection<Card> others) {
        return restrictCommon(StaticAbilityMode.AttackRestrict, card, others);
    }
    static public List<StaticAbility> blockRestrict(final Card card, final Collection<Card> others) {
        return restrictCommon(StaticAbilityMode.BlockRestrict, card, others);
    }

    /**
     * Statics that restrict the card depending on which other creatures attack or block with it.
     */
    static public List<StaticAbility> getRestrictions(StaticAbilityMode mode, final Card card) {
        final Game game = card.getGame();
        List<StaticAbility> result = Lists.newArrayList();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(mode)) {
                    continue;
                }
                if (!stAb.matchesValidParam("ValidCard", card)) {
                    continue;
                }
                result.add(stAb);
            }
        }
        return result;
    }

    static public List<StaticAbility> restrictCommon(StaticAbilityMode mode, final Card card, final Collection<Card> others) {
        List<StaticAbility> result = getRestrictions(mode, card);
        result.removeIf(stAb -> !isViolated(stAb, card, others));
        return result;
    }

    /**
     * Whether a restriction of the card stays unfulfilled even if all of the potential others join in.
     */
    static public boolean cantBeFulfilled(StaticAbilityMode mode, final Card card, final Collection<Card> potentialOthers) {
        for (final StaticAbility stAb : getRestrictions(mode, card)) {
            if (needsOthers(stAb) && isViolated(stAb, card, potentialOthers)) {
                return true;
            }
        }
        return false;
    }

    static public boolean countsAsOther(StaticAbility stAb, Card card, Card other) {
        if (other.equals(card)) {
            return false;
        }
        // ValidOthersRelative is seen from the restricted card instead of the host
        return stAb.matchesValidParam("ValidOthers", other) && stAb.matchesValidParam("ValidOthersRelative", other, card);
    }

    static public int countOthers(StaticAbility stAb, Card card, Collection<Card> others) {
        int size = 0;
        for (final Card other : others) {
            if (countsAsOther(stAb, card, other)) {
                size++;
            }
        }
        return size;
    }

    static public boolean isViolated(StaticAbility stAb, Card card, Collection<Card> others) {
        return isViolated(stAb, card, countOthers(stAb, card, others));
    }

    static public boolean isViolated(StaticAbility stAb, Card card, int others) {
        return !Expressions.compare(others, getOthersCompare(stAb), getOthersAmount(stAb, card));
    }

    private static String getOthersCompare(StaticAbility stAb) {
        return stAb.getParamOrDefault("OthersCompare", "GE1").substring(0, 2);
    }

    static public int getOthersAmount(StaticAbility stAb, Card card) {
        return AbilityUtils.calculateAmount(card, stAb.getParamOrDefault("OthersCompare", "GE1").substring(2), stAb);
    }

    /**
     * More others can only help to fulfill this restriction.
     */
    static public boolean needsOthers(StaticAbility stAb) {
        final String compare = getOthersCompare(stAb);
        return compare.equals("GE") || compare.equals("GT");
    }

    /**
     * More others can only break this restriction.
     */
    static public boolean limitsOthers(StaticAbility stAb) {
        final String compare = getOthersCompare(stAb);
        return compare.equals("LT") || compare.equals("LE");
    }
}
