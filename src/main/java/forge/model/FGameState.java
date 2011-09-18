package forge.model;

import java.util.HashMap;
import java.util.Map;

import forge.AIPlayer;
import forge.Combat;
import forge.Constant;
import forge.DefaultPlayerZone;
import forge.EndOfCombat;
import forge.EndOfTurn;
import forge.GameAction;
import forge.HumanPlayer;
import forge.MagicStack;
import forge.Phase;
import forge.Player;
import forge.PlayerZone;
import forge.PlayerZone_ComesIntoPlay;
import forge.StaticEffects;
import forge.Upkeep;
import forge.card.trigger.TriggerHandler;

/**
 * Represents the Forge Game State.
 */
public class FGameState {
    private Player humanPlayer = new HumanPlayer("Human");
    private Player computerPlayer = new AIPlayer("Computer");
    private EndOfTurn endOfTurn = new EndOfTurn();
    private EndOfCombat endOfCombat = new EndOfCombat();
    private Upkeep upkeep = new Upkeep();
    private Phase phase = new Phase();
    private MagicStack stack = new MagicStack();
    private GameAction gameAction = new GameAction();
    private StaticEffects staticEffects = new StaticEffects();
    private TriggerHandler triggerHandler = new TriggerHandler();
    private Combat combat = new Combat();


    private PlayerZone stackZone = new DefaultPlayerZone(Constant.Zone.Stack, null);

    private long timestamp = 0;

    /**
     * Constructor.
     */
    public FGameState() { /* no more zones to map here */ }


    /**
     * @return the humanPlayer
     */
    public final Player getHumanPlayer() {
        return humanPlayer;
    }


    /**
     * @param humanPlayer0 the humanPlayer to set
     */
    protected final void setHumanPlayer(final Player humanPlayer0) {
        this.humanPlayer = humanPlayer0;
    }


    /**
     * @return the computerPlayer
     */
    public final Player getComputerPlayer() {
        return computerPlayer;
    }


    /**
     * @param computerPlayer0 the computerPlayer to set
     */
    protected final void setComputerPlayer(final Player computerPlayer0) {
        this.computerPlayer = computerPlayer0;
    }


    /**
     * @return the endOfTurn
     */
    public final EndOfTurn getEndOfTurn() {
        return endOfTurn;
    }


    /**
     * @param endOfTurn0 the endOfTurn to set
     */
    protected final void setEndOfTurn(final EndOfTurn endOfTurn0) {
        this.endOfTurn = endOfTurn0;
    }


    /**
     * @return the endOfCombat
     */
    public final EndOfCombat getEndOfCombat() {
        return endOfCombat;
    }


    /**
     * @param endOfCombat0 the endOfCombat to set
     */
    protected final void setEndOfCombat(final EndOfCombat endOfCombat0) {
        this.endOfCombat = endOfCombat0;
    }


    /**
     * @return the upkeep
     */
    public final Upkeep getUpkeep() {
        return upkeep;
    }


    /**
     * @param upkeep0 the upkeep to set
     */
    protected final void setUpkeep(final Upkeep upkeep0) {
        this.upkeep = upkeep0;
    }


    /**
     * @return the phase
     */
    public final Phase getPhase() {
        return phase;
    }


    /**
     * @param phase0 the phase to set
     */
    protected final void setPhase(final Phase phase0) {
        this.phase = phase0;
    }


    /**
     * @return the stack
     */
    public final MagicStack getStack() {
        return stack;
    }


    /**
     * @param stack0 the stack to set
     */
    protected final void setStack(final MagicStack stack0) {
        this.stack = stack0;
    }


    /**
     * @return the gameAction
     */
    public final GameAction getGameAction() {
        return gameAction;
    }


    /**
     * @param gameAction0 the gameAction to set
     */
    protected final void setGameAction(final GameAction gameAction0) {
        this.gameAction = gameAction0;
    }


    /**
     * @return the staticEffects
     */
    public final StaticEffects getStaticEffects() {
        return staticEffects;
    }


    /**
     * @param staticEffects0 the staticEffects to set
     */
    protected final void setStaticEffects(final StaticEffects staticEffects0) {
        this.staticEffects = staticEffects0;
    }


    /**
     * @return the triggerHandler
     */
    public final TriggerHandler getTriggerHandler() {
        return triggerHandler;
    }


    /**
     * @param triggerHandler0 the triggerHandler to set
     */
    protected final void setTriggerHandler(final TriggerHandler triggerHandler0) {
        this.triggerHandler = triggerHandler0;
    }


    /**
     * @return the combat
     */
    public final Combat getCombat() {
        return combat;
    }


    /**
     * @param combat0 the combat to set
     */
    public final void setCombat(final Combat combat0) {
        this.combat = combat0;
    }


    /**
     * @return the stackZone
     */
    public final PlayerZone getStackZone() {
        return stackZone;
    }


    /**
     * @param stackZone0 the stackZone to set
     */
    protected final void setStackZone(final PlayerZone stackZone0) {
        this.stackZone = stackZone0;
    }

    /**
     * Create and return the next timestamp.
     * 
     * @return the next timestamp
     */
    public final long getNextTimestamp() {
        setTimestamp(getTimestamp() + 1);
        return getTimestamp();
    }


    /**
     * @return the timestamp
     */
    public final long getTimestamp() {
        return timestamp;
    }


    /**
     * @param timestamp0 the timestamp to set
     */
    protected final void setTimestamp(final long timestamp0) {
        this.timestamp = timestamp0;
    }

}
