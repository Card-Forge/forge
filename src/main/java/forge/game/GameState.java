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
package forge.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import forge.GameLog;
import forge.StaticEffects;
import forge.card.replacement.ReplacementHandler;
import forge.card.trigger.TriggerHandler;
import forge.game.phase.Cleanup;
import forge.game.phase.Combat;
import forge.game.phase.EndOfCombat;
import forge.game.phase.EndOfTurn;
import forge.game.phase.PhaseHandler;
import forge.game.phase.Untap;
import forge.game.phase.Upkeep;
import forge.game.player.AIPlayer;
import forge.game.player.HumanPlayer;
import forge.game.player.Player;
import forge.game.zone.PlayerZone;
import forge.game.zone.MagicStack;
import forge.game.zone.ZoneType;

/**
 * Represents the state of a <i>single game</i> and is
 * "cleaned up" at each new game.
 */
public class GameState {

    /** The Constant HUMAN_PLAYER_NAME. */
    public static final String HUMAN_PLAYER_NAME = "Human";

    /** The Constant AI_PLAYER_NAME. */
    public static final String AI_PLAYER_NAME = "Computer";

    private final List<Player> players = new ArrayList<Player>(); 
    private final List<Player> roPlayers;
    private final Cleanup cleanup = new Cleanup();
    private final EndOfTurn endOfTurn = new EndOfTurn();
    private final EndOfCombat endOfCombat = new EndOfCombat();
    private final Untap untap = new Untap();
    private final Upkeep upkeep = new Upkeep();
    private final PhaseHandler phaseHandler = new PhaseHandler();
    private final MagicStack stack = new MagicStack();
    private final StaticEffects staticEffects = new StaticEffects();
    private final TriggerHandler triggerHandler = new TriggerHandler();
    private final ReplacementHandler replacementHandler = new ReplacementHandler();
    private Combat combat = new Combat();
    private final GameLog gameLog = new GameLog();
    private boolean gameOver = false;

    private final PlayerZone stackZone = new PlayerZone(ZoneType.Stack, null);

    private long timestamp = 0;
    private GameSummary gameSummary;

    /**
     * Constructor.
     */
    public GameState() { /* no more zones to map here */
        players.add(new HumanPlayer(GameState.HUMAN_PLAYER_NAME));
        players.add(new AIPlayer(GameState.AI_PLAYER_NAME));
        
        roPlayers = Collections.unmodifiableList(players);
    }

    /**
     * Gets the players.
     * 
     * @return the players
     */
    public final List<Player> getPlayers() {
        return roPlayers;
    }

    /**
     * Gets the cleanup step.
     * 
     * @return the cleanup step
     */
    public final Cleanup getCleanup() {
        return this.cleanup;
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
     * Gets the end of combat.
     * 
     * @return the endOfCombat
     */
    public final EndOfCombat getEndOfCombat() {
        return this.endOfCombat;
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
     * Gets the untap.
     * 
     * @return the upkeep
     */
    public final Untap getUntap() {
        return this.untap;
    }

    /**
     * Gets the phaseHandler.
     * 
     * @return the phaseHandler
     */
    public final PhaseHandler getPhaseHandler() {
        return this.phaseHandler;
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
     * Gets the static effects.
     * 
     * @return the staticEffects
     */
    public final StaticEffects getStaticEffects() {
        return this.staticEffects;
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
     * Gets the stack zone.
     * 
     * @return the stackZone
     */
    public final PlayerZone getStackZone() {
        return this.stackZone;
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
    public final GameSummary getGameSummary() {
        return this.gameSummary;
    }

    /**
     * Sets the game info.
     * 
     * @param summary0 {@link forge.game.GameSummary}
     */
    public final void setGameSummary(final GameSummary summary0) {
        this.gameSummary = summary0;
    }

    /**
     * @return the replacementHandler
     */
    public ReplacementHandler getReplacementHandler() {
        return replacementHandler;
    }

    /**
     * @return the gameOver
     */
    public boolean isGameOver() {
        return gameOver;
    }

    /**
     * @param go the gameOver to set
     */
    public void setGameOver(boolean go) {
        this.gameOver = go;
    }

}
