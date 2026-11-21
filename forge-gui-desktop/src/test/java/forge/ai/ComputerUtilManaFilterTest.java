package forge.ai;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Mana abilities that cost mana themselves ("{1}, {T}: Add {W}{U}" on the Signets, "{W/U}, {T}: Add {W}{W}, {W}{U},
 * or {U}{U}" on the Shadowmoor filter lands) used to be invisible to ComputerUtilMana, for the AI and for the human's auto-pay.
 */
public class ComputerUtilManaFilterTest extends AITest {

    private boolean payForReal(Card card, Player ai) {
        SpellAbility sa = card.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertTrue(ComputerUtilMana.canPayManaCost(sa, ai, 0, false));
        ManaCostBeingPaid cost = new ManaCostBeingPaid(sa.getPayCosts().getTotalMana());
        return ComputerUtilMana.payManaCost(cost, sa, ai, false) && cost.isPaid();
    }

    @Test
    public void signetIsUsedWhenLandsAloneFallShort() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        addCards("Plains", 2, ai);
        Card signet = addCard("Azorius Signet", ai);
        Card troops = addCardToZone("Standing Troops", ai, ZoneType.Hand); // {2}{W}

        AssertJUnit.assertTrue(payForReal(troops, ai));
        AssertJUnit.assertTrue(signet.isTapped());
    }

    @Test
    public void signetCostDoesNotTakeTheOnlySourceOfANeededColor() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        addCard("Swamp", ai);
        addCards("Plains", 2, ai);
        addCard("Azorius Signet", ai);
        Card zur = addCardToZone("Zur the Enchanter", ai, ZoneType.Hand); // {1}{W}{U}{B}

        AssertJUnit.assertTrue("Swamp must pay {B}, a Plains must pay for the Signet", payForReal(zur, ai));
    }

    @Test
    public void filterLandPaysForBothColors() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        addCard("Plains", ai);
        Card expanse = addCard("Skycloud Expanse", ai);
        Card firstWing = addCardToZone("Azorius First-Wing", ai, ZoneType.Hand); // {W}{U}

        AssertJUnit.assertTrue(payForReal(firstWing, ai));
        AssertJUnit.assertTrue(expanse.isTapped());
    }

    @Test
    public void hybridFilterLandTurnsOneColorIntoTwoOfAnother() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        addCard("Plains", ai);
        Card gate = addCard("Mystic Gate", ai); // {W/U}, {T}: Add {W}{W}, {W}{U}, or {U}{U}
        Card counterspell = addCardToZone("Counterspell", ai, ZoneType.Hand); // {U}{U}

        AssertJUnit.assertTrue(payForReal(counterspell, ai));
        AssertJUnit.assertTrue(gate.isTapped());
    }

    @Test
    public void filterLandIsUsedBeforeTheLandThatPaysForIt() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        addCard("Plains", ai);
        addCard("Mystic Gate", ai);
        Card knight = addCardToZone("Silver Knight", ai, ZoneType.Hand); // {W}{W}

        AssertJUnit.assertTrue("the Plains pays for the Gate, not for a {W}", payForReal(knight, ai));
    }

    @Test
    public void hybridFilterLandCannotPayForItself() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        Card gate = addCard("Mystic Gate", ai);
        Card counterspell = addCardToZone("Counterspell", ai, ZoneType.Hand);

        SpellAbility sa = counterspell.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertFalse(ComputerUtilMana.canPayManaCost(sa, ai, 0, false));
        AssertJUnit.assertEquals("the color choice made while testing is undone", "",
                gate.getManaAbilities().get(1).getManaPart().getExpressChoice());
    }

    @Test
    public void reflectingSourcePaysForTheSignet() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);

        addCard("Plains", opponent);
        addCard("Island", opponent);
        addCard("Exotic Orchard", ai);
        addCard("Azorius Signet", ai);
        Card firstWing = addCardToZone("Azorius First-Wing", ai, ZoneType.Hand); // {W}{U}

        AssertJUnit.assertTrue(payForReal(firstWing, ai));
    }

    @Test
    public void manaEstimateCountsAChoiceOfColorsOnce() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        addCard("Plains", ai);
        addCard("Azorius Guildgate", ai); // Add {W} or {U}
        addCard("Mystic Gate", ai); // {W/U}, {T}: Add {W}{W}, {W}{U}, or {U}{U}
        addCard("Azorius Signet", ai); // {1}, {T}: Add {W}{U}
        addCard("Crystal Quarry", ai); // {T}: Add {C}, or {5}, {T}: Add {W}{U}{B}{R}{G}
        addCard("River of Tears", ai); // Add {U}, or {B} instead after a land drop
        ai.setLandsPlayedThisTurn(1);

        AssertJUnit.assertEquals("each of the six makes one mana more than it costs", 6,
                ComputerUtilMana.getAvailableManaEstimate(ai));
    }

    @Test
    public void firstLandForAOneDropIsNotAFilterLand() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        addCardToZone("Mystic Gate", ai, ZoneType.Hand);
        Card plains = addCardToZone("Plains", ai, ZoneType.Hand);
        addCardToZone("Savannah Lions", ai, ZoneType.Hand); // {W}

        List<SpellAbility> play = ((PlayerControllerAi) ai.getController()).getAi().chooseSpellAbilityToPlay();
        AssertJUnit.assertEquals("the Gate alone can't make {W}", plains, play.get(0).getHostCard());
    }

    @Test
    public void barbedSextantPaysAColorTheLandsLack() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        fillLibrary(ai, 10);
        addCard("Plains", ai);
        addCard("Barbed Sextant", ai); // {1}, {T}, Sacrifice: Add one mana of any color. Draw a card next upkeep
        Card flyingMen = addCardToZone("Flying Men", ai, ZoneType.Hand); // {U}

        AssertJUnit.assertTrue(payForReal(flyingMen, ai));
        AssertJUnit.assertEquals(1, countCardsWithName(game, "Barbed Sextant", ZoneType.Graveyard));
    }

    @Test
    public void paymentCheckWithChromaticStarChangesNothing() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        fillLibrary(ai, 10);
        Card plains = addCard("Plains", ai);
        Card star = addCard("Chromatic Star", ai); // draws a card when it is put into a graveyard
        Card flyingMen = addCardToZone("Flying Men", ai, ZoneType.Hand);
        int handSize = ai.getCardsIn(ZoneType.Hand).size();

        SpellAbility sa = flyingMen.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertTrue(ComputerUtilMana.canPayManaCost(sa, ai, 0, false));
        AssertJUnit.assertTrue(star.isInZone(ZoneType.Battlefield));
        AssertJUnit.assertFalse(star.isTapped() || plains.isTapped());
        AssertJUnit.assertEquals(handSize, ai.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertEquals("", star.getManaAbilities().get(0).getManaPart().getExpressChoice());
    }

    @Test
    public void implementsOfSacrificeTurnsOneManaIntoTwoOfAColor() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        addCard("Plains", ai);
        addCard("Implements of Sacrifice", ai); // {1}, {T}, Sacrifice: Add two mana of any one color
        Card counterspell = addCardToZone("Counterspell", ai, ZoneType.Hand); // {U}{U}

        AssertJUnit.assertTrue(payForReal(counterspell, ai));
        AssertJUnit.assertEquals(1, countCardsWithName(game, "Implements of Sacrifice", ZoneType.Graveyard));
    }

    @Test
    public void colorFixerStaysLastWhenTheLandIsTheOnlySourceOfAHeldColor() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        addCard("Scoured Barrens", ai); // Add {W} or {B}
        addCard("Agent of Stromgald", ai).setSickness(false); // {R}: Add {B}
        addCardToZone("Savannah Lions", ai, ZoneType.Hand); // the Barrens is the only {W} source
        Card carnophage = addCardToZone("Carnophage", ai, ZoneType.Hand); // {B}

        SpellAbility sa = carnophage.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertTrue("no source makes the Agent's {R}, so the Barrens pays", ComputerUtilMana.canPayManaCost(sa, ai, 0, false));
    }

    @Test
    public void creatureIsNotSacrificedAsAColorFixer() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        addCards("Island", 3, ai);
        addCard("Coal Golem", ai); // {3}, Sacrifice: Add {R}{R}{R}
        Card bolt = addCardToZone("Lightning Bolt", ai, ZoneType.Hand);

        SpellAbility sa = bolt.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertFalse(ComputerUtilMana.canPayManaCost(sa, ai, 0, false));
    }

    @Test
    public void preferredColorDoesNotPutTheColorFixerFirst() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        addCards("Plains", 4, ai);
        Card kaleidostone = addCard("Kaleidostone", ai); // {5}, {T}, Sacrifice: Add {W}{U}{B}{R}{G}
        Card paladin = addCardToZone("Ardenvale Paladin", ai, ZoneType.Hand); // {3}{W}, prefers {W} for adamant

        AssertJUnit.assertTrue(payForReal(paladin, ai));
        AssertJUnit.assertTrue(kaleidostone.isInZone(ZoneType.Battlefield));
    }

    @Test
    public void colorFixerGoesAfterTheFilterWhenFiltersGoFirst() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        addCard("Forest", ai);
        addCard("Plains", ai);
        addCard("Knotvine Mystic", ai).setSickness(false); // {1}, {T}: Add {R}{G}{W}
        Card sprites = addCard("Fire Sprites", ai); // {G}, {T}: Add {R}
        sprites.setSickness(false);
        Card giant = addCardToZone("Hill Giant", ai, ZoneType.Hand); // {3}{R}

        AssertJUnit.assertTrue(payForReal(giant, ai));
        AssertJUnit.assertFalse(sprites.isTapped());
    }

    @Test
    public void exhaustManaAbilityIsNotSpentOnAPayment() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        addCard("Forest", ai);
        addCard("Loot, the Pathfinder", ai); // {G}, {T}, Exhaust: Add three mana of any one color
        Card flyingMen = addCardToZone("Flying Men", ai, ZoneType.Hand); // {U}

        SpellAbility sa = flyingMen.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertFalse(ComputerUtilMana.canPayManaCost(sa, ai, 0, false));
    }
}
