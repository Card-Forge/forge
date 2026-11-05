package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.simulation.ComboGuard;
import forge.ai.simulation.ComboPlan;
import forge.game.Game;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Pacts (Pact of Negation, Summoner's Pact, Slaughter Pact, Pact of the Titan, Intervention Pact): the AI loses the game
 * when it cannot pay at its next upkeep, so Flat Monte Carlo must not cast one it cannot pay for.
 */
public class FlatMonteCarloPactGuardTest extends AITest {

    private SpellAbility pact(final Game game, final Player ai, final String name) {
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        final SpellAbility sa = addCardToZone(name, ai, ZoneType.Hand).getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        return sa;
    }

    @Test
    public void thePactCostIsReadFromTheScript() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        AssertJUnit.assertEquals("3 U U", ComboGuard.pactCost(pact(game, ai, "Pact of Negation")));
        AssertJUnit.assertEquals("2 B", ComboGuard.pactCost(pact(game, ai, "Slaughter Pact")));
        AssertJUnit.assertNull(ComboGuard.pactCost(pact(game, ai, "Lightning Bolt")));
    }

    @Test
    public void aPactIsRefusedWithTooFewLands() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        for (int i = 0; i < 3; i++) {
            addCard("Island", ai);
        }
        final SpellAbility sa = pact(game, ai, "Pact of Negation");
        AssertJUnit.assertEquals("pact-unpayable", ComboGuard.problem(ai, sa, null, ComboPlan.EMPTY));
    }

    @Test
    public void aPactIsRefusedWithoutTheColor() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        for (int i = 0; i < 6; i++) {
            addCard("Mountain", ai);
        }
        final SpellAbility sa = pact(game, ai, "Pact of Negation");
        AssertJUnit.assertEquals("pact-unpayable", ComboGuard.problem(ai, sa, null, ComboPlan.EMPTY));
    }

    @Test
    public void aPactIsAllowedWhenTheCostCanBePaid() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        for (int i = 0; i < 3; i++) {
            addCard("Island", ai);
        }
        for (int i = 0; i < 2; i++) {
            addCard("Swamp", ai);
        }
        final SpellAbility sa = pact(game, ai, "Pact of Negation");
        AssertJUnit.assertNull(ComboGuard.problem(ai, sa, null, ComboPlan.EMPTY));
    }

    @Test
    public void oneShotManaDoesNotPayForThePact() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        for (int i = 0; i < 3; i++) {
            addCard("Island", ai);
        }
        addCard("Lotus Petal", ai);
        addCard("Lion's Eye Diamond", ai);
        final SpellAbility sa = pact(game, ai, "Pact of Negation");
        AssertJUnit.assertEquals("pact-unpayable", ComboGuard.problem(ai, sa, null, ComboPlan.EMPTY));
    }

    @Test
    public void aBlackPactIsRefusedWithOnlyIslands() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        for (int i = 0; i < 5; i++) {
            addCard("Island", ai);
        }
        AssertJUnit.assertEquals("pact-unpayable",
                ComboGuard.problem(ai, pact(game, ai, "Slaughter Pact"), null, ComboPlan.EMPTY));
    }

    @Test
    public void aPactIsAllowedWhenTheLandDropIsStillToCome() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        addCard("Swamp", ai);
        addCard("Swamp", ai);
        addCardToZone("Swamp", ai, ZoneType.Hand);
        final SpellAbility sa = addCardToZone("Slaughter Pact", ai, ZoneType.Hand).getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertNull(ComboGuard.problem(ai, sa, null, ComboPlan.EMPTY));
    }
}
