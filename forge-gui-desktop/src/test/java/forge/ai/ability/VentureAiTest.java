package forge.ai.ability;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

public class VentureAiTest extends AITest {
    // The AI holds the initiative and ventures from its upkeep trigger, so the choice between
    // Forge and Lost Well is made while that trigger is resolving.
    @Test
    public void testUpkeepVentureCanChooseEitherRoom() {
        Set<String> rooms = new HashSet<>();
        for (long seed = 1; seed <= 20; seed++) {
            MyRandom.setRandom(new Random(seed));
            Game game = initAndCreateGame();
            Player ai = game.getPlayers().get(1);
            Player opp = game.getPlayers().get(0);
            fillLibrary(ai, 10);
            fillLibrary(opp, 10);
            addCard("Wall of Stone", ai);
            game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
            game.getAction().checkStateEffects(true);
            game.getAction().takeInitiative(ai, "CLB");
            playUntilStackClear(game);
            playUntilNextTurn(game);
            playUntilNextTurn(game);
            playUntilPhase(game, PhaseType.DRAW);
            for (Card c : ai.getCardsIn(ZoneType.Command)) {
                if (c.getType().isDungeon()) {
                    rooms.add(c.getCurrentRoom());
                }
            }
        }
        AssertJUnit.assertTrue("AI never entered Forge: " + rooms, rooms.contains("Forge"));
        AssertJUnit.assertTrue("AI never entered Lost Well: " + rooms, rooms.contains("Lost Well"));
    }
}
