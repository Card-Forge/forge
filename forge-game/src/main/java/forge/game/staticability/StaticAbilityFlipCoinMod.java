package forge.game.staticability;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class StaticAbilityFlipCoinMod {

    public static Boolean fixedResult(final Player player) {
        final Game game = player.getGame();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(StaticAbilityMode.FlipCoinMod)) {
                    continue;
                }
                if (applyFlipCoinMod(stAb, player)) {
                    return Boolean.valueOf(stAb.getParam("Result"));
                }
            }
        }
        return null;
    }

    private static boolean applyFlipCoinMod(final StaticAbility stAb, final Player player) {
        if (!stAb.matchesValidParam("ValidPlayer", player)) {
            return false;
        }
        return true;
    }
}
