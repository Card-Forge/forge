package forge.game.staticability;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class StaticAbilityDrawFromBottom {

    public static boolean drawsFromBottom(final Player player) {
        final Game game = player.getGame();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(StaticAbilityMode.DrawFromBottom)) {
                    continue;
                }
                if (stAb.matchesValidParam("ValidPlayer", player)) {
                    return true;
                }
            }
        }
        return false;
    }
}
