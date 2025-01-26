package forge.game.staticability;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class StaticAbilityTapPowerValue {

    static String MODE = "TapPowerValue";

    public static boolean withToughness(final Card card, final SpellAbility sa) {
        final Game game = card.getGame();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(MODE)) {
                    continue;
                }
                if (withToughness(stAb, card, sa)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean withToughness(final StaticAbility stAb, final Card card, final SpellAbility sa) {
        if (!stAb.getParam("Value").equals("Toughness")) {
            return false;
        }
        if (!stAb.matchesValidParam("ValidCard", card)) {
            return false;
        }
        if (!stAb.matchesValidParam("ValidSA", sa)) {
            return false;
        }
        return true;
    }

    public static int getMod(final Card card, SpellAbility sa) {
        int i = 0;
        final Game game = card.getGame();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(MODE)) {
                    continue;
                }
                if (!stAb.matchesValidParam("ValidCard", card)) {
                    continue;
                }
                if (!stAb.matchesValidParam("ValidSA", sa)) {
                    continue;
                }
                int t = Integer.parseInt(stAb.getParam("Value"));
                i = i + t;
            }
        }
        return i;
    }

}
