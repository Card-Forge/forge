package forge;


import forge.card.cardFactory.CardFactoryInterface;
import forge.card.cardFactory.PreloadingCardFactory;
import forge.card.mana.ManaPool;
import forge.card.trigger.TriggerHandler;
import forge.deck.DeckManager;
import forge.game.GameSummary;
import forge.gui.input.InputControl;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.quest.data.QuestMatchState;

import java.util.HashMap;
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

    /** Constant <code>HumanPlayer</code> */
    private static final Player HumanPlayer = new HumanPlayer("Human");
    /** Constant <code>ComputerPlayer</code> */
    private static final Player ComputerPlayer = new AIPlayer("Computer");

    /** Constant <code>QuestData</code> */
    private static forge.quest.data.QuestData QuestData = null;
    /** Constant <code>QuestAssignment</code> */
    private static Quest_Assignment QuestAssignment = null;
    /** Constant <code>NameChanger</code> */
    private static final NameChanger NameChanger = new NameChanger();
    /** Constant <code>ColorChanger</code> */
    private static final ColorChanger colorChanger = new ColorChanger();

    /** Constant <code>EndOfTurn</code> */
    private static EndOfTurn EndOfTurn = new EndOfTurn();
    /** Constant <code>EndOfCombat</code> */
    private static EndOfCombat EndOfCombat = new EndOfCombat();
    private static Upkeep Upkeep = new Upkeep();

    /** Constant <code>Phase</code> */
    private static final Phase Phase = new Phase();

    // Phase is now a prerequisite for CardFactory
    /** Constant <code>CardFactory</code> */
    private static CardFactoryInterface cardFactory = null;

    /** Constant <code>Stack</code> */
    private static final MagicStack Stack = new MagicStack();
    /** Constant <code>InputControl</code> */
    private static final InputControl InputControl = new InputControl();
    /** Constant <code>GameAction</code> */
    private static final GameAction GameAction = new GameAction();
    /** Constant <code>StaticEffects</code> */
    private static final StaticEffects StaticEffects = new StaticEffects();
    
    /** Game state observer <code>GameSummary</code> collects statistics and players' performance*/
    private static GameSummary gameInfo = new GameSummary();
    /** Match State for quests are stored in a <code>QuestMatchState</code> class instance*/
    public static QuestMatchState matchState = new QuestMatchState();

    /** Constant <code>TriggerHandler</code> */
    private static final TriggerHandler TriggerHandler = new TriggerHandler();

    //initialized at Runtime since it has to be the last object constructed

    /** Constant <code>Computer</code> */
    private static ComputerAI_Input Computer;

    //shared between Input_Attack, Input_Block, Input_CombatDamage , InputState_Computer

    /** Constant <code>Combat</code> */
    private static Combat Combat = new Combat();

    //Human_Play, Computer_Play is different because Card.comesIntoPlay() is called when a card is added by PlayerZone.add(Card)
    /** Constant <code>Human_Battlefield</code> */
    private final static PlayerZone Human_Battlefield = new PlayerZone_ComesIntoPlay(Constant.Zone.Battlefield, AllZone.getHumanPlayer());
    /** Constant <code>Human_Hand</code> */
    private final static PlayerZone Human_Hand = new DefaultPlayerZone(Constant.Zone.Hand, AllZone.getHumanPlayer());
    /** Constant <code>Human_Graveyard</code> */
    private final static PlayerZone Human_Graveyard = new DefaultPlayerZone(Constant.Zone.Graveyard, AllZone.getHumanPlayer());
    /** Constant <code>Human_Library</code> */
    private final static PlayerZone Human_Library = new DefaultPlayerZone(Constant.Zone.Library, AllZone.getHumanPlayer());
    /** Constant <code>Human_Exile</code> */
    private final static PlayerZone Human_Exile = new DefaultPlayerZone(Constant.Zone.Exile, AllZone.getHumanPlayer());
    /** Constant <code>Human_Command</code> */
    private final static PlayerZone Human_Command = new DefaultPlayerZone(Constant.Zone.Command, AllZone.getHumanPlayer());

    /** Constant <code>Computer_Battlefield</code> */
    private final static PlayerZone Computer_Battlefield = new PlayerZone_ComesIntoPlay(Constant.Zone.Battlefield, AllZone.getComputerPlayer());
    /** Constant <code>Computer_Hand</code> */
    private final static PlayerZone Computer_Hand = new DefaultPlayerZone(Constant.Zone.Hand, AllZone.getComputerPlayer());
    /** Constant <code>Computer_Graveyard</code> */
    private final static PlayerZone Computer_Graveyard = new DefaultPlayerZone(Constant.Zone.Graveyard, AllZone.getComputerPlayer());
    /** Constant <code>Computer_Library</code> */
    private final static PlayerZone Computer_Library = new DefaultPlayerZone(Constant.Zone.Library, AllZone.getComputerPlayer());
    /** Constant <code>Computer_Exile</code> */
    private final static PlayerZone Computer_Exile = new DefaultPlayerZone(Constant.Zone.Exile, AllZone.getComputerPlayer());
    /** Constant <code>Computer_Command</code> */
    private final static PlayerZone Computer_Command = new DefaultPlayerZone(Constant.Zone.Command, AllZone.getComputerPlayer());

    /** Constant <code>Stack_Zone</code> */
    private final static PlayerZone Stack_Zone = new DefaultPlayerZone(Constant.Zone.Stack, null);

    /** Constant <code>Display</code> */
    private static Display Display;

    /** Constant <code>map</code> */
    private final static Map<String, PlayerZone> map = new HashMap<String, PlayerZone>();

    static {
        map.put(Constant.Zone.Graveyard + AllZone.getHumanPlayer(), Human_Graveyard);
        map.put(Constant.Zone.Hand + AllZone.getHumanPlayer(), Human_Hand);
        map.put(Constant.Zone.Library + AllZone.getHumanPlayer(), Human_Library);
        map.put(Constant.Zone.Battlefield + AllZone.getHumanPlayer(), Human_Battlefield);
        map.put(Constant.Zone.Exile + AllZone.getHumanPlayer(), Human_Exile);
        map.put(Constant.Zone.Command + AllZone.getHumanPlayer(), Human_Command);

        map.put(Constant.Zone.Graveyard + AllZone.getComputerPlayer(), Computer_Graveyard);
        map.put(Constant.Zone.Hand + AllZone.getComputerPlayer(), Computer_Hand);
        map.put(Constant.Zone.Library + AllZone.getComputerPlayer(), Computer_Library);
        map.put(Constant.Zone.Battlefield + AllZone.getComputerPlayer(), Computer_Battlefield);
        map.put(Constant.Zone.Exile + AllZone.getComputerPlayer(), Computer_Exile);
        map.put(Constant.Zone.Command + AllZone.getComputerPlayer(), Computer_Command);

        map.put(Constant.Zone.Stack + null, Stack_Zone);
    }
    
    private static long timestamp = 0;
    
    /** Constant <code>DeckManager</code> */
    private final static DeckManager dMgr = new DeckManager(ForgeProps.getFile(NEW_DECKS));

    /**
     * <p>getHumanPlayer.</p>
     *
     * @return a {@link forge.Player} object.
     * @since 1.0.15
     */
    public static Player getHumanPlayer() {
        return HumanPlayer;
    }

    /**
     * <p>getComputerPlayer.</p>
     *
     * @return a {@link forge.Player} object.
     * @since 1.0.15
     */
    public static Player getComputerPlayer() {
        return ComputerPlayer;
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
     * @return a {@link forge.EndOfTurn} object.
     * @since 1.0.15
     */
    public static EndOfTurn getEndOfTurn() {
        return EndOfTurn;
    }

    /**
     * <p>getEndOfCombat.</p>
     *
     * @return a {@link forge.EndOfCombat} object.
     * @since 1.0.15
     */
    public static forge.EndOfCombat getEndOfCombat() {
        return EndOfCombat;
    }
    
    /**
     * <p>getUpkeep.</p>
     *
     * @return a {@link forge.EndOfCombat} object.
     * @since 1.0.16
     */
    public static forge.Upkeep getUpkeep() {
        return Upkeep;
    }

    /**
     * <p>getPhase.</p>
     *
     * @return a {@link forge.Phase} object.
     * @since 1.0.15
     */
    public static Phase getPhase() {
        return Phase;
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
     * @return a {@link forge.MagicStack} object.
     * @since 1.0.15
     */
    public static MagicStack getStack() {
        return Stack;
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
     * @return a {@link forge.GameAction} object.
     * @since 1.0.15
     */
    public static GameAction getGameAction() {
        return GameAction;
    }

    /**
     * <p>getStaticEffects.</p>
     *
     * @return a {@link forge.StaticEffects} object.
     * @since 1.0.15
     */
    public static StaticEffects getStaticEffects() {
        return StaticEffects;
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
     * @return a {@link forge.card.trigger.TriggerHandler} object.
     * @since 1.0.15
     */
    public static TriggerHandler getTriggerHandler() {
        return TriggerHandler;
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
     * @return a {@link forge.Combat} object.
     * @since 1.0.15
     */
    public static Combat getCombat() {
        return Combat;
    }

    /**
     * <p>setCombat.</p>
     *
     * @param attackers a {@link forge.Combat} object.
     * @since 1.0.15
     */
    public static void setCombat(Combat attackers) {
        Combat = attackers;
    }

    //Human_Play, Computer_Play is different because Card.comesIntoPlay() is called when a card is added by PlayerZone.add(Card)
    /**
     * <p>getHumanBattlefield.</p>
     *
     * @return a {@link forge.PlayerZone} object.
     * @since 1.0.15
     */
    public static PlayerZone getHumanBattlefield() {
        return Human_Battlefield;
    }

    /**
     * <p>getHumanHand.</p>
     *
     * @return a {@link forge.PlayerZone} object.
     * @since 1.0.15
     */
    public static PlayerZone getHumanHand() {
        return Human_Hand;
    }

    /**
     * <p>getHumanGraveyard.</p>
     *
     * @return a {@link forge.PlayerZone} object.
     * @since 1.0.15
     */
    public static PlayerZone getHumanGraveyard() {
        return Human_Graveyard;
    }

    /**
     * <p>getHumanLibrary.</p>
     *
     * @return a {@link forge.PlayerZone} object.
     * @since 1.0.15
     */
    public static PlayerZone getHumanLibrary() {
        return Human_Library;
    }

    /**
     * <p>getHumanExile.</p>
     *
     * @return a {@link forge.PlayerZone} object.
     * @since 1.0.15
     */
    public static PlayerZone getHumanExile() {
        return Human_Exile;
    }

    /**
     * <p>getHumanCommand.</p>
     *
     * @return a {@link forge.PlayerZone} object.
     * @since 1.0.15
     */
    public static PlayerZone getHumanCommand() {
        return Human_Command;
    }

    /**
     * <p>getComputerBattlefield.</p>
     *
     * @return a {@link forge.PlayerZone} object.
     * @since 1.0.15
     */
    public static PlayerZone getComputerBattlefield() {
        return Computer_Battlefield;
    }

    /**
     * <p>getComputerHand.</p>
     *
     * @return a {@link forge.PlayerZone} object.
     * @since 1.0.15
     */
    public static PlayerZone getComputerHand() {
        return Computer_Hand;
    }

    /**
     * <p>getComputerGraveyard.</p>
     *
     * @return a {@link forge.PlayerZone} object.
     * @since 1.0.15
     */
    public static PlayerZone getComputerGraveyard() {
        return Computer_Graveyard;
    }

    /**
     * <p>getComputerLibrary.</p>
     *
     * @return a {@link forge.PlayerZone} object.
     * @since 1.0.15
     */
    public static PlayerZone getComputerLibrary() {
        return Computer_Library;
    }

    /**
     * <p>getComputerExile.</p>
     *
     * @return a {@link forge.PlayerZone} object.
     * @since 1.0.15
     */
    public static PlayerZone getComputerExile() {
        return Computer_Exile;
    }

    /**
     * <p>getComputerCommand.</p>
     *
     * @return a {@link forge.PlayerZone} object.
     * @since 1.0.15
     */
    public static PlayerZone getComputerCommand() {
        return Computer_Command;
    }

    /**
     * <p>getStackZone.</p>
     *
     * @return a {@link forge.PlayerZone} object.
     * @since 1.0.15
     */
    public static PlayerZone getStackZone() {
        return Stack_Zone;
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
     * @return a {@link java.util.Map} object.
     */
    private static Map<String, PlayerZone> getMap() {
        return map;
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
    
    public static long getNextTimestamp() {
    	timestamp++;
    	return timestamp;
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
