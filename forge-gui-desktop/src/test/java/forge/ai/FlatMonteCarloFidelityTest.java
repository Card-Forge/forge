package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.simulation.GameSimulator;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;

/**
 * The copied game a rollout plays in must remember what happened this turn and must survive being made while a combat
 * is going on, otherwise plays that depend on it are judged wrongly (or not at all).
 */
public class FlatMonteCarloFidelityTest extends AITest {

    @Test
    public void copyRemembersWhatLeftTheBattlefieldThisTurn() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card bear = addCard("Grizzly Bears", ai);
        game.getAction().destroy(bear, null, false, null);
        AssertJUnit.assertEquals("the real game saw it leave", 1, game.getLeftBattlefieldThisTurn().size());

        final GameSimulator sim = new GameSimulator(game, ai);
        final Game copy = sim.getSimulatedGameState();
        AssertJUnit.assertEquals("revolt needs this in the copy", 1, copy.getLeftBattlefieldThisTurn().size());
        AssertJUnit.assertEquals("Grizzly Bears", copy.getLeftBattlefieldThisTurn().get(0).getName());
    }

    @Test
    public void copyOfAnEmptyTurnHasNoHistory() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCard("Grizzly Bears", ai);
        final Game copy = new GameSimulator(game, ai).getSimulatedGameState();
        AssertJUnit.assertTrue(copy.getLeftBattlefieldThisTurn().isEmpty());
    }

    @Test
    public void copyDuringCombatSurvivesAPlaneswalkerThatLeft() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        final Card attacker = addCard("Grizzly Bears", opp);
        attacker.setSickness(false);
        final Card walker = addCard("Jace Beleren", ai);
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_ATTACKERS, opp, false);
        game.getPhaseHandler().setCombat(new forge.game.combat.Combat(opp));
        game.getCombat().addAttacker(attacker, walker);
        game.getAction().destroy(walker, null, false, null);
        // the defender is gone from the game, still named in the combat: copying must not throw
        final Game copy = new GameSimulator(game, ai).getSimulatedGameState();
        AssertJUnit.assertNotNull(copy);
    }
}
