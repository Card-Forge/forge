package forge.view;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.time.StopWatch;

import forge.LobbyPlayer;
import forge.ai.AiProfileUtil;
import forge.deck.Deck;
import forge.deck.DeckGroup;
import forge.deck.io.DeckSerializer;
import forge.game.Game;
import forge.game.GameEndReason;
import forge.game.GameLogEntry;
import forge.game.GameLogEntryType;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.Match;
import forge.game.decision.DeterminismTrace;
import forge.game.decision.DiagnosticOutputPaths;
import forge.game.decision.ReferenceGameplayObserver;
import forge.game.decision.DeterminismTraceHasher;
import forge.game.player.RegisteredPlayer;
import forge.gamemodes.tournament.system.AbstractTournament;
import forge.gamemodes.tournament.system.TournamentBracket;
import forge.gamemodes.tournament.system.TournamentPairing;
import forge.gamemodes.tournament.system.TournamentPlayer;
import forge.gamemodes.tournament.system.TournamentRoundRobin;
import forge.gamemodes.tournament.system.TournamentSwiss;
import forge.localinstance.properties.ForgeConstants;
import forge.model.FModel;
import forge.player.GamePlayerUtil;
import forge.util.Lang;
import forge.util.DeterminismAuditRandom;
import forge.util.MyRandom;
import forge.util.TextUtil;
import forge.util.WordUtil;
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

        Long seed = null;
        if (params.containsKey("s")) {
            seed = Long.parseLong(params.get("s").get(0));
            MyRandom.setRandom(seededRandom(seed));
        } else if (DiagnosticOutputPaths.resolve().determinismTraceDirectory().isPresent()
                || Boolean.getBoolean(DeterminismTrace.AUDIT_RANDOM_PROPERTY)) {
            System.err.println("Determinism trace mode requires an explicit simulation seed");
            return;
        }

        GameType type = GameType.Constructed;
        if (params.containsKey("f")) {
            type = GameType.valueOf(WordUtil.capitalize(params.get("f").get(0)));
        }

        GameRules rules = new GameRules(type);
        rules.setAppliedVariants(EnumSet.of(type));

        if (matchSize != 0) {
            rules.setGamesPerMatch(matchSize);
        }

        if (params.containsKey("t")) {
            simulateTournament(params, rules, outputGamelog);
            System.out.flush();
            return;
        }

        List<RegisteredPlayer> pp = new ArrayList<>();
        StringBuilder sb = new StringBuilder();

        int i = 1;

        // Optional AI profile per player, in the same order as the decks. Lets a run pit one set of
        // AI settings against another, which is the only way to tell from the results whether an AI
        // change actually helped.
        List<String> aiProfiles = params.get("a");
        if (aiProfiles != null) {
            for (String profile : aiProfiles) {
                if (!AiProfileUtil.getProfilesDisplayList().contains(profile)) {
                    System.out.println(TextUtil.concatNoSpace("Unknown AI profile - ", profile,
                            ". Available profiles: ", String.join(", ", AiProfileUtil.getProfilesDisplayList())));
                    return;
                }
            }
        }

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
                String profile = aiProfiles != null && aiProfiles.size() >= i ? aiProfiles.get(i - 1) : "";
                String name = TextUtil.concatNoSpace("Ai(", String.valueOf(i), ")-", d.getName());
                sb.append(name);
                if (!profile.isEmpty()) {
                    sb.append(" [").append(profile).append("]");
                }

                RegisteredPlayer rp;

                if (type.equals(GameType.Commander)) {
                    rp = RegisteredPlayer.forCommander(d);
                } else {
                    rp = new RegisteredPlayer(d);
                }
                rp.setPlayer(GamePlayerUtil.createAiPlayer(name, i - 1, profile));
                pp.add(rp);
                i++;
            }
        }

        if (params.containsKey("c")) {
            rules.setSimTimeout(Integer.parseInt(params.get("c").get(0)));
        }

        sb.append(" - ").append(Lang.nounWithNumeral(nGames, "game")).append(" of ").append(type);
        if (seed != null) {
            sb.append(" seed ").append(seed);
        }

        System.out.println(sb.toString());

        Match mc = new Match(rules, pp, "Test");

        if (matchSize != 0) {
            int iGame = 0;
            while (!mc.isMatchOver()) {
                // play games until the match ends
                simulateSingleMatch(mc, iGame, outputGamelog);
                iGame++;
            }
        } else {
            for (int iGame = 0; iGame < nGames; iGame++) {
                simulateSingleMatch(mc, iGame, outputGamelog);
            }
        }

        System.out.flush();
    }

    private static void argumentHelp() {
        System.out.println("Syntax: forge.exe sim -d <deck1[.dck]> ... <deckX[.dck]> -D [D] -n [N] -m [M] -t [T] -p [P] -f [F] -s [S] -a [A] -q");
        System.out.println("\tsim - stands for simulation mode");
        System.out.println("\tdeck1 (or deck2,...,X) - constructed deck name or filename (has to be quoted when contains multiple words)");
        System.out.println("\tdeck is treated as file if it ends with a dot followed by three numbers or letters");
        System.out.println("\tD - absolute directory to load decks from");
        System.out.println("\tN - number of games, defaults to 1 (Ignores match setting)");
        System.out.println("\tM - Play full match of X games, typically 1,3,5 games. (Optional, overrides N)");
        System.out.println("\tT - Type of tournament to run with all provided decks (Bracket, RoundRobin, Swiss)");
        System.out.println("\tP - Amount of players per match (used only with Tournaments, defaults to 2)");
        System.out.println("\tF - format of games, defaults to constructed");
        System.out.println("\tS - RNG seed for simulation");
        System.out.println("\tA - AI profile per player, in the same order as the decks (e.g. -a Default Experimental)");
        System.out.println("\tc - Clock flag. Set the maximum time in seconds before calling the match a draw, defaults to 120.");
        System.out.println("\tq - Quiet flag. Output just the game result, not the entire game log.");
    }

    public static void simulateSingleMatch(final Match mc, int iGame, boolean outputGamelog) {
        final StopWatch sw = new StopWatch();
        sw.start();

        final long rngStartIndex = MyRandom.getRandom() instanceof DeterminismAuditRandom random
                ? random.getDrawCount() : 0L;
        final Game g1 = mc.createGame();
        final ReferenceGameplayObserver referenceObserver = attachReferenceObserver(g1, iGame);
        final DeterminismTrace determinismTrace = attachDeterminismTrace(g1, iGame, rngStartIndex);
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
            if (sw.isStarted()) {
                sw.stop();
            }
            g1.setGameOver(GameEndReason.Draw);
            finishDeterminismTrace(determinismTrace);
            finishReferenceObserver(referenceObserver);
            writeReferenceRng(iGame, rngStartIndex);
        }

        for (GameLogEntry l : reportableLogEntries(g1, outputGamelog)) {
            System.out.println(l);
        }

        final SimulationOutcomeClassification outcome = SimulationOutcomeClassification.classify(g1);
        writeReferenceOutcome(iGame, outcome);
        if (outcome.getKind() == SimulationOutcomeClassification.Kind.DRAW) {
            System.out.printf("\nGame Result: Game %d ended in a Draw! Took %d ms.%n", 1 + iGame, sw.getTime());
        } else if (outcome.getKind() == SimulationOutcomeClassification.Kind.SINGLE_WINNER) {
            System.out.printf("\nGame Result: Game %d ended in %d ms. %s has won!\n%n", 1 + iGame,
                    sw.getTime(), outcome.getWinnerName().orElseThrow());
        } else {
            System.out.printf("\nGame Result: Game %d has %s. Took %d ms.%n", 1 + iGame,
                    outcome.canonical(), sw.getTime());
        }
    }

    static List<GameLogEntry> reportableLogEntries(final Game game, final boolean outputGamelog) {
        final List<GameLogEntry> entries = new ArrayList<>(game.getGameLog().getLogEntries(
                outputGamelog ? null : GameLogEntryType.MATCH_RESULTS));
        entries.removeIf(entry -> entry.type() == GameLogEntryType.MATCH_RESULTS);
        Collections.reverse(entries);
        return entries;
    }

    private static void simulateTournament(Map<String, List<String>> params, GameRules rules, boolean outputGamelog) {
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
                        simulateSingleMatch(mc, iGame, outputGamelog);
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

    static Random seededRandom(final long seed) {
        if (DiagnosticOutputPaths.resolve().determinismTraceDirectory().isEmpty()
                && !Boolean.getBoolean(DeterminismTrace.AUDIT_RANDOM_PROPERTY)) {
            return new Random(seed);
        }
        return new DeterminismAuditRandom(seed);
    }

    private static DeterminismTrace attachDeterminismTrace(final Game game, final int gameIndex,
            final long rngStartIndex) {
        final Optional<Path> outputDirectory = DiagnosticOutputPaths.resolve().determinismTraceDirectory();
        if (outputDirectory.isEmpty()) {
            return null;
        }
        if (!(MyRandom.getRandom() instanceof DeterminismAuditRandom random)) {
            throw new IllegalStateException("Determinism trace mode requires the seeded audit RNG");
        }
        try {
            return DeterminismTrace.attach(game, gameIndex, random, outputDirectory.orElseThrow(), rngStartIndex);
        } catch (final IOException ex) {
            throw new IllegalStateException("Unable to initialize determinism trace output", ex);
        }
    }

    private static void finishDeterminismTrace(final DeterminismTrace trace) {
        if (trace == null) {
            return;
        }
        try {
            trace.finish();
        } catch (final IOException ex) {
            throw new IllegalStateException("Unable to write determinism trace output", ex);
        }
    }

    private static ReferenceGameplayObserver attachReferenceObserver(final Game game, final int gameIndex) {
        final String outputDirectory = System.getProperty(ReferenceGameplayObserver.OUTPUT_DIRECTORY_PROPERTY, "");
        if (outputDirectory.isBlank()) {
            return null;
        }
        try {
            return ReferenceGameplayObserver.attach(game, gameIndex, Path.of(outputDirectory));
        } catch (final IOException ex) {
            throw new IllegalStateException("Unable to initialize reference gameplay output", ex);
        }
    }

    private static void finishReferenceObserver(final ReferenceGameplayObserver observer) {
        if (observer == null) {
            return;
        }
        try {
            observer.finish();
        } catch (final IOException ex) {
            throw new IllegalStateException("Unable to write reference gameplay output", ex);
        }
    }

    private static void writeReferenceRng(final int gameIndex, final long rngStartIndex) {
        final Optional<Path> outputDirectory = referenceOutputDirectory();
        if (outputDirectory.isEmpty()) {
            return;
        }
        if (!(MyRandom.getRandom() instanceof DeterminismAuditRandom random)) {
            throw new IllegalStateException("Reference RNG output requires the seeded audit RNG");
        }
        final long rngEndIndex = random.getDrawCount();
        final List<String> records = random.getCanonicalRecords(rngStartIndex, rngEndIndex);
        final String prefix = String.format(Locale.ROOT, "game-%03d", gameIndex + 1);
        try {
            Files.write(outputDirectory.orElseThrow().resolve(prefix + ".reference-rng.trace"), records,
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
            Files.write(outputDirectory.orElseThrow().resolve(prefix + ".reference-rng-summary.properties"),
                    List.of("rngTraceVersion=" + DeterminismTrace.RNG_TRACE_VERSION,
                            "rngHash=" + DeterminismTraceHasher.sha256(records),
                            "rngDrawStart=" + rngStartIndex,
                            "rngDrawEnd=" + rngEndIndex,
                            "rngDrawCount=" + (rngEndIndex - rngStartIndex)), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        } catch (final IOException ex) {
            throw new IllegalStateException("Unable to write reference RNG output", ex);
        }
    }

    private static void writeReferenceOutcome(final int gameIndex,
            final SimulationOutcomeClassification outcome) {
        final Optional<Path> outputDirectory = referenceOutputDirectory();
        if (outputDirectory.isEmpty()) {
            return;
        }
        final String prefix = String.format(Locale.ROOT, "game-%03d", gameIndex + 1);
        try {
            Files.writeString(outputDirectory.orElseThrow().resolve(prefix + ".reference-outcome.txt"),
                    outcome.canonical() + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        } catch (final IOException ex) {
            throw new IllegalStateException("Unable to write reference outcome", ex);
        }
    }

    private static Optional<Path> referenceOutputDirectory() {
        final String outputDirectory = System.getProperty(ReferenceGameplayObserver.OUTPUT_DIRECTORY_PROPERTY, "");
        return outputDirectory.isBlank() ? Optional.empty() : Optional.of(Path.of(outputDirectory));
    }

    private static Deck deckFromCommandLineParameter(String deckname, GameType type) {
        int dotpos = deckname.lastIndexOf('.');
        if (dotpos > 0 && dotpos == deckname.length() - 4) {
            String baseDir = type.equals(GameType.Commander) ?
                    ForgeConstants.DECK_COMMANDER_DIR : ForgeConstants.DECK_CONSTRUCTED_DIR;

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
        } else {
            deckStore = FModel.getDecks().getConstructed();
        }

        return deckStore.get(deckname);
    }

}
