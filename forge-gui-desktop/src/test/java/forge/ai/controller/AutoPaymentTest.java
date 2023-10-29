package forge.ai.controller;

import forge.ai.simulation.GameSimulator;
import forge.ai.simulation.SimulationTest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.util.List;


public class AutoPaymentTest extends SimulationTest {

    @Test
    public void dontPayWithAshnodsAltar() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        String llanowar = "Llanowar Elves";

        Card elf = addCard(llanowar,  p);
        elf.setSickness(false);
        Card altar = addCard("Ashnod's Altar", p);
        Card treasure = addToken("c_a_treasure_sac", p);

        // Two choices tap elf and sac treasure
        // OR Sac elf to Altar

        String stone = "Mind Stone";
        Card mindstone = addCardToZone(stone, p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        GameSimulator sim = createSimulator(game, p);
        int score = sim.simulateSpellAbility(mindstone.getFirstSpellAbility()).value;

        AssertJUnit.assertTrue(score > 0);
        Game simGame = sim.getSimulatedGameState();

        Card mindstoneBF = findCardWithName(simGame, stone);
        AssertJUnit.assertNotNull(mindstoneBF);

        Card elfCopy = findCardWithName(simGame, llanowar);
        AssertJUnit.assertNotNull(elfCopy);
    }

    @Test
    public void payWithTreasuresOverPhyrexianAltar() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        String squire = "Squire";

        List<Card> squires = addCards(squire, 6,  p);
        Card altar = addCard("Phyrexian Altar", p);
        List<Card> treasures = addTokens("c_a_treasure_sac", 6, p);

        String shivan = "Shivan Dragon";
        Card dragon = addCardToZone(shivan, p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        GameSimulator sim = createSimulator(game, p);
        int score = sim.simulateSpellAbility(dragon.getFirstSpellAbility()).value;

        AssertJUnit.assertTrue(score > 0);
        Game simGame = sim.getSimulatedGameState();

        Card dragonBF = findCardWithName(simGame, shivan);
        AssertJUnit.assertNotNull(dragonBF);

        Card squireCopy = findCardWithName(simGame, squire);
        AssertJUnit.assertNotNull(squireCopy);

        Card treasureCopy = findCardWithName(simGame, "Treasure Token");
        AssertJUnit.assertNull(treasureCopy);
    }
}
