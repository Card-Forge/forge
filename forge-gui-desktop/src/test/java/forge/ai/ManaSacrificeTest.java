package forge.ai;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import forge.ai.simulation.FlatMcTestBase;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * ManaSacrifice (PR #12508): mana abilities that sacrifice a permanent (Ashnod's Altar, Phyrexian Tower, ...).
 * The AI pays with them only when the spell is worth clearly more than what is given up, gives up what it would
 * miss least, and counts such mana in its plans only when something that costs next to nothing can be sacrificed.
 * <p>
 * Values are in evaluator points (a 2/2 creature is about 150). The tests compare values with each other instead of
 * hard-coding the evaluator's numbers, so they survive a retuning of the evaluator but not a change of the idea.
 */
public class ManaSacrificeTest extends FlatMcTestBase {

    private SpellAbility altarMana(final Card altar) {
        final SpellAbility ma = altar.getManaAbilities().get(0);
        ma.setActivatingPlayer(ai);
        return ma;
    }

    // ===================================================================================== detection

    @Test
    public void altarIsSacrificeManaButOrdinaryManaIsNot() {
        setUpGame();
        final Card altar = put("Ashnod's Altar", ai, ZoneType.Battlefield);
        final Card forest = put("Forest", ai, ZoneType.Battlefield);
        final Card elves = put("Llanowar Elves", ai, ZoneType.Battlefield);
        assertTrue(ManaSacrifice.isSacrificeMana(altarMana(altar)));
        assertFalse(ManaSacrifice.isSacrificeMana(forest.getManaAbilities().get(0)));
        assertFalse(ManaSacrifice.isSacrificeMana(elves.getManaAbilities().get(0)));
        assertFalse(ManaSacrifice.isSacrificeMana(null));
    }

    @Test
    public void towersSacrificeAbilityIsDetected() {
        setUpGame();
        final Card tower = put("Phyrexian Tower", ai, ZoneType.Battlefield);
        boolean foundSacrifice = false;
        for (final SpellAbility ma : tower.getManaAbilities()) {
            foundSacrifice |= ManaSacrifice.isSacrificeMana(ma);
        }
        assertTrue(foundSacrifice, "the second mana ability of the Tower sacrifices a creature");
    }

    // ===================================================================================== what it costs

    @Test
    public void aRealCreatureIsNotFodder() {
        setUpGame();
        final Card bears = put("Grizzly Bears", ai, ZoneType.Battlefield);
        assertTrue(ManaSacrifice.sacrificeValue(ai, bears) > ManaSacrifice.FODDER_MAX,
                "a 2/2 for two must not count as nothing");
    }

    @Test
    public void bigCreatureCostsMoreThanSmallCreature() {
        setUpGame();
        final Card bears = put("Grizzly Bears", ai, ZoneType.Battlefield);
        final Card angel = put("Serra Angel", ai, ZoneType.Battlefield);
        assertTrue(ManaSacrifice.sacrificeValue(ai, angel) > ManaSacrifice.sacrificeValue(ai, bears));
    }

    @Test
    public void markedFodderCostsNothing() {
        setUpGame();
        final Card bears = put("Grizzly Bears", ai, ZoneType.Battlefield);
        bears.setSVar("SacMe", "1");
        assertEquals(ManaSacrifice.sacrificeValue(ai, bears), 0);
    }

    @Test
    public void aCreatureThatComesBackCostsAQuarter() {
        setUpGame();
        final Card finks = put("Kitchen Finks", ai, ZoneType.Battlefield); // persist
        finks.setSickness(false); // a creature that cannot attack yet is worth 20% less, which is not what this tests
        final double full = forge.ai.ComputerUtilCard.evaluateCreature(finks);
        final int value = ManaSacrifice.sacrificeValue(ai, finks);
        assertTrue(value <= full * 0.26 && value >= full * 0.24, "persist: " + value + " of " + full);
    }

    @Test
    public void aTokenCostsHalf() {
        setUpGame();
        final Card real = put("Grizzly Bears", ai, ZoneType.Battlefield);
        final Card token = put("Grizzly Bears", ai, ZoneType.Battlefield);
        real.setSickness(false);
        token.setSickness(false);
        token.setGamePieceType(forge.card.GamePieceType.TOKEN);
        final int r = ManaSacrifice.sacrificeValue(ai, real);
        final int t = ManaSacrifice.sacrificeValue(ai, token);
        assertEquals(t, (int) Math.round(ComputerUtilCard.evaluateCreature(token) * 0.5), "half of the token's own evaluation");
        assertTrue(t < r / 2 + 1, "and never more than half of the real card: token " + t + " vs real " + r);
    }

    @Test
    public void aNonCreatureCostsBySize() {
        setUpGame();
        final Card ring = put("Sol Ring", ai, ZoneType.Battlefield);
        assertEquals(ManaSacrifice.sacrificeValue(ai, ring), 40 + 25 * ring.getCMC());
    }

    @Test
    public void chooseFodderTakesWhatTheAiWouldMissLeast() {
        setUpGame();
        final Card angel = put("Serra Angel", ai, ZoneType.Battlefield);
        final Card bears = put("Grizzly Bears", ai, ZoneType.Battlefield);
        final Card finks = put("Kitchen Finks", ai, ZoneType.Battlefield);
        assertSame(ManaSacrifice.chooseFodder(ai, collection(angel, bears, finks)), finks, "it comes back");
        assertSame(ManaSacrifice.chooseFodder(ai, collection(angel, bears)), bears);
        assertNull(ManaSacrifice.chooseFodder(ai, new CardCollection()));
    }

    // ===================================================================================== what it buys

    @Test
    public void aSpellIsWorthMoreThanAnAbility() {
        setUpGame();
        final Card mongrel = put("Wild Mongrel", ai, ZoneType.Battlefield);
        final Card angel = put("Serra Angel", ai, ZoneType.Hand);
        assertEquals(ManaSacrifice.worth(ai, discardAbilityOf(mongrel)), 40, "an ability is worth little by itself");
        assertTrue(ManaSacrifice.worth(ai, angel.getFirstSpellAbility()) > 40);
    }

    @Test
    public void instantsAndPermanentsAreWorthTheirManaValue() {
        setUpGame();
        final Card bolt = put("Lightning Bolt", ai, ZoneType.Hand);
        final Card ring = put("Sol Ring", ai, ZoneType.Hand);
        assertEquals(ManaSacrifice.worth(ai, bolt.getFirstSpellAbility()), 40 + 12 * bolt.getCMC());
        assertEquals(ManaSacrifice.worth(ai, ring.getFirstSpellAbility()), 50 + 30 * ring.getCMC());
    }

    @Test
    public void aCreatureSpellIsWorthItsEvaluation() {
        setUpGame();
        final Card angel = put("Serra Angel", ai, ZoneType.Hand);
        assertEquals(ManaSacrifice.worth(ai, angel.getFirstSpellAbility()), ComputerUtilCard.evaluateCreature(angel));
    }

    // ===================================================================================== the trade

    @Test
    public void doesNotSacrificeARealCreatureForACheapSpell() {
        setUpGame();
        final Card altar = put("Ashnod's Altar", ai, ZoneType.Battlefield);
        put("Grizzly Bears", ai, ZoneType.Battlefield);
        final Card bolt = put("Lightning Bolt", ai, ZoneType.Hand);
        assertFalse(ManaSacrifice.worthIt(ai, altarMana(altar), bolt.getFirstSpellAbility()),
                "paying a 2/2 to cast a bolt is the trade the old code made");
    }

    @Test
    public void sacrificesARealCreatureForAMuchBetterSpell() {
        setUpGame();
        final Card altar = put("Ashnod's Altar", ai, ZoneType.Battlefield);
        put("Grizzly Bears", ai, ZoneType.Battlefield);
        final Card angel = put("Serra Angel", ai, ZoneType.Hand);
        assertTrue(ManaSacrifice.worthIt(ai, altarMana(altar), angel.getFirstSpellAbility()),
                "a 4/4 flyer is worth clearly more than the bears");
    }

    @Test
    public void fodderIsAlwaysWorthIt() {
        setUpGame();
        final Card altar = put("Ashnod's Altar", ai, ZoneType.Battlefield);
        final Card fodder = put("Grizzly Bears", ai, ZoneType.Battlefield);
        fodder.setSVar("SacMe", "1");
        final Card bolt = put("Lightning Bolt", ai, ZoneType.Hand);
        assertTrue(ManaSacrifice.worthIt(ai, altarMana(altar), bolt.getFirstSpellAbility()));
    }

    @Test
    public void noSacrificeMeansNoObjection() {
        setUpGame();
        final Card forest = put("Forest", ai, ZoneType.Battlefield);
        final Card bolt = put("Lightning Bolt", ai, ZoneType.Hand);
        assertTrue(ManaSacrifice.worthIt(ai, forest.getManaAbilities().get(0), bolt.getFirstSpellAbility()));
        assertTrue(ManaSacrifice.worthIt(ai, null, bolt.getFirstSpellAbility()));
        assertTrue(ManaSacrifice.worthIt(null, null, null));
    }

    @Test
    public void manaForManaAbilitiesIsNotJudged() {
        setUpGame();
        final Card altar = put("Ashnod's Altar", ai, ZoneType.Battlefield);
        final Card other = put("Ashnod's Altar", ai, ZoneType.Battlefield);
        put("Grizzly Bears", ai, ZoneType.Battlefield);
        assertTrue(ManaSacrifice.worthIt(ai, altarMana(altar), altarMana(other)),
                "chained mana abilities are decided by the spell they finally pay for");
    }

    @Test
    public void aKeyCardIsWorthTheSacrifice() {
        setUpGame();
        final Card altar = put("Ashnod's Altar", ai, ZoneType.Battlefield);
        put("Serra Angel", ai, ZoneType.Battlefield);
        final Card bolt = put("Lightning Bolt", ai, ZoneType.Hand);
        assertFalse(ManaSacrifice.worthIt(ai, altarMana(altar), bolt.getFirstSpellAbility()));
        bolt.setCommander(true); // the card the deck is built around
        assertTrue(ManaSacrifice.worth(ai, bolt.getFirstSpellAbility())
                >= 40 + 12 * bolt.getCMC() + ManaSacrifice.KEY_CARD_BONUS);
    }

    // ===================================================================================== planning with such mana

    @Test
    public void sacrificeManaOnlyCountsWithExpendableFodder() {
        setUpGame();
        final Card altar = put("Ashnod's Altar", ai, ZoneType.Battlefield);
        assertFalse(ManaSacrifice.hasExpendableFodder(ai, altarMana(altar)), "nothing to sacrifice");
        final Card bears = put("Grizzly Bears", ai, ZoneType.Battlefield);
        assertFalse(ManaSacrifice.hasExpendableFodder(ai, altarMana(altar)), "a 2/2 is not fodder");
        bears.setSVar("SacMe", "1");
        assertTrue(ManaSacrifice.hasExpendableFodder(ai, altarMana(altar)), "marked fodder is");
    }

    @Test
    public void ordinaryManaIsAlwaysPlannedOn() {
        setUpGame();
        final Card forest = put("Forest", ai, ZoneType.Battlefield);
        assertTrue(ManaSacrifice.hasExpendableFodder(ai, forest.getManaAbilities().get(0)));
    }

    @Test
    public void availableManaEstimateIgnoresAltarWithoutFodder() {
        setUpGame();
        put("Forest", 2, ai, ZoneType.Battlefield);
        final int base = ComputerUtilMana.getAvailableManaEstimate(ai, true);
        put("Ashnod's Altar", ai, ZoneType.Battlefield);
        put("Serra Angel", ai, ZoneType.Battlefield);
        assertEquals(ComputerUtilMana.getAvailableManaEstimate(ai, true), base,
                "the AI would not sacrifice the angel: the Altar's two mana are not planned on");
    }

    @Test
    public void availableManaEstimateCountsAltarWithFodder() {
        setUpGame();
        put("Forest", 2, ai, ZoneType.Battlefield);
        final int base = ComputerUtilMana.getAvailableManaEstimate(ai, true);
        put("Ashnod's Altar", ai, ZoneType.Battlefield);
        final Card fodder = put("Grizzly Bears", ai, ZoneType.Battlefield);
        fodder.setSVar("SacMe", "1");
        assertEquals(ComputerUtilMana.getAvailableManaEstimate(ai, true), base + 2);
    }

    // ===================================================================================== which permanent

    @Test
    public void payingForManaGivesUpACreatureThatComesBack() {
        setUpGame();
        final Card altar = put("Ashnod's Altar", ai, ZoneType.Battlefield);
        put("Serra Angel", ai, ZoneType.Battlefield);
        put("Grizzly Bears", ai, ZoneType.Battlefield);
        final Card finks = put("Kitchen Finks", ai, ZoneType.Battlefield);
        final SpellAbility ma = altarMana(altar);
        final CardCollectionView sacrificed = ComputerUtil.chooseSacrificeType(ai, "Creature", ma, null, false, 1,
                new CardCollection());
        assertNotNull(sacrificed);
        assertEquals(sacrificed.size(), 1);
        assertSame(sacrificed.get(0), finks, "persist: it returns, so it is the cheapest to give up");
    }
}