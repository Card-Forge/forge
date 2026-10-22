package forge.game.ability;

import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import static junit.framework.Assert.assertEquals;

/** Chooser$ names the player who decides how a ChangeZone moves its cards. */
public class ChangeZoneChooserTest extends AITest {

    /** A Chooser$ that names nobody leaves no one to choose, so the ability does nothing. */
    @Test
    public void aChooserWhoIsNobodyMovesNothing() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card host = addCard("Runeclaw Bear", p);
        SpellAbility sa = AbilityFactory.getAbility("DB$ ChangeZone | Origin$ Battlefield | Destination$ Exile"
                + " | Defined$ Self | Chooser$ Remembered", host);
        sa.setActivatingPlayer(p);

        AbilityUtils.resolve(sa);

        assertEquals("no chooser, so nothing moved", ZoneType.Battlefield, host.getZone().getZoneType());
    }
}
