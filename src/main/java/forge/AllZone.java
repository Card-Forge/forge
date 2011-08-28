package forge;


import forge.card.cardFactory.CardFactoryInterface;
import forge.card.cardFactory.PreloadingCardFactory;
import forge.card.mana.ManaPool;
import forge.card.trigger.TriggerHandler;
import forge.deck.DeckManager;
import forge.game.GameSummary;
import forge.gui.input.InputControl;
import forge.model.FGameState;
import forge.model.FModel;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.quest.data.QuestMatchState;

import java.util.Iterator;
import java.util.Map;

import net.slightlymagic.braids.util.UtilFunctions;


/**
 * Please use public getters and setters instead of direct field access.
 * <p/>
 * If you need a setter, by all means, add it.
 *
 * @author Forge
 * @version $Id$
 */
public class AllZone implements NewConstants {
    //only for testing, should read decks from local directory
//  public static final IO IO = new IO("all-decks");


    /** Constant <code>QuestData</code> */
    private static forge.quest.data.QuestData QuestData = null;
    /** Constant <code>QuestAssignment</code> */
    private static Quest_Assignment QuestAssignment = null;
    /** Constant <code>NameChanger</code> */
    private static final NameChanger NameChanger = new NameChanger();
    /** Constant <code>ColorChanger</code> */
    private static final ColorChanger colorChanger = new ColorChanger();

    // Phase is now a prerequisite for CardFactory
    /** Constant <code>CardFactory</code> */
    private static CardFactoryInterface cardFactory = null;

    /** Constant <code>InputControl</code> */
    private static final InputControl InputControl = new InputControl();
    
    /** Game state observer <code>GameSummary</code> collects statistics and players' performance*/
    private static GameSummary gameInfo = new GameSummary();
    /** Match State for quests are stored in a <code>QuestMatchState</code> class instance*/
    public static QuestMatchState matchState = new QuestMatchState();

    //initialized at Runtime since it has to be the last object constructed

    /** Constant <code>Computer</code> */
    private static ComputerAI_Input Computer;

    //shared between Input_Attack, Input_Block, Input_CombatDamage , InputState_Computer

    /** Constant <code>Display</code> */
    private static Display Display;

    /** Constant <code>DeckManager</code> */
    private final static DeckManager dMgr = new DeckManager(ForgeProps.getFile(NEW_DECKS));

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
        return QuestData;
    }

    /**
     * <p>setQuestData.</p>
     *
     * @param questData a {@link forge.quest.data.QuestData} object.
     * @since 1.0.15
     */
    public static void setQuestData(forge.quest.data.QuestData questData) {
        QuestData = questData;
    }

    /**
     * <p>getQuestAssignment.</p>
     *
     * @return a {@link forge.Quest_Assignment} object.
     * @since 1.0.15
     */
    public static Quest_Assignment getQuestAssignment() {
        return QuestAssignment;
    }

    /**
     * <p>setQuestAssignment.</p>
     *
     * @param assignment a {@link forge.Quest_Assignment} object.
     * @since 1.0.15
     */
    public static void setQuestAssignment(Quest_Assignment assignment) {
        QuestAssignment = assignment;
    }

    /**
     * <p>getNameChanger.</p>
     *
     * @return a {@link forge.NameChanger} object.
     * @since 1.0.15
     */
    public static NameChanger getNameChanger() {
        return NameChanger;
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
    		setCardFactory(new PreloadingCardFactory(ForgeProps.getFile(CARDSFOLDER)));
    	}
        return cardFactory;
    }

    public static void setCardFactory(CardFactoryInterface factory) {
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
        return InputControl;
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
        return Computer;
    }

    /**
     * <p>setComputer.</p>
     *
     * @param input a {@link forge.ComputerAI_Input} object.
     * @since 1.0.15
     */
    public static void setComputer(ComputerAI_Input input) {
        Computer = input;
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
    public static void setCombat(Combat attackers) {
        Singletons.getModel().getGameState().setCombat(attackers);
    }

    //Human_Play, Computer_Play is different because Card.comesIntoPlay() is called when a card is added by PlayerZone.add(Card)
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
        return Display;
    }

    /**
     * <p>setDisplay.</p>
     *
     * @param display a {@link forge.Display} object.
     * @since 1.0.15
     */
    public static void setDisplay(Display display) {
        Display = display;
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
    public static PlayerZone getZone(Card c) {
        Iterator<PlayerZone> it = getMap().values().iterator();
        PlayerZone p;
        while (it.hasNext()) {
            p = (PlayerZone) it.next();

            if (AllZoneUtil.isCardInZone(p, c))
                return p;
        }
        return null;
    }

    /**
     * <p>getZone.</p>
     *
     * @param zone a {@link java.lang.String} object.
     * @param player a {@link forge.Player} object.
     * @return a {@link forge.PlayerZone} object.
     */
    public static PlayerZone getZone(String zone, Player player) {
        if (zone.equals("Stack")) player = null;
        Object o = getMap().get(zone + player);
        if (o == null)
            throw new RuntimeException("AllZone : getZone() invalid parameters " + zone + " " + player);

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
     *  <p>getDeckManager.</p>
     */
    public static DeckManager getDeckManager() {
    	return dMgr;
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
     * <p>Resets everything possible to set a new game</p>
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

    public static QuestMatchState getMatchState() {
        return matchState;
    }

    public static ColorChanger getColorChanger() {
        // TODO Auto-generated method stub
        return colorChanger;
    }
}//AllZone
