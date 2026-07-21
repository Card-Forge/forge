package forge.game.staticability;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.zone.ZoneType;

public class StaticAbilityCombatDamageToughness {

    public static boolean combatDamageToughness(final Card card)  {
        final Game game = card.getGame();
        return !game.visitCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES, ca -> {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(StaticAbilityMode.CombatDamageToughness)) {
                    continue;
                }

                if (applyCombatDamageToughnessAbility(stAb, card)) {
                    return false;
                }
            }
            return true;
        });
    }

    public static boolean applyCombatDamageToughnessAbility(final StaticAbility stAb, final Card card) {
        if (!stAb.matchesValidParam("ValidCard", card)) {
            return false;
        }
        return true;
    }
}
