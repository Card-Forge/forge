package forge.ai.ability;

import forge.ai.AiPlayDecision;
import forge.ai.SpellAbilityAi;
import forge.ai.SpellApiToAi;
import forge.ai.simulation.SimulationTest;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import org.testng.annotations.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

public class CountersPutAiTest extends SimulationTest {

    @Test
    public void testPowerUp_savesAttackerFromLethalCombat_aiActivates() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);

        // Brave Brawler: 2/1, Power-Up {4}{W}: put two +1/+1 counters on this creature (becomes 4/3)
        Card brawler = addCard("Brave Brawler", ai);
        addCards("Plains", 5, ai);

        // Giant Spider: 2/4 - unpumped Brawler dies without trading; pumped Brawler (4/3) survives and kills it
        Card spider = addCard("Giant Spider", opponent);

        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, ai);
        game.getAction().checkStateEffects(true);

        Combat combat = new Combat(ai);
        combat.addAttacker(brawler, opponent);
        combat.addBlocker(brawler, spider);
        game.getPhaseHandler().setCombat(combat);

        SpellAbility powerUpSA = findSAWithPrefix(brawler, "Power-Up");
        assertNotNull(powerUpSA);
        powerUpSA.setActivatingPlayer(ai);

        SpellAbilityAi aiLogic = SpellApiToAi.Converter.get(ApiType.PutCounter);
        AiPlayDecision decision = aiLogic.canPlayWithSubs(ai, powerUpSA).decision();
        assertEquals("AI should activate Power-Up to save its attacker and win combat",
                AiPlayDecision.WillPlay, decision);
    }
}
