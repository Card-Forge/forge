package forge.game.staticability;

import forge.game.CardTraitBase;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.zone.ZoneType;

public class StaticAbilityCantBeBeamedUp {

    public static boolean cantBeBeamedUp(final Card card, final CardTraitBase sa)  {
        final Game game = card.getGame();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(StaticAbilityMode.CantBeBeamedUp)) {
                    continue;
                }

                if (applyCantBeBeamedUpAbility(stAb, card, sa)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean applyCantBeBeamedUpAbility(final StaticAbility stAb, final Card card, final CardTraitBase sa) {
        if (!stAb.matchesValidParam("ValidCard", card)) {
            return false;
        }
        return stAb.matchesValidParam("ValidSA", sa);
    }
}
