package forge.game.staticability;

import forge.game.CardTraitBase;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.zone.ZoneType;

public class StaticAbilityTapPowerValue {

    public static boolean withToughness(final Card card, final CardTraitBase ctb) {
        final Game game = card.getGame();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(StaticAbilityMode.TapPowerValue)) {
                    continue;
                }
                if (withToughness(stAb, card, ctb)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean withToughness(final StaticAbility stAb, final Card card, final CardTraitBase ctb) {
        if (!stAb.getParam("Value").equals("Toughness")) {
            return false;
        }
        if (!stAb.matchesValidParam("ValidCard", card)) {
            return false;
        }
        if (!stAb.matchesValidParam("ValidSA", ctb)) {
            return false;
        }
        return true;
    }

    public static int getMod(final Card card, final CardTraitBase ctb) {
        int i = 0;
        final Game game = card.getGame();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(StaticAbilityMode.TapPowerValue)) {
                    continue;
                }
                if (!stAb.matchesValidParam("ValidCard", card)) {
                    continue;
                }
                if (!stAb.matchesValidParam("ValidSA", ctb)) {
                    continue;
                }
                i += Integer.parseInt(stAb.getParam("Value"));
            }
        }
        return i;
    }

}
