package forge.ai.simulation;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.AITest;
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
}
