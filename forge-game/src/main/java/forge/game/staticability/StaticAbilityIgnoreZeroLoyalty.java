package forge.game.staticability;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.zone.ZoneType;

public class StaticAbilityIgnoreZeroLoyalty {
    public static boolean ignorePlaneswalkerZeroLoyaltyRule(final Card card)  {
        final Game game = card.getGame();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(StaticAbilityMode.IgnorePlaneswalkerZeroLoyaltyRule)) {
                    continue;
                }

                if (applyIgnorePlaneswalkerZeroLoyaltyRuleAbility(stAb, card)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean applyIgnorePlaneswalkerZeroLoyaltyRuleAbility(final StaticAbility stAb, final Card card) {
        return stAb.matchesValidParam("ValidCard", card);
    }
}
