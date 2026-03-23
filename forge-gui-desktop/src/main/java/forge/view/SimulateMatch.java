package forge.view;

import com.google.common.eventbus.Subscribe;
import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.time.StopWatch;

import forge.LobbyPlayer;
import forge.deck.Deck;
import forge.deck.DeckGroup;
import forge.deck.io.DeckSerializer;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardView;
import forge.game.GameEndReason;
import forge.game.GameLogEntry;
import forge.game.GameLogEntryType;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.Match;
import forge.game.event.GameEventTurnBegan;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.gamemodes.tournament.system.AbstractTournament;
import forge.gamemodes.tournament.system.TournamentBracket;
import forge.gamemodes.tournament.system.TournamentPairing;
import forge.gamemodes.tournament.system.TournamentPlayer;
import forge.gamemodes.tournament.system.TournamentRoundRobin;
import forge.gamemodes.tournament.system.TournamentSwiss;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.localinstance.properties.ForgeConstants;
import forge.model.FModel;
import forge.sim.SimVerboseConfig;
import forge.player.GamePlayerUtil;
import forge.util.CardTranslation;
import forge.util.Lang;
import forge.util.TextUtil;
import forge.util.storage.IStorage;

public class SimulateMatch {
    public static void simulate(String[] args) {
        FModel.initialize(null, null);

        System.out.println("Simulation mode");
        if (args.length < 4) {
            argumentHelp();
            return;
        }

        final Map<String, List<String>> params = new HashMap<>();
        List<String> options = null;

        for (int i = 1; i < args.length; i++) {
            // "sim" is in the 0th slot
            final String a = args[i];

            if (a.charAt(0) == '-') {
                if (a.length() < 2) {
                    System.err.println("Error at argument " + a);
                    argumentHelp();
                    return;
                }

                options = new ArrayList<>();
                params.put(a.substring(1), options);
            } else if (options != null) {
                options.add(a);
            } else {
                System.err.println("Illegal parameter usage");
                return;
            }
        }

        int nGames = 1;
        if (params.containsKey("n")) {
            // Number of games should only be a single string
            nGames = Integer.parseInt(params.get("n").get(0));
        }

        int matchSize = 0;
        if (params.containsKey("m")) {
            // Match size ("best of X games")
            matchSize = Integer.parseInt(params.get("m").get(0));
        }

        boolean outputGamelog = !params.containsKey("q");

        GameType type = GameType.Constructed;
        if (params.containsKey("f")) {
            final String requestedFormat = params.get("f").get(0);
            type = parseGameType(requestedFormat);
            if (type == null) {
                System.out.println("Unknown format - " + requestedFormat);
                argumentHelp();
                return;
            }
        }

        GameRules rules = new GameRules(type);
        rules.setAppliedVariants(EnumSet.of(type));

        if (matchSize != 0) {
            rules.setGamesPerMatch(matchSize);
        }

        if (params.containsKey("t")) {
            final int maxTurns = params.containsKey("x") ? Integer.parseInt(params.get("x").get(0)) : 0;
            final SimVerboseConfig verboseCfg = params.containsKey("v") ? SimVerboseConfig.load() : null;
            simulateTournament(params, rules, outputGamelog, maxTurns, verboseCfg);
            System.out.flush();
            return;
        }

        List<RegisteredPlayer> pp = new ArrayList<>();
        StringBuilder sb = new StringBuilder();

        int i = 1;

        if (params.containsKey("d")) {
            for (String deck : params.get("d")) {
                Deck d = deckFromCommandLineParameter(deck, type);
                if (d == null) {
                    System.out.println(TextUtil.concatNoSpace("Could not load deck - ", deck, ", match cannot start"));
                    return;
                }
                if (i > 1) {
                    sb.append(" vs ");
                }
                String name = TextUtil.concatNoSpace("Ai(", String.valueOf(i), ")-", d.getName());
                sb.append(name);

                RegisteredPlayer rp;

                if (type.equals(GameType.Commander)) {
                    rp = RegisteredPlayer.forCommander(d);
                } else {
                    rp = new RegisteredPlayer(d);
                }
                rp.setPlayer(GamePlayerUtil.createAiPlayer(name, i - 1));
                pp.add(rp);
                i++;
            }
        }

        if (params.containsKey("c")) {
            rules.setSimTimeout(Integer.parseInt(params.get("c").get(0)));
        }
        final int maxTurns = params.containsKey("x") ? Integer.parseInt(params.get("x").get(0)) : 0;
        final SimVerboseConfig verboseCfg = params.containsKey("v") ? SimVerboseConfig.load() : null;

        sb.append(" - ").append(Lang.nounWithNumeral(nGames, "game")).append(" of ").append(type);

        System.out.println(sb.toString());

        Match mc = new Match(rules, pp, "Test");

        if (matchSize != 0) {
            int iGame = 0;
            while (!mc.isMatchOver()) {
                // play games until the match ends
                simulateSingleMatch(mc, iGame, outputGamelog, maxTurns, verboseCfg);
                iGame++;
            }
        } else {
            for (int iGame = 0; iGame < nGames; iGame++) {
                simulateSingleMatch(mc, iGame, outputGamelog, maxTurns, verboseCfg);
            }
        }

        System.out.flush();
    }

    private static void argumentHelp() {
        System.out.println("Syntax: forge.exe sim -d <deck1[.dck]> ... <deckX[.dck]> -D [D] -n [N] -m [M] -t [T] -p [P] -f [F] -x [X] -v -q");
        System.out.println("\tsim - stands for simulation mode");
        System.out.println("\tdeck1 (or deck2,...,X) - constructed deck name or filename (has to be quoted when contains multiple words)");
        System.out.println("\tdeck is treated as file if it ends with a dot followed by three numbers or letters");
        System.out.println("\tD - absolute directory to load decks from");
        System.out.println("\tN - number of games, defaults to 1 (Ignores match setting)");
        System.out.println("\tM - Play full match of X games, typically 1,3,5 games. (Optional, overrides N)");
        System.out.println("\tT - Type of tournament to run with all provided decks (Bracket, RoundRobin, Swiss)");
        System.out.println("\tP - Amount of players per match (used only with Tournaments, defaults to 2)");
        System.out.println("\tF - format of games, defaults to constructed");
        System.out.println("\tX - Maximum number of turns allowed in a game. Reaching this ends the game as a draw.");
        System.out.println("\tv - Verbose mode. Extra sim logging merges sim-verbose.properties from Forge userDir/sim/, then ./sim/, then working directory (later file overrides; see "
                + ForgeConstants.SIM_VERBOSE_CONFIG_EXAMPLE + "). With full game log, [verbose] lines appear in time order; with -q they print after match results.");
        System.out.println("\tc - Clock flag. Set the maximum time in seconds before calling the match a draw, defaults to 120.");
        System.out.println("\tq - Quiet flag. Output just the game result, not the entire game log.");
    }

    private static GameType parseGameType(final String rawFormat) {
        if (rawFormat == null || rawFormat.isEmpty()) {
            return null;
        }

        final String normalized = rawFormat.replaceAll("[\\s_\\-]", "");
        for (final GameType gameType : GameType.values()) {
            final String enumName = gameType.name();
            if (enumName.equalsIgnoreCase(rawFormat)
                    || enumName.equalsIgnoreCase(normalized)
                    || enumName.replaceAll("[\\s_\\-]", "").equalsIgnoreCase(normalized)) {
                return gameType;
            }
        }
        return null;
    }

    /**
     * @param verboseConfig loaded when {@code -v} was passed; {@code null} disables verbose logging
     */
    public static void simulateSingleMatch(final Match mc, int iGame, boolean outputGamelog, int maxTurns,
            final SimVerboseConfig verboseConfig) {
        final StopWatch sw = new StopWatch();
        sw.start();

        final Game g1 = mc.createGame();
        final AtomicBoolean turnCapReached = new AtomicBoolean(false);
        final AtomicBoolean stopTurnWatcher = new AtomicBoolean(false);
        final Thread turnWatcher;
        final List<String> verboseQuietBuffer = verboseConfig != null && verboseConfig.anyEnabled() && !outputGamelog
                ? Collections.synchronizedList(new ArrayList<>()) : null;
        if (verboseConfig != null && verboseConfig.isEnabled(SimVerboseConfig.DRAWS)) {
            // Log every Library -> Hand move. Do not dedupe by card id: the same Card can return to the
            // library (mulligan) and be drawn again; dedupe would hide later draws (e.g. draw step).
            // With full game log, append to GameLog so output matches game chronology (not all [verbose] first).
            g1.subscribeToEvents(new VerboseDrawEventLogger(g1, verboseQuietBuffer));
        }
        if (verboseConfig != null && (verboseConfig.isEnabled(SimVerboseConfig.BEGINNING_CARDS_IN_HAND)
                || verboseConfig.logsBeginningLibrary())) {
            g1.subscribeToEvents(new VerboseTurnBeginLogger(g1, verboseQuietBuffer, verboseConfig));
        }
        if (maxTurns > 0) {
            turnWatcher = new Thread(() -> {
                while (!stopTurnWatcher.get() && !g1.isGameOver()) {
                    if (g1.getPhaseHandler().getTurn() >= maxTurns) {
                        turnCapReached.set(true);
                        g1.setGameOver(GameEndReason.Draw);
                        break;
                    }
                    try {
                        Thread.sleep(20L);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }, "sim-turn-cap-watcher");
            turnWatcher.setDaemon(true);
            turnWatcher.start();
        } else {
            turnWatcher = null;
        }
        // will run match in the same thread
        try {
            TimeLimitedCodeBlock.runWithTimeout(() -> {
                mc.startGame(g1);
                sw.stop();
            }, mc.getRules().getSimTimeout(), TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            System.out.println("Stopping slow match as draw");
        } catch (Exception | StackOverflowError e) {
            e.printStackTrace();
        } finally {
            stopTurnWatcher.set(true);
            if (turnWatcher != null) {
                turnWatcher.interrupt();
            }
            if (sw.isStarted()) {
                sw.stop();
            }
            if (!g1.isGameOver()) {
                g1.setGameOver(GameEndReason.Draw);
            }
        }

        List<GameLogEntry> log;
        if (outputGamelog) {
            log = g1.getGameLog().getLogEntries(null);
        } else {
            log = g1.getGameLog().getLogEntries(GameLogEntryType.MATCH_RESULTS);
        }
        Collections.reverse(log);
        for (GameLogEntry l : log) {
            if (l.type() == GameLogEntryType.INFORMATION && l.message() != null
                    && l.message().startsWith("[verbose]")) {
                System.out.println(l.message());
            } else {
                System.out.println(l);
            }
        }
        if (verboseQuietBuffer != null && !verboseQuietBuffer.isEmpty()) {
            for (final String line : verboseQuietBuffer) {
                System.out.println(line);
            }
        }

        // If both players life totals to 0 in a single turn, the game should end in a draw
        if (g1.getOutcome().isDraw()) {
            System.out.printf("\nGame Result: Game %d ended in a Draw! Took %d ms.%n", 1 + iGame, sw.getTime());
            if (turnCapReached.get()) {
                System.out.printf("Draw reason: reached maximum turn limit (%d).%n", maxTurns);
            }
        } else {
            System.out.printf("\nGame Result: Game %d ended in %d ms. %s has won!\n%n", 1 + iGame, sw.getTime(), g1.getOutcome().getWinningLobbyPlayer().getName());
        }
    }

    private static void simulateTournament(Map<String, List<String>> params, GameRules rules, boolean outputGamelog,
            int maxTurns, final SimVerboseConfig verboseConfig) {
        String tournament = params.get("t").get(0);
        AbstractTournament tourney = null;
        int matchPlayers = params.containsKey("p") ? Integer.parseInt(params.get("p").get(0)) : 2;

        DeckGroup deckGroup = new DeckGroup("SimulatedTournament");
        List<TournamentPlayer> players = new ArrayList<>();
        int numPlayers = 0;
        if (params.containsKey("d")) {
            for (String deck : params.get("d")) {
                Deck d = deckFromCommandLineParameter(deck, rules.getGameType());
                if (d == null) {
                    System.out.println(TextUtil.concatNoSpace("Could not load deck - ", deck, ", match cannot start"));
                    return;
                }

                deckGroup.addAiDeck(d);
                players.add(new TournamentPlayer(GamePlayerUtil.createAiPlayer(d.getName(), 0), numPlayers));
                numPlayers++;
            }
        }

        if (params.containsKey("D")) {
            // Direc
            String foldName = params.get("D").get(0);
            File folder = new File(foldName);
            if (!folder.isDirectory()) {
                System.out.println("Directory not found - " + foldName);
            } else {
                for (File deck : folder.listFiles((dir, name) -> name.endsWith(".dck"))) {
                    Deck d = DeckSerializer.fromFile(deck);
                    if (d == null) {
                        System.out.println(TextUtil.concatNoSpace("Could not load deck - ", deck.getName(), ", match cannot start"));
                        return;
                    }
                    deckGroup.addAiDeck(d);
                    players.add(new TournamentPlayer(GamePlayerUtil.createAiPlayer(d.getName(), 0), numPlayers));
                    numPlayers++;
                }
            }
        }

        if (numPlayers == 0) {
            System.out.println("No decks/Players found. Please try again.");
        }

        if ("bracket".equalsIgnoreCase(tournament)) {
            tourney = new TournamentBracket(players, matchPlayers);
        } else if ("roundrobin".equalsIgnoreCase(tournament)) {
            tourney = new TournamentRoundRobin(players, matchPlayers);
        } else if ("swiss".equalsIgnoreCase(tournament)) {
            tourney = new TournamentSwiss(players, matchPlayers);
        }
        if (tourney == null) {
            System.out.println("Failed to initialize tournament, bailing out");
            return;
        }

        tourney.initializeTournament();

        String lastWinner = "";
        int curRound = 0;
        System.out.println(TextUtil.concatNoSpace("Starting a ", tournament, " tournament with ",
                String.valueOf(numPlayers), " players over ",
                String.valueOf(tourney.getTotalRounds()), " rounds"));
        while (!tourney.isTournamentOver()) {
            if (tourney.getActiveRound() != curRound) {
                if (curRound != 0) {
                    System.out.println(TextUtil.concatNoSpace("End Round - ", String.valueOf(curRound)));
                }
                curRound = tourney.getActiveRound();
                System.out.println();
                System.out.println(TextUtil.concatNoSpace("Round ", String.valueOf(curRound), " Pairings:"));

                for (TournamentPairing pairing : tourney.getActivePairings()) {
                    System.out.println(pairing.outputHeader());
                }
                System.out.println();
            }

            TournamentPairing pairing = tourney.getNextPairing();
            List<RegisteredPlayer> regPlayers = AbstractTournament.registerTournamentPlayers(pairing, deckGroup);

            StringBuilder sb = new StringBuilder();
            sb.append("Round ").append(tourney.getActiveRound()).append(" - ");
            sb.append(pairing.outputHeader());
            System.out.println(sb.toString());

            if (!pairing.isBye()) {
                Match mc = new Match(rules, regPlayers, "TourneyMatch");

                int exceptions = 0;
                int iGame = 0;
                while (!mc.isMatchOver()) {
                    // play games until the match ends
                    try {
                        simulateSingleMatch(mc, iGame, outputGamelog, maxTurns, verboseConfig);
                        iGame++;
                    } catch (Exception e) {
                        exceptions++;
                        System.out.println(e.toString());
                        if (exceptions > 5) {
                            System.out.println("Exceeded number of exceptions thrown. Abandoning match...");
                            break;
                        } else {
                            System.out.println("Game threw exception. Abandoning game and continuing...");
                        }
                    }

                }
                LobbyPlayer winner = mc.getWinner().getPlayer();
                for (TournamentPlayer tp : pairing.getPairedPlayers()) {
                    if (winner.equals(tp.getPlayer())) {
                        pairing.setWinner(tp);
                        lastWinner = winner.getName();
                        System.out.println(TextUtil.concatNoSpace("Match Winner - ", lastWinner, "!"));
                        System.out.println();
                        break;
                    }
                }
            }

            tourney.reportMatchCompletion(pairing);
        }
        tourney.outputTournamentResults();
    }

    public static Match simulateOffthreadGame(List<Deck> decks, GameType format, int games) {
        return null;
    }

    /**
     * Card name plus runtime game id in parentheses, matching {@link CardView#toString()} name/id suffix
     * (without the leading zone prefix used in full {@code toString()}).
     */
    private static String verboseCardLabel(final CardView view) {
        if (view == null) {
            return "?";
        }
        if (view.getName() == null || view.getName().isEmpty()) {
            return view.toString();
        }
        final String name = CardTranslation.getTranslatedName(view.getName());
        final int id = view.getId();
        if (id <= 0) {
            return name;
        }
        return name + " (" + id + ")";
    }

    private static String verboseCardLabel(final Card card) {
        if (card == null) {
            return "?";
        }
        final CardView v = card.getView();
        return v != null ? verboseCardLabel(v) : card.getName();
    }

    private static void addVerboseSimLine(final Game game, final List<String> quietBuffer, final String line) {
        if (quietBuffer != null) {
            quietBuffer.add(line);
        } else {
            game.getGameLog().add(GameLogEntryType.INFORMATION, line);
        }
    }

    private static final class VerboseDrawEventLogger {
        private final Game game;
        /** When non-null (-q), game log omits INFORMATION; buffer and print after match lines. */
        private final List<String> quietBuffer;

        private VerboseDrawEventLogger(final Game game0, final List<String> quietBuffer0) {
            this.game = game0;
            this.quietBuffer = quietBuffer0;
        }

        @Subscribe
        public void onCardChangeZone(final forge.game.event.GameEventCardChangeZone event) {
            if (event == null || event.from() == null || event.to() == null || event.card() == null) {
                return;
            }
            if (event.from().zoneType() != forge.game.zone.ZoneType.Library
                    || event.to().zoneType() != forge.game.zone.ZoneType.Hand) {
                return;
            }
            final String playerName = event.to().player() == null ? "Unknown player" : event.to().player().getName();
            final String line = String.format("[verbose] %s drew: %s", playerName, verboseCardLabel(event.card()));
            addVerboseSimLine(game, quietBuffer, line);
        }
    }

    /**
     * At {@link GameEventTurnBegan}: optional hand list and/or top-of-library names per sim-verbose.properties.
     */
    private static final class VerboseTurnBeginLogger {
        private final Game game;
        private final List<String> quietBuffer;
        private final SimVerboseConfig config;

        private VerboseTurnBeginLogger(final Game game0, final List<String> quietBuffer0,
                final SimVerboseConfig config0) {
            this.game = game0;
            this.quietBuffer = quietBuffer0;
            this.config = config0;
        }

        @Subscribe
        public void onTurnBegan(final GameEventTurnBegan event) {
            if (event == null) {
                return;
            }
            // Active player is already set when TurnBegan fires; avoids cache/view mismatch.
            final Player p = game.getPhaseHandler().getPlayerTurn();
            if (p == null) {
                return;
            }
            final int turn = event.turnNumber();
            final String pname = p.getName();

            if (config.isEnabled(SimVerboseConfig.BEGINNING_CARDS_IN_HAND)) {
                final StringBuilder sb = new StringBuilder();
                for (final Card c : p.getCardsIn(ZoneType.Hand)) {
                    if (sb.length() > 0) {
                        sb.append(", ");
                    }
                    sb.append(verboseCardLabel(c));
                }
                final String handList = sb.length() == 0 ? "(empty)" : sb.toString();
                addVerboseSimLine(game, quietBuffer,
                        String.format("[verbose] Turn %d: %s hand: %s", turn, pname, handList));
            }

            if (config.logsBeginningLibrary()) {
                final Integer n = config.getBeginningLibraryCardCount();
                final PlayerZone lib = p.getZone(ZoneType.Library);
                final int size = lib.size();
                final int limit = n != null && n == -1 ? size : Math.min(n, size);
                final StringBuilder lb = new StringBuilder();
                for (int i = 0; i < limit; i++) {
                    if (lb.length() > 0) {
                        lb.append(", ");
                    }
                    lb.append(verboseCardLabel(lib.get(i)));
                }
                final String libList = size == 0 ? "(empty)" : lb.toString();
                final String scope = n != null && n == -1
                        ? String.format("all %d", size)
                        : String.format("top %d", limit);
                addVerboseSimLine(game, quietBuffer,
                        String.format("[verbose] Turn %d: %s library (%s): %s", turn, pname, scope, libList));
            }
        }
    }

    private static Deck deckFromCommandLineParameter(String deckname, GameType type) {
        int dotpos = deckname.lastIndexOf('.');
        if (dotpos > 0 && dotpos == deckname.length() - 4) {
            final String baseDir;
            if (type.equals(GameType.Commander)) {
                baseDir = ForgeConstants.DECK_COMMANDER_DIR;
            } else if (type.equals(GameType.DanDan)) {
                baseDir = ForgeConstants.DECK_DANDAN_DIR;
            } else {
                baseDir = ForgeConstants.DECK_CONSTRUCTED_DIR;
            }

            File f = new File(baseDir + deckname);
            if (!f.exists()) {
                System.out.println("No deck found in " + baseDir);
            }

            return DeckSerializer.fromFile(f);
        }

        IStorage<Deck> deckStore = null;

        // Add other game types here...
        if (type.equals(GameType.Commander)) {
            deckStore = FModel.getDecks().getCommander();
        } else if (type.equals(GameType.DanDan)) {
            deckStore = FModel.getDecks().getDanDan();
        } else {
            deckStore = FModel.getDecks().getConstructed();
        }

        return deckStore.get(deckname);
    }

}