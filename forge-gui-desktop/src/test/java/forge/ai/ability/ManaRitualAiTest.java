package forge.ai.ability;

import forge.ai.AITest;
import forge.ai.PlayerControllerAi;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.util.List;

public class ManaRitualAiTest extends AITest {
    @Test
    public void darkRitualFundsAndCastsTheIntendedPayoff() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card firstSwamp = addCard("Swamp", ai);
        final Card secondSwamp = addCard("Swamp", ai);
        addCardToZone("Dark Ritual", ai, ZoneType.Hand);
        addCardToZone("Juggernaut", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);

        final PlayerControllerAi controller = (PlayerControllerAi) ai.getController();
        final SpellAbility ritual = chooseAbility(controller);
        AssertJUnit.assertEquals("Dark Ritual", ritual.getHostCard().getName());

        playAndResolve(controller, game, ritual);
        AssertJUnit.assertEquals(1, countCardsWithName(game, "Dark Ritual", ZoneType.Graveyard));
        AssertJUnit.assertEquals(3, ai.getManaPool().totalMana());

        final SpellAbility payoff = chooseAbility(controller);
        AssertJUnit.assertEquals("Juggernaut", payoff.getHostCard().getName());
        playAndResolve(controller, game, payoff);

        AssertJUnit.assertTrue(ai.isCardInPlay("Juggernaut"));
        AssertJUnit.assertTrue(firstSwamp.isTapped());
        AssertJUnit.assertTrue(secondSwamp.isTapped());
        AssertJUnit.assertEquals(0, ai.getManaPool().totalMana());
    }

    @Test
    public void darkRitualIsKeptInHandWithoutAPayoff() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCard("Swamp", ai);
        addCard("Swamp", ai);
        final Card ritual = addCardToZone("Dark Ritual", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);

        final List<SpellAbility> choices = ((PlayerControllerAi) ai.getController())
                .chooseSpellAbilityToPlay();

        AssertJUnit.assertNull(choices);
        AssertJUnit.assertTrue(ritual.isInZone(ZoneType.Hand));
    }

    @Test
    public void seethingSongFundsAndCastsARedPayoff() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCards("Mountain", 3, ai);
        addCardToZone("Seething Song", ai, ZoneType.Hand);
        addCardToZone("Fire Elemental", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);

        final PlayerControllerAi controller = (PlayerControllerAi) ai.getController();
        final SpellAbility ritual = chooseAbility(controller);
        AssertJUnit.assertEquals("Seething Song", ritual.getHostCard().getName());
        playAndResolve(controller, game, ritual);
        AssertJUnit.assertEquals(5, ai.getManaPool().totalMana());

        final SpellAbility payoff = chooseAbility(controller);
        AssertJUnit.assertEquals("Fire Elemental", payoff.getHostCard().getName());
        playAndResolve(controller, game, payoff);
        AssertJUnit.assertTrue(ai.isCardInPlay("Fire Elemental"));
        AssertJUnit.assertEquals(0, ai.getManaPool().totalMana());
    }

    @Test
    public void battleHymnUsesItsDynamicManaAmountToCastAPayoff() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCards("Mountain", 2, ai);
        addCards("Runeclaw Bear", 5, ai);
        addCardToZone("Battle Hymn", ai, ZoneType.Hand);
        addCardToZone("Fire Elemental", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);

        final PlayerControllerAi controller = (PlayerControllerAi) ai.getController();
        final SpellAbility ritual = chooseAbility(controller);
        AssertJUnit.assertEquals("Battle Hymn", ritual.getHostCard().getName());
        playAndResolve(controller, game, ritual);
        AssertJUnit.assertEquals(5, ai.getManaPool().totalMana());

        final SpellAbility payoff = chooseAbility(controller);
        AssertJUnit.assertEquals("Fire Elemental", payoff.getHostCard().getName());
        playAndResolve(controller, game, payoff);
        AssertJUnit.assertTrue(ai.isCardInPlay("Fire Elemental"));
    }

    @Test
    public void apprenticeWizardFundsAndCastsAColorlessPayoff() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card wizard = addCard("Apprentice Wizard", ai);
        wizard.setSickness(false);
        final Card island = addCard("Island", ai);
        final Card wastes = addCard("Wastes", ai);
        addCardToZone("Juggernaut", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);

        final PlayerControllerAi controller = (PlayerControllerAi) ai.getController();
        final SpellAbility ritual = chooseAbility(controller);
        AssertJUnit.assertEquals("Apprentice Wizard", ritual.getHostCard().getName());
        playAndResolve(controller, game, ritual);

        AssertJUnit.assertTrue(wizard.isTapped());
        AssertJUnit.assertTrue(island.isTapped());
        AssertJUnit.assertFalse(wastes.isTapped());
        AssertJUnit.assertEquals(3, ai.getManaPool().totalMana());

        final SpellAbility payoff = chooseAbility(controller);
        AssertJUnit.assertEquals("Juggernaut", payoff.getHostCard().getName());
        playAndResolve(controller, game, payoff);
        AssertJUnit.assertTrue(ai.isCardInPlay("Juggernaut"));
        AssertJUnit.assertTrue(wastes.isTapped());
    }

    private static SpellAbility chooseAbility(PlayerControllerAi controller) {
        final List<SpellAbility> choices = controller.chooseSpellAbilityToPlay();
        AssertJUnit.assertNotNull(choices);
        AssertJUnit.assertEquals(1, choices.size());
        return choices.get(0);
    }

    private static void playAndResolve(PlayerControllerAi controller, Game game, SpellAbility ability) {
        controller.playChosenSpellAbility(ability);
        if (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
        }
    }
}
