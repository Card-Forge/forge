package forge.view;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.gson.Gson;

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
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.player.GamePlayerUtil;

/**
 * Self-play harness for the LLM sidecar. Runs a sidecar-piloted deck through
 * many automated games and emits one JSONL record per sidecar seat per game
 * (archetype, pilot_mode, opponent, won, win_turn) for the reflection pipeline.
 *
 * <p>Two configs:</p>
 * <ul>
 *   <li><b>goldfish</b> — P1 is sidecar-piloted in the quarantined {@code solve}
 *       mode against a passive opponent (e.g. 60 Islands). Cleanest signal for a
 *       deck's fastest unobstructed line. Only P1 is a sidecar seat.</li>
 *   <li><b>mirror</b> — both seats are sidecar-piloted in {@code normal} mode.
 *       Two trajectories per engine game; interactive matchup lessons.</li>
 * </ul>
 *
 * <p>Quarantine: {@code solve} mode is set ONLY here, via the
 * {@code forge.ai.deckRecognition.pilotMode} system property, and never in a
 * mirror run. Sidecar influence weight defaults to 100 in the AI profile, so no
 * extra wiring is needed to let the sidecar drive plays.</p>
 *
 * <pre>
 *   forge sim is replaced by:
 *   forge selfplay -config goldfish -p1 mydeck.dck -p2 60-islands.dck -n 50 -out runs/ruby.jsonl
 *   forge selfplay -config mirror   -p1 a.dck      -p2 b.dck          -n 50 -out runs/mirror.jsonl
 * </pre>
 */
public final class SelfPlayRunner {

    // Literal keys mirror forge.ai.llm.DeckRecognitionFeature to avoid coupling
    // the runner to AI internals beyond what it must set.
    private static final String RECOGNITION_SYS_PROP = "forge.ai.deckRecognition";
    private static final String PILOT_MODE_SYS_PROP = "forge.ai.deckRecognition.pilotMode";
    private static final String SIDECAR_URL_SYS_PROP = "forge.ai.deckRecognition.url";
    private static final String RUN_ID_SYS_PROP = "forge.ai.deckRecognition.runId";
    private static final int DEFAULT_SIM_TIMEOUT_SECONDS = 1800;

    private static final Gson GSON = new Gson();

    private SelfPlayRunner() {
    }

    public static void run(final String[] args) {
        FModel.initialize(null, null);

        final Map<String, List<String>> params = parseArgs(args);
        final String config = first(params, "config", "goldfish");
        final boolean goldfish = !"mirror".equalsIgnoreCase(config);
        final int nGames = intParam(params, "n", 10);
        final String p1Name = first(params, "p1", null);
        final String p2Name = first(params, "p2", null);
        final String outPath = first(params, "out", "selfplay-runs.jsonl");
        // Results-store metadata. format/label tag the run; record toggles the
        // auto-POST to the sidecar's /selfplay/record (JSONL is written either way).
        final String format = first(params, "format", "");
        final String label = first(params, "label", "");
        final boolean record = !"false".equalsIgnoreCase(first(params, "record", "true"));
        final String url = first(params, "url", "http://localhost:18970");
        // Per-seat sidecar URLs for the two-sidecar gauntlet (mirror). Default
        // each to the shared url so a single sidecar still works.
        final String url1 = first(params, "url1", url);
        final String url2 = first(params, "url2", url);

        if (p1Name == null || p2Name == null) {
            help();
            return;
        }

        // Enable recognition for AI players. NOTE: GamePlayerUtil.createAiPlayer
        // overwrites the forge.ai.deckRecognition system property from this
        // preference every time an AI is created, so the PREFERENCE is the
        // authoritative switch — setting the system property alone is not enough.
        FModel.getPreferences().setPref(FPref.UI_ENABLE_DECK_RECOGNITION, true);
        System.setProperty(RECOGNITION_SYS_PROP, "true");
        System.setProperty(SIDECAR_URL_SYS_PROP, url);
        final String runId = (label.isBlank() ? config : label) + "-" + System.currentTimeMillis();
        System.setProperty(RUN_ID_SYS_PROP, runId);
        if (goldfish) {
            // Quarantined solve prompt; pilotMode/url props survive createAiPlayer.
            System.setProperty(PILOT_MODE_SYS_PROP, "solve");
            // The passive opponent (P2, player id 1) has nothing to pilot and no
            // threats to read — silence its sidecar calls so only the pilot hits
            // the sidecar (clean dashboard, ~2x fewer LLM calls, no own-archetype
            // cross-contamination).
            System.setProperty("forge.ai.deckRecognition.disableSeats", "1");
        } else {
            System.clearProperty(PILOT_MODE_SYS_PROP);
            System.clearProperty("forge.ai.deckRecognition.disableSeats");
            // Mirror: both seats are real pilots — point each at its own sidecar
            // (run sidecar #2 on another port) for separate dashboards.
            System.setProperty("forge.ai.deckRecognition.url.0", url1);
            System.setProperty("forge.ai.deckRecognition.url.1", url2);
        }

        final GameType type = GameType.Constructed;
        final Deck p1Deck = loadDeck(p1Name, type);
        final Deck p2Deck = loadDeck(p2Name, type);
        if (p1Deck == null || p2Deck == null) {
            System.out.println("Could not load one or both decks; aborting.");
            return;
        }

        final GameRules rules = new GameRules(type);
        rules.setAppliedVariants(java.util.EnumSet.of(type));
        rules.setSimTimeout(intParam(params, "c", DEFAULT_SIM_TIMEOUT_SECONDS));

        final String p1PlayerName = "Ai(1)-" + p1Deck.getName();
        final String p2PlayerName = "Ai(2)-" + p2Deck.getName();
        final List<RegisteredPlayer> players = new ArrayList<>();
        players.add(registerAi(p1Deck, p1PlayerName, 0));
        players.add(registerAi(p2Deck, p2PlayerName, 1));

        // Sidecar seats: goldfish pilots only P1; mirror pilots both.
        final boolean p1Seat = true;
        final boolean p2Seat = !goldfish;
        final String pilotMode = goldfish ? "solve" : "normal";

        final Path out = Paths.get(outPath);
        try {
            if (out.getParent() != null) {
                Files.createDirectories(out.getParent());
            }
        } catch (final IOException ex) {
            System.out.println("Cannot create output directory: " + ex.getMessage());
            return;
        }

        System.out.printf("Self-play: config=%s games=%d p1=%s p2=%s out=%s%n",
                config, nGames, p1Deck.getName(), p2Deck.getName(), out);

        // Per-seat records accumulate here so the whole run can be recorded into
        // the sidecar's results store in one POST after all games finish.
        final List<Map<String, Object>> recorded = new ArrayList<>();

        final Match mc = new Match(rules, players, "SelfPlay");
        for (int i = 0; i < nGames; i++) {
            playOne(mc, rules, i, p1Deck, p2Deck, p1PlayerName, p2PlayerName,
                    p1Seat, p2Seat, pilotMode, out, recorded);
        }
        System.out.flush();

        if (record && !recorded.isEmpty()) {
            recordRun(url, config, format, label, outPath, recorded);
        }
    }

    private static void playOne(final Match mc, final GameRules rules, final int iGame,
                                final Deck p1Deck, final Deck p2Deck,
                                final String p1PlayerName, final String p2PlayerName,
                                final boolean p1Seat, final boolean p2Seat,
                                final String pilotMode, final Path out,
                                final List<Map<String, Object>> recorded) {
        final Game game = mc.createGame();
        boolean timedOut = false;
        try {
            TimeLimitedCodeBlock.runWithTimeout(() -> mc.startGame(game),
                    rules.getSimTimeout(), TimeUnit.SECONDS);
        } catch (final TimeoutException e) {
            timedOut = true;
            System.out.println("Game " + (iGame + 1) + " timed out (recorded as no-win).");
        } catch (final Exception | StackOverflowError e) {
            e.printStackTrace();
        } finally {
            if (!game.isGameOver()) {
                game.setGameOver(GameEndReason.Draw);
            }
        }

        // A timeout interrupts the game thread. During unwind, Forge may leave a
        // partial outcome behind; do not trust it as a real game result.
        final String winnerName = timedOut ? null : winnerName(game);

        if (p1Seat) {
            recorded.add(writeRecord(out, game, 0, p1PlayerName, p2PlayerName,
                    p1Deck.getName(), p2Deck.getName(), pilotMode, winnerName, timedOut));
        }
        if (p2Seat) {
            recorded.add(writeRecord(out, game, 1, p2PlayerName, p1PlayerName,
                    p2Deck.getName(), p1Deck.getName(), pilotMode, winnerName, timedOut));
        }

        // Report the winner's OWN turn count, not the global game turn.
        final int winTurn = winnerName == null ? safeTurn(game) : playerTurn(game, winnerName);
        System.out.printf("Game %d: winner=%s turn=%d%s%n", iGame + 1,
                winnerName == null ? "(none)" : winnerName, winTurn,
                timedOut ? " timed_out=true" : "");
    }

    private static Map<String, Object> writeRecord(final Path out, final Game game,
                                    final int seat, final String playerName,
                                    final String opponentPlayerName, final String deckName,
                                    final String opponentName, final String pilotMode,
                                    final String winnerName, final boolean timedOut) {
        final boolean won = winnerName != null && winnerName.equals(playerName);
        final int turn = playerTurn(game, playerName);
        final Map<String, Object> rec = new LinkedHashMap<>();
        rec.put("game_id", String.valueOf(game.getId()));
        rec.put("seat", seat);
        rec.put("player", playerName);
        rec.put("archetype", deckName);
        rec.put("opponent_player", opponentPlayerName);
        rec.put("opponent", opponentName);
        rec.put("pilot_mode", pilotMode);
        rec.put("won", won);
        rec.put("win_turn", won ? turn : null);
        rec.put("turns", turn);
        rec.put("timed_out", timedOut);
        try {
            Files.write(out, (GSON.toJson(rec) + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (final IOException ex) {
            System.out.println("Failed to write record: " + ex.getMessage());
        }
        return rec;
    }

    /**
     * POST the finished run's per-seat records to the sidecar's
     * {@code /selfplay/record} endpoint so it lands in the results store for
     * baselining and over-time tracking. Fail-soft: the JSONL artifact is always
     * written, so a missing/erroring sidecar only skips the persistent record.
     */
    private static void recordRun(final String url, final String config, final String format,
                                  final String label, final String sourceFile,
                                  final List<Map<String, Object>> recorded) {
        final Map<String, Object> body = new LinkedHashMap<>();
        body.put("records", recorded);
        body.put("format", format);
        body.put("config", config);
        body.put("label", label);
        body.put("source_file", sourceFile);
        final String endpoint = url.replaceAll("/+$", "") + "/selfplay/record";
        try {
            // Pin HTTP/1.1: the JDK client defaults to HTTP/2 and attempts an
            // h2c upgrade that uvicorn/h11 rejects ("Invalid HTTP request").
            final java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .version(java.net.http.HttpClient.Version.HTTP_1_1)
                    .build();
            final java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(endpoint))
                    .timeout(java.time.Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(
                            GSON.toJson(body), StandardCharsets.UTF_8))
                    .build();
            final java.net.http.HttpResponse<String> resp = client.send(
                    request, java.net.http.HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 == 2) {
                System.out.println("Recorded run to results store: " + resp.body());
            } else {
                System.out.println("Run record skipped (sidecar returned HTTP "
                        + resp.statusCode() + "): " + resp.body());
            }
        } catch (final IOException | InterruptedException ex) {
            System.out.println("Run record skipped (sidecar unreachable): " + ex.getMessage());
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static int safeTurn(final Game game) {
        try {
            return game.getPhaseHandler().getTurn();
        } catch (final RuntimeException ex) {
            return 0;
        }
    }

    /**
     * Turns taken by the player whose name contains {@code nameNeedle} — i.e.
     * {@link Player#getTurn()}, the per-player count.
     *
     * <p>This is deliberately NOT {@link forge.game.phase.PhaseHandler#getTurn()},
     * which counts <em>every</em> player's turn in the game and so roughly
     * doubles in a 2-player match (a win on the pilot's turn 9 reads as game
     * turn 18). Records are per-seat, so each uses its own player's turn count.</p>
     */
    private static int playerTurn(final Game game, final String nameNeedle) {
        try {
            for (final Player p : game.getPlayers()) {
                if (p.getName() != null && nameNeedle != null && p.getName().contains(nameNeedle)) {
                    return p.getTurn();
                }
            }
        } catch (final RuntimeException ex) {
            // fall through to the global turn as a best-effort fallback
        }
        return safeTurn(game);
    }

    private static String winnerName(final Game game) {
        try {
            if (game.getOutcome() == null || game.getOutcome().isDraw()) {
                return null;
            }
            return game.getOutcome().getWinningLobbyPlayer() == null
                    ? null : game.getOutcome().getWinningLobbyPlayer().getName();
        } catch (final RuntimeException ex) {
            return null;
        }
    }

    private static RegisteredPlayer registerAi(final Deck deck, final String name, final int seat) {
        final RegisteredPlayer rp = new RegisteredPlayer(deck);
        rp.setPlayer(GamePlayerUtil.createAiPlayer(name, seat));
        return rp;
    }

    private static Deck loadDeck(final String nameOrPath, final GameType type) {
        // Treat anything ending in a 3-char extension as a file path.
        final int dot = nameOrPath.lastIndexOf('.');
        if (dot > 0 && dot == nameOrPath.length() - 4) {
            File f = new File(nameOrPath);
            if (!f.exists()) {
                f = new File(ForgeConstants.DECK_CONSTRUCTED_DIR + nameOrPath);
            }
            return f.exists() ? DeckSerializer.fromFile(f) : null;
        }
        return FModel.getDecks().getConstructed().get(nameOrPath);
    }

    private static Map<String, List<String>> parseArgs(final String[] args) {
        final Map<String, List<String>> params = new HashMap<>();
        List<String> current = null;
        for (int i = 1; i < args.length; i++) { // args[0] == "selfplay"
            final String a = args[i];
            if (!a.isEmpty() && a.charAt(0) == '-') {
                current = new ArrayList<>();
                params.put(a.substring(1), current);
            } else if (current != null) {
                current.add(a);
            }
        }
        return params;
    }

    private static String first(final Map<String, List<String>> params, final String key, final String dflt) {
        final List<String> v = params.get(key);
        return (v == null || v.isEmpty()) ? dflt : v.get(0);
    }

    private static int intParam(final Map<String, List<String>> params, final String key, final int dflt) {
        final String v = first(params, key, null);
        if (v == null) {
            return dflt;
        }
        try {
            return Integer.parseInt(v);
        } catch (final NumberFormatException ex) {
            return dflt;
        }
    }

    private static void help() {
        System.out.println("Syntax: forge selfplay -config <goldfish|mirror> -p1 <deck[.dck]> -p2 <deck[.dck]> -n <N> -out <file.jsonl> [-format <fmt>] [-label <tag>] [-record <true|false>] [-url <sidecar>] [-url1 <s1>] [-url2 <s2>] [-c <timeout_s>]");
        System.out.println("  goldfish: P1 sidecar (solve mode) vs passive P2 (recognition disabled); one record/game.");
        System.out.println("  mirror:   both seats sidecar (normal mode); two records/game.");
        System.out.println("  -format:  format tag stored with the run (e.g. modern); used by the results store.");
        System.out.println("  -label:   optional run tag; use 'baseline' to pin this run as the deck's baseline.");
        System.out.println("  -record:  POST the run to the sidecar results store on finish (default true).");
        System.out.println("  -url:     shared sidecar URL (default http://localhost:18970).");
        System.out.println("  -url1/-url2: per-seat sidecar URLs for the mirror gauntlet (two dashboards). Default to -url.");
        System.out.println("  -c:       per-game timeout in seconds (default " + DEFAULT_SIM_TIMEOUT_SECONDS + ").");
    }
}
