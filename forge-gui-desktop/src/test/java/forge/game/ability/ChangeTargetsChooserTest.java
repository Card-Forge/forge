package forge.game.ability;

import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

import static junit.framework.Assert.assertTrue;

/** Chooser$ names who picks the new targets. */
public class ChangeTargetsChooserTest extends AITest {

    /** With nobody to choose (and no spell to retarget) the effect does nothing instead of throwing. */
    @Test
    public void aChooserWhoIsNobodyDoesNothing() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card host = addCard("Grizzly Bears", p);
        SpellAbility sa = AbilityFactory.getAbility("DB$ ChangeTargets | Defined$ Self | Chooser$ Remembered", host);
        sa.setActivatingPlayer(p);

        AbilityUtils.resolve(sa);

        assertTrue("nothing was on the stack to retarget", game.getStack().isEmpty());
    }
}
