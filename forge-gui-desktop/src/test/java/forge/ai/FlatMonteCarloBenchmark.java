package forge.ai;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.testng.annotations.Test;

import forge.ai.simulation.FlatMonteCarloStats;
import forge.deck.Deck;
import forge.deck.io.DeckSerializer;
import forge.game.Game;
import forge.game.GameEndReason;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.Match;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.localinstance.properties.ForgeConstants;
import forge.util.MyRandom;

/**
 * Opt-in A/B benchmark for the flat Monte Carlo AI: plays real AI against AI games with two different AI "arms" and
 * reports win rates and what the Monte Carlo decisions cost. It does nothing unless {@code -Dflatmc.bench=true} is set,
 * so it never slows down the normal test run.
 * <p>
 * Arms: {@code H} the plain heuristic AI, {@code MCorig} Monte Carlo as first proposed (no pruning, mana-cost-only
 * hold values, fixed playout length, no target variants), {@code MCnew} Monte Carlo with the refinements, and
 * {@code MCnoTV} the refinements without trying other targets.
 * <p>
 * Every pair of decks is played in blocks of four games in which both the deck each arm plays and the seat (who goes
 * first) alternate, so that neither a lopsided matchup nor the first player advantage can favour an arm. A few hundred
 * games are needed to see a difference of a few percent: 64 games have a 95% interval of about +-12 points.
 *
 * <pre>
 * mvn -pl forge-gui-desktop -am test -Dtest=FlatMonteCarloBenchmark -Dflatmc.bench=true \
 *     -Dflatmc.arms=H,MCnew -Dflatmc.games=16 -Dflatmc.decks=deckA.dck,deckB.dck,deckC.dck,deckD.dck
 * </pre>
 * Decks are paired in the order given (A against B, C against D, ...). Options: {@code flatmc.games} (multiple of
 * four, default 8), {@code flatmc.timeout} seconds per game before it is called a draw (default 150),
 * {@code flatmc.seed} (default 1000), {@code flatmc.budget=fixed} to take the time budget out of the picture so that
 * results do not depend on how busy the machine is. The last lines of every matchup are the
 * {@code FLATMC-STATS} lines of {@link FlatMonteCarloStats}.
 */
public class FlatMonteCarloBenchmark extends AITest {

    private static final class Arm {
        final String label;
        final String profile;
        final Boolean monteCarlo;

        Arm(final String label, final String profile, final Boolean monteCarlo) {
            this.label = label;
            this.profile = profile;
            this.monteCarlo = monteCarlo;
        }
    }

    private static Arm arm(final String name) {
        switch (name) {
            case "H":
                return new Arm("H", "Default", false);
            case "MCorig":
                return new Arm("MCorig", "FlatMcOrig", true);
            case "MCnoTV":
                return new Arm("MCnoTV", "FlatMcNoTargets", true);
            case "MCnew":
                return new Arm("MCnew", "FlatMcNew", true);
            default:
                throw new IllegalArgumentException("unknown arm " + name + " (use H, MCorig, MCnoTV or MCnew)");
        }
    }

    /** Copies the shipped profiles and adds the arms as profiles of their own; returns the directory. */
    private static File writeProfiles() throws IOException {
        final File dir = Files.createTempDirectory("flatmc-profiles").toFile();
        final File[] shipped = new File(ForgeConstants.AI_PROFILE_DIR).listFiles((d, n) -> n.endsWith(".ai"));
        if (shipped != null) {
            for (final File f : shipped) {
                Files.copy(f.toPath(), new File(dir, f.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
        final String base = new String(Files.readAllBytes(new File(ForgeConstants.AI_PROFILE_DIR, "Default.ai").toPath()),
                StandardCharsets.UTF_8);
        String extra = "";
        if ("fixed".equals(System.getProperty("flatmc.budget"))) {
            extra = "\nFLAT_MC_DECISION_BUDGET_MS=10000\nFLAT_MC_TURN_BUDGET_MS=60000\n";
        }
        final String orig = "\nFLAT_MC_PRUNING=false\nFLAT_MC_CATEGORY_HOLD=false\nFLAT_MC_ADAPTIVE_PLAYOUT=false"
                + "\nFLAT_MC_EXTRA_ROUNDS=0\nFLAT_MC_TARGET_VARIANTS=0\n";
        Files.write(new File(dir, "FlatMcOrig.ai").toPath(), (base + orig + extra).getBytes(StandardCharsets.UTF_8));
        Files.write(new File(dir, "FlatMcNoTargets.ai").toPath(),
                (base + "\nFLAT_MC_TARGET_VARIANTS=0\n" + extra).getBytes(StandardCharsets.UTF_8));
        Files.write(new File(dir, "FlatMcNew.ai").toPath(), (base + extra).getBytes(StandardCharsets.UTF_8));
        return dir;
    }

    /** @return 0 if the first seat won, 1 if the second did, -1 for a draw or a game that took too long */
    private static int play(final ExecutorService pool, final Deck[] decks, final Arm[] arms, final long seed,
                            final int timeoutSeconds, final int[] turnsOut) throws InterruptedException {
        MyRandom.setRandom(new Random(seed));
        final List<RegisteredPlayer> players = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            final LobbyPlayerAi lobby = new LobbyPlayerAi("P" + i + "-" + arms[i].label, null);
            lobby.setAiProfile(arms[i].profile);
            final RegisteredPlayer rp = new RegisteredPlayer(decks[i]);
            rp.setPlayer(lobby);
            players.add(rp);
        }
        final GameRules rules = new GameRules(GameType.Constructed);
        rules.setAppliedVariants(EnumSet.of(GameType.Constructed));
        rules.setSimTimeout(timeoutSeconds);
        final Match match = new Match(rules, players, "flatmc-benchmark");
        final Game game = match.createGame();
        game.setNoGUIUser();
        game.AI_FLAT_MONTE_CARLO = false;
        for (final Player p : game.getPlayers()) {
            final int idx = p.getName().startsWith("P0") ? 0 : 1;
            ((PlayerControllerAi) p.getController()).getAi().setFlatMonteCarloOverride(arms[idx].monteCarlo);
        }
        final Future<?> running = pool.submit(() -> match.startGame(game));
        try {
            running.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (final TimeoutException e) {
            running.cancel(true);
            game.setGameOver(GameEndReason.Draw);
            turnsOut[0] = game.getPhaseHandler().getTurn();
            return -1;
        } catch (final ExecutionException e) {
            System.err.println("benchmark game failed: " + e.getCause());
            return -1;
        }
        turnsOut[0] = game.getPhaseHandler().getTurn();
        if (game.getOutcome() == null || game.getOutcome().isDraw()) {
            return -1;
        }
        return game.getOutcome().getWinningLobbyPlayer().getName().startsWith("P0") ? 0 : 1;
    }

    @Test(timeOut = 24L * 3_600_000)
    public void benchmark() throws Exception {
        if (!Boolean.getBoolean("flatmc.bench")) {
            return;
        }
        final String[] armNames = System.getProperty("flatmc.arms", "H,MCnew").split(",");
        final Arm armA = arm(armNames[0].trim());
        final Arm armB = arm(armNames[1].trim());
        final int games = Integer.getInteger("flatmc.games", 8);
        final int timeout = Integer.getInteger("flatmc.timeout", 150);
        final long seed0 = Long.getLong("flatmc.seed", 1000L);
        final String[] deckFiles = System.getProperty("flatmc.decks", "").split(",");
        if (deckFiles.length < 2) {
            throw new IllegalArgumentException("give at least two decks with -Dflatmc.decks=a.dck,b.dck");
        }

        final File profiles = writeProfiles();
        final ExecutorService pool = Executors.newCachedThreadPool(r -> {
            final Thread t = new Thread(null, r, "flatmc-game", 64L << 20);
            t.setDaemon(true);
            return t;
        });
        AiProfileUtil.loadAllProfiles(profiles.getPath());
        try {
            for (int d = 0; d + 1 < deckFiles.length; d += 2) {
                final Deck deckA = DeckSerializer.fromFile(new File(deckFiles[d].trim()));
                final Deck deckB = DeckSerializer.fromFile(new File(deckFiles[d + 1].trim()));
                FlatMonteCarloStats.reset();
                int winsA = 0;
                int winsB = 0;
                int draws = 0;
                int turnsTotal = 0;
                final long t0 = System.nanoTime();
                for (int g = 0; g < games; g++) {
                    final boolean seatSwap = g % 2 == 1;
                    final boolean deckSwap = g / 2 % 2 == 1;
                    final Deck ofA = deckSwap ? deckB : deckA;
                    final Deck ofB = deckSwap ? deckA : deckB;
                    final int[] turns = new int[1];
                    final int r = seatSwap
                            ? play(pool, new Deck[] { ofB, ofA }, new Arm[] { armB, armA }, seed0 + g, timeout, turns)
                            : play(pool, new Deck[] { ofA, ofB }, new Arm[] { armA, armB }, seed0 + g, timeout, turns);
                    final int w = r < 0 ? -1 : seatSwap ? 1 - r : r; // 0: arm A won
                    if (w < 0) {
                        draws++;
                    } else if (w == 0) {
                        winsA++;
                    } else {
                        winsB++;
                    }
                    turnsTotal += turns[0];
                    System.out.println("GAME " + deckA.getName() + " vs " + deckB.getName() + " g" + g + " winner="
                            + (w < 0 ? "draw" : w == 0 ? armA.label : armB.label) + " turns=" + turns[0]);
                }
                System.out.println("MATCHUP " + deckA.getName() + " vs " + deckB.getName() + " " + armA.label + "=" + winsA
                        + " " + armB.label + "=" + winsB + " draws=" + draws + " games=" + games + " avgTurns="
                        + turnsTotal / (double) games + " totalMs=" + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0));
                System.out.println(FlatMonteCarloStats.summary());
            }
        } finally {
            pool.shutdownNow();
            AiProfileUtil.loadAllProfiles(ForgeConstants.AI_PROFILE_DIR);
        }
    }
}
