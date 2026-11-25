package forge.game.staticability;

import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.zone.ZoneType;

public class StaticAbilityCantPreventDamage {

    public static boolean cantPreventDamage(final Card source, final boolean isCombat) {
        return cantPreventDamage(source, null, isCombat);
    }

    // target: the entity the damage would be dealt to; null when unknown (AI forecast of source-only checks)
    public static boolean cantPreventDamage(final Card source, final GameEntity target, final boolean isCombat) {
        CardCollection list = new CardCollection(source.getGame().getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES));
        list.add(source);
        for (final Card ca : list) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(StaticAbilityMode.CantPreventDamage)) {
                    continue;
                }
                if (applyCantPreventDamage(stAb, source, target, isCombat)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean applyCantPreventDamage(final StaticAbility stAb, final Card source, final GameEntity target, final boolean isCombat) {
        if (stAb.hasParam("IsCombat")) {
            if (stAb.getParam("IsCombat").equals("True") != isCombat) {
                return false;
            }
        }

        if (!stAb.matchesValidParam("ValidSource", source)) {
            return false;
        }
        // Affected$ restricts which damaged entity the effect protects (e.g. Whippoorwill)
        if (stAb.hasParam("Affected")) {
            if (target == null || !stAb.matchesValidParam("Affected", target)) {
                return false;
            }
        }
        return true;
    }

}
