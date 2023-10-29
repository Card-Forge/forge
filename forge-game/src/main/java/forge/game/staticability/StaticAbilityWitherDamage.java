package forge.game.staticability;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.zone.ZoneType;

public class StaticAbilityWitherDamage {

    static String MODE = "WitherDamage";

    static public boolean isWitherDamage(Card source) {
        final Game game = source.getGame();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(MODE)) {
                    continue;
                }
                if (applyWitherDamageAbility(stAb, source)) {
                    return true;
                }
            }
        }
        return false;
    }

    static public boolean applyWitherDamageAbility(StaticAbility stAb, Card source) {
        if (!stAb.matchesValidParam("ValidCard", source)) {
            return false;
        }
        return true;
    }
}
