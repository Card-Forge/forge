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
 * Mana abilities that cost mana themselves ("{1}, {T}: Add {W}{U}" on the Signets and the Odyssey
 * filter lands) used to be invisible to ComputerUtilMana, for the AI and for the human's auto-pay.
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
}
