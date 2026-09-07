package forge.ai.blocking;

import com.google.common.collect.Lists;

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

    // Engulfing Slagwurm destroys every creature that blocks it, before combat damage: a blocker
    // spent on it deals no damage, and against trample it soaks none either.
    private int blockersAgainst(String attackerName, String grantedKeyword, int defenderLife,
            String blockerName, int blockerCount) {
        Game game = initAndCreateGame();
        Player attacker = game.getPlayers().get(1);
        Player defender = game.getPlayers().get(0);

        defender.setLife(defenderLife, null);

        Card threat = addCard(attackerName, attacker);
        threat.setSickness(false);
        if (grantedKeyword != null) {
            threat.addChangedCardKeywords(Lists.newArrayList(grantedKeyword), null, false,
                    game.getNextTimestamp(), null);
        }
        for (int i = 0; i < blockerCount; i++) {
            addCard(blockerName, defender);
        }

        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_ATTACKERS, attacker);
        game.getAction().checkStateEffects(true);

        // the combat has to be the game's own, or the AI's combat trigger checks find no combat
        Combat combat = new Combat(attacker);
        combat.addAttacker(threat, defender);
        game.getPhaseHandler().setCombat(combat);

        ((PlayerControllerAi) attacker.getController()).getAi().declareBlockersFor(defender, combat);
        return combat.getBlockers(threat).size();
    }

    @Test
    public void noGangBlockAgainstBlockerDestroyer() {
        // two Craw Wurms deal 12 and only one of them looks killable, so the AI used to gang up
        // and kill nothing: both are destroyed on block, before they deal any damage
        AssertJUnit.assertEquals("AI should not gang block a creature that destroys its blockers",
                0, blockersAgainst("Engulfing Slagwurm", null, 20, "Craw Wurm", 3));
        // but it should still chump when the damage would otherwise be lethal
        AssertJUnit.assertEquals("AI should still chump block to survive",
                1, blockersAgainst("Engulfing Slagwurm", null, 9, "Craw Wurm", 3));
    }

    @Test
    public void noChumpBlockAgainstTramplingBlockerDestroyer() {
        // the chump dies before damage, so all of it tramples through anyway (the same board
        // without trample is the chump asserted above)
        AssertJUnit.assertEquals("AI should not chump block a trampler that destroys its blockers",
                0, blockersAgainst("Engulfing Slagwurm", "Trample", 9, "Craw Wurm", 3));
    }

    @Test
    public void noNonLethalGangBlockAgainstBlockerDestroyer() {
        // 0/8 walls survive 7 damage, so this double block looks free - but they are destroyed
        // before the damage step, and the attacker takes none
        AssertJUnit.assertEquals("AI should not make a 'nobody dies' block that kills both blockers",
                0, blockersAgainst("Engulfing Slagwurm", "Menace", 20, "Wall of Stone", 2));
        // the same block against a creature with no such trigger is still worth making
        AssertJUnit.assertEquals("AI should still block a menace attacker its walls survive",
                2, blockersAgainst("Craw Wurm", "Menace", 20, "Wall of Stone", 2));
    }

}
