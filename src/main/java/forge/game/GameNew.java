package forge.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import javax.swing.JOptionPane;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.Card;
import forge.CardCharacteristicName;
import forge.CardLists;
import forge.CardPredicates;
import forge.CardUtil;
import forge.GameAction;
import forge.Singletons;
import forge.card.trigger.TriggerHandler;
import forge.card.trigger.TriggerType;
import forge.control.input.InputControl;
import forge.control.input.InputMulligan;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.deck.DeckTempStorage;
import forge.game.event.FlipCoinEvent;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.LobbyPlayer;
import forge.game.player.Player;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
import forge.gui.match.views.VAntes;
import forge.item.CardDb;
import forge.item.CardPrinted;
import forge.properties.ForgePreferences.FPref;
import forge.util.Aggregates;
import forge.util.MyRandom;

/** 
 * Methods for all things related to starting a new game.
 * All of these methods can and should be static.
 */
public class GameNew {
    private static void prepareFirstGameLibrary(Player player, Deck deck, Map<Player, List<String>> removedAnteCards, List<String> rAICards, boolean canRandomFoil, Random generator, boolean useAnte) { 
    
        PlayerZone library = player.getZone(ZoneType.Library);
        PlayerZone sideboard = player.getZone(ZoneType.Sideboard);
        for (final Entry<CardPrinted, Integer> stackOfCards : deck.getMain()) {
            final CardPrinted cardPrinted = stackOfCards.getKey();
            for (int i = 0; i < stackOfCards.getValue(); i++) {

                final Card card = cardPrinted.toForgeCard(player);

                // apply random pictures for cards
                if (player.isComputer() || Singletons.getModel().getPreferences().getPrefBoolean(FPref.UI_RANDOM_CARD_ART)) {
                    final int cntVariants = cardPrinted.getCard().getEditionInfo(cardPrinted.getEdition()).getCopiesCount();
                    if (cntVariants > 1) {
                        card.setRandomPicture(generator.nextInt(cntVariants - 1) + 1);
                        card.setImageFilename(CardUtil.buildFilename(card));
                    }
                }

                // Assign random foiling on approximately 1:20 cards
                if (cardPrinted.isFoil() || (canRandomFoil && MyRandom.percentTrue(5))) {
                    final int iFoil = MyRandom.getRandom().nextInt(9) + 1;
                    card.setFoil(iFoil);
                }

                if (!useAnte && card.hasKeyword("Remove CARDNAME from your deck before playing if you're not playing for ante.")) {
                    if (!removedAnteCards.containsKey(player)) {
                        removedAnteCards.put(player, new ArrayList<String>());
                    }
                    removedAnteCards.get(player).add(card.getName());
                } else {
                    library.add(card);
                }

                // mark card as difficult for AI to play
                if (player.isComputer() && card.getSVar("RemAIDeck").equals("True") && !rAICards.contains(card.getName())) {
                    rAICards.add(card.getName());
                    // get card picture so that it is in the image cache
                    // ImageCache.getImage(card);
                }
            }
        }
        for (final Entry<CardPrinted, Integer> stackOfCards : deck.getSideboard()) {
            final CardPrinted cardPrinted = stackOfCards.getKey();
            for (int i = 0; i < stackOfCards.getValue(); i++) {

                final Card card = cardPrinted.toForgeCard(player);

                // apply random pictures for cards
                if (player.isComputer() || Singletons.getModel().getPreferences().getPrefBoolean(FPref.UI_RANDOM_CARD_ART)) {
                    final int cntVariants = cardPrinted.getCard().getEditionInfo(cardPrinted.getEdition()).getCopiesCount();
                    if (cntVariants > 1) {
                        card.setRandomPicture(generator.nextInt(cntVariants - 1) + 1);
                        card.setImageFilename(CardUtil.buildFilename(card));
                    }
                }

                // Assign random foiling on approximately 1:20 cards
                if (cardPrinted.isFoil() || (canRandomFoil && MyRandom.percentTrue(5))) {
                    final int iFoil = MyRandom.getRandom().nextInt(9) + 1;
                    card.setFoil(iFoil);
                }

                if (!useAnte && card.hasKeyword("Remove CARDNAME from your deck before playing if you're not playing for ante.")) {
                    if (!removedAnteCards.containsKey(player)) {
                        removedAnteCards.put(player, new ArrayList<String>());
                    }
                    removedAnteCards.get(player).add(card.getName());
                } else {
                    sideboard.add(card);
                }

                // TODO: Enable the code below for Limited modes only when the AI can play all the 
                // cards in the sideboard (probably means all cards in Forge because in Limited mode,
                // any cards can end up in the AI sideboard?)
                
                // mark card as difficult for AI to play
                if (player.isComputer() && card.getSVar("RemAIDeck").equals("True") && !rAICards.contains(card.getName())) {
                    if (Singletons.getModel().getMatch().getGameType() != GameType.Draft && 
                        Singletons.getModel().getMatch().getGameType() != GameType.Sealed) {
                        rAICards.add(card.getName());
                        // get card picture so that it is in the image cache
                        // ImageCache.getImage(card);
                    }
                }
            }
        }
    }

    private static void prepareSingleLibrary(final Player player, final Deck deck, final Map<Player, List<String>> removedAnteCards, final List<String> rAICards, boolean canRandomFoil) {
        final Random generator = MyRandom.getRandom();
        boolean useAnte = Singletons.getModel().getPreferences().getPrefBoolean(FPref.UI_ANTE);

        if (Singletons.getModel().getMatch().getPlayedGames().size() == 0) {
            // TODO: this is potentially not network-friendly. If network play is 
            // implemented, and, as such, more than one human player becomes possible
            // in a game, DeckTempStorage may have to be converted to something like an array
            // for each human player or something like that, I imagine...
            // (I could be wrong). Also see other deck usages for DeckTempStorage.
            if (player.isHuman()) {
                DeckTempStorage.setHumanMain(deck.getMain().toForgeCardList());
                DeckTempStorage.setHumanSideboard(deck.getSideboard().toForgeCardList());
            }
            prepareFirstGameLibrary(player, deck, removedAnteCards, rAICards, canRandomFoil, generator, useAnte);
        } else {
            if (!sideboardAndPrepareLibrary(player, deck, canRandomFoil, generator, useAnte)) {
                prepareFirstGameLibrary(player, deck, removedAnteCards, rAICards, canRandomFoil, generator, useAnte);
            }
        }

        // Shuffling
        // Ai may cheat
        if (player.isComputer() && Singletons.getModel().getPreferences().getPrefBoolean(FPref.UI_SMOOTH_LAND)) {
            // do this instead of shuffling Computer's deck
            final Iterable<Card> c1 = GameNew.smoothComputerManaCurve(player.getCardsIn(ZoneType.Library));
            player.getZone(ZoneType.Library).setCards(c1);
        } else {
            player.shuffle();
        }
    }

    private static boolean sideboardAndPrepareLibrary(final Player player, final Deck deck, boolean canRandomFoil, Random generator, boolean useAnte) {
        final GameType gameType = Singletons.getModel().getMatch().getGameType();
        boolean hasSideboard = (deck.getSideboard().countAll() > 0);
       
        PlayerZone library = player.getZone(ZoneType.Library);
        DeckSection sideboard = deck.getSideboard();
        int sideboardSize = (gameType == GameType.Draft || gameType == GameType.Sealed) ? -1 : sideboard.countAll();
       
        if (!hasSideboard) {
            return false;
        }
       
        if (player.isComputer()) {
            // Here is where the AI could sideboard, but needs to gather hints during the first game about what to SB

            // TODO: also something needs to be done with the random card pictures / foiling code
            //       which normally fires from prepareFirstGameLibrary - the code below (for human)
            //       is actually repetitive, it would be good to avoid code duplication both below
            //       and potentially here in the AI sideboarding part.

            return false;
        } else {
            // Human Sideboarding
            boolean validDeck = false;
            List<Card> newDeck = null;
            int deckMinSize = gameType.getDeckMinimum();

            while (!validDeck) {
                newDeck = GuiChoose.getOrderChoices("Sideboard", "Main Deck", sideboardSize,
                    deck.getSideboard().toForgeCardList(), deck.getMain().toForgeCardList(), null, true);

                if (newDeck.size() >= deckMinSize || (gameType != GameType.Draft && gameType != GameType.Sealed)) {
                    validDeck = true;
                } else {
                    StringBuilder errMsg = new StringBuilder("Too few cards in your main deck (minimum ");
                    errMsg.append(deckMinSize);
                    errMsg.append("), please make modifications to your deck again.");
                    JOptionPane.showMessageDialog(null, errMsg.toString(), "Invalid deck", JOptionPane.ERROR_MESSAGE);
                }
            }
           
            for (Card c : newDeck) {
                if (c.isFlipCard() && c.isFlipped()) {
                    c.changeToState(CardCharacteristicName.Original);
                    c.setFlipStaus(false);
                }
                
                CardPrinted cp = CardDb.instance().getCard(c);

                // TODO: avoid code duplication below? (currently copied from prepareFirstGameLibrary)
                // apply random pictures for cards
                if (Singletons.getModel().getPreferences().getPrefBoolean(FPref.UI_RANDOM_CARD_ART)) {
                    final int cntVariants = cp.getCard().getEditionInfo(cp.getEdition()).getCopiesCount();
                    if (cntVariants > 1) {
                        c.setRandomPicture(generator.nextInt(cntVariants - 1) + 1);
                        c.setImageFilename(CardUtil.buildFilename(c));
                    }
                }
                // Assign random foiling on approximately 1:20 cards
                if (cp.isFoil() || (canRandomFoil && MyRandom.percentTrue(5))) {
                    final int iFoil = MyRandom.getRandom().nextInt(9) + 1;
                    c.setFoil(iFoil);
                }

                c.setOwner(player);
                library.add(c);
            }
        }
        return true;
    }

    /**
     * Constructor for new game allowing card lists to be put into play
     * immediately, and life totals to be adjusted, for computer and human.
     * 
     * TODO: Accept something like match state as parameter. Match should be aware of players,
     * their decks and other special starting conditions.
     */
    public static void newGame(final Map<Player, PlayerStartConditions> playersConditions, final GameState game, final boolean canRandomFoil) {
        Singletons.getModel().getMatch().getInput().clearInput();

        Card.resetUniqueNumber();
        // need this code here, otherwise observables fail
        forge.card.trigger.Trigger.resetIDs();
        TriggerHandler trigHandler = game.getTriggerHandler();
        trigHandler.clearTriggerSettings();
        trigHandler.clearDelayedTrigger();

        // friendliness
        final Map<Player, List<String>> removedAnteCards = new HashMap<Player, List<String>>();
        final List<String> rAICards = new ArrayList<String>();

        for (Entry<Player, PlayerStartConditions> p : playersConditions.entrySet()) {
            final Player player = p.getKey();
            player.setStartingLife(p.getValue().getStartingLife());
            int hand = p.getValue().getStartingHand();
            player.setMaxHandSize(hand);
            player.setStartingHandSize(hand);
            // what if I call it for AI player?
            PlayerZone bf = player.getZone(ZoneType.Battlefield);
            Iterable<Card> onTable = p.getValue().getCardsOnBattlefield();
            if (onTable != null) {
                for (final Card c : onTable) {
                    c.setOwner(player);
                    bf.add(c, false);
                    c.setSickness(true);
                    c.setStartsGameInPlay(true);
                    c.refreshUniqueNumber();
                }
            }

            PlayerZone com = player.getZone(ZoneType.Command);
            Iterable<Card> inCommand = p.getValue().getCardsInCommand();
            if (inCommand != null) {
                for (final Card c : inCommand) {
                    c.setOwner(player);
                    
                    com.add(c, false);
                    c.refreshUniqueNumber();
                }
            }
            
            Iterable<Card> schemes = p.getValue().getSchemes();
            if(schemes != null) {
                player.setSchemeDeck(schemes);
            }
            

            prepareSingleLibrary(player, p.getValue().getDeck(), removedAnteCards, rAICards, canRandomFoil);
            player.updateObservers();
            bf.updateObservers();
            player.getZone(ZoneType.Hand).updateObservers();
            player.getZone(ZoneType.Command).updateObservers();
            player.getZone(ZoneType.Battlefield).updateObservers();
        }



        if (rAICards.size() > 0) {
            String message = buildFourColumnList("AI deck contains the following cards that it can't play or may be buggy:", rAICards);
            JOptionPane.showMessageDialog(null, message, "", JOptionPane.INFORMATION_MESSAGE);
        }

        if (!removedAnteCards.isEmpty()) {
            StringBuilder ante = new StringBuilder("The following ante cards were removed:\n\n");
            for (Entry<Player, List<String>> ants : removedAnteCards.entrySet()) {
                ante.append(buildFourColumnList("From the " + ants.getKey().getName() + "'s deck:", ants.getValue()));
            }
            JOptionPane.showMessageDialog(null, ante.toString(), "", JOptionPane.INFORMATION_MESSAGE);
        }

        GameNew.actuateGame(game, false);
    }

    public static void restartGame(final GameState game, final Player startingTurn, Map<Player, List<Card>> playerLibraries) {
        MatchController match = Singletons.getModel().getMatch();

        Map<LobbyPlayer, PlayerStartConditions> players = match.getPlayers();
        Map<Player, PlayerStartConditions> playersConditions = new HashMap<Player, PlayerStartConditions>();

        for (Player p : game.getPlayers()) {
            playersConditions.put(p, players.get(p.getLobbyPlayer()));
        }

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
            // what if I call it for AI player?
            PlayerZone bf = player.getZone(ZoneType.Battlefield);
            Iterable<Card> onTable = p.getValue().getCardsOnBattlefield();
            if (onTable != null) {
                for (final Card c : onTable) {
                    c.addController(player);
                    c.setOwner(player);
                    bf.add(c, false);
                    c.setSickness(true);
                    c.setStartsGameInPlay(true);
                    c.refreshUniqueNumber();
                }
            }

            PlayerZone library = player.getZone(ZoneType.Library);
            List<Card> newLibrary = playerLibraries.get(player);
            for (Card c : newLibrary) {
                action.moveTo(library, c);
            }

            player.shuffle();
            bf.updateObservers();
            player.updateObservers();
            player.getZone(ZoneType.Hand).updateObservers();
        }

        trigHandler.clearSuppression(TriggerType.ChangesZone);

        PhaseHandler phaseHandler = game.getPhaseHandler();
        phaseHandler.setPlayerTurn(startingTurn);

        GameNew.actuateGame(game, true);
    }

    /**
     * This must be separated from the newGame method since life totals and
     * player details could be adjusted before the game is started.
     * 
     * That process (also cleanup and observer updates) should be done in
     * newGame, then when all is ready, call this function.
     * @param isRestartedGame Whether the actuated game is the first start or a restart
     */
    private static void actuateGame(final GameState game, boolean isRestartedGame) {
        if (!isRestartedGame) {
            // Deciding which cards go to ante
            if (Singletons.getModel().getPreferences().getPrefBoolean(FPref.UI_ANTE)) {
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

            GameOutcome lastGameOutcome = Singletons.getModel().getMatch().getLastGameOutcome();
            // Only cut/coin toss if it's the first game of the match
            if (lastGameOutcome == null) {
                GameNew.seeWhoPlaysFirstDice();
            } else {
                Player human = Singletons.getControl().getPlayer();
                Player goesFirst = lastGameOutcome.isWinner(human.getLobbyPlayer()) ? human.getOpponent() : human;
                setPlayersFirstTurn(goesFirst, false);
            }
        }

        // Draw <handsize> cards
        for (final Player p : game.getPlayers()) {
            p.drawCards(p.getMaxHandSize());
        }

        game.getPhaseHandler().setPhaseState(PhaseType.MULLIGAN);
        InputControl control = Singletons.getModel().getMatch().getInput();
        control.setInput(new InputMulligan());
    } // newGame()

    private static String buildFourColumnList(String firstLine, List<String> cAnteRemoved) {
        StringBuilder sb = new StringBuilder(firstLine);
        sb.append("\n");
        for (int i = 0; i < cAnteRemoved.size(); i++) {
            sb.append(cAnteRemoved.get(i));
            if (((i % 4) == 0) && (i > 0)) {
                sb.append("\n");
            } else if (i != (cAnteRemoved.size() - 1)) {
                sb.append(", ");
            }
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
     */
    private static void seeWhoPlaysFirstDice() {
        int playerDie = 0;
        int computerDie = 0;

        while (playerDie == computerDie) {
            playerDie = MyRandom.getRandom().nextInt(20);
            computerDie = MyRandom.getRandom().nextInt(20);
        }

        // Play the Flip Coin sound
        Singletons.getModel().getGame().getEvents().post(new FlipCoinEvent());

        List<Player> allPlayers = Singletons.getModel().getGame().getPlayers();
        setPlayersFirstTurn(allPlayers.get(MyRandom.getRandom().nextInt(allPlayers.size())), true);
    }

    private static void setPlayersFirstTurn(Player goesFirst, boolean firstGame) {
        StringBuilder sb = new StringBuilder(goesFirst.toString());
        if (firstGame) {
            sb.append(" has won the coin toss.");
        }
        else {
          sb.append(" lost the last game.");
        }
        if (goesFirst.isHuman()) {
            if (!humanPlayOrDraw(sb.toString())) {
                goesFirst = goesFirst.getOpponent();
            }
        } else {
            sb.append("\nComputer Going First");
            JOptionPane.showMessageDialog(null, sb.toString(),
                    "Play or Draw?", JOptionPane.INFORMATION_MESSAGE);
        }
        Singletons.getModel().getGame().getPhaseHandler().setPlayerTurn(goesFirst);
    } // seeWhoPlaysFirstDice()

    private static boolean humanPlayOrDraw(String message) {
        final String[] possibleValues = { "Play", "Draw" };

        final Object playDraw = JOptionPane.showOptionDialog(null, message + "\n\nWould you like to play or draw?",
                "Play or Draw?", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null,
                possibleValues, possibleValues[0]);

        return !playDraw.equals(1);
    }
}
