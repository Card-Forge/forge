package forge.game.staticability;

import forge.game.card.Card;
import forge.game.zone.ZoneType;

public class StaticAbilityCantGainControl {

    public static boolean cantGainControl(Card card) {
        for (final Card ca : card.getGame().getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(StaticAbilityMode.CantGainControl)) {
                    continue;
                }
                if (applyCantGainControlAbility(stAb, card)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean applyCantGainControlAbility(StaticAbility stAb, Card card) {
        if (!stAb.matchesValidParam("ValidCard", card)) {
            return false;
        }
        return true;
    }
}
