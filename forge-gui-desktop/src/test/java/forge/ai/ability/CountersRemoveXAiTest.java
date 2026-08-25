package forge.ai.ability;

import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.phase.PhaseType;
import forge.game.player.Player;

import static junit.framework.Assert.assertEquals;

/**
 * Hex Parasite pays X for its counters, so every target rule below the Dark Depths and
 * planeswalker cases was gated on the amount not being an X cost, and never ran.
 */
public class CountersRemoveXAiTest extends AITest {

    @Test
    public void stripsCountersFromAnOpposingCreature() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);

        Card parasite = withParasite(game, ai);
        Card courser = addCard("Centaur Courser", opp);
        courser.setCounters(CounterEnumType.P1P1, 3);

        runMain2(game, ai);

        assertEquals(0, courser.getCounters(CounterEnumType.P1P1));
        assertEquals(4, parasite.getNetPower()); // 1/1 plus one per counter removed
    }

    @Test
    public void clearsItsOwnPersistCreature() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        Card parasite = withParasite(game, ai);
        Card finks = addCard("Kitchen Finks", ai);
        finks.setCounters(CounterEnumType.M1M1, 1);

        runMain2(game, ai);

        assertEquals(0, finks.getCounters(CounterEnumType.M1M1));
        assertEquals(2, parasite.getNetPower());
    }

    @Test
    public void holdsWithNothingWorthRemoving() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);

        Card parasite = withParasite(game, ai);
        addCard("Centaur Courser", opp); // no counters anywhere

        runMain2(game, ai);

        assertEquals(1, parasite.getNetPower());
    }

    private Card withParasite(Game game, Player ai) {
        Card parasite = addCard("Hex Parasite", ai);
        addCards("Swamp", 6, ai);
        return parasite;
    }

    private void runMain2(Game game, Player ai) {
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);
        gameLoopUntilNextPhase(game);
    }
}
