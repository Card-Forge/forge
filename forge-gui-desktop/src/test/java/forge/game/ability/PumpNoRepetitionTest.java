package forge.game.ability;

import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

import static junit.framework.Assert.assertFalse;

/** NoRepetition$ drops the random keywords the creature already has. */
public class PumpNoRepetitionTest extends AITest {

    /** With nothing to pump there is nothing to compare keywords against, so the effect does nothing. */
    @Test
    public void noRepetitionWithNoTargetPumpsNothing() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card host = addCard("Grizzly Bears", p);
        SpellAbility sa = AbilityFactory.getAbility(
                "DB$ Pump | Defined$ Remembered | KW$ Flying & Haste | RandomKeyword$ True | NoRepetition$ True", host);
        sa.setActivatingPlayer(p);

        AbilityUtils.resolve(sa);

        assertFalse("the host was not pumped", host.hasKeyword("Flying") || host.hasKeyword("Haste"));
    }
}
