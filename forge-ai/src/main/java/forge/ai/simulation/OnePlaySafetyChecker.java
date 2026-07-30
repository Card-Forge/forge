package forge.ai.simulation;

import forge.ai.simulation.GameStateEvaluator.Score;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public final class OnePlaySafetyChecker {
    private static final ThreadLocal<Boolean> CHECKING = ThreadLocal.withInitial(() -> false);

    public static boolean isAcceptable(Player player, SpellAbility sa) {
        // Forge keeps the parent ability on the stack while it resolves. Score actions offered
        // during that resolution incrementally; priority responses need two full stack-resolution
        // branches and are not supported yet.
        if (sa == null || CHECKING.get()
                || (!player.getGame().getStack().isEmpty()
                && !player.getGame().getStack().isResolving())) {
            return true;
        }

        CHECKING.set(true);
        try {
            Score originalScore = new GameStateEvaluator().getScoreForGameState(player.getGame(), player);
            SimulationController controller = new SimulationController(originalScore, 0);
            GameSimulator simulator = new GameSimulator(controller, player.getGame(), player, null);
            // TODO this doesn't respect heuristics shaping for the SA yet (targets etc.)
            Score resultScore = simulator.simulateSpellAbility(sa);
            Player simulatedPlayer = (Player) simulator.getGameCopier().find(player);

            if (simulatedPlayer == null) {
                return true;
            }
            if (simulatedPlayer.hasLost()) {
                return false;
            }
            return resultScore.value == Integer.MIN_VALUE
                    || (long) resultScore.value >= (long) originalScore.value - expectedCardScoreLoss(player, sa, simulator);
        } finally {
            CHECKING.remove();
        }
    }

    // sometimes simulation might not see the effect of some heuristics directly, so at least negate any card disadvantage
    private static int expectedCardScoreLoss(Player player, SpellAbility sa, GameSimulator simulator) {
        Card source = sa.getHostCard();
        if (source == null || !source.isInZone(ZoneType.Hand)) {
            return 0;
        }
        Card simulatedSource = (Card) simulator.getGameCopier().find(source);
        if (simulatedSource.isInZone(ZoneType.Hand)) {
            return 0;
        }
        // Match GameStateEvaluator: excess cards are worth one point, other hand cards five.
        return !player.isUnlimitedHandSize() && player.getCardsIn(ZoneType.Hand).size() > player.getMaxHandSize() ? 1 : 5;
    }
}
