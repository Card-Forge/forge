package forge.ai;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

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
}
