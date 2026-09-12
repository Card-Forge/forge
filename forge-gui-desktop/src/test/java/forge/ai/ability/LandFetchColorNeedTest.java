package forge.ai.ability;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;

public class LandFetchColorNeedTest extends AITest {
    /**
     * The AI holds black cards it has no target for, so nothing has been attempted and nothing is
     * on record as unpayable. It has no Plains and no Swamps either, so the count of what it
     * already owns cannot separate them and the fetch falls to the order of BASIC_LANDS. The hand
     * is the only evidence of which colour is wanted.
     */
    @Test
    public void fetchesTheColorTheHandNeedsWithNothingAttempted() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        addCards("Island", 3, ai);
        addCard("Evolving Wilds", ai);
        addCardToZone("Sign in Blood", ai, ZoneType.Hand);
        addCardToZone("Ravenous Chupacabra", ai, ZoneType.Hand);
        addCardToZone("Plains", ai, ZoneType.Library);
        addCardToZone("Swamp", ai, ZoneType.Library);
        fillLibrary(ai, 10);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        playUntilNextTurn(game);

        String fetched = "nothing";
        for (Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (c.getName().equals("Plains") || c.getName().equals("Swamp")) {
                fetched = c.getName();
            }
        }
        assertEquals("the hand is waiting on black", "Swamp", fetched);
    }
}
