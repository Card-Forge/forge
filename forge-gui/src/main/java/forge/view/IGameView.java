package forge.view;

import java.util.List;
import java.util.Observer;

import forge.LobbyPlayer;
import forge.card.MagicColor;
import forge.deck.Deck;
import forge.game.GameLog;
import forge.game.GameLogEntry;
import forge.game.GameLogEntryType;
import forge.game.GameOutcome;
import forge.game.GameType;
import forge.game.phase.PhaseType;
import forge.match.input.Input;
import forge.match.input.InputConfirm;
import forge.player.PlayerControllerHuman.DevModeCheats;
import forge.util.ITriggerEvent;

/**
 * Interface providing access to a game from a client. The access is assumed to
 * be based on a single player.
 * 
 * @author elcnesh
 */
public interface IGameView {

    // Game-related methods
    public abstract boolean isCommander();
    public abstract GameType getGameType();
    public abstract int getTurnNumber();
    public abstract boolean isCommandZoneNeeded();
    public abstract boolean isWinner(LobbyPlayer p);
    public abstract LobbyPlayer getWinningPlayer();
    public abstract int getWinningTeam();

    // Match-related methods
    public abstract boolean isFirstGameInMatch();
    public abstract boolean isMatchOver();
    public abstract int getNumGamesInMatch();
    public abstract int getNumPlayedGamesInMatch();
    public abstract Iterable<GameOutcome> getOutcomesOfMatch();
    public abstract boolean isMatchWonBy(LobbyPlayer p);
    public abstract int getGamesWonBy(LobbyPlayer p);
    public abstract GameOutcome.AnteResult getAnteResult();
    public abstract Deck getDeck(LobbyPlayer player);

    public abstract boolean isGameOver();
    public abstract void updateAchievements();

    public abstract int getPoisonCountersToLose();

    public abstract void subscribeToEvents(Object subscriber);

    /**
     * Get the game's current combat.
     * 
     * @return a representation of the combat, or {@code null} if there is
     *         currently no ongoing combat.
     */
    public abstract CombatView getCombat();

    /**
     * Add an observer to the game log.
     * 
     * @param o
     *            the {@link Observer} to be sent log updates.
     * @see GameLog
     */
    public abstract void addLogObserver(Observer o);

    /**
     * Get all log entries up to a certain level.
     * 
     * @param maxLogLevel
     *            the maximum {@link GameLogEntryType} of log entries to return.
     * @return a list of {@link GameLogEntry}.
     * @see GameLog
     * @see GameLogEntry
     * @see GameLogEntryType
     */
    public abstract List<GameLogEntry> getLogEntries(GameLogEntryType maxLogLevel);

    /**
     * Get all log entries of a certain level.
     * 
     * @param logLevel
     *            the {@link GameLogEntryType} of log entries to return.
     * @return a list of {@link GameLogEntry}, each of which is of type
     *         logLevel.
     * @see GameLog
     * @see GameLogEntry
     * @see GameLogEntryType
     */
    public abstract List<GameLogEntry> getLogEntriesExact(GameLogEntryType logLevel);

    // Input controls
    /**
     * @return {@code true} if and only if the last action performed by this
     *         player can be undone.
     */
    public abstract boolean canUndoLastAction();

    /**
     * Try to undo the last action performed by this player.
     * 
     * @return {@code true} if and only if the action was successfully reverted.
     * @see IGameView#canUndoLastAction()
     */
    public abstract boolean tryUndoLastAction();

    /**
     * Have this player perform the action currently associated with pressing
     * the "OK" button.
     */
    public abstract void selectButtonOk();

    /**
     * Have this player perform the action currently associated with pressing
     * the "Cancel" button.
     */
    public abstract void selectButtonCancel();

    /**
     * Have this player press the "OK" button if and only if the top
     * {@link Input} is an instance of {@link InputConfirm}.
     */
    public abstract void confirm();

    /**
     * Have this player pass priority.
     * @return whether priority was successfully passed.
     */
    public abstract boolean passPriority();

    /**
     * Have this player silently pass priority until the end of the current
     * turn, unless an event occurs.
     * 
     * @return whether priority was successfully passed.
     */
    public abstract boolean passPriorityUntilEndOfTurn();

    /**
     * If possible, have this player use one mana of the specified kind.
     * 
     * @param mana
     *            the mana to spend.
     * @see MagicColor
     */
    public abstract void useMana(byte mana);

    /**
     * If possible, have this player select a player.
     * 
     * @param player
     *            the player to select.
     * @param triggerEvent
     *            the event used to select the player.
     */
    public abstract void selectPlayer(PlayerView player, ITriggerEvent triggerEvent);

    /**
     * If possible, have this player select a card.
     * 
     * @param card
     *            the card to select.
     * @param triggerEvent
     *            the event used to select the card.
     */
    public abstract boolean selectCard(CardView card, ITriggerEvent triggerEvent);

    /**
     * If possible, have this player select a spellability.
     * 
     * @param sa
     *            the spellability to select.
     * @param triggerEvent
     *            the event used to select the spellability.
     */
    public abstract void selectAbility(SpellAbilityView sa);

    /**
     * If possible, have this player attack with as many creatures that can do
     * so.
     */
    public abstract void alphaStrike();

    /**
     * Ask for a list containing the players involved in this game.
     * 
     * @return a list of players.
     */
    public abstract List<PlayerView> getPlayers();

    /**
     * Get the player whose turn it currently is.
     * 
     * @return the player whose turn it is, or {@code null} if it is no-one's
     *         turn (eg. because the current player has lost the game this
     *         turn).
     */
    public abstract PlayerView getPlayerTurn();

    /**
     * @return the current phase the game is in.
     */
    public abstract PhaseType getPhase();

    /**
     * @return a list of items currently on the stack.
     */
    public abstract List<StackItemView> getStack();

    /**
     * @return the top item on the stack, or {@code null} if the stack is empty.
     */
    public abstract StackItemView peekStack();

    /**
     * @param c
     *            a card.
     * @return whether this player may view the specified card.
     */
    public abstract boolean mayShowCard(CardView c);

    /**
     * @param c
     *            a card.
     * @return whether this player may view the front card face of the specified
     *         card.
     */
    public abstract boolean mayShowCardFace(CardView c);

    // Auto-yield related methods
    public abstract Iterable<String> getAutoYields();
    public abstract boolean shouldAutoYield(String key);
    public abstract void setShouldAutoYield(String key, boolean autoYield);
    public abstract boolean getDisableAutoYields();
    public abstract void setDisableAutoYields(boolean b);

    public abstract boolean shouldAlwaysAcceptTrigger(Integer trigger);
    public abstract boolean shouldAlwaysDeclineTrigger(Integer trigger);
    public abstract boolean shouldAlwaysAskTrigger(Integer trigger);

    public abstract void setShouldAlwaysAcceptTrigger(Integer trigger);
    public abstract void setShouldAlwaysDeclineTrigger(Integer trigger);
    public abstract void setShouldAlwaysAskTrigger(Integer trigger);

    /**
     * Request cancellation of a previous request to pass priority
     * automatically.
     */
    public abstract void autoPassCancel();
    
    public abstract boolean canPlayUnlimitedLands();

    //method used to wrap all functions that cheat
    public abstract DevModeCheats cheat();
}