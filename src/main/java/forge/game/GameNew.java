package forge.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import javax.swing.JOptionPane;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.Card;
import forge.CardLists;
import forge.CardPredicates;
import forge.CardUtil;
import forge.Singletons;
import forge.card.trigger.TriggerHandler;
import forge.card.trigger.TriggerType;
import forge.deck.Deck;
import forge.deck.CardPool;
import forge.deck.DeckSection;
import forge.game.event.FlipCoinEvent;
import forge.game.phase.PhaseHandler;
import forge.game.player.AIPlayer;
import forge.game.player.LobbyPlayer;
import forge.game.player.Player;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.gui.match.views.VAntes;
import forge.item.CardPrinted;
import forge.item.IPaperCard;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.util.Aggregates;
import forge.util.MyRandom;

/** 
 * Methods for all things related to starting a new game.
 * All of these methods can and should be static.
 */
public class GameNew {
    
    public static final ForgePreferences preferences = Singletons.getModel().getPreferences();

    private static void preparePlayerLibrary(Player player, final ZoneType zoneType, CardPool secion, boolean canRandomFoil, Random generator) {
        PlayerZone library = player.getZone(zoneType);
        for (final Entry<CardPrinted, Integer> stackOfCards : secion) {
            final CardPrinted cardPrinted = stackOfCards.getKey();
            for (int i = 0; i < stackOfCards.getValue(); i++) {

                final Card card = cardPrinted.toForgeCard(player);

                // apply random pictures for cards
                if (preferences.getPrefBoolean(FPref.UI_RANDOM_CARD_ART)) {
                    final int cntVariants = cardPrinted.getRules().getEditionInfo(cardPrinted.getEdition()).getCopiesCount();
                    if (cntVariants > 1) {
                        card.setRandomPicture(generator.nextInt(cntVariants - 1) + 1);
                        card.setImageFilename(CardUtil.buildFilename(card));
                    }
                }
                
                // Assign random foiling on approximately 1:20 cards
                if (cardPrinted.isFoil() || (canRandomFoil && MyRandom.percentTrue(5))) {
                    final int iFoil = generator.nextInt(9) + 1;
                    card.setFoil(iFoil);
                }

                library.add(card);
            }
        }
    }

    /**
     * Constructor for new game allowing card lists to be put into play
     * immediately, and life totals to be adjusted, for computer and human.
     * 
     * TODO: Accept something like match state as parameter. Match should be aware of players,
     * their decks and other special starting conditions.
     */
    public static void newGame(final MatchController match, final Map<Player, PlayerStartConditions> playersConditions, final GameState game, final boolean canRandomFoil) {

        Card.resetUniqueNumber();
        // need this code here, otherwise observables fail
        forge.card.trigger.Trigger.resetIDs();
        TriggerHandler trigHandler = game.getTriggerHandler();
        trigHandler.clearTriggerSettings();
        trigHandler.clearDelayedTrigger();

        // friendliness
        boolean useAnte = preferences.getPrefBoolean(FPref.UI_ANTE);
        final Set<CardPrinted> rAICards = new HashSet<CardPrinted>();

        Map<Player, Set<CardPrinted>> removedAnteCards = new HashMap<Player, Set<CardPrinted>>();
        
        for (Entry<Player, PlayerStartConditions> p : playersConditions.entrySet()) {
            final Player player = p.getKey();
            final PlayerStartConditions psc = p.getValue();
            player.setStartingLife(psc.getStartingLife());
            player.setMaxHandSize(psc.getStartingHand());
            player.setStartingHandSize(psc.getStartingHand());
            putCardsOnBattlefield(player, psc.getCardsOnBattlefield(player));

            initVariantsZones(player, psc);

            GameType gameType = match.getGameType();
            boolean isFirstGame = match.getPlayedGames().isEmpty();
            boolean hasSideboard = psc.getOriginalDeck().has(DeckSection.Sideboard);
            boolean canSideBoard = !isFirstGame && gameType.isSideboardingAllowed() && hasSideboard;
         
            if (canSideBoard) {
                psc.setCurrentDeck(player.getController().sideboard(psc.getCurrentDeck(), gameType));
            } else { 
                psc.restoreOriginalDeck();
            }
            Deck myDeck = psc.getCurrentDeck();

            Set<CardPrinted> myRemovedAnteCards = useAnte ? null : getRemovedAnteCards(myDeck);
            Random generator = MyRandom.getRandom();

            preparePlayerLibrary(player, ZoneType.Library, myDeck.getMain(), canRandomFoil, generator);
            if(hasSideboard)
                preparePlayerLibrary(player, ZoneType.Sideboard, myDeck.get(DeckSection.Sideboard), canRandomFoil, generator);
            
            // Shuffling
            if (player instanceof AIPlayer && preferences.getPrefBoolean(FPref.UI_SMOOTH_LAND)) {
                // AI may do this instead of shuffling its deck
                final Iterable<Card> c1 = GameNew.smoothComputerManaCurve(player.getCardsIn(ZoneType.Library));
                player.getZone(ZoneType.Library).setCards(c1);
            } else {
                player.shuffle();
            }
            
            if(player instanceof AIPlayer) {
                rAICards.addAll(getCardsAiCantPlayWell(myDeck));
            }

            player.updateObservers();
            player.getZone(ZoneType.Battlefield).updateObservers();
            player.getZone(ZoneType.Hand).updateObservers();
            player.getZone(ZoneType.Command).updateObservers();
            player.getZone(ZoneType.Battlefield).updateObservers();
            
            if( myRemovedAnteCards != null && !myRemovedAnteCards.isEmpty() )
                removedAnteCards.put(player, myRemovedAnteCards);
        }

        if (rAICards.size() > 0) {
            String message = buildFourColumnList("AI deck contains the following cards that it can't play or may be buggy:", rAICards);
            if (GameType.Quest == game.getType() || GameType.Sealed == game.getType() || GameType.Draft == game.getType()) {
                // log, but do not visually warn.  quest decks are supposedly already vetted by the quest creator,
                // sealed and draft decks do not get any AI-unplayable picks but may contain several
                // received/picked but unplayable cards in the sideboard.
                System.err.println(message);
            } else {
                JOptionPane.showMessageDialog(null, message, "", JOptionPane.INFORMATION_MESSAGE);
            }
        }

        if (!removedAnteCards.isEmpty()) {
            StringBuilder ante = new StringBuilder("The following ante cards were removed:\n\n");
            for (Entry<Player, Set<CardPrinted>> ants : removedAnteCards.entrySet()) {
                ante.append(buildFourColumnList("From the " + ants.getKey().getName() + "'s deck:", ants.getValue()));
            }
            JOptionPane.showMessageDialog(null, ante.toString(), "", JOptionPane.INFORMATION_MESSAGE);
        }

        // Deciding which cards go to ante
        if (preferences.getPrefBoolean(FPref.UI_ANTE)) {
            final String nl = System.getProperty("line.separator");
            final StringBuilder msg = new StringBuilder();
            for (final Player p : game.getPlayers()) {

                final List<Card> lib = p.getCardsIn(ZoneType.Library);
                Predicate<Card> goodForAnte = Predicates.not(CardPredicates.Presets.BASIC_LANDS);
                Card ante = Aggregates.random(Iterables.filter(lib, goodForAnte));
                if (ante == null) {
                    throw new RuntimeException(p + " library is empty.");
                }
                game.getGameLog().add("Ante", p + " anted " + ante, 0);
                VAntes.SINGLETON_INSTANCE.addAnteCard(p, ante);
                game.getAction().moveTo(ZoneType.Ante, ante);
                msg.append(p.getName()).append(" ante: ").append(ante).append(nl);
            }
            JOptionPane.showMessageDialog(null, msg, "Ante", JOptionPane.INFORMATION_MESSAGE);
        }

        determineFirstTurnPlayer(match.getLastGameOutcome(), game);
        
        // Draw <handsize> cards
        for (final Player p1 : game.getPlayers()) {
            p1.drawCards(p1.getMaxHandSize());
        }
    }

    // ultimate of Karn the Liberated
    public static void restartGame(final MatchController match, final GameState game, final Player startingTurn, Map<Player, List<Card>> playerLibraries) {
    
        Map<LobbyPlayer, PlayerStartConditions> players = match.getPlayers();
        Map<Player, PlayerStartConditions> playersConditions = new HashMap<Player, PlayerStartConditions>();
    
        for (Player p : game.getPlayers()) {
            playersConditions.put(p, players.get(p.getLobbyPlayer()));
        }
    
        game.setMulliganned(false);
        match.getInput().clearInput();
    
        //Card.resetUniqueNumber();
        // need this code here, otherwise observables fail
        forge.card.trigger.Trigger.resetIDs();
        TriggerHandler trigHandler = game.getTriggerHandler();
        trigHandler.clearTriggerSettings();
        trigHandler.clearDelayedTrigger();
        trigHandler.cleanUpTemporaryTriggers();
        trigHandler.suppressMode(TriggerType.ChangesZone);
    
        game.getStack().reset();
        GameAction action = game.getAction();
    
    
        for (Entry<Player, PlayerStartConditions> p : playersConditions.entrySet()) {
            final Player player = p.getKey();
            player.setStartingLife(p.getValue().getStartingLife());
            player.setNumLandsPlayed(0);
            putCardsOnBattlefield(player, p.getValue().getCardsOnBattlefield(player));
    
            PlayerZone library = player.getZone(ZoneType.Library);
            List<Card> newLibrary = playerLibraries.get(player);
            for (Card c : newLibrary) {
                action.moveTo(library, c);
            }
    
            player.shuffle();
            player.getZone(ZoneType.Battlefield).updateObservers();
            player.updateObservers();
            player.getZone(ZoneType.Hand).updateObservers();
        }
    
        trigHandler.clearSuppression(TriggerType.ChangesZone);
    
        PhaseHandler phaseHandler = game.getPhaseHandler();
        phaseHandler.setPlayerTurn(startingTurn);
    
        // Draw <handsize> cards
        for (final Player p : game.getPlayers()) {
            p.drawCards(p.getMaxHandSize());
        }
    }

    /**
     * TODO: Write javadoc for this method.
     * @param match
     * @param game
     */
    private static void determineFirstTurnPlayer(final GameOutcome lastGameOutcome, final GameState game) {
        // Only cut/coin toss if it's the first game of the match
        Player goesFirst;
        Player humanPlayer = Singletons.getControl().getPlayer();
        boolean isFirstGame = lastGameOutcome == null;
        if (isFirstGame) {
            goesFirst = GameNew.seeWhoPlaysFirstDice(game);
        } else {
            
            goesFirst = lastGameOutcome.isWinner(humanPlayer.getLobbyPlayer()) ? humanPlayer.getOpponent() : humanPlayer;
        }
        String message = goesFirst + ( isFirstGame ? " has won the coin toss." : " lost the last game.");
        boolean willPlay = goesFirst.getController().getWillPlayOnFirstTurn(message);
        if ( goesFirst != humanPlayer ) {
            JOptionPane.showMessageDialog(null, message + "\nComputer Going First", "You are drawing", JOptionPane.INFORMATION_MESSAGE);
        }
        goesFirst = willPlay ? goesFirst : goesFirst.getOpponent();
        game.getPhaseHandler().setPlayerTurn(goesFirst);
    }

    private static void initVariantsZones(final Player player, final PlayerStartConditions psc) {
        PlayerZone com = player.getZone(ZoneType.Command);

        // Mainly for avatar, but might find something else here
        for (final IPaperCard c : psc.getCardsInCommand(player)) {
            com.add(c.toForgeCard(player), false);
        }

        // Schemes
        List<Card> sd = new ArrayList<Card>();
        for(IPaperCard cp : psc.getSchemes(player)) sd.add(cp.toForgeCard(player));
        if ( !sd.isEmpty()) player.setSchemeDeck(sd);

        // Planes
        List<Card> l = new ArrayList<Card>();
        for(IPaperCard cp : psc.getPlanes(player)) l.add(cp.toForgeCard(player));
        if ( !l.isEmpty() ) player.setPlanarDeck(l);
    }

    private static List<CardPrinted> getCardsAiCantPlayWell(final Deck toUse) {
        List<CardPrinted> result = new ArrayList<CardPrinted>();
        
        for ( Entry<DeckSection, CardPool> ds : toUse ) {
            for (Entry<CardPrinted, Integer> cp : ds.getValue()) {
                if ( cp.getKey().getRules().getAiHints().getRemAIDecks() ) 
                    result.add(cp.getKey());
            }
        }
        return result;
    }

    private static Set<CardPrinted> getRemovedAnteCards(Deck toUse) {
        final String keywordToRemove = "Remove CARDNAME from your deck before playing if you're not playing for ante.";
        Set<CardPrinted> myRemovedAnteCards = new HashSet<CardPrinted>();
        for ( Entry<DeckSection, CardPool> ds : toUse ) {
            for (Entry<CardPrinted, Integer> cp : ds.getValue()) {
                if ( Iterables.contains(cp.getKey().getRules().getMainPart().getKeywords(), keywordToRemove) ) 
                    myRemovedAnteCards.add(cp.getKey());
            }
        }

        for(CardPrinted cp: myRemovedAnteCards) {
            for ( Entry<DeckSection, CardPool> ds : toUse ) {
                ds.getValue().remove(cp, Integer.MAX_VALUE);
            }
        }
        return myRemovedAnteCards;
    }
    
    private static void putCardsOnBattlefield(Player player, Iterable<? extends IPaperCard> cards) {
        PlayerZone bf = player.getZone(ZoneType.Battlefield);
        if (cards != null) {
            for (final IPaperCard cp : cards) {
                Card c = cp.toForgeCard(player);
                c.setOwner(player);
                c.addController(player);
                bf.add(c, false);
                c.setSickness(true);
                c.setStartsGameInPlay(true);
                c.refreshUniqueNumber();
            }
        }
    
    }

    private static String buildFourColumnList(String firstLine, Iterable<CardPrinted> cAnteRemoved) {
        StringBuilder sb = new StringBuilder(firstLine);
        int i = 0;
        for(CardPrinted cp: cAnteRemoved) {
            if ( i != 0 ) sb.append(", ");
            if ( i % 4 == 0 ) sb.append("\n");
            sb.append(cp);
            i++;
        }
        return sb.toString();
    }

 // this is where the computer cheats
    // changes AllZone.getComputerPlayer().getZone(Zone.Library)

    /**
     * <p>
     * smoothComputerManaCurve.
     * </p>
     * 
     * @param in
     *            an array of {@link forge.Card} objects.
     * @return an array of {@link forge.Card} objects.
     */
    private static Iterable<Card> smoothComputerManaCurve(final Iterable<Card> in) {
        final List<Card> library = Lists.newArrayList(in);
        CardLists.shuffle(library);

        // remove all land, keep non-basicland in there, shuffled
        List<Card> land = CardLists.filter(library, CardPredicates.Presets.LANDS);
        for (Card c : land) {
            if (c.isLand()) {
                library.remove(c);
            }
        }

        try {
            // mana weave, total of 7 land
            // The Following have all been reduced by 1, to account for the
            // computer starting first.
            library.add(5, land.get(0));
            library.add(6, land.get(1));
            library.add(8, land.get(2));
            library.add(9, land.get(3));
            library.add(10, land.get(4));

            library.add(12, land.get(5));
            library.add(15, land.get(6));
        } catch (final IndexOutOfBoundsException e) {
            System.err.println("Error: cannot smooth mana curve, not enough land");
            return in;
        }

        // add the rest of land to the end of the deck
        for (int i = 0; i < land.size(); i++) {
            if (!library.contains(land.get(i))) {
                library.add(land.get(i));
            }
        }

        // check
        for (int i = 0; i < library.size(); i++) {
            System.out.println(library.get(i));
        }

        return library;
    } // smoothComputerManaCurve()

    // decides who goes first when starting another game, used by newGame()
    /**
     * <p>
     * seeWhoPlaysFirstCoinToss.
     * </p>
     * @return 
     */
    private static Player seeWhoPlaysFirstDice(final GameState game) {
        // Play the Flip Coin sound
        game.getEvents().post(new FlipCoinEvent());

        List<Player> allPlayers = game.getPlayers();
        return allPlayers.get(MyRandom.getRandom().nextInt(allPlayers.size()));
    }

}
