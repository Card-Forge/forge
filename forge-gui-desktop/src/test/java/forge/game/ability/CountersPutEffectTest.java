package forge.game.ability;

import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

import static junit.framework.Assert.assertEquals;

/** DividedRandomly drops targets that are no longer valid while it walks the target list. */
public class CountersPutEffectTest extends AITest {

    /** A player is not a card, so DividedRandomly drops it and has nothing left to put counters on. */
    @Test
    public void dividedRandomlyDropsANonCardTarget() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card host = addCard("Grizzly Bears", p);
        SpellAbility sa = AbilityFactory.getAbility(
                "DB$ PutCounter | Defined$ You | CounterType$ P1P1 | CounterNum$ 2 | DividedRandomly$ True", host);
        sa.setActivatingPlayer(p);

        AbilityUtils.resolve(sa);

        assertEquals("no card was a valid target", 0, host.getCounters(CounterEnumType.P1P1));
    }
}
