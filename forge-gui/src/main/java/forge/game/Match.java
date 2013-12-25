package forge.game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import forge.Singletons;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.game.card.Card;
import forge.game.event.GameEventAnteCardsSelected;
import forge.game.player.LobbyPlayer;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.trigger.Trigger;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.properties.ForgePreferences.FPref;
import forge.util.MyRandom;

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
    public Match(GameType type, List<RegisteredPlayer> players0, int games) {
        gamesPlayedRo = Collections.unmodifiableList(gamesPlayed);
        players = Collections.unmodifiableList(Lists.newArrayList(players0));
        gameType = type;
        
        gamesPerMatch = games;
        gamesToWinMatch = (int)Math.ceil((gamesPerMatch+1)/2);
    }
    
    public Match(GameType type, List<RegisteredPlayer> players0) {
    	this(type,players0,3);
    }
    
    public Match(GameType type, List<RegisteredPlayer> players0, Boolean overrideAnte) {
        this(type, players0);
        if( overrideAnte != null )
            this.useAnte = overrideAnte.booleanValue();
    }
    
    public Match(GameType type, List<RegisteredPlayer> players0, Boolean overrideAnte, int games) {
    	this(type, players0, games);
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
        Game game = new Game(players, gameType, this);
        return game;
    }

    /**
     * TODO: Write javadoc for this method.
     */
    public void startGame(final Game game, final CountDownLatch latch) {
        final boolean canRandomFoil = Singletons.getModel().getPreferences().getPrefBoolean(FPref.UI_RANDOM_FOIL) && gameType == GameType.Constructed;
        

        // This code could be run run from EDT.
        game.getAction().invoke(new Runnable() {
            @Override
            public void run() {
                newGame(game, canRandomFoil);
                
                if (useAnte) {  // Deciding which cards go to ante
                    Multimap<Player, Card> list = game.chooseCardsForAnte();
                    for(Entry<Player, Card> kv : list.entries()) {
                        Player p = kv.getKey();
                        p.getGame().getAction().moveTo(ZoneType.Ante, kv.getValue());
                        p.getGame().getGameLog().add(GameLogEntryType.ANTE, p + " anted " + kv.getValue());
                    }
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
            LobbyPlayer winner = go.getWinningLobbyPlayer();
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
            if (questPlayer.equals(go.getWinningLobbyPlayer())) {
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
    
    private static Set<PaperCard> getRemovedAnteCards(Deck toUse) {
        final String keywordToRemove = "Remove CARDNAME from your deck before playing if you're not playing for ante.";
        Set<PaperCard> myRemovedAnteCards = new HashSet<PaperCard>();
        for (Entry<DeckSection, CardPool> ds : toUse) {
            for (Entry<PaperCard, Integer> cp : ds.getValue()) {
                if ( Iterables.contains(cp.getKey().getRules().getMainPart().getKeywords(), keywordToRemove) ) 
                    myRemovedAnteCards.add(cp.getKey());
            }
        }

        return myRemovedAnteCards;
    }

    private static void preparePlayerLibrary(Player player, final ZoneType zoneType, CardPool section, boolean canRandomFoil, Random generator) {
        PlayerZone library = player.getZone(zoneType);
        List<Card> newLibrary = new ArrayList<Card>();
        for (final Entry<PaperCard, Integer> stackOfCards : section) {
            final PaperCard cp = stackOfCards.getKey();
            for (int i = 0; i < stackOfCards.getValue(); i++) {
                final Card card = Card.fromPaperCard(cp, player);
                // Assign card-specific foiling or random foiling on approximately 1:20 cards if enabled
                if (cp.isFoil() || (canRandomFoil && MyRandom.percentTrue(5))) {
                    card.setRandomFoil();
                }
                newLibrary.add(card);
            }
        }
        library.setCards(newLibrary);
    }

    /**
     * Constructor for new game allowing card lists to be put into play
     * immediately, and life totals to be adjusted, for computer and human.
     * 
     * TODO: Accept something like match state as parameter. Match should be aware of players,
     * their decks and other special starting conditions.
     * @param forceAnte Forces ante on or off no matter what your preferences
     */
    private void newGame(final Game game, final boolean canRandomFoil) {
        // need this code here, otherwise observables fail
        Trigger.resetIDs();
        game.getTriggerHandler().clearDelayedTrigger();

        // friendliness
        Multimap<Player, PaperCard> rAICards = HashMultimap.create();
        Multimap<Player, PaperCard> removedAnteCards = ArrayListMultimap.create();

        GameType gameType = game.getType();
        boolean isFirstGame = game.getMatch().getPlayedGames().isEmpty();
        boolean canSideBoard = !isFirstGame && gameType.isSideboardingAllowed();
        
        final List<RegisteredPlayer> playersConditions = game.getMatch().getPlayers();
        for (int i = 0; i < playersConditions.size(); i++) {
            Player player = game.getPlayers().get(i);
            final RegisteredPlayer psc = playersConditions.get(i);

            player.initVariantsZones(psc);

            if (canSideBoard) {
                Deck sideboarded = player.getController().sideboard(psc.getCurrentDeck(), gameType);
                psc.setCurrentDeck(sideboarded);
            } else { 
                psc.restoreOriginalDeck();
            }
            Deck myDeck = psc.getCurrentDeck();
            boolean hasSideboard = myDeck.has(DeckSection.Sideboard);

            Set<PaperCard> myRemovedAnteCards = null;
            if( useAnte ) {
                myRemovedAnteCards = getRemovedAnteCards(myDeck);
                for(PaperCard cp: myRemovedAnteCards) {
                    for ( Entry<DeckSection, CardPool> ds : myDeck ) {
                        ds.getValue().removeAll(cp);
                    }
                }
            }
            
            
            Random generator = MyRandom.getRandom();

            preparePlayerLibrary(player, ZoneType.Library, myDeck.getMain(), canRandomFoil, generator);
            if(hasSideboard)
                preparePlayerLibrary(player, ZoneType.Sideboard, myDeck.get(DeckSection.Sideboard), canRandomFoil, generator);
            
            player.shuffle(null);

            if(isFirstGame) {
                Collection<? extends PaperCard> cardsComplained = player.getController().complainCardsCantPlayWell(myDeck); 
                if( null != cardsComplained )
                    rAICards.putAll(player, cardsComplained);
            }

            if( myRemovedAnteCards != null && !myRemovedAnteCards.isEmpty() )
                removedAnteCards.putAll(player, myRemovedAnteCards);
        }

        if (!rAICards.isEmpty() || GameType.Quest == game.getType() || GameType.Sealed == game.getType() || GameType.Draft == game.getType() ) {
            game.getAction().revealAnte("These cards from AIs' decks cannot be played or may be buggy", rAICards);
        }

        if (!removedAnteCards.isEmpty()) {
            game.getAction().revealAnte("These ante cards were removed:", removedAnteCards);
        }
    }
}
