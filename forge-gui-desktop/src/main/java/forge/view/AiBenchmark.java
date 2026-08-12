/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.view;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import forge.ai.AiProfileUtil;
import forge.deck.Deck;
import forge.deck.io.DeckSerializer;
import forge.game.Game;
import forge.game.GameEndReason;
import forge.game.GameRules;
import forge.game.GameStateDigest;
import forge.game.GameType;
import forge.game.Match;
import forge.game.player.RegisteredPlayer;
import forge.model.FModel;
import forge.player.GamePlayerUtil;
import forge.util.MyRandom;
import forge.util.WordUtil;
import forge.util.perf.DecisionKind;
import forge.util.perf.DecisionTraceWriter;
import forge.util.perf.PerfCounter;
import forge.util.perf.PerfProbe;
import forge.util.perf.PerfReport;
import forge.util.perf.PerfTimer;
import forge.util.perf.TracingRandom;

/**
 * A reproducible headless harness for measuring AI decision cost.
 *
 * <p>The AI performance plan requires a measurement foundation before any optimisation is attempted,
 * because the historical evidence it cites was gathered on other revisions and cannot be used to
 * rank work on this one. This command supplies that foundation: fixed decks, fixed per-game seeds, a
 * warm-up phase whose samples are discarded, per-decision latency distributions and work counters,
 * an optional decision trace for parity checking, and a canonical end-state digest per game.</p>
 *
 * <h2>Determinism</h2>
 * <p>Every game index {@code i} is played with seed {@code baseSeed + i}, installed into the
 * process-global generator before the game starts. A {@link TracingRandom} is used so that draws can
 * be counted; it produces exactly the sequence {@code new Random(seed)} would, so installing it
 * cannot change an outcome. Re-running the same command must therefore reproduce the same winners,
 * turn counts and state digests, and — with {@code -t} — a byte-identical trace file.</p>
 *
 * <h2>Usage</h2>
 * <pre>
 * forge bench -d &lt;deck1&gt; &lt;deck2&gt; [-n games] [-w warmupGames] [-s seed] [-f format]
 *             [-a profile1 profile2] [-c simTimeoutSeconds] [-o outputDir] [-t] [-jfr] [-q]
 * </pre>
 * The measured results are printed as a summary and written to {@code <outputDir>/report.json};
 * traces, when enabled, go to {@code <outputDir>/trace.jsonl}.
 */
public final class AiBenchmark {
    private AiBenchmark() {
    }

    public static void benchmark(final String[] args) {
        FModel.initialize(null, null);

        final Map<String, List<String>> params = parseArgs(args);
        if (params == null) {
            argumentHelp();
            return;
        }
        if (!params.containsKey("d")) {
            System.out.println("At least one deck is required.");
            argumentHelp();
            return;
        }

        final int games = intParam(params, "n", 1);
        final int warmupGames = intParam(params, "w", 0);
        final long baseSeed = longParam(params, "s", 0L);
        final boolean tracing = params.containsKey("t");
        final boolean quiet = params.containsKey("q");
        final File outputDir = new File(params.containsKey("o") ? params.get("o").get(0) : "forge-bench-out");

        final GameType type = params.containsKey("f")
                ? GameType.valueOf(WordUtil.capitalize(params.get("f").get(0)))
                : GameType.Constructed;

        final List<String> aiProfiles = params.get("a");
        if (aiProfiles != null) {
            for (final String profile : aiProfiles) {
                if (!AiProfileUtil.getProfilesDisplayList().contains(profile)) {
                    System.out.println("Unknown AI profile - " + profile + ". Available profiles: "
                            + String.join(", ", AiProfileUtil.getProfilesDisplayList()));
                    return;
                }
            }
        }

        // A benchmark corpus should live next to the fixtures it belongs to rather than in the user
        // profile, so -D points the deck lookup at an explicit directory.
        final File deckDir = params.containsKey("D") && !params.get("D").isEmpty()
                ? new File(params.get("D").get(0)) : null;
        if (deckDir != null && !deckDir.isDirectory()) {
            System.out.println("Deck directory not found - " + deckDir.getAbsolutePath());
            return;
        }

        final List<Deck> decks = new ArrayList<>();
        for (final String deckName : params.get("d")) {
            final Deck deck = deckDir == null
                    ? SimulateMatch.deckFromCommandLineParameter(deckName, type)
                    : DeckSerializer.fromFile(new File(deckDir, deckName));
            if (deck == null) {
                System.out.println("Could not load deck - " + deckName + ", benchmark cannot start");
                return;
            }
            decks.add(deck);
        }

        final GameRules rules = new GameRules(type);
        rules.setAppliedVariants(EnumSet.of(type));
        if (params.containsKey("c")) {
            rules.setSimTimeout(Integer.parseInt(params.get("c").get(0)));
        }

        System.out.println("AI benchmark: " + decks.size() + " deck(s), " + games + " measured game(s), "
                + warmupGames + " warm-up game(s), base seed " + baseSeed
                + (tracing ? ", tracing on" : ""));

        // Warm-up runs first, with probing off entirely: JIT compilation and the card-script caches
        // must not be paid for inside the measured samples, and warm-up decisions must not reach the
        // report. Their seeds are deliberately disjoint from the measured ones.
        if (warmupGames > 0) {
            PerfProbe.setEnabled(false);
            for (int i = 0; i < warmupGames; i++) {
                playGame(decks, rules, type, aiProfiles, baseSeed - 1L - i, true);
            }
            System.out.println("Warm-up complete.");
        }

        final PerfReport report = new PerfReport("ai-benchmark");
        report.withMetadata("javaVersion", System.getProperty("java.version"))
                .withMetadata("javaVm", System.getProperty("java.vm.name"))
                .withMetadata("os", System.getProperty("os.name") + " " + System.getProperty("os.arch"))
                .withMetadata("logicalCpus", String.valueOf(Runtime.getRuntime().availableProcessors()))
                .withMetadata("maxHeapBytes", String.valueOf(Runtime.getRuntime().maxMemory()))
                .withMetadata("gameType", type.name())
                .withMetadata("decks", String.join(" vs ", decks.stream().map(Deck::getName).toList()))
                .withMetadata("aiProfiles", aiProfiles == null ? "" : String.join(",", aiProfiles))
                .withMetadata("baseSeed", String.valueOf(baseSeed))
                .withMetadata("measuredGames", String.valueOf(games))
                .withMetadata("warmupGames", String.valueOf(warmupGames));

        DecisionTraceWriter traceWriter = null;
        if (tracing) {
            try {
                traceWriter = new DecisionTraceWriter(new File(outputDir, "trace.jsonl"));
            } catch (final IOException e) {
                System.err.println("Could not open trace file: " + e);
                return;
            }
        }

        JfrPerfSink jfrSink = null;
        final long startNanos;
        try {
            if (traceWriter != null) {
                PerfProbe.addSink(traceWriter);
            }
            if (params.containsKey("jfr")) {
                jfrSink = JfrPerfSink.createIfAvailable();
                if (jfrSink != null) {
                    PerfProbe.addSink(jfrSink);
                }
            }
            PerfProbe.addSink(report);
            PerfProbe.resetGlobal();
            PerfProbe.setEnabled(true);
            PerfProbe.setTracing(tracing);

            startNanos = System.nanoTime();
            for (int i = 0; i < games; i++) {
                final GameResult result = playGame(decks, rules, type, aiProfiles, baseSeed + i, quiet);
                if (!quiet) {
                    System.out.printf("game %d: seed=%d turns=%d result=%s time=%dms digest=%s%n",
                            i, baseSeed + i, result.turns, result.outcome, result.millis, result.digest);
                }
            }
            report.setWallClockNanos(System.nanoTime() - startNanos);
        } finally {
            PerfProbe.setEnabled(false);
            PerfProbe.removeSink(report);
            if (jfrSink != null) {
                PerfProbe.removeSink(jfrSink);
            }
            if (traceWriter != null) {
                PerfProbe.removeSink(traceWriter);
                try {
                    traceWriter.close();
                } catch (final IOException e) {
                    System.err.println("Could not close trace file: " + e);
                }
            }
        }

        System.out.println();
        System.out.println(report.summary());
        System.out.println(workSummary(report));

        final File reportFile = new File(outputDir, "report.json");
        try {
            report.writeJson(reportFile);
            System.out.println("Wrote " + reportFile.getAbsolutePath());
            if (tracing) {
                System.out.println("Wrote " + new File(outputDir, "trace.jsonl").getAbsolutePath());
            }
        } catch (final IOException e) {
            System.err.println("Could not write report: " + e);
        }
        System.out.flush();
    }

    /** What one measured game produced, beyond its decision records. */
    private static final class GameResult {
        private int turns;
        private long millis;
        private String outcome = "unknown";
        private String digest = "";
    }

    private static GameResult playGame(final List<Deck> decks, final GameRules rules, final GameType type,
            final List<String> aiProfiles, final long seed, final boolean quiet) {
        // A tracing generator produces the same sequence as new Random(seed); it only observes it.
        MyRandom.setRandom(new TracingRandom(seed));

        final List<RegisteredPlayer> players = new ArrayList<>();
        for (int i = 0; i < decks.size(); i++) {
            final Deck deck = decks.get(i);
            final String profile = aiProfiles != null && aiProfiles.size() > i ? aiProfiles.get(i) : "";
            final RegisteredPlayer rp = type == GameType.Commander
                    ? RegisteredPlayer.forCommander(deck)
                    : new RegisteredPlayer(deck);
            rp.setPlayer(GamePlayerUtil.createAiPlayer("Ai(" + (i + 1) + ")-" + deck.getName(), i, profile));
            players.add(rp);
        }

        final Match match = new Match(rules, players, "AiBenchmark");
        final Game game = match.createGame();
        final GameResult result = new GameResult();
        final long start = System.nanoTime();
        try {
            TimeLimitedCodeBlock.runWithTimeout(() -> match.startGame(game), rules.getSimTimeout(), TimeUnit.SECONDS);
        } catch (final TimeoutException e) {
            result.outcome = "timeout";
            game.setGameOver(GameEndReason.Draw);
        } catch (final Exception | StackOverflowError e) {
            result.outcome = "error:" + e.getClass().getSimpleName();
            if (!quiet) {
                e.printStackTrace();
            }
            game.setGameOver(GameEndReason.Draw);
        }
        result.millis = (System.nanoTime() - start) / 1_000_000L;
        result.turns = game.getPhaseHandler() == null ? -1 : game.getPhaseHandler().getTurn();
        if ("unknown".equals(result.outcome)) {
            if (game.getOutcome() == null) {
                result.outcome = "incomplete";
            } else if (game.getOutcome().isDraw()) {
                result.outcome = "draw";
            } else {
                result.outcome = "win:" + game.getOutcome().getWinningLobbyPlayer().getName();
            }
        }
        // The canonical digest is what makes "same fixture, same run" checkable across builds
        // without comparing whole logs.
        result.digest = GameStateDigest.digest(game);
        return result;
    }

    /** A compact view of the work counters that normalise the latency numbers. */
    private static String workSummary(final PerfReport report) {
        final StringBuilder sb = new StringBuilder("work per decision:");
        for (final DecisionKind kind : DecisionKind.values()) {
            final int count = report.getDecisionCount(kind);
            if (count == 0) {
                continue;
            }
            final var totals = report.getTotals(kind);
            sb.append(String.format("%n  %-8s", kind.jsonName()));
            for (final PerfCounter counter : PerfCounter.values()) {
                final long value = totals.get(counter);
                if (value == 0L) {
                    continue;
                }
                sb.append(String.format(" %s=%.1f", counter.jsonName(), value / (double) count));
            }
            for (final PerfTimer timer : PerfTimer.values()) {
                final long nanos = totals.getNanos(timer);
                if (nanos == 0L) {
                    continue;
                }
                sb.append(String.format(" %s=%.3fms", timer.jsonName(), nanos / 1e6d / count));
            }
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ argument parsing

    private static Map<String, List<String>> parseArgs(final String[] args) {
        final Map<String, List<String>> params = new HashMap<>();
        List<String> current = null;
        // "bench" occupies slot 0.
        for (int i = 1; i < args.length; i++) {
            final String a = args[i];
            if (!a.isEmpty() && a.charAt(0) == '-') {
                if (a.length() < 2) {
                    System.err.println("Error at argument " + a);
                    return null;
                }
                current = new ArrayList<>();
                params.put(a.substring(1), current);
            } else if (current != null) {
                current.add(a);
            } else {
                System.err.println("Illegal parameter usage at " + a);
                return null;
            }
        }
        return params;
    }

    private static int intParam(final Map<String, List<String>> params, final String key, final int fallback) {
        final List<String> values = params.get(key);
        return values == null || values.isEmpty() ? fallback : Integer.parseInt(values.get(0));
    }

    private static long longParam(final Map<String, List<String>> params, final String key, final long fallback) {
        final List<String> values = params.get(key);
        return values == null || values.isEmpty() ? fallback : Long.parseLong(values.get(0));
    }

    private static void argumentHelp() {
        System.out.println("Syntax: forge bench -d <deck1> <deck2> [-n N] [-w W] [-s S] [-f F] [-a A...] [-c C] [-o DIR] [-t] [-jfr] [-q]");
        System.out.println("\tbench - AI performance measurement mode");
        System.out.println("\td - deck names or .dck filenames, one per player");
        System.out.println("\tD - directory to load the .dck files from, instead of the user deck folder");
        System.out.println("\tN - measured games, defaults to 1");
        System.out.println("\tW - warm-up games, discarded from the report, defaults to 0");
        System.out.println("\tS - base RNG seed; game i uses S+i, defaults to 0");
        System.out.println("\tF - format, defaults to constructed");
        System.out.println("\tA - AI profile per player, in deck order");
        System.out.println("\tC - per-game clock in seconds before calling a draw");
        System.out.println("\tDIR - output directory, defaults to forge-bench-out");
        System.out.println("\tt - record a decision trace for parity comparison (much slower; do not mix with timing runs)");
        System.out.println("\tjfr - also emit JFR events; combine with -XX:StartFlightRecording");
        System.out.println("\tq - quiet: only the final summary");
    }
}
