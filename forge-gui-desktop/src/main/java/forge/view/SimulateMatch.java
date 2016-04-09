package forge.view;

import java.io.File;
import java.util.*;

import forge.util.storage.IStorage;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.commons.lang3.time.StopWatch;

import forge.deck.Deck;
import forge.deck.io.DeckSerializer;
import forge.game.Game;
import forge.game.GameLogEntry;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.GameLogEntryType;
import forge.game.Match;
import forge.game.player.RegisteredPlayer;
import forge.model.FModel;
import forge.player.GamePlayerUtil;
import forge.util.Lang;

public class SimulateMatch {
    public static void simulate(String[] args) {
        FModel.initialize(null);

        System.out.println("Simulation mode");
        if(args.length < 4) {
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
            }
            else if (options != null) {
                options.add(a);
            }
            else {
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
            type = GameType.valueOf(WordUtils.capitalize(params.get("f").get(0)));
        }

        List<RegisteredPlayer> pp = new ArrayList<RegisteredPlayer>();
        StringBuilder sb = new StringBuilder();

        int i = 1;
        for(String deck : params.get("d")) {
            Deck d = deckFromCommandLineParameter(deck, type);
            if (d == null) {
                System.out.println(String.format("Could not load deck - %s, match cannot start", deck));
                return;
            }
            if (i > 1) {
                sb.append(" vs ");
            }
            String name = String.format("Ai(%s)-%s", i, d.getName());
            sb.append(name);

            RegisteredPlayer rp = null;

            if (type.equals(GameType.Commander)) {
                rp = RegisteredPlayer.forCommander(d);
            } else {
                rp = new RegisteredPlayer(d);
            }
            rp.setPlayer(GamePlayerUtil.createAiPlayer(name, i - 1));
            pp.add(rp);
            i++;
        }
        sb.append(" - ").append(Lang.nounWithNumeral(nGames, "game")).append(" of ").append(type);

        System.out.println(sb.toString());

        GameRules rules = new GameRules(type);
        rules.setAppliedVariants(EnumSet.of(type));

        if (matchSize != 0) {
            rules.setGamesPerMatch(matchSize);
        }
        Match mc = new Match(rules, pp, "Test");

        if (matchSize != 0) {
            int iGame = 0;
            while(!mc.isMatchOver()) {
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
        System.out.println("Syntax: forge.exe sim -d <deck1[.dck]> ... <deckX[.dck]> -n [N] -m [M] -f [F] -q");
        System.out.println("\tsim - stands for simulation mode");
        System.out.println("\tdeck1 (or deck2,...,X) - constructed deck name or filename (has to be quoted when contains multiple words)");
        System.out.println("\tdeck is treated as file if it ends with a dot followed by three numbers or letters");
        System.out.println("\tN - number of games, defaults to 1 (Ignores match setting)");
        System.out.println("\tM - Play full match of X games, typically 1,3,5 games. (Optional, overrides N)");
        System.out.println("\tF - format of games, defaults to constructed");
        System.out.println("\tq - Quiet flag. Output just the game result, not the entire game log.");
    }

    /**
     * TODO: Write javadoc for this method.
     * @param mc
     * @param iGame
     * @param outputGamelog
     */
    private static void simulateSingleMatch(Match mc, int iGame, boolean outputGamelog) {
        StopWatch sw = new StopWatch();
        sw.start();

        Game g1 = mc.createGame();
        // will run match in the same thread
        mc.startGame(g1);
        sw.stop();

        List<GameLogEntry> log;
        if (outputGamelog) {
            log = g1.getGameLog().getLogEntries(null);
        } else {
            log = g1.getGameLog().getLogEntries(GameLogEntryType.MATCH_RESULTS);
        }
        Collections.reverse(log);
        for(GameLogEntry l : log) {
            System.out.println(l);
        }

        System.out.println(String.format("\nGame %d ended in %d ms. %s has won!\n", 1+iGame, sw.getTime(), g1.getOutcome().getWinningLobbyPlayer().getName()));
    }

    public static Match simulateOffthreadGame(List<Deck> decks, GameType format, int games) {
        return null;
    }

    private static Deck deckFromCommandLineParameter(String deckname, GameType type) {
        int dotpos = deckname.lastIndexOf('.');
        if(dotpos > 0 && dotpos == deckname.length()-4)
            return DeckSerializer.fromFile(new File(deckname));

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