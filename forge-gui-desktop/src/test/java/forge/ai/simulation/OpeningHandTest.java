package forge.ai.simulation;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.ai.ComputerUtil;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/** Keeping and mulliganing opening hands by the deck's plan. */
public class OpeningHandTest extends AITest {
    private Player ai;

    private void setUp(final int islands, final int mountains) {
        final Game game = initAndCreateGame();
        ai = game.getPlayers().get(1);
        for (int i = 0; i < islands; i++) {
            addCardToZone("Island", ai, ZoneType.Library);
        }
        for (int i = 0; i < mountains; i++) {
            addCardToZone("Mountain", ai, ZoneType.Library);
        }
        for (int i = 0; i < 20; i++) { // the spells left in the library
            addCardToZone("Grizzly Bears", ai, ZoneType.Library);
        }
    }

    private CardCollection hand(final String... names) {
        final CardCollection hand = new CardCollection();
        for (final String n : names) {
            hand.add(addCardToZone(n, ai, ZoneType.Hand));
        }
        return hand;
    }

    private static final String[] SPELLS = {"Grizzly Bears", "Hill Giant", "Shock", "Air Elemental"};

    private boolean keeps(final DeckPlan plan, final boolean onPlay, final String... names) {
        return OpeningHand.keep(ai, hand(names), 0, onPlay, plan, 5);
    }

    private static String[] mix(final String[] a, final String... b) {
        final String[] r = new String[a.length + b.length];
        System.arraycopy(b, 0, r, 0, b.length);
        System.arraycopy(a, 0, r, b.length, a.length);
        return r;
    }

    @Test
    public void aTempleOfTheFalseGodIsNoLandForTheFirstTurns() {
        setUp(9, 8);
        // one Mountain and the Temple: the Temple makes no mana before the fifth land, so this is a one-lander
        AssertJUnit.assertFalse(keeps(DeckPlan.of(DeckPlan.Archetype.MIDRANGE), true,
                mix(SPELLS, "Mountain", "Temple of the False God", "Grizzly Bears")));
    }

    @Test
    public void keepsThreeLandsAndFourSpells() {
        setUp(9, 8);
        AssertJUnit.assertTrue(keeps(DeckPlan.of(DeckPlan.Archetype.MIDRANGE), true,
                mix(SPELLS, "Island", "Island", "Mountain")));
    }

    @Test
    public void sendsBackAHandWithoutLands() {
        setUp(9, 8);
        AssertJUnit.assertFalse(keeps(DeckPlan.of(DeckPlan.Archetype.MIDRANGE), true,
                "Grizzly Bears", "Hill Giant", "Shock", "Air Elemental", "Grizzly Bears", "Hill Giant", "Shock"));
    }

    @Test
    public void sendsBackAOneLanderOnThePlay() {
        setUp(9, 8);
        AssertJUnit.assertFalse(keeps(DeckPlan.of(DeckPlan.Archetype.MIDRANGE), true,
                "Island", "Grizzly Bears", "Hill Giant", "Shock", "Air Elemental", "Grizzly Bears", "Shock"));
    }

    @Test
    public void sendsBackAHandOfOnlyLands() {
        setUp(9, 8);
        AssertJUnit.assertFalse(keeps(DeckPlan.of(DeckPlan.Archetype.MIDRANGE), true,
                "Island", "Island", "Island", "Mountain", "Mountain", "Mountain", "Island"));
    }

    @Test
    public void sendsBackLandsAndCreatureAurasWithoutCreature() {
        setUp(9, 8);
        final CardCollection hand = hand("Island", "Island", "Mountain", "Holy Strength", "Holy Strength", "Rancor", "Holy Strength");
        final DeckPlan plan = DeckPlan.of(DeckPlan.Archetype.MIDRANGE);

        AssertJUnit.assertTrue("the hand is only lands and creature Auras",
                OpeningHand.hasOnlyLandsAndCreatureAuras(hand));
        AssertJUnit.assertFalse("Auras without a creature are not castable opening plays",
                OpeningHand.castsSomething(hand, ai));
        AssertJUnit.assertFalse("the smart mulligan must reject the hand",
                OpeningHand.keep(ai, hand, 0, true, plan, 4));
        AssertJUnit.assertTrue("the classic mulligan must reject the same hand",
                ComputerUtil.wantMulligan(ai, 0));
    }

    @Test
    public void keepsCreatureAurasWhenARealCreatureIsInHand() {
        setUp(9, 8);
        final CardCollection hand = hand("Forest", "Forest", "Plains", "Gladecover Scout", "Rancor", "Holy Strength", "Hyena Umbra");
        final DeckPlan plan = DeckPlan.of(DeckPlan.Archetype.AGGRO);

        AssertJUnit.assertFalse(OpeningHand.hasOnlyLandsAndCreatureAuras(hand));
        AssertJUnit.assertTrue(OpeningHand.castsSomething(hand, ai));
        AssertJUnit.assertTrue("the creature gives the Auras a real target and the hand has a castable play",
                OpeningHand.keep(ai, hand, 0, true, plan, 4));
        AssertJUnit.assertFalse(ComputerUtil.wantMulligan(ai, 0));
    }

    @Test
    public void sendsBackSixLandsAndOneSpell() {
        setUp(9, 8);
        AssertJUnit.assertFalse(keeps(DeckPlan.of(DeckPlan.Archetype.MIDRANGE), false,
                "Island", "Island", "Island", "Mountain", "Mountain", "Mountain", "Shock"));
    }

    @Test
    public void anAggroDeckKeepsAHandOfTwoLandsAndCheapCreatures() {
        setUp(9, 8);
        AssertJUnit.assertTrue(keeps(DeckPlan.of(DeckPlan.Archetype.AGGRO), false,
                "Mountain", "Mountain", "Grizzly Bears", "Grizzly Bears", "Shock", "Grizzly Bears", "Shock"));
    }

    @Test
    public void aControlDeckWantsMoreLandsThanItsHandOfTwoGives() {
        setUp(9, 8);
        // two lands and expensive spells, on the play: aggro-style luck is not enough for a deck that needs four
        AssertJUnit.assertFalse(keeps(DeckPlan.of(DeckPlan.Archetype.CONTROL), true,
                "Island", "Island", "Air Elemental", "Air Elemental", "Hill Giant", "Hill Giant", "Air Elemental"));
    }

    @Test
    public void aSmallHandIsKeptWhenItCanPlayTheGame() {
        setUp(9, 8);
        AssertJUnit.assertTrue(keeps(DeckPlan.of(DeckPlan.Archetype.MIDRANGE), true,
                "Island", "Mountain", "Grizzly Bears", "Shock", "Hill Giant"));
    }

    @Test
    public void aMulliganToFourIsAlwaysKept() {
        setUp(9, 8);
        AssertJUnit.assertTrue(keeps(DeckPlan.of(DeckPlan.Archetype.MIDRANGE), true, "Shock", "Shock", "Shock", "Shock"));
    }

    @Test
    public void putsBackTheLandsWhenFlooded() {
        setUp(9, 8);
        final CardCollection hand = hand("Island", "Island", "Island", "Mountain", "Mountain", "Grizzly Bears", "Shock");
        final CardCollection bottom = OpeningHand.toBottom(ai, hand, 2, true, DeckPlan.of(DeckPlan.Archetype.MIDRANGE));
        AssertJUnit.assertEquals(2, bottom.size());
        for (final Card c : bottom) {
            AssertJUnit.assertTrue(c + " should stay: only extra lands go back", c.isLand());
        }
    }

    @Test
    public void putsBackTheSpellItCannotCastWhenShortOnLands() {
        setUp(9, 8);
        // two Islands and no red source in hand: the Mountains are a minority in the library
        final CardCollection hand = hand("Island", "Island", "Island", "Grizzly Bears", "Air Elemental", "Shock", "Hill Giant");
        final CardCollection bottom = OpeningHand.toBottom(ai, hand, 1, true, DeckPlan.of(DeckPlan.Archetype.MIDRANGE));
        AssertJUnit.assertEquals(1, bottom.size());
        AssertJUnit.assertFalse("the lands are what the hand needs: " + bottom, bottom.get(0).isLand());
    }

    @Test
    public void landDropOddsAreExact() {
        // one land in two draws from 40 cards holding 17 lands
        AssertJUnit.assertEquals(1.0 - (23.0 / 40.0) * (22.0 / 39.0), OpeningHand.atLeast(1, 2, 40, 17), 1e-9);
        AssertJUnit.assertEquals(1.0, OpeningHand.atLeast(0, 3, 40, 17), 0.0);
        AssertJUnit.assertEquals(0.0, OpeningHand.atLeast(3, 2, 40, 17), 0.0);
    }

    @Test
    public void keyCardsFollowThePlan() {
        setUp(9, 8);
        final Card bolt = addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        final Card bears = addCardToZone("Grizzly Bears", ai, ZoneType.Hand);
        final Card elves = addCardToZone("Llanowar Elves", ai, ZoneType.Hand);
        final Card counter = addCardToZone("Counterspell", ai, ZoneType.Hand);
        AssertJUnit.assertTrue(OpeningHand.isKeyCard(bolt, DeckPlan.Archetype.BURN));
        AssertJUnit.assertTrue(OpeningHand.isKeyCard(bears, DeckPlan.Archetype.AGGRO));
        AssertJUnit.assertFalse(OpeningHand.isKeyCard(bolt, DeckPlan.Archetype.AGGRO));
        AssertJUnit.assertTrue(OpeningHand.isKeyCard(elves, DeckPlan.Archetype.RAMP));
        AssertJUnit.assertFalse(OpeningHand.isKeyCard(bears, DeckPlan.Archetype.RAMP));
        AssertJUnit.assertTrue(OpeningHand.isKeyCard(counter, DeckPlan.Archetype.CONTROL));
        AssertJUnit.assertFalse(OpeningHand.isKeyCard(elves, DeckPlan.Archetype.CONTROL));
    }

    private boolean castsSomething(final CardCollection cards) {
        ai.getGame().getAction().checkStateEffects(true);
        return OpeningHand.castsSomething(cards, ai);
    }

    @Test
    public void oneDualLandCannotPayBothColoredPips() {
        setUp(0, 0);
        AssertJUnit.assertFalse("one Underground Sea only pays one of {U} or {B}",
                castsSomething(hand("Underground Sea", "Baleful Strix")));
    }

    @Test
    public void dualLandAndOffColorLandStillCannotPayBothColoredPips() {
        setUp(0, 0);
        AssertJUnit.assertFalse("the Mountain cannot cover the second blue/black pip",
                castsSomething(hand("Underground Sea", "Mountain", "Baleful Strix")));
    }

    @Test
    public void dualLandCanPayEitherColorWhenAnotherSourceCoversTheOther() {
        setUp(0, 0);
        AssertJUnit.assertTrue("the Sea can cover black while the Island covers blue",
                castsSomething(hand("Underground Sea", "Island", "Baleful Strix")));
    }

    @Test
    public void colorlessLandDoesNotBorrowTheLibraryColors() {
        setUp(0, 1); // a Mountain is left in the library, but the hand has no red source
        AssertJUnit.assertFalse("Wastes makes only {C} and must not pretend to find the library's Mountain",
                castsSomething(hand("Wastes", "Plains", "Lightning Bolt")));
    }

    @Test
    public void handWithOnlyColorlessAndOffColorLandsIsNotKept() {
        setUp(0, 1);
        // Wastes and Plains cast none of the red spells, whatever the library still holds
        AssertJUnit.assertFalse(keeps(DeckPlan.of(DeckPlan.Archetype.BURN), true,
                "Wastes", "Plains", "Lightning Bolt", "Shock", "Lightning Bolt", "Shock", "Lightning Bolt"));
    }

    @Test
    public void fetchLandStillGetsTheLibraryColors() {
        setUp(0, 1);
        AssertJUnit.assertTrue("Evolving Wilds can fetch the Mountain that is left in the library",
                castsSomething(hand("Evolving Wilds", "Plains", "Lightning Bolt")));
    }

    @Test
    public void twoFetchLandsCannotBothUseTheOnlyLibraryIsland() {
        setUp(1, 0);

        AssertJUnit.assertFalse(
                "two Evolving Wilds cannot both fetch the only Island for Counterspell",
                castsSomething(hand(
                        "Evolving Wilds",
                        "Evolving Wilds",
                        "Counterspell")));
    }

    @Test
    public void fetchLandCannotFindAnIneligibleNonbasicLand() {
        setUp(0, 0);
        addCardToZone("Tropical Island", ai, ZoneType.Library);

        AssertJUnit.assertFalse(
                "Evolving Wilds cannot fetch a nonbasic Tropical Island",
                castsSomething(hand(
                        "Evolving Wilds",
                        "Plains",
                        "Coral Merfolk")));
    }

    @Test
    public void twoFetchLandsCannotReuseTheSameDualLand() {
        setUp(0, 0);
        addCardToZone("Tropical Island", ai, ZoneType.Library);

        AssertJUnit.assertFalse(
                "one Tropical Island cannot pay both green and blue through two fetch lands",
                castsSomething(hand(
                        "Flooded Strand",
                        "Windswept Heath",
                        "Coiling Oracle")));
    }

    @Test
    public void twoFetchLandsCanFindTwoDifferentLibraryLands() {
        setUp(0, 0);
        addCardToZone("Tropical Island", ai, ZoneType.Library);
        addCardToZone("Forest", ai, ZoneType.Library);

        AssertJUnit.assertTrue(
                "Flooded Strand finds the Tropical Island for blue, Windswept Heath the Forest for green",
                castsSomething(hand(
                        "Flooded Strand",
                        "Windswept Heath",
                        "Coiling Oracle")));
    }

    @Test
    public void twoFetchLandsCanUseTwoLibraryIslands() {
        setUp(2, 0);

        AssertJUnit.assertTrue("each Evolving Wilds can fetch its own Island for Counterspell",
                castsSomething(hand("Evolving Wilds", "Evolving Wilds", "Counterspell")));
    }

    @Test
    public void aDualLandMakesOneColourOfTheTwoPips() {
        setUp(0, 0);
        final Card charm = addCardToZone("Simic Charm", ai, ZoneType.Library); // {G}{U}
        AssertJUnit.assertFalse("one Tropical Island pays {G} or {U}, not both",
                OpeningHand.makesColorsOf(hand("Tropical Island"), ai, charm));
        AssertJUnit.assertTrue("a Forest beside it pays the other pip",
                OpeningHand.makesColorsOf(hand("Tropical Island", "Forest"), ai, charm));
    }
}
