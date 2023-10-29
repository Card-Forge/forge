package forge.game.staticability;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.zone.ZoneType;

public class StaticAbilityAssignCombatDamageAsUnblocked {

    static String MODE = "AssignCombatDamageAsUnblocked";

    public static boolean assignCombatDamageAsUnblocked(final Card card) {
        return assignCombatDamageAsUnblocked(card, true);
    }

    public static boolean assignCombatDamageAsUnblocked(final Card card, final boolean optional)  {
        final Game game = card.getGame();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(MODE)) {
                    continue;
                }

                if (stAb.hasParam("Optional")) {
                    if (!optional) {
                        continue;
                    }
                } else {
                    if (optional) {
                        continue;
                    }
                }

                if (applyAssignCombatDamageAsUnblocked(stAb, card)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean applyAssignCombatDamageAsUnblocked(final StaticAbility stAb, final Card card) {
        if (!stAb.matchesValidParam("ValidCard", card)) {
            return false;
        }
        return true;
    }
}
