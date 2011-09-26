package forge;


import java.util.Arrays;
import java.util.List;

import net.slightlymagic.braids.util.UtilFunctions;
import forge.Constant.Zone;
import forge.card.cardFactory.CardFactoryInterface;
import forge.card.cardFactory.PreloadingCardFactory;
import forge.card.trigger.TriggerHandler;
import forge.deck.DeckManager;
import forge.game.GameSummary;
import forge.gui.input.InputControl;
import forge.model.FGameState;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.quest.data.QuestMatchState;
import forge.quest.data.QuestData;
import forge.quest.gui.main.QuestChallenge;
import forge.quest.gui.main.QuestEventManager;

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

    /** Global <code>QuestChallenge</code>. */
    private static QuestChallenge questChallenge = null;
    
    /** Global <code>questEventManager</code>. */
    private static QuestEventManager questEventManager = null;

    /** Constant <code>NAME_CHANGER</code>. */
    private static final NameChanger NAME_CHANGER = new NameChanger();

    /** Constant <code>COLOR_CHANGER</code>. */
    private static final ColorChanger COLOR_CHANGER = new ColorChanger();

    // Phase is now a prerequisite for CardFactory
    /** Global <code>cardFactory</code>. */
    private static CardFactoryInterface cardFactory = null;

    /** Constant <code>inputControl</code>. */
    private static final InputControl INPUT_CONTROL = new InputControl(Singletons.getModel());

    /** 
     * Match State for challenges are stored in a <code>QuestMatchState</code> class instance.
     */
    private static QuestMatchState matchState = new QuestMatchState();

    //initialized at Runtime since it has to be the last object constructed


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
     * get a list of all players participating in this game.
     *
     * @return a list of all player participating in this game
     */
    public static List<Player> getPlayersInGame() {
        return Arrays.asList(Singletons.getModel().getGameState().getPlayers());
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
     * <p>getQuestChallenge.</p>
     *
     * @return a {@link forge.Quest_Assignment} object.
     * @since 1.0.15
     */
    public static QuestChallenge getQuestChallenge() {
        return questChallenge;
    }

    /**
     * <p>setQuestChallenge.</p>
     *
     * @param q 
     */
    public static void setQuestChallenge(final QuestChallenge q) {
        questChallenge = q;
    }
    
    /**
     * <p>getQuestEvents.</p>
     *
     * @return a QuestChallenge object.
     * @since 1.0.15
     */
    public static QuestEventManager getQuestEventManager() {
        return questEventManager;
    }

    /**
     * <p>setQuestEvents.</p>
     *
     * @param q 
     */
    public static void setQuestEventManager(final QuestEventManager qem) {
        questEventManager = qem;
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
        return Singletons.getModel().getGameState().getGameInfo();
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
     * <p>getZone.</p>
     *
     * @param c a {@link forge.Card} object.
     * @return a {@link forge.PlayerZone} object.
     */
    public static PlayerZone getZoneOf(final Card c) {
        final FGameState gameState = Singletons.getModel().getGameState();
        if (gameState == null) { return null; }

        if (gameState.getStackZone().contains(c)) {
            return gameState.getStackZone();
        }

        for (Player p : gameState.getPlayers()) {
            for(Zone z : Player.ALL_ZONES) {
                PlayerZone pz = p.getZone(z);
                if (pz.contains(c))
                    return pz;
            }
        }

        return null;
    }

    /**
     * <p>resetZoneMoveTracking.</p>
     */
    public static void resetZoneMoveTracking() {
        final FGameState gameState = Singletons.getModel().getGameState();
        if (gameState == null) { return; }
        for (Player p : gameState.getPlayers()) {
            for(Zone z : Player.ALL_ZONES) {
                p.getZone(z).resetCardsAddedThisTurn();
            }
        }
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
        Singletons.getModel().getGameState().newGameCleanup();
        
        getDisplay().showCombat("");
        getDisplay().loadPrefs();

        getInputControl().clearInput();

        getColorChanger().reset();
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
