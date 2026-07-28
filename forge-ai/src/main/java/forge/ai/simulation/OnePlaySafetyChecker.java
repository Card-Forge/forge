package forge.ai.simulation;

import forge.ai.simulation.GameStateEvaluator.Score;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

public final class OnePlaySafetyChecker {
    private static final ThreadLocal<Boolean> CHECKING = ThreadLocal.withInitial(() -> false);

    public static boolean isAcceptable(Player player, SpellAbility sa) {
        // Forge keeps the parent ability on the stack while it resolves. Score actions offered
        // during that resolution incrementally; priority responses need two full stack-resolution
        // branches and are not supported yet.
        // Non-stack special actions such as suspend defer their payoff beyond this simulation.
        if (sa == null || CHECKING.get()
                || (!sa.isSpell() && !sa.isActivatedAbility() && !sa.isLandAbility())
                || (!player.getGame().getStack().isEmpty()
                && !player.getGame().getStack().isResolving())) {
            return true;
        }

        CHECKING.set(true);
        try {
            Score originalScore = new GameStateEvaluator().getScoreForGameState(player.getGame(), player);
            SimulationController controller = new SimulationController(originalScore, 0);
            GameSimulator simulator = new GameSimulator(controller, player.getGame(), player, null);
            Score resultScore = simulator.simulateSpellAbility(sa);
            Player simulatedPlayer = (Player) simulator.getGameCopier().find(player);

            if (simulatedPlayer == null) {
                return true;
            }
            if (simulatedPlayer.hasLost()) {
                return false;
            }
            return resultScore.value == Integer.MIN_VALUE
                    || resultScore.value >= originalScore.value;
        } finally {
            CHECKING.remove();
        }
    }
}
