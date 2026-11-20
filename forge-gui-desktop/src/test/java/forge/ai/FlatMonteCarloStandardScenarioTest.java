package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Scenario tests for the Standard metagame combos: the real AI (Flat Monte Carlo on) is put in a first main phase and
 * plays it out, and what is on the battlefield afterwards says whether the combo was played. Each scenario has one
 * clearly best line; the negative ones show the AI holding back where the line would lose.
 */
public class FlatMonteCarloStandardScenarioTest extends AITest {
    private static final int LONG_TIMEOUT_MS = 180_000;

    private static AiController brainOf(final Player p) {
        return ((PlayerControllerAi) p.getController()).getAi();
    }

    private static final String DIMIR_EXCRUCIATOR = "3 Seasoned Cryomancer\n1 Uldaros Theorix\n2 Theorix Charm\n"
            + "2 Doomsday Excruciator\n4 Restless Reef\n3 Winternight Stories\n4 Deceit\n3 Duress\n7 Swamp\n"
            + "4 Superior Spider-Man\n3 Bitter Triumph\n2 Cavern of Souls\n4 Shipwreck Marsh\n1 Undercity Sewers\n"
            + "1 Deadly Cover-Up\n3 Requiting Hex\n2 Day of Black Sun\n4 Watery Grave\n1 Strategic Betrayal\n"
            + "4 Gloomlake Verge\n2 Emeritus of Ideation\n";

    private void dimirLibrary(final Player p) {
        for (final String line : DIMIR_EXCRUCIATOR.split("\n")) {
            final int space = line.indexOf(' ');
            for (int n = Integer.parseInt(line.substring(0, space)); n > 0; n--) {
                addCardToZone(line.substring(space + 1), p, ZoneType.Library);
            }
        }
    }

    private Game newGame() {
        final Game game = initAndCreateGame();
        game.AI_FLAT_MONTE_CARLO = true;
        game.AI_TIMEOUT = 10;
        game.getPlayers().get(1).setTeam(0);
        game.getPlayers().get(0).setTeam(1);
        return game;
    }

    private void libraryOf(final Player p, final int count, final String... names) {
        for (int i = 0; i < count; i++) {
            addCardToZone(names[i % names.length], p, ZoneType.Library);
        }
    }

    /**
     * The AI's turn from its first main phase to its end step, both main phases included: it casts most creatures in
     * the second one. A loop that never ends (Aang and Appa under Doc Aurlock) is cut off by the step limit.
     */
    private void playFirstMain(final Game game, final Player ai) {
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        for (int i = 0; i < 600 && !game.isGameOver() && !game.getPhaseHandler().is(PhaseType.END_OF_TURN)
                && game.getPhaseHandler().getPlayerTurn() == ai; i++) {
            game.getPhaseHandler().mainLoopStep();
        }
    }

    /** Aang is one card with two faces: after Waterbend {8} it is "Aang and La, Ocean's Fury" and still the same Aang. */
    private int aangsInPlay(final Game game) {
        return countCardsWithName(game, "Aang, Swift Savior", ZoneType.Battlefield)
                + countCardsWithName(game, "Aang and La, Ocean's Fury", ZoneType.Battlefield);
    }

    private static int tokens(final Player p) {
        int n = 0;
        for (final Card c : p.getCardsIn(ZoneType.Battlefield)) {
            if (c.isToken()) {
                n++;
            }
        }
        return n;
    }

    // ------------------------------------------------------------------------------------- reanimator

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void spiderManCopiesBringerFromTheGraveyardAndWipesTheBoard() {
        final Game game = newGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        libraryOf(ai, 20, "Forest", "Island", "Cache Grab", "Formidable Speaker", "Seasoned Cryomancer");
        libraryOf(opp, 20, "Plains");
        addCards("Swamp", 2, ai);
        addCards("Island", 2, ai);
        addCards("Grizzly Bears", 2, opp);
        addCardToZone("Bringer of the Last Gift", ai, ZoneType.Graveyard);
        addCardToZone("Superior Spider-Man", ai, ZoneType.Hand);

        playFirstMain(game, ai);

        AssertJUnit.assertEquals(gameStateToString(game), 1,
                countCardsWithName(game, "Superior Spider-Man", ZoneType.Battlefield));
        AssertJUnit.assertEquals("Bringer's enter trigger removed the opposing creatures", 0,
                countCardsWithName(game, "Grizzly Bears", ZoneType.Battlefield));
    }

    // ----------------------------------------------------------------------------------- Doc Aurlock

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void aangAirbendsAppaWhileDocAurlockMakesTheRecastFree() {
        final Game game = newGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        libraryOf(ai, 20, "Forest", "Plains", "Island", "Llanowar Elves", "Aang, Swift Savior", "Appa, Steadfast Guardian",
                "Doc Aurlock, Grizzled Genius");
        libraryOf(opp, 20, "Mountain");
        addCard("Plains", ai);
        addCard("Island", ai);
        addCard("Forest", ai);
        addCard("Doc Aurlock, Grizzled Genius", ai);
        addCard("Appa, Steadfast Guardian", ai);
        addCardToZone("Aang, Swift Savior", ai, ZoneType.Hand);

        playFirstMain(game, ai);

        // Aang airbends Appa, Appa is recast for nothing (an Ally token for every cast from exile) and airbends Aang
        // back; the loop is cut off after a number of casts so that the game is not a draw: both end up in play
        AssertJUnit.assertTrue("the loop made Ally tokens: " + tokens(ai) + "\n" + gameStateToString(game), tokens(ai) >= 3);
        AssertJUnit.assertTrue("the loop was cut off: " + tokens(ai), tokens(ai) <= 20);
        AssertJUnit.assertEquals(gameStateToString(game), 1, aangsInPlay(game));
        AssertJUnit.assertEquals(gameStateToString(game), 1,
                countCardsWithName(game, "Appa, Steadfast Guardian", ZoneType.Battlefield));
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void aangDoesNotAirbendItsOwnAppaWithoutDocAurlock() {
        final Game game = newGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        libraryOf(ai, 20, "Forest", "Plains", "Island", "Llanowar Elves", "Aang, Swift Savior", "Appa, Steadfast Guardian",
                "Doc Aurlock, Grizzled Genius");
        libraryOf(opp, 20, "Mountain");
        addCard("Plains", ai);
        addCard("Island", ai);
        addCard("Forest", ai);
        addCard("Appa, Steadfast Guardian", ai);
        addCardToZone("Aang, Swift Savior", ai, ZoneType.Hand);

        playFirstMain(game, ai);

        AssertJUnit.assertEquals("Appa stays where it is: " + gameStateToString(game), 1,
                countCardsWithName(game, "Appa, Steadfast Guardian", ZoneType.Battlefield));
        AssertJUnit.assertEquals(0, countCardsWithName(game, "Appa, Steadfast Guardian", ZoneType.Exile));
    }

    // ------------------------------------------------------------------------------ Temur Battlecrier

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void devastatingOnslaughtCopiesMarangRiverRegentAndBouncesTheBoard() {
        final Game game = newGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        libraryOf(ai, 20, "Forest", "Island", "Llanowar Elves", "Devastating Onslaught", "Marang River Regent",
                "Quantum Riddler", "Temur Battlecrier");
        libraryOf(opp, 20, "Mountain");
        addCards("Island", 5, ai);
        addCard("Mountain", ai);
        addCard("Marang River Regent", ai);
        addCards("Grizzly Bears", 3, opp);
        addCardToZone("Devastating Onslaught", ai, ZoneType.Hand);

        playFirstMain(game, ai);

        AssertJUnit.assertEquals(gameStateToString(game), 1,
                countCardsWithName(game, "Devastating Onslaught", ZoneType.Graveyard));
        AssertJUnit.assertTrue("the hasty copies are on the battlefield: " + gameStateToString(game),
                countCardsWithName(game, "Marang River Regent", ZoneType.Battlefield) >= 2);
    }

    // ------------------------------------------------------------------------------ Dimir Excruciator

    private Game excruciatorGame(final boolean withReef) {
        final Game game = newGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        dimirLibrary(ai);
        libraryOf(opp, 20, "Plains");
        if (withReef) {
            addCard("Restless Reef", ai);
        } else {
            addCard("Island", ai);
        }
        addCards("Swamp", 2, ai);
        addCard("Island", ai);
        addCardToZone("Doomsday Excruciator", ai, ZoneType.Graveyard);
        addCardToZone("Superior Spider-Man", ai, ZoneType.Hand);
        return game;
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void spiderManCopiesTheExcruciatorWhenARestlessReefIsThereToFinish() {
        final Game game = excruciatorGame(true);
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);

        playFirstMain(game, ai);

        AssertJUnit.assertEquals(gameStateToString(game), 1,
                countCardsWithName(game, "Superior Spider-Man", ZoneType.Battlefield));
        AssertJUnit.assertTrue("both libraries were cut: " + ai.getCardsIn(ZoneType.Library).size() + "/"
                + opp.getCardsIn(ZoneType.Library).size(),
                ai.getCardsIn(ZoneType.Library).size() <= 6 && opp.getCardsIn(ZoneType.Library).size() <= 6);
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void theAiDoesNotCutTheLibrariesWithoutAFinisher() {
        final Game game = excruciatorGame(false);
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);

        playFirstMain(game, ai);

        AssertJUnit.assertTrue("neither library may be cut: " + ai.getCardsIn(ZoneType.Library).size() + "/"
                + opp.getCardsIn(ZoneType.Library).size(),
                ai.getCardsIn(ZoneType.Library).size() > 6 && opp.getCardsIn(ZoneType.Library).size() > 6);
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void theAiDoesNotHardCastTheExcruciatorWithoutAFinisher() {
        final Game game = newGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        dimirLibrary(ai);
        libraryOf(opp, 20, "Plains");
        addCards("Swamp", 6, ai);
        addCardToZone("Doomsday Excruciator", ai, ZoneType.Hand);

        playFirstMain(game, ai);

        AssertJUnit.assertEquals(gameStateToString(game), 0,
                countCardsWithName(game, "Doomsday Excruciator", ZoneType.Battlefield));
        AssertJUnit.assertTrue(ai.getCardsIn(ZoneType.Library).size() > 6);
    }

    // ------------------------------------------------------------------------------------ RemoveDeck

    /**
     * Springleaf Drum is AI:RemoveDeck:All: the heuristics never approve it, and Flat MC is offered it as a play of its
     * own. With mana to spare and nothing else to do the rollouts must take it (a permanent that makes mana).
     */
    @Test(timeOut = LONG_TIMEOUT_MS)
    public void springleafDrumIsOfferedToFlatMcAndCastWithSpareMana() {
        final Game game = newGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        libraryOf(ai, 20, "Plains", "Momo, Friendly Flier", "Starfield Shepherd", "Springleaf Drum");
        libraryOf(opp, 20, "Mountain");
        addCards("Plains", 4, ai);
        addCard("Momo, Friendly Flier", ai);
        final Card drum = addCardToZone("Springleaf Drum", ai, ZoneType.Hand);
        final SpellAbility cast = drum.getFirstSpellAbility();
        cast.setActivatingPlayer(ai);

        if (!brainOf(ai).flatMonteCarloActive()) {
            return; // the profile of the test run has Flat MC off: nothing to show
        }
        AssertJUnit.assertTrue("only Flat MC may play it", brainOf(ai).isFlatMcOnlyPlay(cast));

        playFirstMain(game, ai);

        AssertJUnit.assertEquals(gameStateToString(game), 1,
                countCardsWithName(game, "Springleaf Drum", ZoneType.Battlefield));
    }

    /** Without Flat MC the heuristics' own filter stays: Springleaf Drum is not offered as a Flat MC play. */
    @Test(timeOut = LONG_TIMEOUT_MS)
    public void springleafDrumIsNotAFlatMcPlayWhenFlatMcIsOff() {
        final Game game = newGame();
        game.AI_FLAT_MONTE_CARLO = false;
        final Player ai = game.getPlayers().get(1);
        final Card drum = addCardToZone("Springleaf Drum", ai, ZoneType.Hand);
        final SpellAbility cast = drum.getFirstSpellAbility();
        cast.setActivatingPlayer(ai);
        AssertJUnit.assertFalse(brainOf(ai).isFlatMcOnlyPlay(cast));
    }

    /** A card with AI:RemoveDeck:Random (Omniscience) is a combo piece: it is not a "Flat MC only" play but a normal one. */
    @Test(timeOut = LONG_TIMEOUT_MS)
    public void omniscienceIsNotTreatedAsAFlatMcOnlyPlay() {
        final Game game = newGame();
        final Player ai = game.getPlayers().get(1);
        final Card omniscience = addCardToZone("Omniscience", ai, ZoneType.Hand);
        final SpellAbility cast = omniscience.getFirstSpellAbility();
        cast.setActivatingPlayer(ai);
        AssertJUnit.assertFalse(brainOf(ai).isFlatMcOnlyPlay(cast));
    }

    /** With Bontu's Monument every cast from exile drains a point of life: the loop runs until the opponent is dead. */
    @Test(timeOut = LONG_TIMEOUT_MS)
    public void theAirbendLoopWithBontusMonumentDrainsTheOpponentOut() {
        final Game game = newGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        libraryOf(ai, 20, "Forest", "Plains", "Island", "Aang, Swift Savior", "Appa, Steadfast Guardian",
                "Doc Aurlock, Grizzled Genius", "Bontu's Monument");
        libraryOf(opp, 20, "Mountain");
        addCard("Plains", ai);
        addCard("Island", ai);
        addCard("Forest", ai);
        addCard("Doc Aurlock, Grizzled Genius", ai);
        addCard("Appa, Steadfast Guardian", ai);
        addCard("Bontu's Monument", ai);
        addCardToZone("Aang, Swift Savior", ai, ZoneType.Hand);

        playFirstMain(game, ai);

        AssertJUnit.assertTrue("the opponent was drained: " + opp.getLife() + "\n" + gameStateToString(game),
                game.isGameOver() || opp.getLife() <= 0);
    }
}
