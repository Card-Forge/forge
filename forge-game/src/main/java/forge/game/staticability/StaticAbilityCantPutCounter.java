package forge.game.staticability;

import forge.game.card.Card;
import forge.game.card.CounterType;
import forge.game.player.Player;

public class StaticAbilityCantPutCounter {

    public static boolean applyCantPutCounter(final StaticAbility stAb, final Card card, final CounterType type) {
        if (stAb.hasParam("CounterType")) {
            CounterType t = CounterType.getType(stAb.getParam("CounterType"));
            if (t != null && !type.equals(t)) {
                return false;
            }
        }

        // for the other part
        if (!stAb.matchesValidParam("ValidCard", card)) {
            return false;
        } else if (stAb.hasParam("ValidPlayer")) {
            // for the other part
            return false;
        }
        return true;
    }

    public static boolean applyCantPutCounter(final StaticAbility stAb, final Player player, final CounterType type) {
        if (stAb.hasParam("CounterType")) {
            CounterType t = CounterType.getType(stAb.getParam("CounterType"));
            if (t != null && !type.equals(t)) {
                return false;
            }
        }

        // for the other part
        if (!stAb.matchesValidParam("ValidPlayer", player)) {
            return false;
        } else if (stAb.hasParam("ValidCard")) {
            // for the other part
            return false;
        }
        return true;
    }
}
