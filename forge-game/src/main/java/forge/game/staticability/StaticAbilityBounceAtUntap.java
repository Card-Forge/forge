package forge.game.staticability;

import forge.game.card.Card;
import forge.game.zone.ZoneType;

public class StaticAbilityBounceAtUntap {

    public static boolean shouldBounceAtUntap(Card card) {
        for (final Card ca : card.getGame().getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(StaticAbilityMode.BounceAtUntap)) {
                    continue;
                }
                if (applyBounceAtUntapAbility(stAb, card)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean applyBounceAtUntapAbility(StaticAbility stAb, Card card) {
        if (!stAb.matchesValidParam("ValidCard", card)) {
            return false;
        }
        return true;
    }
}
