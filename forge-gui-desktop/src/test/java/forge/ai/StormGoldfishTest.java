package forge.ai;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.testng.annotations.Test;

import com.google.common.eventbus.Subscribe;

import forge.ai.simulation.FlatMonteCarloStats;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.game.Game;
import forge.game.GameEndReason;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.Match;
import forge.game.event.GameEventLandPlayed;
import forge.game.event.GameEventSpellAbilityCast;
import forge.game.event.GameEventTurnBegan;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.util.MyRandom;

/**
 * Opt-in goldfish benchmark for the Beseech Storm deck: the AI plays the deck alone against an opponent that does
 * nothing (only Plains), and the test reports on which of its own turns it won and what it played turn by turn. It
 * does nothing unless {@code -Dstorm.bench=true} is set, so it never slows down the normal test run.
 *
 * <pre>
 * mvn -pl forge-gui-desktop -am test -Dtest=StormGoldfishTest -Dsurefire.failIfNoSpecifiedTests=false \
 *     -Dstorm.bench=true -Dstorm.games=10
 * </pre>
 * Options: {@code storm.games} (default 6), {@code storm.turns} own turns to play before calling it a failure
 * (default 5), {@code storm.mc} (default true: flat Monte Carlo on), {@code storm.seed} (default 4000),
 * {@code storm.timeout} seconds per game (default 120).
 * <p>
 * The output has one line per game ({@code STORM-GAME}) with the plays of every own turn, then one
 * {@code STORM-SUMMARY} line: how many games were won by own turn 1, 2, 3, 4 and 5, and the average.
 */
public class StormGoldfishTest extends AITest {

    private static final Object[] MAIN = {
        "Dark Ritual", 4, "Lion's Eye Diamond", 4, "Lotus Petal", 4, "Chrome Mox", 4, "Mox Opal", 4,
        "Giant's Boulder", 2, "Infernal Tutor", 4, "Beseech the Mirror", 2, "Burning Wish", 3, "Thoughtseize", 4,
        "Duress", 3, "Hexing Squelcher", 2, "Gaea's Will", 1, "Tendrils of Agony", 1, "Wishclaw Talisman", 1,
        "Urza's Saga", 4, "Vault of Whispers", 4, "Spire of Industry", 3, "City of Traitors", 2, "Swamp", 4 };
    private static final Object[] SIDE = {
        "Beseech the Mirror", 1, "Tendrils of Agony", 1, "Peer into the Abyss", 1, "Assassin's Trophy", 3,
        "Nihil Spellbomb", 2, "Hexing Squelcher", 2, "Pithing Needle", 1, "Haywire Mite", 1, "Boomerang Basics", 1,
        "Duress", 1, "Debt to the Deathless", 1 };

    private static void fill(final forge.deck.CardPool pool, final Object[] list) {
        for (int i = 0; i < list.length; i += 2) {
            try {
                pool.add((String) list[i], (Integer) list[i + 1]);
            } catch (final RuntimeException e) {
                System.err.println("STORM-WARNING: could not add " + list[i] + ": " + e);
            }
        }
    }

    private static Deck stormDeck() {
        final Deck d = new Deck("Beseech Storm");
        fill(d.getMain(), MAIN);
        fill(d.getOrCreate(DeckSection.Sideboard), SIDE);
        return d;
    }

    private static Deck plainsDeck() {
        final Deck d = new Deck("Plains");
        d.getMain().add("Plains", 60);
        return d;
    }

    /** Records what the Storm player does, turn by turn, and ends the game when it has had enough turns. */
    public static final class Recorder {
        final String name;
        final Game game;
        final int maxTurns;
        int ownTurn;
        final StringBuilder log = new StringBuilder();
        int castsThisTurn;
        /** The harness ended the game: whatever the outcome says, that is not a win. */
        boolean cutOff;

        Recorder(final String name, final Game game, final int maxTurns) {
            this.name = name;
            this.game = game;
            this.maxTurns = maxTurns;
        }

        @Subscribe
        public void onTurn(final GameEventTurnBegan e) {
            if (!e.turnOwner().getName().equals(name)) {
                return;
            }
            ownTurn++;
            castsThisTurn = 0;
            if (ownTurn > maxTurns) {
                cutOff = true;
                game.setGameOver(GameEndReason.Draw);
                return;
            }
            log.append(" | T").append(ownTurn).append(':');
        }

        @Subscribe
        public void onCast(final GameEventSpellAbilityCast e) {
            if (e.sa().getHostCard() == null || !e.sa().getHostCard().getController().getName().equals(name)) {
                return;
            }
            castsThisTurn++;
            log.append(' ').append(e.sa().getHostCard().getName()).append(';');
        }

        @Subscribe
        public void onLand(final GameEventLandPlayed e) {
            if (e.player().getName().equals(name)) {
                log.append(" land ").append(e.land().getName()).append(';');
            }
        }
    }

    private static int payFails;
    private static final Map<String, Integer> PAYFAIL_BY_CARD = new TreeMap<>();

    @Test(timeOut = 24L * 3_600_000)
    public void goldfish() throws Exception {
        if (!Boolean.getBoolean("storm.bench")) {
            return;
        }
        // the engine prints "Did not have activator set ..." for every simulated check of some cards (Spire of Industry):
        // drop those lines so that the STORM-* lines can be found; -Dstorm.noise=true keeps them
        final java.io.PrintStream realOut = System.out;
        payFails = 0;
        PAYFAIL_BY_CARD.clear();
        if (!Boolean.getBoolean("storm.noise")) {
            System.setOut(new java.io.PrintStream(new java.io.FilterOutputStream(realOut) {
                private final StringBuilder line = new StringBuilder();

                @Override
                public void write(final int b) throws java.io.IOException {
                    line.append((char) b);
                    if (b == '\n') {
                        flushLine();
                    }
                }

                @Override
                public void write(final byte[] b, final int off, final int len) throws java.io.IOException {
                    for (int i = 0; i < len; i++) {
                        write(b[off + i]);
                    }
                }

                private void flushLine() throws java.io.IOException {
                    final String text = line.toString();
                    line.setLength(0);
                    if (text.contains("payManaCost() cost was not paid") || text.contains("AI failed to play")) {
                        if (text.contains("AI failed to play")) {
                            final Matcher m = Pattern.compile("AI failed to play (.+?) \\(").matcher(text);
                            if (m.find()) {
                                PAYFAIL_BY_CARD.merge(m.group(1), 1, Integer::sum);
                            }
                            payFails++; // one failure prints two lines: count it once
                        }
                        if (payFails <= 8) {
                            out.write(("STORM-PAYFAIL " + text).getBytes());
                        }
                    } else if (!text.contains("Did not have activator set") && !text.startsWith("Default player choice logic")
                            && !text.startsWith("Correcting zone for")) {
                        out.write(text.getBytes());
                    }
                }
            }, true));
        }
        try {
            runGoldfish();
        } finally {
            System.setOut(realOut);
        }
    }

    private void runGoldfish() throws Exception {
        final int games = Integer.getInteger("storm.games", 6);
        final int maxTurns = Integer.getInteger("storm.turns", 5);
        final boolean mc = Boolean.parseBoolean(System.getProperty("storm.mc", "true"));
        final long seed0 = Long.getLong("storm.seed", 4000L);
        final int timeout = Integer.getInteger("storm.timeout", 120);

        final ExecutorService pool = Executors.newCachedThreadPool(r -> {
            final Thread t = new Thread(null, r, "storm-game", 64L << 20);
            t.setDaemon(true);
            return t;
        });
        final int[] wonByTurn = new int[maxTurns + 2];
        int wins = 0;
        int ownTurnsTotal = 0;
        try {
            FlatMonteCarloStats.reset();
            for (int g = 0; g < games; g++) {
                MyRandom.setRandom(new Random(seed0 + g));
                final List<RegisteredPlayer> players = new ArrayList<>();
                final Deck[] decks = { stormDeck(), plainsDeck() };
                final String[] names = { "Storm", "Goldfish" };
                for (int i = 0; i < 2; i++) {
                    final LobbyPlayerAi lobby = new LobbyPlayerAi(names[i], null);
                    lobby.setAiProfile("Default");
                    final RegisteredPlayer rp = new RegisteredPlayer(decks[i]);
                    rp.setPlayer(lobby);
                    players.add(rp);
                }
                final GameRules rules = new GameRules(GameType.Constructed);
                rules.setAppliedVariants(EnumSet.of(GameType.Constructed));
                rules.setSimTimeout(timeout);
                final Match match = new Match(rules, players, "storm-goldfish");
                final Game game = match.createGame();
                game.setNoGUIUser();
                game.AI_FLAT_MONTE_CARLO = mc;
                for (final Player p : game.getPlayers()) {
                    ((PlayerControllerAi) p.getController()).getAi().setFlatMonteCarloOverride(p.getName().equals("Storm") ? mc : Boolean.FALSE);
                }
                final Recorder rec = new Recorder("Storm", game, maxTurns);
                game.subscribeToEvents(rec);
                final long t0 = System.nanoTime();
                final Future<?> running = pool.submit(() -> match.startGame(game));
                String how = "";
                try {
                    running.get(timeout, TimeUnit.SECONDS);
                } catch (final TimeoutException e) {
                    running.cancel(true);
                    game.setGameOver(GameEndReason.Draw);
                    how = " (timeout)";
                } catch (final ExecutionException e) {
                    how = " (error: " + e.getCause() + ")";
                }
                boolean won = !rec.cutOff && how.isEmpty() && game.getOutcome() != null && !game.getOutcome().isDraw()
                        && game.getOutcome().getWinningLobbyPlayer().getName().equals("Storm");
                for (final Player p : game.getPlayers()) {
                    if (!p.getName().equals("Storm") && p.getLife() > 0) {
                        won = false; // a win is an opponent at 0 or less
                    }
                }
                if (won) {
                    wins++;
                    wonByTurn[Math.min(rec.ownTurn, maxTurns + 1)]++;
                    ownTurnsTotal += rec.ownTurn;
                }
                System.out.println("STORM-GAME g" + g + " seed=" + (seed0 + g) + " " + (won ? "WON on own turn " + rec.ownTurn : "no win")
                        + how + " ms=" + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0) + rec.log);
            }
        } finally {
            pool.shutdownNow();
        }
        final StringBuilder sb = new StringBuilder("STORM-SUMMARY games=" + games + " mc=" + mc + " wins=" + wins);
        int cumulative = 0;
        for (int t = 1; t <= maxTurns; t++) {
            cumulative += wonByTurn[t];
            sb.append(" byTurn").append(t).append('=').append(cumulative);
        }
        sb.append(" avgWinTurn=").append(wins == 0 ? "-" : String.format("%.2f", ownTurnsTotal / (double) wins));
        sb.append(" payFailures=").append(payFails).append(" (plays the AI chose but could not pay for)");
        sb.append(" payFailByCard=").append(PAYFAIL_BY_CARD);
        System.out.println(sb);
        System.out.println(FlatMonteCarloStats.summary());
    }
}
