package forge.ai.simulation;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import forge.game.card.Card;
import forge.game.zone.ZoneType;

/**
 * Psychatog / Aquamoeba / Wild Mongrel decks (PR #12508, ComboPlan.Kind.ATOG): creatures that grow by throwing away
 * cards or exiling the graveyard. The plan recognises such a deck, values a full graveyard according to how close the
 * atog is, and must NOT turn the atog's discard ability into a "fuel play" (that is only for madness and reanimator).
 */
public class AtogComboTest extends FlatMcTestBase {

    // ===================================================================================== detection

    @Test
    public void atogDeckIsRecognised() {
        setUpGame();
        addAtogDeck();
        assertTrue(ComboPlan.scan(ai).has(ComboPlan.Kind.ATOG));
    }

    @Test
    public void twoAtogsAreNotAnAtogDeck() {
        setUpGame();
        put("Psychatog", 2, ai, ZoneType.Library);
        put("Giant Growth", 12, ai, ZoneType.Library);
        assertFalse(ComboPlan.scan(ai).has(ComboPlan.Kind.ATOG), "3 atogs are needed");
    }

    @Test
    public void tooFewInstantsAndSorceriesAreNoAtogDeck() {
        setUpGame();
        put("Psychatog", 3, ai, ZoneType.Library);
        put("Giant Growth", 9, ai, ZoneType.Library);
        assertFalse(ComboPlan.scan(ai).has(ComboPlan.Kind.ATOG), "9 fillers are not enough to feed it");
    }

    @Test
    public void anAtogDeckIsNotAMadnessDeck() {
        setUpGame();
        addAtogDeck();
        assertFalse(ComboPlan.scan(ai).has(ComboPlan.Kind.MADNESS));
    }

    // ===================================================================================== what it is worth

    @Test
    public void aFullGraveyardIsWorthMoreTheCloserTheAtogIs() {
        setUpGame();
        addAtogDeck();
        final ComboPlan plan = ComboPlan.scan(ai);
        put("Giant Growth", 6, ai, ZoneType.Graveyard);
        final int none = plan.term(ai);
        put("Psychatog", ai, ZoneType.Hand);
        final int inHand = plan.term(ai);
        put("Psychatog", ai, ZoneType.Battlefield);
        final int onBoard = plan.term(ai);
        assertTrue(none > 0, "even before the atog is out the graveyard is worth something");
        assertTrue(inHand > none, "atog in hand: " + inHand + " vs " + none);
        assertTrue(onBoard > inHand, "atog on the battlefield: " + onBoard + " vs " + inHand);
    }

    @Test
    public void anEmptyGraveyardIsWorthNothing() {
        setUpGame();
        addAtogDeck();
        put("Psychatog", ai, ZoneType.Battlefield);
        assertEquals(ComboPlan.scan(ai).term(ai), 0);
    }

    @Test
    public void graveyardValueStopsGrowingAtTwelveCards() {
        setUpGame();
        addAtogDeck();
        final ComboPlan plan = ComboPlan.scan(ai);
        put("Psychatog", ai, ZoneType.Battlefield);
        put("Giant Growth", 12, ai, ZoneType.Graveyard);
        final int twelve = plan.term(ai);
        put("Giant Growth", 8, ai, ZoneType.Graveyard);
        assertEquals(plan.term(ai), twelve, "the atog cannot use more than it can pay for in a turn");
    }

    @Test
    public void cardsInHandAreFuelWhileTheAtogIsOut() {
        setUpGame();
        addAtogDeck();
        final ComboPlan plan = ComboPlan.scan(ai);
        put("Psychatog", ai, ZoneType.Battlefield);
        put("Giant Growth", 2, ai, ZoneType.Graveyard);
        final int emptyHand = plan.term(ai);
        put("Giant Growth", 3, ai, ZoneType.Hand);
        assertTrue(plan.term(ai) > emptyHand);
    }

    // ===================================================================================== what it must not do

    @Test
    public void theAtogsDiscardAbilityIsNoFuelPlay() {
        setUpGame();
        addAtogDeck();
        final Card atog = put("Psychatog", ai, ZoneType.Battlefield);
        put("Giant Growth", ai, ZoneType.Hand);
        at(forge.game.phase.PhaseType.MAIN1, ai);
        assertFalse(ComboPlan.scan(ai).isFuelPlay(discardAbilityOf(atog)),
                "pumping the atog is decided by combat, not by the deck plan");
    }

    @Test
    public void atogFuelIsNotProtectedFromDiscard() {
        setUpGame();
        addAtogDeck();
        final Card spell = put("Giant Growth", ai, ZoneType.Hand);
        assertTrue(ComboPlan.scan(ai).withoutProtected(collection(spell)).contains(spell));
        final Card atog = put("Psychatog", ai, ZoneType.Hand);
        assertTrue(ComboPlan.scan(ai).withoutProtected(collection(atog)).contains(atog),
                "the atog's fuel is the hand: nothing in it is protected");
    }
}