package forge.ai;

import forge.card.mana.ManaAtom;
import forge.game.GameActionUtil;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.mana.Mana;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.OptionalCost;
import forge.game.spellability.OptionalCostValue;
import forge.game.spellability.SpellAbility;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.util.List;

public class AIIntegrationTests extends AITest {
    @Test
    public void testSwingForLethal() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        p.setTeam(0);
        addCard("Nest Robber", p);
        addCard("Nest Robber", p);

        Player opponent = game.getPlayers().get(0);
        opponent.setTeam(1);

        addCard("Runeclaw Bear", opponent);
        opponent.setLife(2, null);

        this.playUntilPhase(game, PhaseType.END_OF_TURN);

        AssertJUnit.assertTrue(game.isGameOver());
    }

    @Test
    public void testSuspendAI() {
        // Test that the AI can cast a suspend spell
        // They should suspend it, then deal three damage to their opponent
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        p.setTeam(0);
        addCard("Mountain", p);
        addCardToZone("Rift Bolt", p, ZoneType.Hand);

        Player opponent = game.getPlayers().get(0);
        opponent.setTeam(1);

        // Fill deck with lands so we can goldfish a few turns
        for (int i = 0; i < 60; i++) {
            addCardToZone("Island", opponent, ZoneType.Library);
            // Add something they can't cast
            addCardToZone("Stone Golem", p, ZoneType.Library);
        }

        for (int i = 0; i < 3; i++) {
            this.playUntilNextTurn(game);
        }

        AssertJUnit.assertEquals(17, opponent.getLife());
    }

    @Test
    public void testAttackTriggers() {
        // Test that attack triggers actually trigger
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        p.setTeam(0);
        addCard("Hellrider", p);
        addCard("Raging Goblin", p);

        Player opponent = game.getPlayers().get(0);
        opponent.setTeam(1);

        // Fill deck with lands so we can goldfish a few turns
        for (int i = 0; i < 60; i++) {
            addCardToZone("Island", opponent, ZoneType.Library);
            // Add something they can't cast
            addCardToZone("Stone Golem", p, ZoneType.Library);
        }

        this.playUntilNextTurn(game);

        AssertJUnit.assertEquals(14, opponent.getLife());
    }

    @Test
    public void testDoesNotCastRepopulateWhenNoCreaturesInOpponentGraveyard() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        p.setTeam(0);

        Player opponent = game.getPlayers().get(0);
        opponent.setTeam(1);

        // AI has Repopulate in hand, mana to cast, and no other playable cards.
        Zone hand = p.getZone(ZoneType.Hand);
        Card repopulate = addCardToZone("Repopulate", p, ZoneType.Hand);
        AssertJUnit.assertTrue(hand.contains(repopulate));

        // Setup mana
        Card colorlessMana = createCard("Ash Barrens", p);
        Card greenMana = createCard("Forest", p);
        p.getManaPool().addMana(new Mana((byte) ManaAtom.COLORLESS, colorlessMana, null, p));
        p.getManaPool().addMana(new Mana((byte) ManaAtom.GREEN, greenMana, null, p));
        AssertJUnit.assertEquals(2, p.getManaPool().totalMana());

        // Put a non-creature card in opponent's graveyard
        addCardToZone("Swamp", opponent, ZoneType.Graveyard);

        // Opponent has 0 creatures in graveyard.
        AssertJUnit.assertFalse(opponent.getZone(ZoneType.Graveyard).getCards().anyMatch(Card::isCreature));

        this.playUntilPhase(game, PhaseType.END_OF_TURN);

        AssertJUnit.assertTrue("Repopulate must still be in hand", hand.contains(repopulate));
    }

    @Test
    public void testCrushContrabandChoosesBothModes() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        p.setTeam(0);

        Card crushContraband = addCardToZone("Crush Contraband", p, ZoneType.Hand);
        SpellAbility sa = crushContraband.getSpellAbilities().get(0);
        sa.setActivatingPlayer(p);

        Player opponent = game.getPlayers().get(0);
        opponent.setTeam(1);
        addCard("Sol Ring", opponent);
        addCard("Honor of the Pure", opponent);

        AiPlayDecision decision = ((PlayerControllerAi) p.getController()).getAi().canPlaySa(sa);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decision);
        AssertJUnit.assertEquals("AI should choose both available modes", 2, sa.getChosenList().size());
    }

    @Test
    public void testEscalateDoesNotChooseUnaffordableExtraMode() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        p.setTeam(0);
        addCard("Forest", p);
        addCard("Wastes", p);

        Card collectiveResistance = addCardToZone("Collective Resistance", p, ZoneType.Hand);
        SpellAbility sa = collectiveResistance.getSpellAbilities().get(0);
        sa.setActivatingPlayer(p);

        Player opponent = game.getPlayers().get(0);
        opponent.setTeam(1);
        addCard("Sol Ring", opponent);
        addCard("Honor of the Pure", opponent);

        AiPlayDecision decision = ((PlayerControllerAi) p.getController()).getAi().canPlaySa(sa);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decision);
        AssertJUnit.assertEquals("AI should not choose an extra Escalate mode it cannot pay for", 1, sa.getChosenList().size());
    }

    @Test
    public void testEscalateChoosesAffordableExtraMode() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        p.setTeam(0);
        addCards("Forest", 3, p);

        Card collectiveResistance = addCardToZone("Collective Resistance", p, ZoneType.Hand);
        SpellAbility sa = collectiveResistance.getSpellAbilities().get(0);
        sa.setActivatingPlayer(p);

        Player opponent = game.getPlayers().get(0);
        opponent.setTeam(1);
        addCard("Sol Ring", opponent);
        addCard("Honor of the Pure", opponent);

        AiPlayDecision decision = ((PlayerControllerAi) p.getController()).getAi().canPlaySa(sa);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decision);
        AssertJUnit.assertEquals("AI should choose an extra Escalate mode when it is useful and payable", 2, sa.getChosenList().size());
    }

    @Test
    public void testEntwineChoosesCostWhenAllModesAreUseful() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        p.setTeam(0);
        addCards("Forest", 6, p);
        addCardToZone("Forest", p, ZoneType.Library);
        addCardToZone("Plains", p, ZoneType.Library);

        Card journey = addCardToZone("Journey of Discovery", p, ZoneType.Hand);
        SpellAbility sa = journey.getSpellAbilities().get(0);
        sa.setActivatingPlayer(p);

        List<OptionalCostValue> optionalCosts = GameActionUtil.getOptionalCostValues(sa);
        List<OptionalCostValue> chosenCosts = p.getController().chooseOptionalCosts(sa, optionalCosts);

        AssertJUnit.assertTrue("AI should pay Entwine when both modes are useful",
                chosenCosts.stream().anyMatch(cost -> cost.getType() == OptionalCost.Entwine));
    }

    @Test
    public void testSpreeDoesNotChooseUnaffordableExtraMode() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        p.setTeam(0);
        addCard("Plains", p);
        addCard("Wastes", p);

        Card requisitionRaid = addCardToZone("Requisition Raid", p, ZoneType.Hand);
        SpellAbility sa = requisitionRaid.getSpellAbilities().get(0);
        sa.setActivatingPlayer(p);

        Player opponent = game.getPlayers().get(0);
        opponent.setTeam(1);
        addCard("Sol Ring", opponent);
        addCard("Honor of the Pure", opponent);

        AiPlayDecision decision = ((PlayerControllerAi) p.getController()).getAi().canPlaySa(sa);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decision);
        AssertJUnit.assertEquals("AI should not choose a Spree mode it cannot pay for", 1, sa.getChosenList().size());
    }

    @Test
    public void testSpreeChoosesAffordableExtraMode() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        p.setTeam(0);
        addCard("Plains", p);
        addCards("Wastes", 2, p);

        Card requisitionRaid = addCardToZone("Requisition Raid", p, ZoneType.Hand);
        SpellAbility sa = requisitionRaid.getSpellAbilities().get(0);
        sa.setActivatingPlayer(p);

        Player opponent = game.getPlayers().get(0);
        opponent.setTeam(1);
        addCard("Sol Ring", opponent);
        addCard("Honor of the Pure", opponent);

        AiPlayDecision decision = ((PlayerControllerAi) p.getController()).getAi().canPlaySa(sa);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decision);
        AssertJUnit.assertEquals("AI should choose an extra Spree mode when it is useful and payable", 2, sa.getChosenList().size());
    }
}
