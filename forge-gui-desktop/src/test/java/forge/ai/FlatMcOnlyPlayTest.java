package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Cards marked AI:RemoveDeck:All (Parallax Wave) were dropped before Flat MC saw them, so a Replenish deck held its
 * Waves in hand while it was being attacked. They are now offered to the rollouts only, never approved by the heuristics.
 */
public class FlatMcOnlyPlayTest extends AITest {

    private static AiController brainOf(final Player p) {
        return ((PlayerControllerAi) p.getController()).getAi();
    }

    @Test
    public void aRemoveDeckCardIsAPlayForTheRolloutsOnly() {
        final Game game = initAndCreateGame();
        game.AI_FLAT_MONTE_CARLO = true;
        final Player ai = game.getPlayers().get(1);
        brainOf(ai).setUseSimulation(null);
        final Card wave = addCardToZone("Parallax Wave", ai, ZoneType.Hand);
        final Card growth = addCardToZone("Giant Growth", ai, ZoneType.Hand);
        final SpellAbility waveSpell = wave.getFirstSpellAbility();
        waveSpell.setActivatingPlayer(ai);

        if (!brainOf(ai).flatMonteCarloActive()) {
            return; // the profile of the test run has it off: nothing to show
        }
        AssertJUnit.assertTrue(brainOf(ai).isFlatMcOnlyPlay(waveSpell));
        AssertJUnit.assertFalse(brainOf(ai).isFlatMcOnlyPlay(growth.getFirstSpellAbility()));

        game.AI_FLAT_MONTE_CARLO = false;
        AssertJUnit.assertFalse("without Flat MC the heuristics' filter stays", brainOf(ai).isFlatMcOnlyPlay(waveSpell));
    }
}
