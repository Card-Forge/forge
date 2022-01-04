package forge.game.staticability;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.zone.ZoneType;

public class StaticAbilityCantSetSchemesInMotion {

    static String MODE = "CantSetSchemesInMotion";

    public static boolean any(final Game game) {
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.getParam("Mode").equals(MODE) || stAb.isSuppressed() || !stAb.checkConditions()) {
                    continue;
                }
                return true;
            }
        }
        return false;
    }
}
