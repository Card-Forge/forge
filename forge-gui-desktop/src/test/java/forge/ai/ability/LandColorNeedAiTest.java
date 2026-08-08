package forge.ai.ability;

import org.testng.annotations.Test;

import com.google.common.collect.Lists;

import java.util.List;

import forge.ai.AITest;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCost;
import forge.card.ColorSet;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

/**
 * Land searches used to pick by list order, so a fetchland that had settled on the right colour
 * still chose its second colour arbitrarily. Both the play and the search path now rank candidates
 * by what the colours they add would let us pay for, plus depth in the colours we are thin on.
 */
public class LandColorNeedAiTest extends AITest {

    /** two duals sharing the searched-for type, differing in the colour beside it */
    private Card[] twoDuals(Player owner) {
        return new Card[] {
                addCardToZone("Tundra", owner, ZoneType.Library),          // Plains Island
                addCardToZone("Underground Sea", owner, ZoneType.Library)  // Island Swamp
        };
    }

    @Test
    public void scoresTheColorTheHandIsWaitingOn() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        addCards("Island", 3, ai);
        // castable the moment we can make black, and not before
        addCardToZone("Sign in Blood", ai, ZoneType.Hand);
        addCardToZone("Ravenous Chupacabra", ai, ZoneType.Hand);
        game.getAction().checkStateEffects(true);

        Card[] duals = twoDuals(ai);
        int white = ComputerUtilCard.getColorFixingValue(ai, duals[0]);
        int black = ComputerUtilCard.getColorFixingValue(ai, duals[1]);
        assertTrue("black unblocks the hand, white does not", black > white);

        // and that is what ranking picks: the two duals are worth the same as lands, so the
        // colour we are waiting on is the only thing separating them
        assertEquals("Underground Sea",
                ComputerUtilCard.getBestLandAI(ai, Lists.newArrayList(duals)).getName());

        // whether our sources happen to be tapped says nothing about which colours the board can
        // make, so holding the land drop until main 2 must not change the answer
        for (Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            c.setTapped(true);
        }
        game.getAction().checkStateEffects(true);
        assertEquals("tapping out must not change the measure",
                white, ComputerUtilCard.getColorFixingValue(ai, duals[0]));
        assertEquals(black, ComputerUtilCard.getColorFixingValue(ai, duals[1]));
    }

    @Test
    public void scoresTheOtherColorWhenTheHandChanges() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        addCards("Island", 3, ai);
        addCardToZone("Wrath of God", ai, ZoneType.Hand);
        addCardToZone("Swords to Plowshares", ai, ZoneType.Hand);
        game.getAction().checkStateEffects(true);

        Card[] duals = twoDuals(ai);
        assertTrue("now it is white we are waiting on",
                ComputerUtilCard.getColorFixingValue(ai, duals[0])
                        > ComputerUtilCard.getColorFixingValue(ai, duals[1]));
    }

    /** a permanent has already been paid for, so it must not make its own colours look needed */
    @Test
    public void aResolvedPermanentDoesNotLookLikeDemand() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        addCards("Island", 3, ai);
        // already resolved, so neither its casting cost nor its Adventure half is waiting on a
        // colour - a permanent must not make its own colours look needed
        addCard("Bonecrusher Giant", ai);
        game.getAction().checkStateEffects(true);

        // nothing is in hand, so a Mountain is worth depth on one new colour and nothing else.
        // If the Giant's casting cost or its Adventure half counted, this would be far higher.
        assertEquals("only depth on the new colour, no demand from the Giant",
                ComputerUtilCard.COLOR_FIXING_WEIGHT,
                ComputerUtilCard.getColorFixingValue(ai, addCardToZone("Mountain", ai, ZoneType.Library)));

        // and with no player to ask, ranking falls through to land value rather than a coin flip
        assertEquals("Raffine's Tower", ComputerUtilCard.getBestLandAI(null, Lists.newArrayList(
                addCardToZone("Tundra", ai, ZoneType.Library),
                addCardToZone("Raffine's Tower", ai, ZoneType.Library))).getName());
    }

    /**
     * A colour mask cannot tell one source of a colour from two, so it thinks {@code BB} is
     * payable off a single Swamp. Counting pips is what makes the second source worth having.
     */
    @Test
    public void aSecondSourceCountsForDoublePips() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        // one of each, so depth alone cannot tell the two candidates apart and only the pip
        // counting can - a colour mask would call BB payable off the single Swamp
        addCards("Island", 1, ai);
        addCards("Swamp", 1, ai);
        addCardToZone("Sign in Blood", ai, ZoneType.Hand);  // BB, so it wants a second black
        game.getAction().checkStateEffects(true);

        assertTrue("a second black source is progress towards BB, a second blue source is not",
                ComputerUtilCard.getColorFixingValue(ai, addCardToZone("Swamp", ai, ZoneType.Library))
                        > ComputerUtilCard.getColorFixingValue(ai, addCardToZone("Island", ai, ZoneType.Library)));
    }

    /** with nothing waiting on a colour, depth still prefers the colours we are thin on */
    @Test
    public void depthPrefersTheColorWeAreThinnestOn() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        addCards("Island", 3, ai);
        addCards("Plains", 1, ai);
        game.getAction().checkStateEffects(true); // empty hand: nothing to unblock at all

        int firstSwamp = ComputerUtilCard.getColorFixingValue(ai, addCardToZone("Swamp", ai, ZoneType.Library));
        int secondPlains = ComputerUtilCard.getColorFixingValue(ai, addCardToZone("Plains", ai, ZoneType.Library));
        int fourthIsland = ComputerUtilCard.getColorFixingValue(ai, addCardToZone("Island", ai, ZoneType.Library));

        assertTrue("a colour we have none of beats a second source", firstSwamp > secondPlains);
        assertTrue("and a second source still beats a fourth", secondPlains > fourthIsland);
        assertTrue("but a fourth is not worthless", fourthIsland > 0);
    }

    /**
     * Every caller runs getAvailableManaColors through ColorSet.fromNames, which keeps only colour
     * names - so a source whose script says {@code Produced$ Any} used to contribute nothing, and
     * a board of nothing but City of Brass read as unable to cast anything coloured.
     */
    @Test
    public void anyColorSourcesOfferEveryColor() {
        assertTrue("an any-colour source offers white", canPayWhiteOff("City of Brass"));
        assertTrue(canPayWhiteOff("Mana Confluence"));
        // and it still says no when the colour really is absent
        assertFalse("a blue source does not offer white", canPayWhiteOff("Island"));
    }

    private boolean canPayWhiteOff(String landName) {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        for (int i = 0; i < 3; i++) {
            addCard(landName, ai).setSickness(false);
        }
        Card swords = addCardToZone("Swords to Plowshares", ai, ZoneType.Hand);
        game.getAction().checkStateEffects(true);

        ColorSet available = ColorSet.fromNames(
                ComputerUtilCost.getAvailableManaColors(ai, (List<Card>) null));
        return swords.getManaCost().canBePaidWithAvailable(available.getColor());
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
}
