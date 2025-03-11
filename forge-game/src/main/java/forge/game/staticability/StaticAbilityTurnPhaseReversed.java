package forge.game.staticability;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class StaticAbilityTurnPhaseReversed {
    static String TURN_MODE = "TurnReversed";
    static String PHASE_MODE = "PhaseReversed";

    public static boolean isTurnReversed(Player player) {
        return anyTurnPhaseReversed(player, TURN_MODE);
    }
    public static boolean isPhaseReversed(Player player) {
        return anyTurnPhaseReversed(player, PHASE_MODE);
    }

    protected static boolean anyTurnPhaseReversed(Player player, final String mode)
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
