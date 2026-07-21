package forge.game.staticability;

import forge.game.card.Card;
import forge.game.zone.ZoneType;

public class StaticAbilityAssignNoCombatDamage {

    public static boolean assignNoCombatDamage(final Card card) {
        if (hasAssignNoCombatDamageAbility(card, card)) {
            return true;
        }
        return !card.getGame().visitCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES, ca ->
                ca == card || !hasAssignNoCombatDamageAbility(ca, card));
    }

    private static boolean hasAssignNoCombatDamageAbility(final Card source, final Card card) {
        for (final StaticAbility stAb : source.getStaticAbilities()) {
            if (stAb.checkConditions(StaticAbilityMode.AssignNoCombatDamage)
                    && applyAssignNoCombatDamage(stAb, card)) {
                return true;
            }
        }
        return false;
    }

    public static boolean applyAssignNoCombatDamage(final StaticAbility stAb, final Card card) {
        if (!stAb.matchesValidParam("ValidCard", card)) {
            return false;
        }
        return true;
    }

}
