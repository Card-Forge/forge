package forge.game.staticability;

import forge.game.CardTraitBase;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.zone.ZoneType;

public class StaticAbilityCantTransform {

    static String MODE = "CantTransform";

    static public boolean cantTransform(Card card, CardTraitBase cause) {
        final Game game = card.getGame();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.getParam("Mode").equals(MODE) || stAb.isSuppressed() || !stAb.checkConditions()) {
                    continue;
                }
                if (applyCantTransformAbility(stAb, card, cause)) {
                    return true;
                }
            }
        }
        return false;
    }

    static public boolean applyCantTransformAbility(StaticAbility stAb, Card card, CardTraitBase cause) {
        if (!stAb.matchesValidParam("ValidCard", card)) {
            return false;
        }
        if (stAb.hasParam("ExceptCause")) {
            if (stAb.matchesValidParam("ExceptCause", cause)) {
                return false;
            }
        }
        return true;
    }
}
