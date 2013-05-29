package forge.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Lists;

import forge.Card;
import forge.Singletons;
import forge.game.event.GameEventAnteCardsSelected;
import forge.game.event.GameEventFlipCoin;
import forge.game.player.LobbyPlayer;
import forge.game.player.Player;
import forge.properties.ForgePreferences.FPref;
import forge.util.Aggregates;

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

    private Game currentGame = null;

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

    public void addGamePlayed(GameOutcome outcome) {
        if (!currentGame.isGameOver()) {
            throw new IllegalStateException("Game is not over yet.");
        }
        gamesPlayed.add(outcome);
    }
    

    /**
     * TODO: Write javadoc for this method.
     */
    public void startRound() {

        currentGame = new Game(players, gameType, this);
        
        Singletons.getControl().attachToGame(currentGame);

        final boolean canRandomFoil = Singletons.getModel().getPreferences().getPrefBoolean(FPref.UI_RANDOM_FOIL) && gameType == GameType.Constructed;
        GameNew.newGame(currentGame, canRandomFoil, this.useAnte);

        if (useAnte) {  // Deciding which cards go to ante
            List<Pair<Player, Card>> list = GameNew.chooseCardsForAnte(currentGame);
            GameNew.moveCardsToAnte(list);
            currentGame.fireEvent(new GameEventAnteCardsSelected(list));
        }

        // This code was run from EDT.
        currentGame.getAction().invoke(new Runnable() {
            @Override
            public void run() {
                final Player firstPlayer = determineFirstTurnPlayer(getLastGameOutcome(), currentGame);
                currentGame.getAction().startGame(firstPlayer);
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

    /**
     * TODO: Write javadoc for this method.
     * 
     * @return
     */
    public GameOutcome getLastGameOutcome() {
        return gamesPlayed.isEmpty() ? null : gamesPlayed.get(gamesPlayed.size() - 1);
    }
    
    public Iterable<GameOutcome> getOutcomes() {
        return gamesPlayedRo;
    }

    public Game getCurrentGame() {
        return currentGame;
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
    

    private Player determineFirstTurnPlayer(final GameOutcome lastGameOutcome, final Game game) {
        // Only cut/coin toss if it's the first game of the match
        Player goesFirst = null;

        boolean isFirstGame = lastGameOutcome == null;
        if (isFirstGame) {
            game.fireEvent(new GameEventFlipCoin()); // Play the Flip Coin sound
            goesFirst = Aggregates.random(game.getPlayers());
        } else {
            for(Player p : game.getPlayers()) {
                if(!lastGameOutcome.isWinner(p.getLobbyPlayer())) { 
                    goesFirst = p;
                    break;
                }
            }
        }

        boolean willPlay = goesFirst.getController().getWillPlayOnFirstTurn(isFirstGame);
        goesFirst = willPlay ? goesFirst : goesFirst.getOpponent();
        return goesFirst;
    }
}
