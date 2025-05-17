package forge.ai.blocking;

import forge.ai.ComputerUtilCard;
import forge.ai.PlayerControllerAi;
import forge.ai.simulation.SimulationTest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

public class BasicBlockTests extends SimulationTest {

    @Test
    public void noBlockingVsDeathtouch() {
        Game game = initAndCreateGame();
        Player attacker = game.getPlayers().get(1);
        Player defender = game.getPlayers().get(0);

        defender.setLife(10, null);

        String bears = "Grizzly Bears";
        String deathtouch = "Ankle Biter";

        Card bear1 = addCard(bears, defender);
        Card dtAttacker = addCard(deathtouch, attacker);

        int bearEvaluation = ComputerUtilCard.evaluateCreature(bear1);
        int dtEvaluation = ComputerUtilCard.evaluateCreature(dtAttacker);

        // Make sure the bears can attack
        bear1.setSickness(false);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, attacker);
        game.getAction().checkStateEffects(true);

        // Get the simulated game state after AI has declared attackers
        Combat combat = ((PlayerControllerAi)attacker.getController()).getAi().getPredictedCombat();
        combat.addAttacker(dtAttacker, defender);
        ((PlayerControllerAi)attacker.getController()).getAi().declareBlockersFor(defender, combat);

        // Check how many creatures are attacking
        int blockers = combat.getBlockers(dtAttacker).size();
        AssertJUnit.assertEquals("AI should not block", 0, blockers);
    }

}
