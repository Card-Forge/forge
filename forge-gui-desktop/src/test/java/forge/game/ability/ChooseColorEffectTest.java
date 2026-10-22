package forge.game.ability;

import org.testng.annotations.Test;

import com.google.common.collect.Lists;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/** Choices$ narrows the colours on offer; Exclude$ and Random$ then remove from that list. */
public class ChooseColorEffectTest extends AITest {

    private Card resolve(String script) {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card host = addCard("Grizzly Bears", p);
        SpellAbility sa = AbilityFactory.getAbility(script, host);
        sa.setActivatingPlayer(p);
        AbilityUtils.resolve(sa);
        return host;
    }

    @Test
    public void excludeRemovesFromRestrictedChoices() {
        Card host = resolve("DB$ ChooseColor | Defined$ You | Choices$ white,blue,black | Exclude$ white");

        assertEquals(1, Lists.newArrayList(host.getChosenColors()).size());
        assertTrue("white was excluded", !host.getChosenColors().iterator().next().equals("white"));
    }

    @Test
    public void randomPicksFromRestrictedChoices() {
        Card host = resolve("DB$ ChooseColor | Defined$ You | Choices$ white,blue | Random$ True");

        String chosen = host.getChosenColor();
        assertTrue("one of the offered colours: " + chosen, chosen.equals("white") || chosen.equals("blue"));
    }
}
