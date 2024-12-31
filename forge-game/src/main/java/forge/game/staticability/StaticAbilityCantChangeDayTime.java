package forge.game.staticability;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.zone.ZoneType;

public class StaticAbilityCantChangeDayTime {

    static String MODE = "CantChangeDayTime";

    public static boolean cantChangeDay(final Game game, Boolean value) {
        if (value == null) {
            return false;
        }
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(MODE)) {
                    continue;
                }
                if (cantChangeDayCheck(stAb, value)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean cantChangeDayCheck(final StaticAbility stAb, final Boolean value) {
        if (stAb.hasParam("NewTime")) {
            switch(stAb.getParam("NewTime")) {
            case "Day":
                if (value != false) {
                    return false;
                }
            case "Night":
                if (value != true) {
                    return false;
                }
            }
        }
        return true;
    }
}
