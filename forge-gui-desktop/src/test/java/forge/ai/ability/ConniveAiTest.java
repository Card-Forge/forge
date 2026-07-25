package forge.ai.ability;

import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

import static junit.framework.Assert.assertEquals;

public class ConniveAiTest extends AITest {

    /**
     * Hypnotic Grifter's connive does not target, which the AI used to treat as having failed
     * to find a target, so it never activated it. Resolving one also used to throw
     * UnsupportedOperationException part-way through, because the effect wrote back over the
     * immutable map its caller handed it.
     */
    @Test
    public void aiConnivesAndResolvesIt() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        Card grifter = addCard("Hypnotic Grifter", ai); // {3}: connives
        grifter.setSickness(false);
        addCards("Island", 5, ai);
        // green cards, so nothing drawn or held can be cast off Islands and confuse the counts,
        // and nonland so discarding one earns the +1/+1 counter
        addCardToZone("Centaur Courser", ai, ZoneType.Library);
        addCardToZone("Centaur Courser", ai, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        gameLoopUntilNextPhase(game);

        // drew one and discarded one, so the hand is back to the size it started
        assertEquals(1, ai.getCardsIn(ZoneType.Hand).size());
        assertEquals(1, ai.getCardsIn(ZoneType.Graveyard).size());
        // the discard was a nonland, which earns the counter
        assertEquals(1, grifter.getCounters(CounterEnumType.P1P1));
    }
}
