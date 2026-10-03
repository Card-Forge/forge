package forge.ai.ability;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

public class ChooseNumberAiTest extends AITest {

    @Test
    public void onlyOpponentOfItazuraBidsNothing() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        Card itazura = addCard("Itazura, Lingering Wick", opp);
        SpellAbility sa = itazura.getTriggers().get(0).ensureAbility().getSubAbility();
        sa.setActivatingPlayer(opp);
        for (int i = 0; i < 20; i++) {
            AssertJUnit.assertEquals(0, ai.getController().chooseNumber(sa, "", 0, 99));
        }
    }
}
