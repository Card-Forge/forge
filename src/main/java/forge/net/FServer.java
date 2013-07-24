package forge.net;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.lang.time.StopWatch;

import forge.GameLogEntry;
import forge.Singletons;
import forge.deck.Deck;
import forge.game.Game;
import forge.game.GameType;
import forge.game.Match;
import forge.game.RegisteredPlayer;
import forge.util.Lang;


/** 
 * TODO: Write javadoc for this type.
 *
 */
public enum FServer {
    instance();
    
    private boolean interactiveMode = true;
    private Lobby lobby = null;
    
    public Lobby getLobby() {
        if (lobby == null) {
            lobby = new Lobby();
        }
        return lobby;
    }
    
    /**
     * TODO: Write javadoc for this method.
     * @return
     */
    private final NetServer server = new NetServer();
    public NetServer getServer() {
        // TODO Auto-generated method stub
        return server;
    }

    /**
     * TODO: Write javadoc for this method.
     * @param args
     */
    public void simulateMatches(String[] args) {
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
        pp.add(RegisteredPlayer.fromDeck(d1).setPlayer(FServer.instance.getLobby().getAiPlayer("Ai-" + d1.getName())));
        pp.add(RegisteredPlayer.fromDeck(d2).setPlayer(FServer.instance.getLobby().getAiPlayer("Ai_" + d2.getName())));
        
        Match mc = new Match(GameType.Constructed, pp);
        for(int iGame = 0; iGame < nGames; iGame++)
            simulateSingleMatch(mc, iGame);
        System.out.flush();
    }
    /**
     * TODO: Write javadoc for this method.
     * @param sw
     * @param pp
     */
    private void simulateSingleMatch(Match mc, int iGame) {
        StopWatch sw = new StopWatch();
        sw.start();

        CountDownLatch cdl = new CountDownLatch(1);
        
        Game g1 = mc.createGame();
        mc.startGame(g1, cdl);
        try {
            cdl.await(); // wait until game ends (in other thread)
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block ignores the exception, but sends it to System.err and probably forge.log.
            e.printStackTrace();
        }
        sw.stop();
        
        List<GameLogEntry> log = g1.getGameLog().getLogEntries(null);
        Collections.reverse(log);
        
        for(GameLogEntry l : log)
            System.out.println(l);

        System.out.println(String.format("\nGame %d ended in %d ms. %s has won!\n", 1+iGame, sw.getTime(), g1.getOutcome().getWinner().getName()));
    }


    private Deck deckFromCommandLineParameter(String deckname) {
        int dotpos = deckname.lastIndexOf('.');
        if(dotpos > 0 && dotpos == deckname.length()-4)
            return Deck.fromFile(new File(deckname));
        return Singletons.getModel().getDecks().getConstructed().get(deckname);
    }
    /**
     * TODO: Write javadoc for this method.
     * @return
     */
    public boolean isInteractiveMode() {
        return interactiveMode;
    }

    
}
