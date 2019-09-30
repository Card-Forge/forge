package forge.game.staticability;

import forge.game.card.Card;

public class StaticAbilityCantPreventDamage {

    public StaticAbilityCantPreventDamage() {

    }

    public static boolean applyCantPreventDamage(final StaticAbility st, final Card source, final boolean isCombat) {
        final Card hostCard = st.getHostCard();

        if (st.hasParam("IsCombat")) {
            if (st.getParamOrDefault("IsCombat", "False").equals("True") != isCombat) {
                return false;
            }
        }

        if (st.hasParam("ValidSource")) {
            if (!source.isValid(st.getParam("ValidSource").split(","), hostCard.getController(), hostCard, null)) {
                return false;
            }
        }
        return true;
    }

}
