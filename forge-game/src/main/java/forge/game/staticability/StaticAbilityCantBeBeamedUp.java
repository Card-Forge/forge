package forge.game.staticability;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.zone.ZoneType;

public class StaticAbilityCantBeBeamedUp {

    public static boolean cantBeBeamedUp(final Card card)  {
        final Game game = card.getGame();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(StaticAbilityMode.CantBeBeamedUp)) {
                    continue;
                }

                if (applyCantBeBeamedUpAbility(stAb, card)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean applyCantBeBeamedUpAbility(final StaticAbility stAb, final Card card) {
        return stAb.matchesValidParam("ValidCard", card);
    }
}
