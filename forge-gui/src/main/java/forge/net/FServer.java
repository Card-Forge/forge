package forge.net;

import com.google.common.base.Supplier;

import forge.deck.Deck;
import forge.deck.io.DeckSerializer;
import forge.game.*;
import forge.game.player.LobbyPlayer;
import forge.game.player.RegisteredPlayer;
import forge.model.FModel;
import forge.player.LobbyPlayerHuman;
import forge.util.Lang;

import org.apache.commons.lang3.time.StopWatch;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class FServer {
    private FServer() {
        
    }

    private static boolean interactiveMode = true;
    private static Lobby lobby = null;
    
    public static Lobby getLobby() {
        if (lobby == null) {
            //not a very good solution still
            lobby = new Lobby(new Supplier<LobbyPlayer>() {
                @Override
                public LobbyPlayer get() {
                    // TODO Auto-generated method stub
                    return new LobbyPlayerHuman("Human");
                }
            });
        }
        return lobby;
    }
    
    /**
     * TODO: Write javadoc for this method.
     * @return
     */
    private static final NetServer server = new NetServer();
    public static NetServer getServer() {
        // TODO Auto-generated method stub
        return server;
    }

    public static void simulateMatches(String[] args) {
        FModel.initialize(null);

        interactiveMode = false;
        System.out.println("Simulation mode");
        if(args.length < 3 ) {
            System.out.println("Syntax: forge.exe sim <deck1[.dck]> <deck2[.dck]> [N]");
            System.out.println("\tsim - stands for simulation mode");
            System.out.println("\tdeck1 (or deck2) - constructed deck name or filename (has to be quoted when contains multiple words)");
            System.out.println("\tdeck is treated as file if it ends with a dot followed by three numbers or letters");
            System.out.println("\tN - number of games, defaults to 1");
            return;
        }
        Deck d1 = deckFromCommandLineParameter(args[1]);
        Deck d2 = deckFromCommandLineParameter(args[2]);
        if(d1 == null || d2 == null) {
            System.out.println("One of decks could not be loaded, match cannot start");
            return;
        }
        
        int nGames = args.length >= 4 ? Integer.parseInt(args[3]) : 1;
        
        System.out.println(String.format("Ai-%s vs Ai_%s - %s", d1.getName(), d2.getName(), Lang.nounWithNumeral(nGames, "game")));
        
        List<RegisteredPlayer> pp = new ArrayList<RegisteredPlayer>();
        pp.add(new RegisteredPlayer(d1).setPlayer(getLobby().getAiPlayer("Ai-" + d1.getName())));
        pp.add(new RegisteredPlayer(d2).setPlayer(getLobby().getAiPlayer("Ai_" + d2.getName())));
        GameRules rules = new GameRules(GameType.Constructed);
        Match mc = new Match(rules, pp);
        for(int iGame = 0; iGame < nGames; iGame++)
            simulateSingleMatch(mc, iGame);
        System.out.flush();
    }
    /**
     * TODO: Write javadoc for this method.
     * @param sw
     * @param pp
     */
    private static void simulateSingleMatch(Match mc, int iGame) {
        StopWatch sw = new StopWatch();
        sw.start();

        Game g1 = mc.createGame();
        // will run match in the same thread
        mc.startGame(g1);
        sw.stop();
        
        List<GameLogEntry> log = g1.getGameLog().getLogEntries(null);
        Collections.reverse(log);
        
        for(GameLogEntry l : log)
            System.out.println(l);

        System.out.println(String.format("\nGame %d ended in %d ms. %s has won!\n", 1+iGame, sw.getTime(), g1.getOutcome().getWinningLobbyPlayer().getName()));
    }


    private static Deck deckFromCommandLineParameter(String deckname) {
        int dotpos = deckname.lastIndexOf('.');
        if(dotpos > 0 && dotpos == deckname.length()-4)
            return DeckSerializer.fromFile(new File(deckname));
        return FModel.getDecks().getConstructed().get(deckname);
    }
    /**
     * TODO: Write javadoc for this method.
     * @return
     */
    public static boolean isInteractiveMode() {
        return interactiveMode;
    }
}
