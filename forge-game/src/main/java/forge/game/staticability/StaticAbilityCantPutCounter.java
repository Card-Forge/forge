package forge.game.staticability;

import forge.game.card.Card;
import forge.game.card.CounterType;
import forge.game.player.Player;

public class StaticAbilityCantPutCounter {

    public static boolean applyCantPutCounter(final StaticAbility staticAbility, final Card card,
            final CounterType type) {
        final Card hostCard = staticAbility.getHostCard();

        if (staticAbility.hasParam("CounterType")) {
            CounterType t = CounterType.getType(staticAbility.getParam("CounterType"));
            if (t != null && !type.equals(t)) {
                return false;
            }
        }

        // for the other part
        if (staticAbility.hasParam("ValidCard")) {
            if (!card.isValid(staticAbility.getParam("ValidCard").split(","), hostCard.getController(), hostCard, null)) {
                return false;
            }
        } else if (staticAbility.hasParam("ValidPlayer")) {
            // for the other part
            return false;
        }
        return true;
    }

    public static boolean applyCantPutCounter(final StaticAbility staticAbility, final Player player,
            final CounterType type) {
        final Card hostCard = staticAbility.getHostCard();

        if (staticAbility.hasParam("CounterType")) {
            CounterType t = CounterType.getType(staticAbility.getParam("CounterType"));
            if (t != null && !type.equals(t)) {
                return false;
            }
        }

        // for the other part
        if (staticAbility.hasParam("ValidPlayer")) {
            if (!player.isValid(staticAbility.getParam("ValidPlayer").split(","), hostCard.getController(), hostCard, null)) {
                return false;
            }
        } else if (staticAbility.hasParam("ValidCard")) {
            // for the other part
            return false;
        }
        return true;
    }
}
