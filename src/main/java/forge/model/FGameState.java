/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.model;

import forge.AIPlayer;
import forge.Combat;
import forge.Constant;
import forge.Constant.Zone;
import forge.DefaultPlayerZone;
import forge.EndOfCombat;
import forge.EndOfTurn;
import forge.GameAction;
import forge.GameLog;
import forge.HumanPlayer;
import forge.MagicStack;
import forge.Phase;
import forge.Player;
import forge.PlayerZone;
import forge.StaticEffects;
import forge.Upkeep;
import forge.Untap;
import forge.card.replacement.ReplacementHandler;
import forge.card.trigger.TriggerHandler;
import forge.game.GameSummary;

/**
 * Represents the Forge Game State.
 */
public class FGameState {

    /** The Constant HUMAN_PLAYER_NAME. */
    public static final String HUMAN_PLAYER_NAME = "Human";

    /** The Constant AI_PLAYER_NAME. */
    public static final String AI_PLAYER_NAME = "Computer";

    private Player humanPlayer = new HumanPlayer(FGameState.HUMAN_PLAYER_NAME);
    private Player computerPlayer = new AIPlayer(FGameState.AI_PLAYER_NAME);
    private EndOfTurn endOfTurn = new EndOfTurn();
    private EndOfCombat endOfCombat = new EndOfCombat();
    private Untap untap = new Untap();
    private Upkeep upkeep = new Upkeep();
    private Phase phase = new Phase();
    private MagicStack stack = new MagicStack();
    private GameAction gameAction = new GameAction();
    private StaticEffects staticEffects = new StaticEffects();
    private TriggerHandler triggerHandler = new TriggerHandler();
    private ReplacementHandler replacementHandler = new ReplacementHandler();
    private Combat combat = new Combat();
    private GameLog gameLog = new GameLog();

    private PlayerZone stackZone = new DefaultPlayerZone(Constant.Zone.Stack, null);

    private long timestamp = 0;
    private GameSummary gameInfo;

    /**
     * Constructor.
     */
    public FGameState() { /* no more zones to map here */
    }

    /**
     * Gets the human player.
     * 
     * @return the humanPlayer
     */
    public final Player getHumanPlayer() {
        return this.humanPlayer;
    }

    /**
     * Sets the human player.
     * 
     * @param humanPlayer0
     *            the humanPlayer to set
     */
    protected final void setHumanPlayer(final Player humanPlayer0) {
        this.humanPlayer = humanPlayer0;
    }

    /**
     * Gets the computer player.
     * 
     * @return the computerPlayer
     */
    public final Player getComputerPlayer() {
        return this.computerPlayer;
    }

    /**
     * Sets the computer player.
     * 
     * @param computerPlayer0
     *            the computerPlayer to set
     */
    protected final void setComputerPlayer(final Player computerPlayer0) {
        this.computerPlayer = computerPlayer0;
    }

    /**
     * Gets the players.
     * 
     * @return the players
     */
    public final Player[] getPlayers() {
        return new Player[] { this.humanPlayer, this.computerPlayer };
    }

    /**
     * Gets the end of turn.
     * 
     * @return the endOfTurn
     */
    public final EndOfTurn getEndOfTurn() {
        return this.endOfTurn;
    }

    /**
     * Sets the end of turn.
     * 
     * @param endOfTurn0
     *            the endOfTurn to set
     */
    protected final void setEndOfTurn(final EndOfTurn endOfTurn0) {
        this.endOfTurn = endOfTurn0;
    }

    /**
     * Gets the end of combat.
     * 
     * @return the endOfCombat
     */
    public final EndOfCombat getEndOfCombat() {
        return this.endOfCombat;
    }

    /**
     * Sets the end of combat.
     * 
     * @param endOfCombat0
     *            the endOfCombat to set
     */
    protected final void setEndOfCombat(final EndOfCombat endOfCombat0) {
        this.endOfCombat = endOfCombat0;
    }

    /**
     * Gets the upkeep.
     * 
     * @return the upkeep
     */
    public final Upkeep getUpkeep() {
        return this.upkeep;
    }

    /**
     * Sets the upkeep.
     * 
     * @param upkeep0
     *            the upkeep to set
     */
    protected final void setUpkeep(final Upkeep upkeep0) {
        this.upkeep = upkeep0;
    }

    /**
     * Gets the untap.
     * 
     * @return the upkeep
     */
    public final Untap getUntap() {
        return this.untap;
    }

    /**
     * Sets the untap.
     * 
     * @param untap0
     *            the upkeep to set
     */
    protected final void setUntap(final Untap untap0) {
        this.untap = untap0;
    }

    /**
     * Gets the phase.
     * 
     * @return the phase
     */
    public final Phase getPhase() {
        return this.phase;
    }

    /**
     * Sets the phase.
     * 
     * @param phase0
     *            the phase to set
     */
    protected final void setPhase(final Phase phase0) {
        this.phase = phase0;
    }

    /**
     * Gets the stack.
     * 
     * @return the stack
     */
    public final MagicStack getStack() {
        return this.stack;
    }

    /**
     * Sets the stack.
     * 
     * @param stack0
     *            the stack to set
     */
    protected final void setStack(final MagicStack stack0) {
        this.stack = stack0;
    }

    /**
     * Gets the game action.
     * 
     * @return the gameAction
     */
    public final GameAction getGameAction() {
        return this.gameAction;
    }

    /**
     * Sets the game action.
     * 
     * @param gameAction0
     *            the gameAction to set
     */
    protected final void setGameAction(final GameAction gameAction0) {
        this.gameAction = gameAction0;
    }

    /**
     * Gets the static effects.
     * 
     * @return the staticEffects
     */
    public final StaticEffects getStaticEffects() {
        return this.staticEffects;
    }

    /**
     * Sets the static effects.
     * 
     * @param staticEffects0
     *            the staticEffects to set
     */
    protected final void setStaticEffects(final StaticEffects staticEffects0) {
        this.staticEffects = staticEffects0;
    }

    /**
     * Gets the trigger handler.
     * 
     * @return the triggerHandler
     */
    public final TriggerHandler getTriggerHandler() {
        return this.triggerHandler;
    }

    /**
     * Sets the trigger handler.
     * 
     * @param triggerHandler0
     *            the triggerHandler to set
     */
    protected final void setTriggerHandler(final TriggerHandler triggerHandler0) {
        this.triggerHandler = triggerHandler0;
    }

    /**
     * Gets the combat.
     * 
     * @return the combat
     */
    public final Combat getCombat() {
        return this.combat;
    }

    /**
     * Sets the combat.
     * 
     * @param combat0
     *            the combat to set
     */
    public final void setCombat(final Combat combat0) {
        this.combat = combat0;
    }

    /**
     * Gets the game log.
     * 
     * @return the game log
     */
    public final GameLog getGameLog() {
        return this.gameLog;
    }

    /**
     * Sets the game log.
     *
     * @param gl the new game log
     */
    public final void setgameLog(final GameLog gl) {
        this.gameLog = gl;
    }

    /**
     * Gets the stack zone.
     * 
     * @return the stackZone
     */
    public final PlayerZone getStackZone() {
        return this.stackZone;
    }

    /**
     * Sets the stack zone.
     * 
     * @param stackZone0
     *            the stackZone to set
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
        this.setTimestamp(this.getTimestamp() + 1);
        return this.getTimestamp();
    }

    /**
     * Gets the timestamp.
     * 
     * @return the timestamp
     */
    public final long getTimestamp() {
        return this.timestamp;
    }

    /**
     * Sets the timestamp.
     * 
     * @param timestamp0
     *            the timestamp to set
     */
    protected final void setTimestamp(final long timestamp0) {
        this.timestamp = timestamp0;
    }

    /**
     * Gets the game info.
     * 
     * @return the game info
     */
    public final GameSummary getGameInfo() {
        return this.gameInfo;
    }

    /**
     * Call this each time you start a new game, ok?.
     */
    public final void newGameCleanup() {
        this.gameInfo = new GameSummary(this.humanPlayer.getName(), this.computerPlayer.getName());

        this.getHumanPlayer().reset();
        this.getComputerPlayer().reset();

        this.getPhase().reset();
        this.getStack().reset();
        this.getCombat().reset();

        this.getGameLog().reset();

        for (final Player p : this.getPlayers()) {
            for (final Zone z : Player.ALL_ZONES) {
                p.getZone(z).reset();
            }
        }

        this.getStaticEffects().reset();
    }

    /**
     * @return the replacementHandler
     */
    public ReplacementHandler getReplacementHandler() {
        return replacementHandler;
    }

}
