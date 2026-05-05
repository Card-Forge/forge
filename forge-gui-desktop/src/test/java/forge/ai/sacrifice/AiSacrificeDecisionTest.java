package forge.ai.sacrifice;

import forge.ai.AITest;
import forge.ai.PlayerControllerAi;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

/**
 * Tests for AI sacrifice decision-making when facing lethal consequences.
 */
public class AiSacrificeDecisionTest extends AITest {

    @Test
    public void testSimulationSacrificeChoiceIsDeterministic() {
        Game game = initAndCreateGame();

        Player ai = game.getPlayers().get(1);
        SpellAbility source = sacrificeSource(ai);

        Card bear = addCard("Runeclaw Bear", ai);
        addCard("Serra Angel", ai);
        addCard("Colossal Dreadmaw", ai);

        CardCollection validTargets = creatures(ai);

        for (int i = 0; i < 5; i++) {
            CardCollectionView chosen = chooseWithSimulationController(game, ai, source, 1, validTargets);

            AssertJUnit.assertEquals("Simulation sacrifice choice should be stable", 1, chosen.size());
            AssertJUnit.assertEquals("AI should consistently choose the least valuable creature",
                    bear, chosen.get(0));
        }
    }

    @Test
    public void testSimulationSacrificeChoiceUsesOnlyLegalCandidates() {
        Game game = initAndCreateGame();

        Player ai = game.getPlayers().get(1);
        SpellAbility source = sacrificeSource(ai);

        addCard("Runeclaw Bear", ai);
        Card angel = addCard("Serra Angel", ai);
        Card dreadmaw = addCard("Colossal Dreadmaw", ai);

        CardCollection validTargets = new CardCollection();
        validTargets.add(angel);
        validTargets.add(dreadmaw);

        CardCollectionView chosen = chooseWithSimulationController(game, ai, source, 1, validTargets);

        AssertJUnit.assertEquals("AI should choose one legal creature to sacrifice", 1, chosen.size());
        AssertJUnit.assertEquals("AI should choose the least valuable legal creature",
                angel, chosen.get(0));
    }

    @Test
    public void testSimulationSacrificeChoiceChoosesWorstCreaturesFirst() {
        Game game = initAndCreateGame();

        Player ai = game.getPlayers().get(1);
        SpellAbility source = sacrificeSource(ai);

        Card bear = addCard("Runeclaw Bear", ai);
        Card angel = addCard("Serra Angel", ai);
        Card dreadmaw = addCard("Colossal Dreadmaw", ai);

        CardCollectionView chosen = chooseWithSimulationController(game, ai, source, 2, creatures(ai));

        AssertJUnit.assertEquals("AI should choose the requested number of creatures", 2, chosen.size());
        AssertJUnit.assertTrue("AI should sacrifice Runeclaw Bear before better creatures", chosen.contains(bear));
        AssertJUnit.assertTrue("AI should sacrifice Serra Angel before Colossal Dreadmaw", chosen.contains(angel));
        AssertJUnit.assertFalse("AI should preserve the most valuable creature when sacrificing two of three",
                chosen.contains(dreadmaw));
    }

    @Test
    public void testSimulationSacrificeChoiceCanChooseLeastValuablePermanent() {
        Game game = initAndCreateGame();

        Player ai = game.getPlayers().get(1);
        SpellAbility source = sacrificeSource(ai);

        Card ornithopter = addCard("Ornithopter", ai);
        addCard("Sol Ring", ai);
        addCard("Serra Angel", ai);

        CardCollection validTargets = new CardCollection(ai.getCardsIn(ZoneType.Battlefield));

        CardCollectionView chosen = chooseWithSimulationController(game, ai, source, 1, validTargets);

        AssertJUnit.assertEquals("AI should choose one permanent to sacrifice", 1, chosen.size());
        AssertJUnit.assertEquals("AI should choose the cheapest artifact/enchantment in a mixed permanent set",
                ornithopter, chosen.get(0));
    }

    @Test
    public void testAiSacrificesCreatureToAvoidLethalLifeLoss() {
        Game game = initAndCreateGame();

        Player p = game.getPlayers().get(1);
        p.setTeam(0);

        Player opponent = game.getPlayers().get(0);
        opponent.setTeam(1);
        opponent.setLife(1, null);

        // Player 1 controls Fandaniel
        addCard("Fandaniel, Telophoroi Ascian", p);

        // Player 1 has a sorcery in graveyard (causes 2 life loss per sorcery/instant)
        addCardToZone("The Crystal's Chosen", p, ZoneType.Graveyard);

        // Opponent controls a creature they can sacrifice
        Card adelbert = addCard("Adelbert Steiner", opponent);

        fillLibraries(p, opponent, 10);

        // Play until next turn (after Fandaniel's end step trigger resolves)
        this.playUntilNextTurn(game);

        // The game should NOT be over - opponent should have sacrificed to survive
        AssertJUnit.assertFalse("Game should not be over - AI should have sacrificed to survive", game.isGameOver());

        assertInZone(opponent, adelbert, ZoneType.Graveyard,
                "Adelbert Steiner should be in graveyard after being sacrificed");

        // Opponent should still have 1 life (didn't lose life because they sacrificed)
        AssertJUnit.assertEquals("Opponent should still have 1 life after sacrificing", 1, opponent.getLife());
    }


    @Test
    public void testAiDoesNotSacrificeWhenLifeLossIsNotLethal() {
        Game game = initAndCreateGame();

        Player p = game.getPlayers().get(1);
        p.setTeam(0);

        Player opponent = game.getPlayers().get(0);
        opponent.setTeam(1);
        opponent.setLife(10, null);

        // Player 1 controls Fandaniel
        addCard("Fandaniel, Telophoroi Ascian", p);

        // Player 1 has a sorcery in graveyard
        addCardToZone("The Crystal's Chosen", p, ZoneType.Graveyard);

        // Opponent controls a creature
        Card adelbert = addCard("Adelbert Steiner", opponent);

        fillLibraries(p, opponent, 10);

        // Play until next turn (after Fandaniel's end step trigger resolves)
        this.playUntilNextTurn(game);

        // Game should not be over
        AssertJUnit.assertFalse("Game should not be over", game.isGameOver());

        assertInZone(opponent, adelbert, ZoneType.Battlefield,
                "Adelbert Steiner should still be on battlefield when life loss isn't lethal");

        // Opponent should have 8 life (lost 2 from not sacrificing)
        AssertJUnit.assertEquals("Opponent should have lost 2 life from not sacrificing", 8, opponent.getLife());
    }


    @Test
    public void testAiSacrificesToPillarTombsToAvoidLethalLifeLoss() {
        Game game = initAndCreateGame();

        Player p = game.getPlayers().get(1);
        p.setTeam(0);

        Player opponent = game.getPlayers().get(0);
        opponent.setTeam(1);
        opponent.setLife(4, null);

        // Player 1 controls Pillar Tombs of Aku
        addCard("Pillar Tombs of Aku", p);

        // Opponent controls a creature they can sacrifice
        Card bear = addCard("Runeclaw Bear", opponent);

        fillLibraries(p, opponent, 10);

        // Play two turns - first ends Player 1's turn, second plays through opponent's upkeep
        // where Pillar Tombs triggers and the AI must decide whether to sacrifice
        this.playUntilNextTurn(game);
        this.playUntilNextTurn(game);

        // The game should NOT be over - AI should have sacrificed to survive
        AssertJUnit.assertFalse("Game should not be over - AI should have sacrificed to survive", game.isGameOver());

        assertInZone(opponent, bear, ZoneType.Graveyard,
                "Runeclaw Bear should be in graveyard after being sacrificed");

        // Opponent should still have 4 life (didn't lose life because they sacrificed)
        AssertJUnit.assertEquals("Opponent should still have 4 life after sacrificing", 4, opponent.getLife());
    }

    private SpellAbility sacrificeSource(Player ai) {
        SpellAbility source = createCard("Innocent Blood", ai).getFirstSpellAbility();
        source.setActivatingPlayer(ai);
        return source;
    }

    private CardCollection creatures(Player player) {
        CardCollection creatures = new CardCollection();
        for (Card card : player.getCardsIn(ZoneType.Battlefield)) {
            if (card.isCreature()) {
                creatures.add(card);
            }
        }
        return creatures;
    }

    private void fillLibraries(Player first, Player second, int count) {
        for (int i = 0; i < count; i++) {
            addCardToZone("Plains", first, ZoneType.Library);
            addCardToZone("Plains", second, ZoneType.Library);
        }
    }

    private void assertInZone(Player player, Card card, ZoneType zone, String message) {
        AssertJUnit.assertTrue(message, player.getZone(zone).contains(card));
    }

    private CardCollectionView chooseWithSimulationController(Game game, Player ai, SpellAbility source,
            int amount, CardCollectionView validTargets) {
        PlayerControllerAi controller = new PlayerControllerAi(game, ai, ai.getLobbyPlayer());
        controller.setUseSimulation(true);

        final CardCollectionView[] chosen = new CardCollectionView[1];
        ai.runWithController(() -> chosen[0] = ai.getController().choosePermanentsToSacrifice(
                source, amount, amount, validTargets, "Choose permanents to sacrifice"), controller);
        return chosen[0];
    }
}
