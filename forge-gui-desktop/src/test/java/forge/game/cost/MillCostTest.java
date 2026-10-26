package forge.game.cost;

import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

/** CR 701.17b: a cost to mill N cards can be paid unless N is greater than the number of cards in the library. */
public class MillCostTest extends AITest {

    private boolean canPayMill(Game game, Player p, int libraryCards, String amount) {
        p.getZone(ZoneType.Library).removeAllCards(true);
        for (int i = 0; i < libraryCards; i++) {
            addCardToZone("Island", p, ZoneType.Library);
        }
        Card source = addCard("Deranged Assistant", p);
        SpellAbility sa = AbilityFactory.getAbility(
                "AB$ Mana | Cost$ T Mill<" + amount + "> | Produced$ C", source);
        sa.setActivatingPlayer(p);
        return new CostMill(amount).canPay(sa, p, false);
    }

    @Test(timeOut = 60000)
    public void aLibraryOfExactlyTheMilledAmountCanPay() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        assertTrue("milling the whole library is legal", canPayMill(game, p, 1, "1"));
        assertTrue("milling the whole library is legal", canPayMill(game, p, 3, "3"));
    }

    @Test(timeOut = 60000)
    public void aLibraryShorterThanTheMilledAmountCannotPay() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        assertFalse(canPayMill(game, p, 0, "1"));
        assertFalse(canPayMill(game, p, 2, "3"));
    }
}
