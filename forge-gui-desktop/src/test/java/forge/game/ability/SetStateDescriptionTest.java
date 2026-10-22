package forge.game.ability;

import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

import static junit.framework.Assert.assertNotNull;

/** The stack text of SetState, which Skittish Valesk writes with NewState$ and no Mode$. */
public class SetStateDescriptionTest extends AITest {

    @Test
    public void aStackDescriptionWithoutAModeDoesNotThrow() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card host = addCard("Grizzly Bears", p);
        SpellAbility sa = AbilityFactory.getAbility("DB$ SetState | Defined$ Self | NewState$ FaceDown", host);
        sa.setActivatingPlayer(p);

        assertNotNull(sa.getStackDescription());
    }
}
