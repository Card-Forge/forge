package forge.game.staticability;

import forge.game.card.Card;
import forge.game.zone.ZoneType;

public class StaticAbilityMustBeBlockedByAll {

    public static boolean mustBeBlockedByAll(final Card attacker, final Card blocker) {
        final Card host = attacker; // Default host is attacker if keyword is on attacker
        
        // Check Static Abilities in the game (Global Static Abilities)
        for (final Card ca : attacker.getGame().getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(StaticAbilityMode.MustBeBlockedByAll)) {
                    continue;
                }
                if (applyMustBeBlockedByAll(stAb, attacker, blocker)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean applyMustBeBlockedByAll(final StaticAbility stAb, final Card attacker, final Card blocker) {
        // ValidCard defines which attacker is affected (e.g. "Creature.EnchantedBy")
        if (!stAb.matchesValidParam("ValidCard", attacker)) {
            return false;
        }

        // ValidBlocker defines which blockers must block (e.g. "Creature" or specific types)
        if (stAb.hasParam("ValidBlocker")) {
            if (!blocker.isValid(stAb.getParam("ValidBlocker"), attacker.getController(), attacker, stAb)) {
                return false;
            }
        }
        
        return true;
    }
}
