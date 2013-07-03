package forge.game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.Card;
import forge.CardLists;
import forge.CardPredicates;
import forge.GameLogEntryType;
import forge.Singletons;
import forge.card.CardDb;
import forge.card.CardEdition;
import forge.card.trigger.Trigger;
import forge.card.trigger.TriggerHandler;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.game.player.Player;
import forge.game.player.PlayerType;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.gui.GuiDialog;
import forge.item.PaperCard;
import forge.item.IPaperCard;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.util.Aggregates;
import forge.util.MyRandom;
import forge.util.TextUtil;
import forge.util.maps.CollectionSuppliers;
import forge.util.maps.HashMapOfLists;
import forge.util.maps.MapOfLists;

/** 
 * Methods for all things related to starting a new game.
 * All of these methods can and should be static.
 */
public class GameNew {
    
    public static final ForgePreferences preferences = forge.Singletons.getModel().getPreferences();

    public static void putCardsOnBattlefield(Player player, Iterable<? extends IPaperCard> cards) {
        PlayerZone bf = player.getZone(ZoneType.Battlefield);
        if (cards != null) {
            for (final IPaperCard cp : cards) {
                Card c = cp.toForgeCard(player);
                c.setOwner(player);
                bf.add(c);
                c.setSickness(true);
                c.setStartsGameInPlay(true);
                c.refreshUniqueNumber();
            }
        }
    
    }

    private static void initVariantsZones(final Player player, final RegisteredPlayer psc) {
        PlayerZone com = player.getZone(ZoneType.Command);
    
        // Mainly for avatar, but might find something else here
        for (final IPaperCard c : psc.getCardsInCommand(player)) {
            com.add(c.toForgeCard(player));
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

    private static Set<PaperCard> getRemovedAnteCards(Deck toUse) {
        final String keywordToRemove = "Remove CARDNAME from your deck before playing if you're not playing for ante.";
        Set<PaperCard> myRemovedAnteCards = new HashSet<PaperCard>();
        for ( Entry<DeckSection, CardPool> ds : toUse ) {
            for (Entry<PaperCard, Integer> cp : ds.getValue()) {
                if ( Iterables.contains(cp.getKey().getRules().getMainPart().getKeywords(), keywordToRemove) ) 
                    myRemovedAnteCards.add(cp.getKey());
            }
        }
    
        for(PaperCard cp: myRemovedAnteCards) {
            for ( Entry<DeckSection, CardPool> ds : toUse ) {
                ds.getValue().remove(cp, Integer.MAX_VALUE);
            }
        }
        return myRemovedAnteCards;
    }

    private static void preparePlayerLibrary(Player player, final ZoneType zoneType, CardPool section, boolean canRandomFoil, Random generator) {
        PlayerZone library = player.getZone(zoneType);
        for (final Entry<PaperCard, Integer> stackOfCards : section) {
            final PaperCard cp = stackOfCards.getKey();
            for (int i = 0; i < stackOfCards.getValue(); i++) {

                PaperCard cpi = cp;
                // apply random pictures for cards
                if (preferences.getPrefBoolean(FPref.UI_RANDOM_CARD_ART)) {
                    cpi = CardDb.instance().getCard(cp.getName(), cp.getEdition(), -1);
                    if ( cp.isFoil() )
                        cpi = CardDb.instance().getFoiled(cpi);
                }

                final Card card = cpi.toForgeCard(player);
                
                // Assign card-specific foiling or random foiling on approximately 1:20 cards if enabled
                CardEdition.FoilType foilType = Singletons.getModel().getEditions().get(card.getCurSetCode()).getFoilType();
                if (foilType != CardEdition.FoilType.NOT_SUPPORTED && (cp.isFoil() || (canRandomFoil && MyRandom.percentTrue(5)))) {
                    int iFoil = 0;

                    switch(foilType) {
                        case MODERN:
                            iFoil = generator.nextInt(9) + 1; // modern foils in slots 1-10
                            break; 
                        case OLD_STYLE:
                            iFoil = generator.nextInt(9) + 11; // old style foils in slots 11-20
                            break;
                        default:
                            System.out.println(String.format("Unexpected foil type for card %s in edition %s.", card.getName(), card.getCurSetCode()));
                    }

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

    private static List<PaperCard> getCardsAiCantPlayWell(final Deck toUse) {
        List<PaperCard> result = new ArrayList<PaperCard>();
        
        for ( Entry<DeckSection, CardPool> ds : toUse ) {
            for (Entry<PaperCard, Integer> cp : ds.getValue()) {
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
    public static void newGame(final Game game, final boolean canRandomFoil, boolean useAnte) {

        Card.resetUniqueNumber();
        // need this code here, otherwise observables fail
        Trigger.resetIDs();
        TriggerHandler trigHandler = game.getTriggerHandler();
        trigHandler.clearDelayedTrigger();

        // friendliness
        final Set<PaperCard> rAICards = new HashSet<PaperCard>();

        MapOfLists<Player, PaperCard> removedAnteCards = new HashMapOfLists<Player, PaperCard>(CollectionSuppliers.<PaperCard>hashSets());

        GameType gameType = game.getType();
        boolean isFirstGame = game.getMatch().getPlayedGames().isEmpty();
        boolean canSideBoard = !isFirstGame && gameType.isSideboardingAllowed();
        
        final List<RegisteredPlayer> playersConditions = game.getMatch().getPlayers();
        for (int i = 0; i < playersConditions.size(); i++) {
            Player player = game.getPlayers().get(i);
            final RegisteredPlayer psc = playersConditions.get(i);

            putCardsOnBattlefield(player, psc.getCardsOnBattlefield(player));
            initVariantsZones(player, psc);

            if (canSideBoard) {
                Deck sideboarded = player.getController().sideboard(psc.getCurrentDeck(), gameType);
                psc.setCurrentDeck(sideboarded);
            } else { 
                psc.restoreOriginalDeck();
            }
            Deck myDeck = psc.getCurrentDeck();
            boolean hasSideboard = myDeck.has(DeckSection.Sideboard);

            Set<PaperCard> myRemovedAnteCards = useAnte ? null : getRemovedAnteCards(myDeck);
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
            
            if(isFirstGame && player.getLobbyPlayer().getType() == PlayerType.COMPUTER) {
                rAICards.addAll(getCardsAiCantPlayWell(myDeck));
            }

            if( myRemovedAnteCards != null && !myRemovedAnteCards.isEmpty() )
                removedAnteCards.addAll(player, myRemovedAnteCards);
        }

        if (!rAICards.isEmpty()) {
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
            for (Entry<Player, Collection<PaperCard>> ants : removedAnteCards.entrySet()) {
                ante.append(TextUtil.buildFourColumnList("From the " + ants.getKey().getName() + "'s deck:", ants.getValue()));
            }
            GuiDialog.message(ante.toString());
        }
    }

    static List<Pair<Player, Card>> chooseCardsForAnte(final Game game) {
        List<Pair<Player, Card>> anteed = new ArrayList<Pair<Player,Card>>();

        for (final Player p : game.getPlayers()) {
            final List<Card> lib = p.getCardsIn(ZoneType.Library);
            Predicate<Card> goodForAnte = Predicates.not(CardPredicates.Presets.BASIC_LANDS);
            Card ante = Aggregates.random(Iterables.filter(lib, goodForAnte));
            if (ante == null) {
                game.getGameLog().add(GameLogEntryType.ANTE, "Only basic lands found. Will ante one of them");
                ante = Aggregates.random(lib);
            }
            anteed.add(Pair.of(p, ante));
        }
        return anteed;
    }
    
    static void moveCardsToAnte(List<Pair<Player, Card>> cards) {
        for(Pair<Player, Card> kv : cards) {
            Player p = kv.getKey();
            p.getGame().getAction().moveTo(ZoneType.Ante, kv.getValue());
            p.getGame().getGameLog().add(GameLogEntryType.ANTE, p + " anted " + kv.getValue());
        }
    }

 // this is where the computer cheats
    // changes AllZone.getComputerPlayer().getZone(Zone.Library)

}
