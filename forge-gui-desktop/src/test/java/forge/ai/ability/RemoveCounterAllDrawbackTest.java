package forge.ai.ability;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import org.testng.annotations.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * RemoveCounterAll had no AI, so as a sub-ability it refused and took its parent down with it.
 * Alaundo the Seer's {T} ends by ticking time counters on the other cards it owns in exile, so
 * the whole ability was unreachable - on a card the AI is not flagged out of.
 */
public class RemoveCounterAllDrawbackTest extends AITest {

    @Test
    public void bookkeepingSubAbilityDoesNotVetoItsParent() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);

        Card seer = addCard("Alaundo the Seer", ai);
        seer.setSickness(false);
        addCardToZone("Grizzly Bears", ai, ZoneType.Hand);
        addCardToZone("Sol Ring", ai, ZoneType.Hand);
        fillLibrary(ai, 15);
        fillLibrary(opp, 15);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);
        playUntilNextTurn(game);

        assertTrue("it should have activated Alaundo", game.getCardState(seer, seer).isTapped());
        assertEquals("exiling a card from hand", 1, ai.getCardsIn(ZoneType.Exile).size());

        int timeCounters = 0;
        for (Card c : ai.getCardsIn(ZoneType.Exile)) {
            timeCounters += c.getCounters(CounterEnumType.TIME);
        }
        assertTrue("with time counters on it", timeCounters > 0);
    }
}
