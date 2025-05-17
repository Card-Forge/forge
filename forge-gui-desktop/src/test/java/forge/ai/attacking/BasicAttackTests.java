package forge.ai.attacking;

import forge.ai.PlayerControllerAi;
import forge.ai.simulation.SimulationTest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

public class BasicAttackTests extends SimulationTest {

    @Test
    public void assaultForLethal() {
        Game game = initAndCreateGame();
        Player attacker = game.getPlayers().get(1);
        Player defender = game.getPlayers().get(0);

        defender.setLife(10, null);

        String bears = "Grizzly Bears";

        // Add 5 bears and make sure they can attack
        Card bear1 = addCard(bears, attacker);
        Card bear2 = addCard(bears, attacker);
        Card bear3 = addCard(bears, attacker);
        Card bear4 = addCard(bears, attacker);
        Card bear5 = addCard(bears, attacker);

        // Make sure the bears can attack
        bear1.setSickness(false);
        bear2.setSickness(false);
        bear3.setSickness(false);
        bear4.setSickness(false);
        bear5.setSickness(false);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, attacker);
        game.getAction().checkStateEffects(true);

        // Get the simulated game state after AI has declared attackers
        Combat combat = ((PlayerControllerAi)attacker.getController()).getAi().getPredictedCombat();

        // Check how many creatures are attacking
        int attackingCreatures = combat.getAttackers().size();
        AssertJUnit.assertEquals("AI should attack with all 5 bears for lethal", 5, attackingCreatures);
    }

    @Test
    public void assaultWhenAheadIncludesTrade() {
        Game game = initAndCreateGame();
        Player attacker = game.getPlayers().get(1);
        Player defender = game.getPlayers().get(0);

        defender.setLife(10, null);

        String bears = "Grizzly Bears";

        Card bear1 = addCard(bears, attacker);
        Card bear2 = addCard(bears, attacker);
        addCard(bears, defender);

        // Make sure the bears can attack
        bear1.setSickness(false);
        bear2.setSickness(false);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, attacker);
        game.getAction().checkStateEffects(true);

        // Get the simulated game state after AI has declared attackers
        Combat combat = ((PlayerControllerAi)attacker.getController()).getAi().getPredictedCombat();

        // Check how many creatures are attacking
        int attackingCreatures = combat.getAttackers().size();
        AssertJUnit.assertEquals("AI should attack with all 2 bears", 2, attackingCreatures);
    }

    @Test
    public void assaultWhenAheadAgainstDeathtouch() {
        Game game = initAndCreateGame();
        Player attacker = game.getPlayers().get(1);
        Player defender = game.getPlayers().get(0);

        defender.setLife(10, null);

        String bears = "Grizzly Bears";
        String deathtouch = "Ankle Biter";

        Card bear1 = addCard(bears, attacker);
        Card bear2 = addCard(bears, attacker);
        Card bear3 = addCard(bears, attacker);
        addCard(deathtouch, defender);

        // Make sure the bears can attack
        bear1.setSickness(false);
        bear2.setSickness(false);
        bear3.setSickness(false);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, attacker);
        game.getAction().checkStateEffects(true);

        // Get the simulated game state after AI has declared attackers
        Combat combat = ((PlayerControllerAi)attacker.getController()).getAi().getPredictedCombat();

        // Check how many creatures are attacking
        int attackingCreatures = combat.getAttackers().size();
        AssertJUnit.assertEquals("AI should attack with all 3 bears", 3, attackingCreatures);
    }

    @Test
    public void noAttackAgainstDeathtouchTrade() {
        Game game = initAndCreateGame();
        Player attacker = game.getPlayers().get(1);
        Player defender = game.getPlayers().get(0);

        defender.setLife(10, null);

        String bears = "Grizzly Bears";
        String deathtouch = "Ankle Biter";

        Card bear1 = addCard(bears, attacker);
        addCard(deathtouch, defender);

        // Make sure the bears can attack
        bear1.setSickness(false);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, attacker);
        game.getAction().checkStateEffects(true);

        // Get the simulated game state after AI has declared attackers
        Combat combat = ((PlayerControllerAi)attacker.getController()).getAi().getPredictedCombat();

        // Check how many creatures are attacking
        int attackingCreatures = combat.getAttackers().size();
        AssertJUnit.assertEquals("AI should not attack", 0, attackingCreatures);
    }
}
