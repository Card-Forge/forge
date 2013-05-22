package forge.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.Card;
import forge.CardLists;
import forge.CardPredicates;
import forge.card.trigger.Trigger;
import forge.card.trigger.TriggerHandler;
import forge.card.trigger.TriggerType;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.game.phase.PhaseHandler;
import forge.game.player.LobbyPlayer;
import forge.game.player.Player;
import forge.game.player.PlayerType;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.gui.GuiDialog;
import forge.item.CardDb;
import forge.item.CardPrinted;
import forge.item.IPaperCard;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.util.Aggregates;
import forge.util.MyRandom;
import forge.util.TextUtil;

/** 
 * Methods for all things related to starting a new game.
 * All of these methods can and should be static.
 */
public class GameNew {
    
    public static final ForgePreferences preferences = forge.Singletons.getModel().getPreferences();

    private static void putCardsOnBattlefield(Player player, Iterable<? extends IPaperCard> cards) {
        PlayerZone bf = player.getZone(ZoneType.Battlefield);
        if (cards != null) {
            for (final IPaperCard cp : cards) {
                Card c = cp.toForgeCard(player);
                c.setOwner(player);
                bf.add(c, false);
                c.setSickness(true);
                c.setStartsGameInPlay(true);
                c.refreshUniqueNumber();
            }
        }
    
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
        if ( !sd.isEmpty()) {
            for(Card c : sd) {
                player.getZone(ZoneType.SchemeDeck).add(c);
            }
            player.getZone(ZoneType.SchemeDeck).shuffle();
        }
        
    
        // Planes
        List<Card> l = new ArrayList<Card>();
        for(IPaperCard cp : psc.getPlanes(player)) l.add(cp.toForgeCard(player));
        if ( !l.isEmpty() ) {
            for(Card c : l) {
                player.getZone(ZoneType.PlanarDeck).add(c);
            }
            player.getZone(ZoneType.PlanarDeck).shuffle();
        }
        
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

    private static void preparePlayerLibrary(Player player, final ZoneType zoneType, CardPool secion, boolean canRandomFoil, Random generator) {
        PlayerZone library = player.getZone(zoneType);
        for (final Entry<CardPrinted, Integer> stackOfCards : secion) {
            final CardPrinted cp = stackOfCards.getKey();
            for (int i = 0; i < stackOfCards.getValue(); i++) {

                CardPrinted cpi = cp;
                // apply random pictures for cards
                if (preferences.getPrefBoolean(FPref.UI_RANDOM_CARD_ART)) {
                    final int cntVariants = cp.getRules().getEditionInfo(cp.getEdition()).getCopiesCount();
                    if (cntVariants > 1) {
                        cpi = CardDb.instance().getCard(cp.getName(), cp.getEdition(), generator.nextInt(cntVariants));
                        if ( cp.isFoil() )
                            cpi = CardPrinted.makeFoiled(cpi);
                    }
                }

                final Card card = cpi.toForgeCard(player);
                
                // Assign random foiling on approximately 1:20 cards
                if (cp.isFoil() || (canRandomFoil && MyRandom.percentTrue(5))) {
                    final int iFoil = generator.nextInt(9) + 1;
                    card.setFoil(iFoil);
                }

                library.add(card);
            }
        }
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

    /**
     * Constructor for new game allowing card lists to be put into play
     * immediately, and life totals to be adjusted, for computer and human.
     * 
     * TODO: Accept something like match state as parameter. Match should be aware of players,
     * their decks and other special starting conditions.
     * @param forceAnte Forces ante on or off no matter what your preferences
     */
    public static void newGame(final GameState game, final boolean canRandomFoil, Boolean forceAnte) {

        Card.resetUniqueNumber();
        // need this code here, otherwise observables fail
        Trigger.resetIDs();
        TriggerHandler trigHandler = game.getTriggerHandler();
        trigHandler.clearDelayedTrigger();

        // friendliness
        boolean useAnte = forceAnte != null ? forceAnte : preferences.getPrefBoolean(FPref.UI_ANTE);
        final Set<CardPrinted> rAICards = new HashSet<CardPrinted>();

        Map<Player, Set<CardPrinted>> removedAnteCards = new HashMap<Player, Set<CardPrinted>>();

        GameType gameType = game.getType();
        boolean isFirstGame = game.getMatch().getPlayedGames().isEmpty();
        boolean canSideBoard = !isFirstGame && gameType.isSideboardingAllowed();
        
        final Map<LobbyPlayer, PlayerStartConditions> playersConditions = game.getMatch().getPlayers();
        for (Player player : game.getPlayers()) {
            final PlayerStartConditions psc = playersConditions.get(player.getLobbyPlayer());

            putCardsOnBattlefield(player, psc.getCardsOnBattlefield(player));
            initVariantsZones(player, psc);

            boolean hasSideboard = psc.getOriginalDeck().has(DeckSection.Sideboard);
            if (canSideBoard && hasSideboard) {
                Deck sideboarded = player.getController().sideboard(psc.getCurrentDeck(), gameType);
                psc.setCurrentDeck(sideboarded);
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
            if (player.getLobbyPlayer().getType() == PlayerType.COMPUTER && preferences.getPrefBoolean(FPref.UI_SMOOTH_LAND)) {
                // AI may do this instead of shuffling its deck
                final Iterable<Card> c1 = GameNew.smoothComputerManaCurve(player.getCardsIn(ZoneType.Library));
                player.getZone(ZoneType.Library).setCards(c1);
            } else {
                player.shuffle();
            }
            
            if(player.getLobbyPlayer().getType() == PlayerType.COMPUTER) {
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
            String message = TextUtil.buildFourColumnList("AI deck contains the following cards that it can't play or may be buggy:", rAICards);
            if (GameType.Quest == game.getType() || GameType.Sealed == game.getType() || GameType.Draft == game.getType()) {
                // log, but do not visually warn.  quest decks are supposedly already vetted by the quest creator,
                // sealed and draft decks do not get any AI-unplayable picks but may contain several
                // received/picked but unplayable cards in the sideboard.
                System.err.println(message);
            } else {
                GuiDialog.message(message);
            }
        }

        if (!removedAnteCards.isEmpty()) {
            StringBuilder ante = new StringBuilder("The following ante cards were removed:\n\n");
            for (Entry<Player, Set<CardPrinted>> ants : removedAnteCards.entrySet()) {
                ante.append(TextUtil.buildFourColumnList("From the " + ants.getKey().getName() + "'s deck:", ants.getValue()));
            }
            GuiDialog.message(ante.toString());
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
                    game.getGameLog().add("Ante", "Only basic lands found. Will ante one of them", 0);
                    ante = Aggregates.random(lib);
                }
                game.getGameLog().add("Ante", p + " anted " + ante, 0);
                game.getAction().moveTo(ZoneType.Ante, ante);
                msg.append(p.getName()).append(" ante: ").append(ante).append(nl);
            }
            GuiDialog.message(msg.toString(), "Ante");
        }

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
    
        game.setAge(GameAge.Mulligan);
        match.getInput().clearInput();
    
        //Card.resetUniqueNumber();
        // need this code here, otherwise observables fail
        forge.card.trigger.Trigger.resetIDs();
        TriggerHandler trigHandler = game.getTriggerHandler();
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

    

 // this is where the computer cheats
    // changes AllZone.getComputerPlayer().getZone(Zone.Library)

}
