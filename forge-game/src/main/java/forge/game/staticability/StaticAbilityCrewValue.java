package forge.game.staticability;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.zone.ZoneType;

public class StaticAbilityCrewValue {

    static String MODE = "CrewValue";

    public static boolean crewsWithToughness(final Card card) {
        final Game game = card.getGame();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(MODE)) {
                    continue;
                }
                if (crewsWithToughness(stAb, card)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean crewsWithToughness(final StaticAbility stAb, final Card card) {
        return stAb.getParam("Value").equals("Toughness") && stAb.matchesValidParam("ValidCard", card);
    }

    public static int getCrewMod(final Card card) {
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
                int t = Integer.parseInt(stAb.getParam("Value"));
                i = i + t;
            }
        }
        return i;
    }

}
