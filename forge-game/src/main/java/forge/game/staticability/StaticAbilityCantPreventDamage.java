package forge.game.staticability;

import forge.game.card.Card;

public class StaticAbilityCantPreventDamage {

    public StaticAbilityCantPreventDamage() {

    }

    public static boolean applyCantPreventDamage(final StaticAbility stAb, final Card source, final boolean isCombat) {
        if (stAb.hasParam("IsCombat")) {
            if (stAb.getParamOrDefault("IsCombat", "False").equals("True") != isCombat) {
                return false;
            }
        }

        if (!stAb.matchesValidParam("ValidSource", source)) {
            return false;
        }
        return true;
    }

}
