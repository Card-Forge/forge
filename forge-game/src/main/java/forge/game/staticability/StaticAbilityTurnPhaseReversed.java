package forge.game.staticability;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class StaticAbilityTurnPhaseReversed {
    public static boolean isTurnReversed(Player player) {
        return anyTurnPhaseReversed(player, StaticAbilityMode.TurnReversed);
    }
    public static boolean isPhaseReversed(Player player) {
        return anyTurnPhaseReversed(player, StaticAbilityMode.PhaseReversed);
    }

    protected static boolean anyTurnPhaseReversed(Player player, final StaticAbilityMode mode)
    {
        boolean result = false;
        final Game game = player.getGame();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(mode)) {
                    continue;
                }
                if (applyTurnPhaseReversed(stAb, player)) {
                    result = !result;
                }
            }
        }
        return result;
    }

    protected static boolean applyTurnPhaseReversed(StaticAbility stAb, Player player) {
        if (!stAb.matchesValidParam("ValidPlayer", player)) {
            return false;
        }

        return true;
    }
}
