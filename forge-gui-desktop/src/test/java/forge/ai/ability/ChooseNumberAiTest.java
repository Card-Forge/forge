package forge.ai.ability;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

public class ChooseNumberAiTest extends AITest {

    @Test
    public void itazuraBidFollowsTheExiledCards() {
        AssertJUnit.assertEquals("nothing to cast", 0, itazuraBid("Forest"));
        for (int i = 0; i < 20; i++) {
            int bid = itazuraBid("Colossal Dreadmaw");
            AssertJUnit.assertTrue("bid " + bid, bid >= 1 && bid <= 6);
        }
    }

    private int itazuraBid(String exiled) {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        Card itazura = addCard("Itazura, Lingering Wick", opp);
        itazura.addImprintedCard(addCardToZone(exiled, opp, ZoneType.Exile));
        SpellAbility sa = itazura.getTriggers().get(0).ensureAbility().getSubAbility();
        sa.setActivatingPlayer(opp);
        return ai.getController().chooseNumber(sa, "", 0, 99);
    }
}
