package forge.game.staticability;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.zone.ZoneType;

public class StaticAbilityCanBlockAmount {
    static public boolean canBlockAny(final Card blocker) {
        final Game game = blocker.getGame();

        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(StaticAbilityMode.CanBlockAmount)) {
                    continue;
                }
                if (isValid(stAb, blocker) && "Any".equals(stAb.getParam("Amount"))) {
                    return true;
                }
            }
        }
        return false;
    }
    
    static public int canBlockAmount(final Card blocker) {
        final Game game = blocker.getGame();

        int i = 0;
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(StaticAbilityMode.CanBlockAmount)) {
                    continue;
                }
                if (isValid(stAb, blocker) && !"Any".equals(stAb.getParam("Amount"))) {
                    i += AbilityUtils.calculateAmount(stAb.getHostCard(), stAb.getParamOrDefault("Amount", "1"), stAb);
                }
            }
        }
        return i;
        
    }

    static public boolean isValid(StaticAbility stAb, Card blocker) {
        if (!stAb.matchesValidParam("ValidCard", blocker)) {
            return false;
        }
        return true;
    }

}
