package forge.game.staticability;

import forge.game.IGame;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class StaticAbilityDevotion {

    public static int getDevotionMod(final Player player) {
        int i = 0;
        final IGame game = player.getGame();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(StaticAbilityMode.Devotion)) {
                    continue;
                }
                if (!stAb.matchesValidParam("ValidPlayer", player)) {
                    continue;
                }
                int t = Integer.parseInt(stAb.getParamOrDefault("Value", "1"));
                i += t;
            }
        }
        return i;
    }
}
