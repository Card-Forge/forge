package forge.net;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.lang.time.StopWatch;

import forge.GameLogEntry;
import forge.GameLogEntryType;
import forge.Singletons;
import forge.deck.Deck;
import forge.deck.io.DeckSerializer;
import forge.game.Game;
import forge.game.GameType;
import forge.game.Match;
import forge.game.RegisteredPlayer;


/** 
 * TODO: Write javadoc for this type.
 *
 */
public enum FServer {
    instance();
    
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
    public void startFromCommandLine(String[] args) {
        String mode = args[0].toLowerCase();
        
        switch(mode) {
            case "sim":

                
            case "server":
                System.out.println("Dedicated server mode.\nNot implemented.");
                break;
            
            default:
                System.out.println("Unknown mode.\nKnown mode is 'sim' ");
                break;
        }
    }
    /**
     * TODO: Write javadoc for this method.
     * @param args
     */
    public void simulateMatches(String[] args) {
        System.out.println("Simulation mode");
        if(args.length < 3 ) {
            System.out.println("Syntax: forge.exe sim deck1.dck deck2.dck");
            return;
        }
        Deck d1 = deckFromCommandLineParameter(args[1]);
        Deck d2 = deckFromCommandLineParameter(args[2]);
        if(d1 == null || d2 == null) {
            System.out.println("One of decks could not be loaded, match cannot start");
            return;
        }
        
        System.out.println(String.format("%s vs %s", d1.getName(), d2.getName()));
        
        StopWatch sw = new StopWatch();
        sw.start();
        
        List<RegisteredPlayer> pp = new ArrayList<RegisteredPlayer>();
        pp.add(RegisteredPlayer.fromDeck(d1).setPlayer(FServer.instance.getLobby().getAiPlayer("Ai-" + d1.getName())));
        pp.add(RegisteredPlayer.fromDeck(d2).setPlayer(FServer.instance.getLobby().getAiPlayer("Ai_" + d2.getName())));
        Match mc = new Match(GameType.Constructed, pp);
        
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

        System.out.println(String.format("\nGame ended in %d ms", sw.getTime() ));
        System.out.println(g1.getOutcome().getWinner().getName() + " has won!");
        
        System.out.flush();
    }


    private Deck deckFromCommandLineParameter(String deckname) {
        if(deckname.endsWith(DeckSerializer.FILE_EXTENSION))
            return Deck.fromFile(new File(deckname));
        return Singletons.getModel().getDecks().getConstructed().get(deckname);
    }

    
}
