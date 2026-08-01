package forge.performance;

import forge.ai.AITest;
import forge.ai.simulation.GameCopier;
import forge.card.CardType;
import forge.card.GamePieceType;
import forge.deck.Deck;
import forge.game.Game;
import forge.game.GameRules;
import forge.game.GameStage;
import forge.game.GameType;
import forge.game.Match;
import forge.game.card.Card;
import forge.game.card.StackedTokenCard;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.zone.PlayerZoneBattlefield;
import forge.game.zone.ZoneType;
import forge.gamesimulationtests.util.LobbyPlayerForTests;
import forge.gamesimulationtests.util.playeractions.PlayerActions;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * EXPERIMENT 2 (Baseline) + EXPERIMENT 3 (Flyweight, honest real-path rewrite).
 *
 * Measures the two token board-state strategies through the actual entry paths:
 *   - INDIVIDUAL: N distinct Card objects placed on the battlefield via Zone.add.
 *   - ENGINE:     one token at a time, exactly like TokenEffectBase.makeTokenTable
 *                 (prototype copy -> zone entry -> tryStackToken).
 *
 * For each strategy it reports creation time, how much of the batch survives as a
 * StackedTokenCard flyweight, static-ability recalculation time, and the real
 * GameCopier deep-clone cost (used by the full-simulation AI). // doc:1h DONE
 *
 * The ENGINE row's "Stacks" column is the honest flyweight ledger: if the view
 * refresh triggered by each zone entry expands pending stacks, the batch ends up
 * as ~1 stack of quantity 1 and (N-1) materialized cards - i.e. no O(1) win.
 */
public class TokenBenchmarkTest extends AITest {

    @Test
    public void runTokenBenchmark() {
        System.out.println("==============================================================================");
        System.out.println("EXPERIMENT 2 & 3: TOKEN CREATION, STATIC ABILITIES & GAMECOPIER BENCHMARK");
        System.out.println("Degree of Rigor: High (Micro-benchmark Test Execution, real engine paths)");
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

        int[] tokenCounts = new int[] { 10, 50, 100, 250 };

        System.out.println("\n--- BASELINE: Individual Card Objects ---");
        for (int count : tokenCounts) {
            runIndividualTokenBenchmark(game, p1, count);
        }

        System.out.println("\n--- ENGINE PATH: zone entry + tryStackToken (as in TokenEffectBase) ---");
        for (int count : tokenCounts) {
            runEnginePathTokenBenchmark(game, p1, count);
        }

        System.out.println("==============================================================================");
    }

    private Card makeSoldierToken(Game game, Player p1) {
        Card token = new Card(game.nextCardId(), p1.getGame());
        token.setName("Soldier");
        token.setOwner(p1);
        token.setController(p1, 0);
        token.setType(new CardType(Arrays.asList("Creature", "Token", "Soldier"), false));
        token.setBasePower(1);
        token.setBaseToughness(1);
        token.setGamePieceType(GamePieceType.TOKEN);
        return token;
    }

    private void clearBattlefield(PlayerZoneBattlefield battlefield) {
        // Reading getCards() expands pending stacks first, so this removes everything.
        new ArrayList<>(battlefield.getCards()).forEach(battlefield::remove);
    }

    private void runIndividualTokenBenchmark(Game game, Player p1, int count) {
        PlayerZoneBattlefield battlefield = (PlayerZoneBattlefield) p1.getZone(ZoneType.Battlefield);
        clearBattlefield(battlefield);

        System.gc();
        long startMem = usedMemory();
        long startTime = System.nanoTime();

        List<Card> createdTokens = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Card token = makeSoldierToken(game, p1);
            battlefield.add(token);
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
        int copiedBattlefieldCards = copy.getPlayer(p1.getId()).getZone(ZoneType.Battlefield).getCards().size();

        long memDeltaKb = Math.max(0, (usedMemory() - startMem) / 1024);

        System.out.printf("INDIVIDUAL | Tokens: %4d | Creation: %4d ms | Static: %4d ms | GameCopier: %5d ms (clone BF: %4d cards) | Mem Delta: %7d KB%n",
                count, creationMs, staticMs, copyMs, copiedBattlefieldCards, memDeltaKb);
    }

    private void runEnginePathTokenBenchmark(Game game, Player p1, int count) {
        PlayerZoneBattlefield battlefield = (PlayerZoneBattlefield) p1.getZone(ZoneType.Battlefield);
        clearBattlefield(battlefield);

        System.gc();
        long startMem = usedMemory();
        long startTime = System.nanoTime();

        // Same shape as TokenEffectBase.makeTokenTable: enter + stack per token.
        for (int i = 0; i < count; i++) {
            Card token = makeSoldierToken(game, p1);
            battlefield.add(token);
            battlefield.tryStackToken(token);
        }
        long creationMs = elapsed(startTime);

        // Honest flyweight ledger: how much of the batch is still stacked?
        List<StackedTokenCard> stacks = battlefield.getStackedTokens();
        int stackCount = stacks.size();
        int stackQuantity = stacks.stream().mapToInt(StackedTokenCard::getQuantity).sum();
        int materialized = count - stackQuantity;

        long startCopyTime = System.nanoTime();
        GameCopier copier = new GameCopier(game);
        Game copy = copier.makeCopy();
        long copyMs = elapsed(startCopyTime);
        int copiedBattlefieldCards = copy.getPlayer(p1.getId()).getZone(ZoneType.Battlefield).getCards().size();

        long startStaticTime = System.nanoTime();
        game.getAction().checkStaticAbilities(false);
        long staticMs = elapsed(startStaticTime);

        long memDeltaKb = Math.max(0, (usedMemory() - startMem) / 1024);

        System.out.printf("ENGINE     | Tokens: %4d | Creation: %4d ms | Stacks: %2d (qty %3d, %4d materialized) | Static: %4d ms | GameCopier: %5d ms (clone BF: %4d cards) | Mem Delta: %7d KB%n",
                count, creationMs, stackCount, stackQuantity, materialized, staticMs, copyMs, copiedBattlefieldCards, memDeltaKb);
    }

    private long usedMemory() {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    private long elapsed(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
