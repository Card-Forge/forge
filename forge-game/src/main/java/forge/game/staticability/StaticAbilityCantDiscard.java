package forge.game.staticability;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class StaticAbilityCantDiscard {

    static String MODE = "CantDiscard";

    public static boolean cantDiscard(final Player player, final SpellAbility cause, final boolean effect)  {
        final Game game = player.getGame();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.getParam("Mode").equals(MODE) || stAb.isSuppressed() || !stAb.checkConditions()) {
                    continue;
                }

                if (applyCantDiscardAbility(stAb, player, cause, effect)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean applyCantDiscardAbility(final StaticAbility stAb, final Player player, final SpellAbility cause, final boolean effect) {
        if (!stAb.matchesValidParam("ValidPlayer", player)) {
            return false;
        }
        if (stAb.hasParam("ForCost")) {
            if ("True".equalsIgnoreCase(stAb.getParam("ForCost")) == effect) {
                return false;
            }
        }
        if (!stAb.matchesValidParam("ValidCause", cause)) {
            return false;
        }
        return true;
    }
}
