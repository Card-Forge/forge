package forge;


import java.util.Iterator;
import java.util.Map;

import net.slightlymagic.braids.util.UtilFunctions;
import forge.card.cardFactory.CardFactoryInterface;
import forge.card.cardFactory.PreloadingCardFactory;
import forge.card.mana.ManaPool;
import forge.card.trigger.TriggerHandler;
import forge.deck.DeckManager;
import forge.game.GameSummary;
import forge.gui.input.InputControl;
import forge.model.FGameState;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.quest.data.QuestMatchState;
import forge.quest.data.QuestData;
import forge.quest.gui.main.QuestQuest;

/**
 * Please use public getters and setters instead of direct field access.
 * <p/>
 * If you need a setter, by all means, add it.
 *
 * @author Forge
 * @version $Id$
 */
public final class AllZone implements NewConstants {
    //only for testing, should read decks from local directory
    //  public static final IO IO = new IO("all-decks");

    /**
     * Do not instantiate.
     */
    private AllZone() {
        // blank
    }


    /** Global <code>questData</code>. */
    private static forge.quest.data.QuestData questData = null;

    /** Global <code>QuestAssignment</code>. */
    private static QuestQuest questquest = null;

    /** Constant <code>NAME_CHANGER</code>. */
    private static final NameChanger NAME_CHANGER = new NameChanger();

    /** Constant <code>COLOR_CHANGER</code>. */
    private static final ColorChanger COLOR_CHANGER = new ColorChanger();

    // Phase is now a prerequisite for CardFactory
    /** Global <code>cardFactory</code>. */
    private static CardFactoryInterface cardFactory = null;

    /** Constant <code>inputControl</code>. */
    private static final InputControl INPUT_CONTROL = new InputControl();

    /** Game state observer <code>gameInfo</code> collects statistics and players' performance. */
    private static GameSummary gameInfo = new GameSummary();

    /** 
     * Match State for quests are stored in a <code>QuestMatchState</code> class instance.
     * 
     * @deprecated Variable 'matchState' must be private and have accessor methods.
     */
    public static QuestMatchState matchState = new QuestMatchState();

    //initialized at Runtime since it has to be the last object constructed

    /** Global <code>computer</code>. */
    private static ComputerAI_Input computer;

    //shared between Input_Attack, Input_Block, Input_CombatDamage , InputState_Computer

    /** Global <code>display</code>. */
    private static Display display;

    /** Constant <code>DECK_MGR</code>. */
    private static DeckManager deckManager;

    /**
     * <p>getHumanPlayer.</p>
     * 
     * Will eventually be marked deprecated.
     * 
     * @return a {@link forge.Player} object.
     * @since 1.0.15
     */
    public static Player getHumanPlayer() {
        final FGameState gameState = Singletons.getModel().getGameState();

        if (gameState != null) {
            return gameState.getHumanPlayer();
        }

        return null;
    }

    /**
     * <p>getComputerPlayer.</p>
     *
     * Will eventually be marked deprecated.
     * 
     * @return a {@link forge.Player} object.
     * @since 1.0.15
     */
    public static Player getComputerPlayer() {
        return Singletons.getModel().getGameState().getComputerPlayer();
    }

    /**
     * <p>getQuestData.</p>
     *
     * @return a {@link forge.quest.data.QuestData} object.
     * @since 1.0.15
     */
    public static forge.quest.data.QuestData getQuestData() {
        return questData;
    }

    /**
     * <p>setQuestData.</p>
     *
     * @param questData0 a {@link forge.quest.data.QuestData} object.
     * @since 1.0.15
     */
    public static void setQuestData(final QuestData questData0) {
        questData = questData0;
    }

    /**
     * <p>getQuestAssignment.</p>
     *
     * @return a {@link forge.Quest_Assignment} object.
     * @since 1.0.15
     */
    public static QuestQuest getQuestQuest() {
        return questquest;
    }

    /**
     * <p>setQuestAssignment.</p>
     *
     * @param assignment a {@link forge.Quest_Assignment} object.
     * @since 1.0.15
     */
    public static void setQuestQuest(final QuestQuest q) {
        questquest = q;
    }

    /**
     * <p>getNameChanger.</p>
     *
     * @return a {@link forge.NameChanger} object.
     * @since 1.0.15
     */
    public static NameChanger getNameChanger() {
        return NAME_CHANGER;
    }

    /**
     * <p>getEndOfTurn.</p>
     *
     * Will eventually be marked deprecated.
     * 
     * @return a {@link forge.EndOfTurn} object.
     * @since 1.0.15
     */
    public static EndOfTurn getEndOfTurn() {
        return Singletons.getModel().getGameState().getEndOfTurn();
    }

    /**
     * <p>getEndOfCombat.</p>
     *
     * Will eventually be marked deprecated.
     * 
     * @return a {@link forge.EndOfCombat} object.
     * @since 1.0.15
     */
    public static forge.EndOfCombat getEndOfCombat() {
        return Singletons.getModel().getGameState().getEndOfCombat();
    }

    /**
     * <p>getUpkeep.</p>
     *
     * Will eventually be marked deprecated.
     * 
     * @return a {@link forge.EndOfCombat} object.
     * @since 1.0.16
     */
    public static forge.Upkeep getUpkeep() {
        return Singletons.getModel().getGameState().getUpkeep();
    }

    /**
     * <p>getPhase.</p>
     *
     * Will eventually be marked deprecated.
     * 
     * @return a {@link forge.Phase} object; may be null.
     * @since 1.0.15
     */
    public static Phase getPhase() {
        final FGameState gameState = Singletons.getModel().getGameState();

        if (gameState != null) {
            return gameState.getPhase();
        }

        return null;
    }

    /**
     * <p>getCardFactory.</p>
     *
     * @return a {@link forge.card.cardFactory.CardFactoryInterface} object.
     * @since 1.0.15
     */
    public static CardFactoryInterface getCardFactory() {
    	if (cardFactory == null) {
    	    //setCardFactory(new LazyCardFactory(ForgeProps.getFile(CARDSFOLDER)));
    		setCardFactory(new PreloadingCardFactory(ForgeProps.getFile(CARDSFOLDER)));
    	}
        return cardFactory;
    }

    /**
     * Setter for cardFactory.
     * @param factory  the factory to set
     */
    public static void setCardFactory(final CardFactoryInterface factory) {
        UtilFunctions.checkNotNull("factory", factory);
        cardFactory = factory;
    }

    /**
     * <p>getStack.</p>
     *
     * Will eventually be marked deprecated.
     * 
     * @return a {@link forge.MagicStack} object.
     * @since 1.0.15
     */
    public static MagicStack getStack() {
        final FGameState gameState = Singletons.getModel().getGameState();

        if (gameState != null) {
            return gameState.getStack();
        }

        return null;
    }

    /**
     * <p>getInputControl.</p>
     *
     * @return a {@link forge.gui.input.InputControl} object.
     * @since 1.0.15
     */
    public static InputControl getInputControl() {
        return INPUT_CONTROL;
    }

    /**
     * <p>getGameAction.</p>
     *
     * Will eventually be marked deprecated.
     * 
     * @return a {@link forge.GameAction} object.
     * @since 1.0.15
     */
    public static GameAction getGameAction() {
        final FGameState gameState = Singletons.getModel().getGameState();

        if (gameState != null) {
            return gameState.getGameAction();
        }

        return null;
    }

    /**
     * <p>getStaticEffects.</p>
     *
     * Will eventually be marked deprecated.
     * 
     * @return a {@link forge.StaticEffects} object.
     * @since 1.0.15
     */
    public static StaticEffects getStaticEffects() {
        final FGameState gameState = Singletons.getModel().getGameState();

        if (gameState != null) {
            return gameState.getStaticEffects();
        }

        return null;
    }

    /**
     * <p>getGameInfo.</p>
     *
     * @return a {@link forge.GameSummary} object.
     * @since 1.0.15
     */
    public static GameSummary getGameInfo() {
        return gameInfo;
    }

    /**
     * <p>getTriggerHandler.</p>
     *
     * Will eventually be marked deprecated.
     * 
     * @return a {@link forge.card.trigger.TriggerHandler} object.
     * @since 1.0.15
     */
    public static TriggerHandler getTriggerHandler() {
        return Singletons.getModel().getGameState().getTriggerHandler();
    }

    /**
     * <p>getComputer.</p>
     *
     * @return a {@link forge.ComputerAI_Input} object.
     * @since 1.0.15
     */
    public static ComputerAI_Input getComputer() {
        return computer;
    }

    /**
     * <p>setComputer.</p>
     *
     * @param input a {@link forge.ComputerAI_Input} object.
     * @since 1.0.15
     */
    public static void setComputer(final ComputerAI_Input input) {
        computer = input;
    }

    /**
     * <p>getCombat.</p>
     *
     * Will eventually be marked deprecated.
     * 
     * @return a {@link forge.Combat} object.
     * @since 1.0.15
     */
    public static Combat getCombat() {
        return Singletons.getModel().getGameState().getCombat();
    }

    /**
     * <p>setCombat.</p>
     *
     * Will eventually be marked deprecated.
     * 
     * @param attackers a {@link forge.Combat} object.
     * @since 1.0.15
     */
    public static void setCombat(final Combat attackers) {
        Singletons.getModel().getGameState().setCombat(attackers);
    }

    /**
     * <p>getHumanBattlefield.</p>
     *
     * Will eventually be marked deprecated.
     * 
     * @return a {@link forge.PlayerZone} object.
     * @since 1.0.15
     */
    public static PlayerZone getHumanBattlefield() {
        return Singletons.getModel().getGameState().getHumanBattlefield();
    }

    /**
     * <p>getHumanHand.</p>
     *
     * Will eventually be marked deprecated.
     * 
     * @return a {@link forge.PlayerZone} object.
     * @since 1.0.15
     */
    public static PlayerZone getHumanHand() {
        return Singletons.getModel().getGameState().getHumanHand();
    }

    /**
     * <p>getHumanGraveyard.</p>
     *
     * Will eventually be marked deprecated.
     * 
     * @return a {@link forge.PlayerZone} object.
     * @since 1.0.15
     */
    public static PlayerZone getHumanGraveyard() {
        return Singletons.getModel().getGameState().getHumanGraveyard();
    }

    /**
     * <p>getHumanLibrary.</p>
     *
     * Will eventually be marked deprecated.
     * 
     * @return a {@link forge.PlayerZone} object.
     * @since 1.0.15
     */
    public static PlayerZone getHumanLibrary() {
        return Singletons.getModel().getGameState().getHumanLibrary();
    }

    /**
     * <p>getHumanExile.</p>
     *
     * Will eventually be marked deprecated.
     * 
     * @return a {@link forge.PlayerZone} object.
     * @since 1.0.15
     */
    public static PlayerZone getHumanExile() {
        return Singletons.getModel().getGameState().getHumanExile();
    }

    /**
     * <p>getHumanCommand.</p>
     *
     * Will eventually be marked deprecated.
     * 
     * @return a {@link forge.PlayerZone} object.
     * @since 1.0.15
     */
    public static PlayerZone getHumanCommand() {
        return Singletons.getModel().getGameState().getHumanCommand();
    }

    /**
     * <p>getComputerBattlefield.</p>
     *
     * Will eventually be marked deprecated.
     * 
     * @return a {@link forge.PlayerZone} object.
     * @since 1.0.15
     */
    public static PlayerZone getComputerBattlefield() {
        return Singletons.getModel().getGameState().getComputerBattlefield();
    }

    /**
     * <p>getComputerHand.</p>
     *
     * Will eventually be marked deprecated.
     * 
     * @return a {@link forge.PlayerZone} object.
     * @since 1.0.15
     */
    public static PlayerZone getComputerHand() {
        return Singletons.getModel().getGameState().getComputerHand();
    }

    /**
     * <p>getComputerGraveyard.</p>
     *
     * Will eventually be marked deprecated.
     * 
     * @return a {@link forge.PlayerZone} object.
     * @since 1.0.15
     */
    public static PlayerZone getComputerGraveyard() {
        return Singletons.getModel().getGameState().getComputerGraveyard();
    }

    /**
     * <p>getComputerLibrary.</p>
     *
     * Will eventually be marked deprecated.
     * 
     * @return a {@link forge.PlayerZone} object.
     * @since 1.0.15
     */
    public static PlayerZone getComputerLibrary() {
        return Singletons.getModel().getGameState().getComputerLibrary();
    }

    /**
     * <p>getComputerExile.</p>
     *
     * Will eventually be marked deprecated.
     * 
     * @return a {@link forge.PlayerZone} object.
     * @since 1.0.15
     */
    public static PlayerZone getComputerExile() {
        return Singletons.getModel().getGameState().getComputerExile();
    }

    /**
     * <p>getComputerCommand.</p>
     *
     * Will eventually be marked deprecated.
     * 
     * @return a {@link forge.PlayerZone} object.
     * @since 1.0.15
     */
    public static PlayerZone getComputerCommand() {
        return Singletons.getModel().getGameState().getComputerCommand();
    }

    /**
     * <p>getStackZone.</p>
     *
     * Will eventually be marked deprecated.
     * 
     * @return a {@link forge.PlayerZone} object.
     * @since 1.0.15
     */
    public static PlayerZone getStackZone() {
        return Singletons.getModel().getGameState().getStackZone();
    }

    /**
     * <p>getManaPool.</p>
     *
     * @return a {@link forge.card.mana.ManaPool} object.
     * @since 1.0.15
     */
    public static ManaPool getManaPool() {
        return AllZone.getHumanPlayer().getManaPool();
    }

    /**
     * <p>getComputerManaPool.</p>
     *
     * @return a {@link forge.card.mana.ManaPool} object.
     * @since 1.0.15
     */
    public static ManaPool getComputerManaPool() {
        return AllZone.getComputerPlayer().getManaPool();
    }

    /**
     * <p>getDisplay.</p>
     *
     * @return a {@link forge.Display} object.
     * @since 1.0.15
     */
    public static Display getDisplay() {
        return display;
    }

    /**
     * <p>setDisplay.</p>
     *
     * @param display0 a {@link forge.Display} object.
     * @since 1.0.15
     */
    public static void setDisplay(final Display display0) {
        display = display0;
    }

    /**
     * <p>Getter for the field <code>map</code>.</p>
     *
     * Will eventually be marked deprecated.
     * 
     * @return a {@link java.util.Map} object.
     */
    private static Map<String, PlayerZone> getMap() {
        return Singletons.getModel().getGameState().getZoneNamesToPlayerZones();
    }

    /**
     * <p>getZone.</p>
     *
     * @param c a {@link forge.Card} object.
     * @return a {@link forge.PlayerZone} object.
     */
    public static PlayerZone getZone(final Card c) {
        Iterator<PlayerZone> it = getMap().values().iterator();
        PlayerZone p;
        while (it.hasNext()) {
            p = (PlayerZone) it.next();

            if (AllZoneUtil.isCardInZone(p, c)) {
                return p;
            }
        }
        return null;
    }

    /**
     * <p>getZone.</p>
     *
     * @param zone a {@link java.lang.String} object.
     * @param finalPlayer a {@link forge.Player} object.
     * @return a {@link forge.PlayerZone} object.
     */
    public static PlayerZone getZone(final String zone, final Player finalPlayer) {
        Player player;
        if (zone.equals("Stack")) {
            player = null;
        }
        else {
            player = finalPlayer;
        }

        Object o = getMap().get(zone + player);

        if (o == null) {
            throw new RuntimeException("AllZone : getZone() invalid parameters " + zone + " " + player);
        }

        return (PlayerZone) o;
    }

    /**
     * <p>resetZoneMoveTracking.</p>
     */
    public static void resetZoneMoveTracking() {
        ((DefaultPlayerZone) getHumanCommand()).resetCardsAddedThisTurn();
        ((DefaultPlayerZone) getHumanLibrary()).resetCardsAddedThisTurn();
        ((DefaultPlayerZone) getHumanHand()).resetCardsAddedThisTurn();
        ((DefaultPlayerZone) getHumanBattlefield()).resetCardsAddedThisTurn();
        ((DefaultPlayerZone) getHumanGraveyard()).resetCardsAddedThisTurn();
        ((DefaultPlayerZone) getComputerCommand()).resetCardsAddedThisTurn();
        ((DefaultPlayerZone) getComputerLibrary()).resetCardsAddedThisTurn();
        ((DefaultPlayerZone) getComputerHand()).resetCardsAddedThisTurn();
        ((DefaultPlayerZone) getComputerBattlefield()).resetCardsAddedThisTurn();
        ((DefaultPlayerZone) getComputerGraveyard()).resetCardsAddedThisTurn();
    }

    /** 
     * <p>getDeckManager.</p>
     * @return dMgr
     */
    public static DeckManager getDeckManager() {
        if (deckManager == null) {
            deckManager = new DeckManager(ForgeProps.getFile(NEW_DECKS));
        }
        return deckManager;
    }

    /**
     * Create and return the next timestamp.
     * 
     * Will eventually be marked deprecated.
     * 
     * @return the next timestamp
     */
    public static long getNextTimestamp() {
        return Singletons.getModel().getGameState().getNextTimestamp();
    }

    /**
     * <p>Resets everything possible to set a new game.</p>
     */
    public static void newGameCleanup() {

        gameInfo = new GameSummary();

        getHumanPlayer().reset();
        getComputerPlayer().reset();

        getPhase().reset();
        getStack().reset();
        getCombat().reset();
        getDisplay().showCombat("");
        getDisplay().loadPrefs();

        getHumanGraveyard().reset();
        getHumanHand().reset();
        getHumanLibrary().reset();
        getHumanBattlefield().reset();
        getHumanExile().reset();

        getComputerGraveyard().reset();
        getComputerHand().reset();
        getComputerLibrary().reset();
        getComputerBattlefield().reset();
        getComputerExile().reset();

        getInputControl().clearInput();

        getStaticEffects().reset();
        getColorChanger().reset();

        // player.reset() now handles this
        //AllZone.getHumanPlayer().clearHandSizeOperations();
        //AllZone.getComputerPlayer().clearHandSizeOperations();

        getTriggerHandler().clearRegistered();

    }

    /**
     * Getter for matchState.
     * @return the matchState
     */
    public static QuestMatchState getMatchState() {
        return matchState;
    }

    /**
     * Getter for colorChanger.
     * @return the colorChanger
     */
    public static ColorChanger getColorChanger() {
        return COLOR_CHANGER;
    }
} //AllZone
