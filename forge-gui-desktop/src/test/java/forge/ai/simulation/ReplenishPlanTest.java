package forge.ai.simulation;

import org.testng.AssertJUnit;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** The Replenish deck (Azorius Replenish, Premodern): enchantments into the graveyard, Replenish when it is worth it. */
public class ReplenishPlanTest extends FlatMcTestBase {

    @BeforeMethod
    public void setUp() {
        setUpGame();
    }

    /** The library of the deck, added before anything asks for the plan (it is read once). */
    private void addReplenishDeck() {
        put("Replenish", 4, ai, ZoneType.Library);
        put("Intuition", 3, ai, ZoneType.Library);
        put("Attunement", 4, ai, ZoneType.Library);
        put("Opalescence", 3, ai, ZoneType.Library);
        put("Parallax Wave", 4, ai, ZoneType.Library);
        put("Decree of Silence", 3, ai, ZoneType.Library);
        put("Seal of Cleansing", 2, ai, ZoneType.Library);
        put("Abeyance", 2, ai, ZoneType.Library);
        put("Frantic Search", 3, ai, ZoneType.Library);
        put("Swords to Plowshares", 4, ai, ZoneType.Library);
        put("Island", 8, ai, ZoneType.Library);
    }

    private DeckPlan plan() {
        return brain().deckPlan(ai);
    }

    @Test
    public void theDeckIsReadAsAReplenishCombo() {
        addReplenishDeck();
        AssertJUnit.assertTrue(plan().combos.has(ComboPlan.Kind.REPLENISH));
    }

    @Test
    public void replenishWaitsForAGraveyardWorthIt() {
        addReplenishDeck();
        final Card replenish = put("Replenish", ai, ZoneType.Hand);
        final SpellAbility sa = replenish.getFirstSpellAbility();
        AssertJUnit.assertTrue("an empty graveyard", plan().combos.payoffMustWait(ai, sa));
        put("Parallax Wave", ai, ZoneType.Graveyard);
        AssertJUnit.assertTrue("one enchantment is not enough", plan().combos.payoffMustWait(ai, sa));
        put("Opalescence", ai, ZoneType.Graveyard);
        AssertJUnit.assertFalse("Opalescence and a Wave are", plan().combos.payoffMustWait(ai, sa));
    }

    @Test
    public void intuitionAsksForThreeDifferentEnchantments() {
        addReplenishDeck();
        put("Replenish", ai, ZoneType.Hand);
        final CardCollection picks = plan().tutorPicks(ai, ai.getCardsIn(ZoneType.Library), 3);
        AssertJUnit.assertNotNull(picks);
        AssertJUnit.assertEquals(3, picks.size());
        final java.util.Set<String> names = new java.util.HashSet<>();
        for (final Card c : picks) {
            AssertJUnit.assertTrue(c + " is not an enchantment", c.isEnchantment());
            names.add(c.getName());
        }
        AssertJUnit.assertEquals(3, names.size());
    }

    @Test
    public void aSingleCardTutorFetchesReplenishFirstThenAnEnchantment() {
        addReplenishDeck();
        final Card first = plan().tutorPick(ai, ai.getCardsIn(ZoneType.Library));
        AssertJUnit.assertNotNull(first);
        AssertJUnit.assertEquals("Replenish", first.getName());
        put("Replenish", ai, ZoneType.Hand);
        final Card second = plan().tutorPick(ai, ai.getCardsIn(ZoneType.Library));
        AssertJUnit.assertNotNull(second);
        AssertJUnit.assertTrue(second.isEnchantment());
    }

    @Test
    public void discardsThrowAwayEnchantmentsAndKeepReplenish() {
        addReplenishDeck();
        final Card replenish = put("Replenish", ai, ZoneType.Hand);
        final Card opalescence = put("Opalescence", ai, ZoneType.Hand);
        final Card island = put("Island", ai, ZoneType.Hand);
        final CardCollection valid = collection(replenish, opalescence, island);
        final CardCollection chosen = plan().combos.preferredDiscards(ai, valid, 1);
        AssertJUnit.assertNotNull(chosen);
        AssertJUnit.assertEquals(opalescence, chosen.getFirst());
    }
}
