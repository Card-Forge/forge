package forge.game.staticability;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CounterType;
import forge.game.zone.ZoneType;

public class StaticAbilityMaxCounter {
    static String MODE = "MaxCounter";

    public static Integer maxCounter(final Card c, final CounterType type) {
        final Game game = c.getGame();

        Integer result = null;
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(MODE)) {
                    continue;
                }
                if (applyMaxCounter(stAb, c, type)) {
                    int value = AbilityUtils.calculateAmount(stAb.getHostCard(), stAb.getParam("MaxNum"), stAb);
                    if (result == null || result > value) {
                        result = value;
                    }
                }
            }
        }
        return result;
    }

    protected static boolean applyMaxCounter(StaticAbility stAb, final Card c, final CounterType type) {
        if (stAb.hasParam("CounterType")) {
            CounterType t = CounterType.getType(stAb.getParam("CounterType"));
            if (t != null && !type.equals(t)) {
                return false;
            }
        }
        if (!stAb.matchesValidParam("ValidCard", c)) {
            return false;
        }
        return true;
    }
}
