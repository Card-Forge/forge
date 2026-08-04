package forge.ai.ability;

import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.Lists;

import forge.ai.AITest;
import forge.ai.ComputerUtilCard;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;

/**
 * Land searches used to pick by list order, so a fetchland that had settled on the right colour
 * still chose its second colour arbitrarily. The candidates are ranked by the colours the player
 * receiving the land is actually short of.
 */
public class LandColorNeedAiTest extends AITest {

    /** two duals sharing the searched-for type, differing in the colour beside it */
    private List<Card> twoDuals(Player owner) {
        return Lists.newArrayList(
                addCardToZone("Tundra", owner, ZoneType.Library),         // Plains Island
                addCardToZone("Underground Sea", owner, ZoneType.Library) // Island Swamp
        );
    }

    @Test
    public void picksTheColorTheHandIsWaitingOn() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        addCards("Island", 3, ai);
        // castable the moment we can make black, and not before
        addCardToZone("Sign in Blood", ai, ZoneType.Hand);
        addCardToZone("Ravenous Chupacabra", ai, ZoneType.Hand);
        game.getAction().checkStateEffects(true);

        Card chosen = ComputerUtilCard.getBestLandToGainAI(ai, twoDuals(ai), false);
        assertEquals("Underground Sea", chosen.getName());
    }

    @Test
    public void picksTheOtherColorWhenTheHandChanges() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        addCards("Island", 3, ai);
        addCardToZone("Wrath of God", ai, ZoneType.Hand);
        addCardToZone("Swords to Plowshares", ai, ZoneType.Hand);
        game.getAction().checkStateEffects(true);

        Card chosen = ComputerUtilCard.getBestLandToGainAI(ai, twoDuals(ai), false);
        assertEquals("Tundra", chosen.getName());
    }

    @Test
    public void anOpponentChoosingGivesTheLeastUsefulLand() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        addCards("Island", 3, ai);
        addCardToZone("Sign in Blood", ai, ZoneType.Hand);
        addCardToZone("Ravenous Chupacabra", ai, ZoneType.Hand);
        game.getAction().checkStateEffects(true);

        Card chosen = ComputerUtilCard.getBestLandToGainAI(ai, twoDuals(ai), true);
        assertEquals("Tundra", chosen.getName());
    }

    /** with nothing to separate them the caller keeps whatever it was already doing */
    @Test
    public void staysOutOfTheWayWhenItCannotTell() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        addCards("Island", 3, ai);
        game.getAction().checkStateEffects(true);

        List<Card> identical = Lists.newArrayList(
                addCardToZone("Tundra", ai, ZoneType.Library),
                addCardToZone("Tundra", ai, ZoneType.Library));
        assertNull(ComputerUtilCard.getBestLandToGainAI(ai, identical, false));
    }

    /**
     * The same decision through a real fetchland, which is how this is reached in a game:
     * Flooded Strand searches for a Plains or an Island, and every dual carrying one of those
     * types is a legal target.
     */
    @Test
    public void aFetchlandBringsBackTheColorWeAreShortOf() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        Card strand = addCard("Flooded Strand", ai);
        strand.setSickness(false);
        addCards("Island", 3, ai);
        // only castable once we can make black
        addCardToZone("Sign in Blood", ai, ZoneType.Hand);
        addCardToZone("Ravenous Chupacabra", ai, ZoneType.Hand);
        // both carry Island, so both are legal fetches; only one gives us black
        addCardToZone("Tundra", ai, ZoneType.Library);
        addCardToZone("Underground Sea", ai, ZoneType.Library);
        fillLibrary(ai, 10);
        game.getAction().checkStateEffects(true);

        moveToMain2(game, ai);
        playUntilStackClear(game);

        assertEquals("fetched the land that unblocks our hand",
                1, countCardsWithName(game, "Underground Sea"));
        assertEquals(0, countCardsWithName(game, "Tundra"));
    }

    /** callers with no player to ask still get land value instead of a coin flip */
    @Test
    public void fallsBackToLandValueWithNoPlayer() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        List<Card> mixed = Lists.newArrayList(
                addCardToZone("Tundra", ai, ZoneType.Library),
                addCardToZone("Raffine's Tower", ai, ZoneType.Library)); // triome, produces more colours

        assertNull("nothing to say without a player to ask",
                ComputerUtilCard.getBestLandToGainAI(null, mixed, false));
        assertEquals("Raffine's Tower", ComputerUtilCard.getBestLandAI(null, mixed).getName());
    }
}
