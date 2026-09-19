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

/**
 * "Whenever a creature attacks you, its controller loses 1 life" punishes the attacker rather than
 * the attacking creature, so at low life the AI has to count the attackers it sends. That was
 * previously only recognised on Revenge of Ravens by name, which left the AI walking into the
 * several other cards printed with the same trigger.
 */
public class AttackIntoLifeLossPunisherTest extends SimulationTest {

    private int attackersDeclaredAgainst(String punisher, int aiLife, int attackerCount) {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);

        ai.setLife(aiLife, null);
        opp.setLife(40, null);

        if (punisher != null) {
            addCard(punisher, opp);
        }
        for (int i = 0; i < attackerCount; i++) {
            Card attacker = addCard("Runeclaw Bear", ai);
            attacker.setSickness(false);
        }

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        Combat combat = ((PlayerControllerAi) ai.getController()).getAi().getPredictedCombat();
        return combat.getAttackers().size();
    }

    /** Sanity check: with nothing punishing the attack the AI is happy to swing. */
    @Test
    public void attacksFreelyWithNoPunisher() {
        AssertJUnit.assertEquals("AI should attack when nothing punishes it", 3,
                attackersDeclaredAgainst(null, 3, 3));
    }

    /** The case the name check already covered. */
    @Test
    public void holdsBackAgainstRevengeOfRavens() {
        AssertJUnit.assertEquals("AI should not attack into lethal life loss", 0,
                attackersDeclaredAgainst("Revenge of Ravens", 3, 3));
    }

    /** Same trigger, different card - the AI used to attack straight into these. */
    @Test
    public void holdsBackAgainstHissingMiasma() {
        AssertJUnit.assertEquals("AI should not attack into lethal life loss", 0,
                attackersDeclaredAgainst("Hissing Miasma", 3, 3));
    }

    @Test
    public void holdsBackAgainstBloodReckoning() {
        AssertJUnit.assertEquals("AI should not attack into lethal life loss", 0,
                attackersDeclaredAgainst("Blood Reckoning", 3, 3));
    }

    /**
     * Circle of Flame damages the attacking creature rather than its controller, so it costs the
     * AI no life and must not be treated as one of these.
     */
    @Test
    public void stillAttacksIntoCircleOfFlame() {
        AssertJUnit.assertTrue("Circle of Flame costs the attacker no life, so the AI should still attack",
                attackersDeclaredAgainst("Circle of Flame", 3, 3) > 0);
    }
}
