package forge.game.staticability;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.zone.ZoneType;

public class StaticAbilityCrewValue {

    static String MODE = "CrewValue";

    public static boolean hasAnyCrewValue(final Card card) {
        final Game game = card.getGame();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.getParam("Mode").equals(MODE) || stAb.isSuppressed() || !stAb.checkConditions()) {
                    continue;
                }
                if (hasAnyCrewValue(stAb, card)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean hasAnyCrewValue(final StaticAbility stAb, final Card card) {
        return stAb.matchesValidParam("ValidCard", card);
    }

    public static boolean crewsWithToughness(final Card card) {
        final Game game = card.getGame();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.getParam("Mode").equals(MODE) || stAb.isSuppressed() || !stAb.checkConditions()) {
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
                if (!stAb.getParam("Mode").equals(MODE) || stAb.isSuppressed() || !stAb.checkConditions()) {
                    continue;
                }
                int t = Integer.parseInt(stAb.getParam("Value"));
                i = i + t;
            }
        }
        return i;
    }

}
