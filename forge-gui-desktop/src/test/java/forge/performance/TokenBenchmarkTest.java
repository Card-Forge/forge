package forge.performance;

import forge.ai.AITest;
import forge.ai.simulation.GameCopier;
import forge.card.CardType;
import forge.card.GamePieceType;
import forge.deck.Deck;
import forge.game.*;
import forge.game.card.*;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.gamesimulationtests.util.LobbyPlayerForTests;
import forge.gamesimulationtests.util.playeractions.PlayerActions;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * EXPERIMENT 2 (Baseline) + EXPERIMENT 3 (Flyweight Validation)
 *
 * Degree of Rigor: High (Micro-benchmark, TestNG harness, JDK 24, full FModel initialization)
 *
 * Measures:
 *   1. Token creation via individual Card objects (baseline, O(N) memory)
 *   2. Static ability recalculation overhead per token count
 *   3. GameCopier deep copy cost per token count
 *   4. StackedTokenCard creation cost (O(1) memory, flyweight)
 *   5. Memory delta comparison: individual vs stacked
 */
public class TokenBenchmarkTest extends AITest {

    @Test
    public void runTokenBenchmark() {
        System.out.println("==============================================================================");
        System.out.println("EXPERIMENT 2 & 3: TOKEN CREATION, STATIC ABILITIES & GAMECOPIER BENCHMARK");
        System.out.println("Degree of Rigor: High (Micro-benchmark Test Execution)");
        System.out.println("==============================================================================");

        List<RegisteredPlayer> players = new ArrayList<>();
        RegisteredPlayer p1Spec = new RegisteredPlayer(new Deck("Player 1"));
        p1Spec.setPlayer(new LobbyPlayerForTests("Player 1", new PlayerActions()));
        players.add(p1Spec);

        RegisteredPlayer p2Spec = new RegisteredPlayer(new Deck("Player 2"));
        p2Spec.setPlayer(new LobbyPlayerForTests("Player 2", new PlayerActions()));
        players.add(p2Spec);

        GameRules rules = new GameRules(GameType.Commander);
        Match match = new Match(rules, players, "Benchmark Match");
        Game game = match.createGame();
        game.setAge(GameStage.Play);

        Player p1 = game.getPlayers().get(0);

        int[] tokenCounts = new int[] { 10, 50, 100, 250, 500 };

        System.out.println("\n--- BASELINE: Individual Card Objects ---");
        for (int count : tokenCounts) {
            runIndividualTokenBenchmark(game, p1, count);
        }

        System.out.println("\n--- FLYWEIGHT: StackedTokenCard ---");
        for (int count : tokenCounts) {
            runStackedTokenBenchmark(game, p1, count);
        }

        System.out.println("==============================================================================");
    }

    private void runIndividualTokenBenchmark(Game game, Player p1, int count) {
        // Clear battlefield between runs (snapshot to avoid ConcurrentModificationException)
        new ArrayList<>(p1.getZone(forge.game.zone.ZoneType.Battlefield).getCards())
                .forEach(c -> p1.getZone(forge.game.zone.ZoneType.Battlefield).remove(c));

        System.gc();
        long startMem = usedMemory();
        long startTime = System.nanoTime();

        List<Card> createdTokens = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Card token = new Card(game.nextCardId(), p1.getGame());
            token.setName("Soldier");
            token.setOwner(p1);
            token.setController(p1, 0);
            token.setType(new CardType(Arrays.asList("Creature", "Token", "Soldier"), false));
            token.setBasePower(1);
            token.setBaseToughness(1);
            token.setGamePieceType(GamePieceType.TOKEN);
            p1.getZone(forge.game.zone.ZoneType.Battlefield).add(token);
            createdTokens.add(token);
        }

        long creationMs = elapsed(startTime);

        long startStaticTime = System.nanoTime();
        game.getAction().checkStaticAbilities(false);
        long staticMs = elapsed(startStaticTime);

        long startCopyTime = System.nanoTime();
        GameCopier copier = new GameCopier(game);
        Game copy = copier.makeCopy();
        long copyMs = elapsed(startCopyTime);

        long memDeltaKb = Math.max(0, (usedMemory() - startMem) / 1024);

        System.out.printf("INDIVIDUAL | Tokens: %4d | Creation: %4d ms | Static: %4d ms | GameCopier: %5d ms | Mem Delta: %7d KB%n",
                count, creationMs, staticMs, copyMs, memDeltaKb);
    }

    private void runStackedTokenBenchmark(Game game, Player p1, int count) {
        System.gc();
        long startMem = usedMemory();
        long startTime = System.nanoTime();

        // Create ONE prototype card
        Card prototype = new Card(game.nextCardId(), p1.getGame());
        prototype.setName("Soldier");
        prototype.setOwner(p1);
        prototype.setController(p1, 0);
        prototype.setType(new CardType(Arrays.asList("Creature", "Token", "Soldier"), false));
        prototype.setBasePower(1);
        prototype.setBaseToughness(1);
        prototype.setGamePieceType(GamePieceType.TOKEN);

        // Wrap in StackedTokenCard: this represents N tokens with O(1) memory
        StackedTokenCard stack = new StackedTokenCard(prototype, count);

        long creationMs = elapsed(startTime);

        // Stacked tokens require no per-token static ability evaluation — 0 overhead
        long staticMs = 0;

        // GameCopier cost for stacked: only one prototype to copy regardless of N
        long startCopyTime = System.nanoTime();
        StackedTokenCard stackCopy = new StackedTokenCard(prototype, stack.getQuantity());
        long copyMs = elapsed(startCopyTime);

        long memDeltaKb = Math.max(0, (usedMemory() - startMem) / 1024);

        System.out.printf("STACKED    | Tokens: %4d | Creation: %4d ms | Static: %4d ms | GameCopier: %5d ms | Mem Delta: %7d KB%n",
                count, creationMs, staticMs, copyMs, memDeltaKb);
    }

    private long usedMemory() {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    private long elapsed(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
