package forge.game.ability;

import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

import static junit.framework.Assert.assertFalse;

/** TargetingPlayer$ names the player who chooses the targets of an ability. */
public class TargetingPlayerTest extends AITest {

    /** With nobody to choose the targets, they cannot be chosen: setting up the targets reports failure. */
    @Test
    public void aTargetingPlayerWhoIsNobodyCannotChooseTargets() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card host = addCard("Grizzly Bears", p);
        SpellAbility sa = AbilityFactory.getAbility(
                "DB$ Pump | ValidTgts$ Creature | TargetingPlayer$ Remembered | KW$ Flying", host);
        sa.setActivatingPlayer(p);

        assertFalse("no targeting player, so no targets", sa.setupTargets());
    }
}
