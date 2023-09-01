package forge.game.staticability;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.zone.ZoneType;

public class StaticAbilityIgnoreLegendRule {

    static String MODE = "IgnoreLegendRule";

    public static boolean ignoreLegendRule(final Card card)  {
        final Game game = card.getGame();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(MODE)) {
                    continue;
                }

                if (applyIgnoreLegendRuleAbility(stAb, card)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean applyIgnoreLegendRuleAbility(final StaticAbility stAb, final Card card) {
        if (!stAb.matchesValidParam("ValidCard", card)) {
            return false;
        }
        return true;
    }
}
