package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** Regression tests for AI targeting of discard spells. */
public class FlatMonteCarloDiscardTargetTest extends AITest {

    private static AiController brain(final Player player) {
        return ((PlayerControllerAi) player.getController()).getAi();
    }

    private void addComboScanCards(final Player player) {
        // ComboPlan does not inspect decks with fewer than twelve nonland spells.
        for (int i = 0; i < 12; i++) {
            addCardToZone("Lightning Bolt", player, ZoneType.Library);
        }
    }

    @Test
    public void mindTwistDoesNotTargetItselfForRandomDiscard() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        ai.setTeam(0);
        opp.setTeam(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);

        addComboScanCards(ai);
        addCardToZone("Bridge from Below", ai, ZoneType.Hand);
        final Card mindTwist = addCardToZone("Mind Twist", ai, ZoneType.Hand);
        addCards("Swamp", 6, ai);
        addCardToZone("Brainstorm", opp, ZoneType.Hand);
        addCardToZone("Counterspell", opp, ZoneType.Hand);
        addCardToZone("Lightning Bolt", opp, ZoneType.Hand);

        final SpellAbility sa = mindTwist.getSpellAbilities().get(0);
        sa.setActivatingPlayer(ai);

        // Prove that the old self-discard exception would otherwise prefer the AI.
        AssertJUnit.assertTrue(brain(ai).deckPlan(ai).combos.selfDiscardWanted(ai, mindTwist));
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, brain(ai).canPlaySa(sa));
        AssertJUnit.assertSame("random discard must target the opponent", opp,
                sa.getTargets().getFirstTargetedPlayer());
    }

    @Test
    public void selectiveDiscardCanStillTargetItselfForReanimatorFuel() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        ai.setTeam(0);
        opp.setTeam(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);

        addComboScanCards(ai);
        addCardToZone("Bridge from Below", ai, ZoneType.Hand);
        final Card thoughtseize = addCardToZone("Thoughtseize", ai, ZoneType.Hand);
        addCards("Swamp", 1, ai);
        addCardToZone("Brainstorm", opp, ZoneType.Hand);
        addCardToZone("Counterspell", opp, ZoneType.Hand);

        final SpellAbility sa = thoughtseize.getSpellAbilities().get(0);
        sa.setActivatingPlayer(ai);

        AssertJUnit.assertTrue(brain(ai).deckPlan(ai).combos.selfDiscardWanted(ai, thoughtseize));
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, brain(ai).canPlaySa(sa));
        AssertJUnit.assertSame("selective discard may target the AI for graveyard value", ai,
                sa.getTargets().getFirstTargetedPlayer());
    }
}
