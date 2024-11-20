package forge;

import forge.ai.simulation.SimulationTest;
import forge.card.MagicColor;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.mana.Mana;
import forge.game.player.Player;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.testng.Assert.assertEquals;

public class ManaPoolTest extends SimulationTest {
    /**
     * Just a quick test for ManaPool.
     */
    @Test
    void testCompletableFuture() {
        Game game = initAndCreateGame();
        Player p0 = game.getPlayers().get(0);
        Mana m = new Mana(MagicColor.COLORLESS, new Card(1, game), null);
        p0.getManaPool().addMana(m, false);
        p0.getManaPool().addMana(m, false);
        p0.getManaPool().addMana(m, false);

        List<CompletableFuture<Integer>> futures = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                p0.getManaPool().removeMana(m, true);
                return 0;
            }));
        }
        CompletableFuture<?>[] futuresArray = futures.toArray(new CompletableFuture<?>[0]);
        CompletableFuture.allOf(futuresArray).join();
        futures.clear();
        assertEquals(p0.getManaPool().getAmountOfColor(MagicColor.COLORLESS), 1);
    }

    @Test
    void testCompletableFutureTwo() {
        Game game = initAndCreateGame();
        Player p0 = game.getPlayers().get(0);
        Player p1 = game.getPlayers().get(1);
        Mana m = new Mana(MagicColor.COLORLESS, new Card(1, game), null);

        List<CompletableFuture<Integer>> futures = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                p0.getManaPool().addMana(m, true);
                p1.getManaPool().addMana(m, true);
                return 0;
            }));
        }
        CompletableFuture<?>[] futuresArray = futures.toArray(new CompletableFuture<?>[0]);
        CompletableFuture.allOf(futuresArray).join();
        futures.clear();
        assertEquals(p0.getManaPool().getAmountOfColor(MagicColor.COLORLESS), 4);
        assertEquals(p1.getManaPool().getAmountOfColor(MagicColor.COLORLESS), 4);
    }
}
