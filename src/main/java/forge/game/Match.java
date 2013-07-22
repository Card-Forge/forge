package forge.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Lists;
import forge.Card;
import forge.Singletons;
import forge.game.event.GameEventAnteCardsSelected;
import forge.game.player.LobbyPlayer;
import forge.game.player.Player;
import forge.properties.ForgePreferences.FPref;

/**
 * TODO: Write javadoc for this type.
 * 
 */

public class Match {

    private final List<RegisteredPlayer> players;
    private final GameType gameType;

    private int gamesPerMatch = 3;
    private int gamesToWinMatch = 2;
    
    private boolean useAnte = Singletons.getModel().getPreferences().getPrefBoolean(FPref.UI_ANTE);

    private final List<GameOutcome> gamesPlayed = new ArrayList<GameOutcome>();
    private final List<GameOutcome> gamesPlayedRo;

    /**
     * This should become constructor once.
     */
    public Match(GameType type, List<RegisteredPlayer> players0) {
        gamesPlayedRo = Collections.unmodifiableList(gamesPlayed);
        players = Collections.unmodifiableList(Lists.newArrayList(players0));
        gameType = type;
    }
    
    public Match(GameType type, List<RegisteredPlayer> players0, Boolean overrideAnte) {
        this(type, players0);
        if( overrideAnte != null )
            this.useAnte = overrideAnte.booleanValue();
    }

    /**
     * Gets the games played.
     * 
     * @return the games played
     */
    public final List<GameOutcome> getPlayedGames() {
        return this.gamesPlayedRo;
    }

    /** @return int */
    public int getGamesPerMatch() {
        return gamesPerMatch;
    }

    /** @return int */
    public int getGamesToWinMatch() {
        return gamesToWinMatch;
    }

    public void addGamePlayed(Game finished) {
        if (!finished.isGameOver()) {
            throw new IllegalStateException("Game is not over yet.");
        }
        gamesPlayed.add(finished.getOutcome());
    }
    

    /**
     * TODO: Write javadoc for this method.
     */
    public Game createGame() {
        return new Game(players, gameType, this);
    }

    /**
     * TODO: Write javadoc for this method.
     */
    public void startGame(final Game game, final CountDownLatch latch) {
        final boolean canRandomFoil = Singletons.getModel().getPreferences().getPrefBoolean(FPref.UI_RANDOM_FOIL) && gameType == GameType.Constructed;
        GameNew.newGame(game, canRandomFoil, this.useAnte);

        // This code could be run run from EDT.
        game.getAction().invoke(new Runnable() {
            @Override
            public void run() {
                if (useAnte) {  // Deciding which cards go to ante
                    List<Pair<Player, Card>> list = GameNew.chooseCardsForAnte(game);
                    GameNew.moveCardsToAnte(list);
                    game.fireEvent(new GameEventAnteCardsSelected(list));
                }
                
                GameOutcome lastOutcome = gamesPlayed.isEmpty() ? null : gamesPlayed.get(gamesPlayed.size() - 1);
                game.getAction().startGame(lastOutcome);
                
                if( null != latch )
                    latch.countDown();
            }
        });
    }

    public void clearGamesPlayed() {
        gamesPlayed.clear();
    }
    
    public void clearLastGame() {
        gamesPlayed.remove(gamesPlayed.size() - 1);
    }

    /**
     * TODO: Write javadoc for this method.
     * 
     * @return
     */
    public GameType getGameType() {
        return gameType;
    }

    public Iterable<GameOutcome> getOutcomes() {
        return gamesPlayedRo;
    }

    /**
     * TODO: Write javadoc for this method.
     * 
     * @return
     */
    public boolean isMatchOver() {
        int[] victories = new int[players.size()];
        for (GameOutcome go : gamesPlayed) {
            LobbyPlayer winner = go.getWinner();
            int i = 0;
            for (RegisteredPlayer p : players) {
                if (p.getPlayer().equals(winner)) {
                    victories[i]++;
                    break; // can't have 2 winners per game
                }
                i++;
            }
        }

        for (int score : victories) {
            if (score >= gamesToWinMatch) {
                return true;
            }
        }
        return gamesPlayed.size() >= gamesPerMatch;
    }

    /**
     * TODO: Write javadoc for this method.
     * 
     * @param questPlayer
     * @return
     */
    public int getGamesWonBy(LobbyPlayer questPlayer) {
        int sum = 0;
        for (GameOutcome go : gamesPlayed) {
            if (questPlayer.equals(go.getWinner())) {
                sum++;
            }
        }
        return sum;
    }

    /**
     * TODO: Write javadoc for this method.
     * 
     * @param questPlayer
     * @return
     */
    public boolean isWonBy(LobbyPlayer questPlayer) {
        return getGamesWonBy(questPlayer) >= gamesToWinMatch;
    }

    public List<RegisteredPlayer> getPlayers() {
        return players;
    }

    /**
     * TODO: Write javadoc for this method.
     * @return
     */
    public static int getPoisonCountersAmountToLose() {
        return 10;
    }
    
}
