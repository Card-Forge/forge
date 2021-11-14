package forge.game.staticability;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class StaticAbilityCantDraw {

    static String MODE = "CantDraw";

    public static boolean canDrawThisAmount(final Player player, int startAmount) {
        if (startAmount <= 0) {
            return true;
        }
        return startAmount <= canDrawAmount(player, startAmount);
    }
    public static int canDrawAmount(final Player player, int startAmount) {
        int amount = startAmount;
        if (startAmount <= 0)
            return 0;
        final Game game = player.getGame();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.getParam("Mode").equals(MODE) || stAb.isSuppressed() || !stAb.checkConditions()) {
                    continue;
                }
                amount = applyCantDrawAmountAbility(stAb, player, amount);
            }
        }
        return amount;
    }

    public static int applyCantDrawAmountAbility(final StaticAbility stAb, final Player player, int amount) {
        if (!stAb.matchesValidParam("ValidPlayer", player)) {
            return amount;
        }
        int limit = Integer.valueOf(stAb.getParamOrDefault("DrawLimit", "0"));
        int drawn = player.getNumDrawnThisTurn();
        return Math.min(Math.max(limit - drawn, 0), amount);
    }
}
