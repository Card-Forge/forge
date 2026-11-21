package forge.ai;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.testng.annotations.Test;

import forge.ai.simulation.VetoLog;
import forge.deck.Deck;
import forge.deck.io.DeckSerializer;
import forge.game.Game;
import forge.game.GameEndReason;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.Match;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgeConstants;
import forge.util.MyRandom;

/**
 * Opt-in sweep that plays full decks against each other with the flat Monte Carlo AI, headless, and reports what the
 * AI's guards refused ({@link VetoLog}) and which games stalled, so a pile of combo decks can be checked in one run
 * instead of by watching games. It does nothing unless {@code -Dflatmc.vetosweep=true} is set.
 *
 * <pre>
 * mvn -pl forge-gui-desktop -am test -Dtest=FlatMonteCarloVetoSweep -Dflatmc.vetosweep=true \
 *     -Dflatmc.vetodecks=/path/to/decks -Dflatmc.vetocards="Glimpse of Nature,Birthing Pod,High Tide" \
 *     -Dflatmc.vetogames=2 -Dflatmc.vetomax=40
 * </pre>
 * Options:
 * <ul>
 * <li>{@code flatmc.vetodecks}: directories (searched for .dck and .dec files) or deck files, comma separated;</li>
 * <li>{@code flatmc.vetocards}: keep only decks whose main deck has one of these cards (the archetype's key cards);</li>
 * <li>{@code flatmc.vetoname}: keep only decks whose file name contains one of these words (comma separated);</li>
 * <li>{@code flatmc.vetomax}: at most this many decks (default 20); {@code flatmc.vetogames}: games per deck, default 2;</li>
 * <li>{@code flatmc.vetoopp}: the deck every deck plays against (default: the next deck of the list);</li>
 * <li>{@code flatmc.timeout}: seconds before a game is called stalled (default 150); {@code flatmc.seed} (default 1000);</li>
 * <li>{@code flatmc.vetoout}: the report file (default target/veto-sweep.txt); the same text is printed.</li>
 * </ul>
 * The report lists, per deck, the games (winner, turns, stalled), the refusals of that deck's player by count, and at
 * the end the refusals of every deck together. A refusal is not an error by itself: the guards turn down plays that do
 * nothing; the ones to read are a key card refused again and again, and the games that stalled.
 */
public class FlatMonteCarloVetoSweep extends AITest {
    /**
     * Keeps the engine's chatter out of the sweep's output: the lines it prints for every land and every rollout
     * (missing activators, default AI implementations, "Moving spell to Library") are dropped, and any other line, a stack
     * trace included, is printed once and then counted. What was dropped is summed up at the end, so a real error that
     * repeats shows up as a count and not as a wall of text.
     */
    private static final class QuietStream extends OutputStream {
        private static final String[] NOISE = { " Did not have activator set in ", "Warning: default (ie. inherited from base class)",
                "Moving spell to Library", "has neither ManaCost nor Color" };
        private final PrintStream real;
        private final java.io.ByteArrayOutputStream line = new java.io.ByteArrayOutputStream();
        private final Set<String> seen = new HashSet<>();
        private final Map<String, Integer> dropped = new TreeMap<>();

        QuietStream(final PrintStream real) {
            this.real = real;
        }

        @Override
        public synchronized void write(final int b) {
            if (b == '\n') {
                final String text = line.toString().replace("\r", "");
                line.reset();
                handle(text);
            } else {
                line.write(b);
            }
        }

        private void handle(final String text) {
            for (final String noise : NOISE) {
                if (text.contains(noise)) {
                    dropped.merge(noise.trim(), 1, Integer::sum);
                    return;
                }
            }
            final String key = text.replaceAll("[0-9]+", "#");
            if (seen.add(key)) {
                real.println(text);
            } else if (!text.startsWith("\tat ")) {
                // the head names the spell and the tail holds what comes after the description (the targets that were chosen)
                dropped.merge("repeated: " + (key.length() > 230 ? key.substring(0, 80) + " ... " + key.substring(key.length() - 140) : key),
                        1, Integer::sum);
            }
        }

        synchronized String summary() {
            final StringBuilder sb = new StringBuilder();
            dropped.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue()).limit(15)
                    .forEach(e -> sb.append("  ").append(e.getValue()).append("x ").append(e.getKey()).append('\n'));
            return sb.toString();
        }
    }

    private static List<File> findDecks(final String spec) {
        final List<File> out = new ArrayList<>();
        for (final String part : spec.split(",")) {
            if (part.trim().isEmpty()) {
                continue;
            }
            final File f = new File(part.trim());
            if (f.isDirectory()) {
                final File[] all = f.listFiles((d, n) -> n.endsWith(".dck") || n.endsWith(".dec"));
                if (all != null) {
                    Arrays.sort(all);
                    out.addAll(Arrays.asList(all));
                }
            } else if (f.isFile()) {
                out.add(f);
            }
        }
        return out;
    }

    private static boolean hasAnyCard(final Deck deck, final List<String> names) {
        if (names.isEmpty()) {
            return true;
        }
        for (final Map.Entry<PaperCard, Integer> e : deck.getMain()) {
            if (names.contains(e.getKey().getName())) {
                return true;
            }
        }
        return false;
    }

    private static List<String> split(final String s) {
        final List<String> out = new ArrayList<>();
        for (final String p : s.split(",")) {
            if (!p.trim().isEmpty()) {
                out.add(p.trim());
            }
        }
        return out;
    }

    /** One game; the winner (0 or 1), -1 for a draw, -2 for a game that stalled or failed. */
    private static int play(final ExecutorService pool, final Deck[] decks, final long seed, final int timeoutSeconds,
                            final int[] turnsOut, final StringBuilder note) {
        MyRandom.setRandom(new Random(seed));
        final List<RegisteredPlayer> players = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            final LobbyPlayerAi lobby = new LobbyPlayerAi("P" + i, null);
            lobby.setAiProfile("Default");
            final RegisteredPlayer rp = new RegisteredPlayer(decks[i]);
            rp.setPlayer(lobby);
            players.add(rp);
        }
        final GameRules rules = new GameRules(GameType.Constructed);
        rules.setAppliedVariants(EnumSet.of(GameType.Constructed));
        rules.setSimTimeout(timeoutSeconds);
        final Match match = new Match(rules, players, "flatmc-veto-sweep");
        final Game game = match.createGame();
        game.setNoGUIUser();
        game.AI_FLAT_MONTE_CARLO = false;
        for (final Player p : game.getPlayers()) {
            ((PlayerControllerAi) p.getController()).getAi().setFlatMonteCarloOverride(Boolean.TRUE);
            ((PlayerControllerAi) p.getController()).getAi().setFlatMonteCarloMaxOverride(Boolean.FALSE);
        }
        final Future<?> running = pool.submit(() -> match.startGame(game));
        try {
            running.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (final TimeoutException e) {
            running.cancel(true);
            game.setGameOver(GameEndReason.Draw);
            turnsOut[0] = game.getPhaseHandler().getTurn();
            note.append("STALLED after ").append(timeoutSeconds).append("s");
            return -2;
        } catch (final ExecutionException e) {
            note.append("FAILED ").append(e.getCause());
            return -2;
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            return -2;
        }
        turnsOut[0] = game.getPhaseHandler().getTurn();
        if (game.getOutcome() == null || game.getOutcome().isDraw()) {
            return -1;
        }
        return game.getOutcome().getWinningLobbyPlayer().getName().startsWith("P0") ? 0 : 1;
    }

    @Test(timeOut = 24L * 3_600_000)
    public void sweep() throws Exception {
        if (!Boolean.getBoolean("flatmc.vetosweep")) {
            return;
        }
        final List<File> files = findDecks(System.getProperty("flatmc.vetodecks", ""));
        if (files.isEmpty()) {
            throw new IllegalArgumentException("give decks with -Dflatmc.vetodecks=<dir or files>");
        }
        final List<String> cards = split(System.getProperty("flatmc.vetocards", ""));
        final List<String> words = split(System.getProperty("flatmc.vetoname", ""));
        final int max = Integer.getInteger("flatmc.vetomax", 20);
        final int games = Integer.getInteger("flatmc.vetogames", 2);
        final int timeout = Integer.getInteger("flatmc.timeout", 150);
        final long seed0 = Long.getLong("flatmc.seed", 1000L);
        final String oppFile = System.getProperty("flatmc.vetoopp", "");

        final List<Deck> decks = new ArrayList<>();
        int skipped = 0;
        int unreadable = 0;
        int tooSmall = 0;
        int filtered = 0;
        final List<String> why = new ArrayList<>();
        for (final File f : files) {
            if (decks.size() >= max) {
                break;
            }
            if (!words.isEmpty() && words.stream().noneMatch(w -> f.getName().toLowerCase().contains(w.toLowerCase()))) {
                filtered++;
                continue;
            }
            Deck d = null;
            try {
                d = DeckSerializer.fromFile(f);
            } catch (final RuntimeException e) {
                if (why.size() < 3) {
                    why.add(f.getName() + ": " + e);
                }
            }
            if (d == null) {
                unreadable++;
                skipped++;
                continue;
            }
            if (d.getMain().countAll() < 40) {
                tooSmall++;
                skipped++;
                if (why.size() < 3) {
                    why.add(f.getName() + ": only " + d.getMain().countAll() + " cards in the main deck");
                }
                continue;
            }
            if (!hasAnyCard(d, cards)) {
                filtered++;
                skipped++;
                continue;
            }
            decks.add(d);
        }
        if (decks.isEmpty()) {
            throw new IllegalArgumentException("no deck matched: " + files.size() + " files, " + unreadable
                    + " unreadable, " + tooSmall + " under 40 cards, " + filtered + " without the cards " + cards
                    + " or the words " + words + "; first problems: " + why);
        }
        final Deck fixedOpp = oppFile.isEmpty() ? null : DeckSerializer.fromFile(new File(oppFile));

        final StringBuilder report = new StringBuilder();
        final Map<String, Integer> total = new TreeMap<>();
        final List<String> stalled = new ArrayList<>();
        final ExecutorService pool = Executors.newCachedThreadPool(r -> {
            final Thread t = new Thread(null, r, "flatmc-sweep", 64L << 20);
            t.setDaemon(true);
            return t;
        });
        AiProfileUtil.loadAllProfiles(ForgeConstants.AI_PROFILE_DIR);
        final PrintStream realOut = System.out;
        final PrintStream realErr = System.err;
        final QuietStream quietOut = new QuietStream(realOut);
        final QuietStream quietErr = new QuietStream(realErr);
        System.setOut(new PrintStream(quietOut, true));
        System.setErr(new PrintStream(quietErr, true));
        VetoLog.setEnabled(true);
        VetoLog.setEcho(false);
        try {
            report.append("VETO SWEEP: ").append(decks.size()).append(" decks, ").append(games).append(" games each, ")
                    .append(skipped).append(" skipped\n");
            for (int i = 0; i < decks.size(); i++) {
                final Deck mine = decks.get(i);
                final Deck other = fixedOpp != null ? fixedOpp : decks.get((i + 1) % decks.size());
                report.append("\n== ").append(mine.getName()).append("  vs  ").append(other.getName()).append('\n');
                final Map<String, Integer> perDeck = new TreeMap<>();
                for (int g = 0; g < games; g++) {
                    VetoLog.reset();
                    final boolean first = g % 2 == 0; // the deck under test is seat 0 in the even games
                    final int[] turns = new int[1];
                    final StringBuilder note = new StringBuilder();
                    final int r = play(pool, first ? new Deck[] { mine, other } : new Deck[] { other, mine },
                            seed0 + 31L * i + g, timeout, turns, note);
                    final String me = first ? "P0" : "P1";
                    final String result = r == -2 ? note.toString() : r == -1 ? "draw"
                            : (first ? r == 0 : r == 1) ? "won" : "lost";
                    report.append("game ").append(g).append(": ").append(result).append(" turns=").append(turns[0]).append('\n');
                    realOut.println("[sweep] " + (i + 1) + "/" + decks.size() + " " + mine.getName() + " g" + g + ": " + result
                            + " turns=" + turns[0]);
                    if (r == -2) {
                        stalled.add(mine.getName() + " g" + g + " " + note);
                    }
                    for (final String line : VetoLog.summaryOf(me)) {
                        final int x = line.indexOf("x ");
                        final int n = Integer.parseInt(line.substring(0, x));
                        final String key = line.substring(x + 2);
                        perDeck.merge(key, n, Integer::sum);
                        total.merge(key, n, Integer::sum);
                    }
                }
                perDeck.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue()).limit(25)
                        .forEach(e -> report.append("  veto ").append(e.getValue()).append("x ").append(e.getKey()).append('\n'));
            }
            report.append("\n== STALLED OR FAILED GAMES (").append(stalled.size()).append(")\n");
            stalled.forEach(s -> report.append("  ").append(s).append('\n'));
            report.append("\n== REFUSALS OF ALL DECKS (top 60)\n");
            total.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue()).limit(60)
                    .forEach(e -> report.append("  ").append(e.getValue()).append("x ").append(e.getKey()).append('\n'));
        } finally {
            pool.shutdownNow();
            System.setOut(realOut);
            System.setErr(realErr);
            final String chatter = quietOut.summary() + quietErr.summary();
            if (!chatter.isEmpty()) {
                report.append("\n== ENGINE OUTPUT LEFT OUT (count, line)\n").append(chatter);
            }
            VetoLog.setEnabled(false);
            VetoLog.setEcho(true);
            VetoLog.reset();
        }
        System.out.println(report);
        final File out = new File(System.getProperty("flatmc.vetoout", "target/veto-sweep.txt"));
        try {
            if (out.getParentFile() != null) {
                out.getParentFile().mkdirs();
            }
            Files.write(out.toPath(), report.toString().getBytes(StandardCharsets.UTF_8));
        } catch (final IOException e) {
            System.err.println("could not write " + out + ": " + e);
        }
    }
}
