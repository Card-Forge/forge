package forge.game.staticability;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
    
public class StaticAbilityExhaust {

    static String MODE = "CanExhaust";

    public static boolean anyWithExhaust(final Player player) {
        final Game game = player.getGame();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(MODE)) {
                    continue;
                }
                if (applyWithExhaust(stAb, player)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean applyWithExhaust(final StaticAbility stAb, final Player player) {
        if (!stAb.matchesValidParam("ValidPlayer", player)) {
            return false;
        }

        return true;
    }
}