package forge.game.staticability;

import forge.game.card.Card;
import forge.game.card.CounterType;
import forge.game.player.Player;

public class StaticAbilityCantPutCounter {

    public static boolean applyCantPutCounter(final StaticAbility staticAbility, final Card card,
            final CounterType type) {
        final Card hostCard = staticAbility.getHostCard();

        if (staticAbility.hasParam("CounterType")) {
            CounterType t = CounterType.valueOf(staticAbility.getParam("CounterType"));
            if (t != null && !type.equals(t)) {
                return false;
            }
        }

        // for the other part
        if (staticAbility.hasParam("ValidCard")) {
            return card.isValid(staticAbility.getParam("ValidCard").split(","), hostCard.getController(), hostCard, null);
        } else return !staticAbility.hasParam("ValidPlayer");

    }

    public static boolean applyCantPutCounter(final StaticAbility staticAbility, final Player player,
            final CounterType type) {
        final Card hostCard = staticAbility.getHostCard();

        if (staticAbility.hasParam("CounterType")) {
            CounterType t = CounterType.valueOf(staticAbility.getParam("CounterType"));
            if (t != null && !type.equals(t)) {
                return false;
            }
        }

        // for the other part
        if (staticAbility.hasParam("ValidPlayer")) {
            return player.isValid(staticAbility.getParam("ValidPlayer").split(","), hostCard.getController(), hostCard, null);
        } else return !staticAbility.hasParam("ValidCard");

    }
}
