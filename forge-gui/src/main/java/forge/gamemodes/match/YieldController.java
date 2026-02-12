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
package forge.gamemodes.match;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.trackable.TrackableTypes;
import forge.util.Localizer;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manages yield state and logic for the experimental yield system.
 * Handles automatic priority passing, interrupt conditions, and smart suggestions.
 *
 * This class is GUI-layer only and does not modify game state or network protocol.
 * Each client manages its own yield state independently.
 */
public class YieldController {

    /**
     * Callback interface for GUI updates and game state access.
     */
    public interface YieldCallback {
        void showPromptMessage(PlayerView player, String message);
        void updateButtons(PlayerView player, boolean ok, boolean cancel, boolean focusOk);
        void awaitNextInput();
        void cancelAwaitNextInput();
        GameView getGameView();
        /**
         * Sync yield mode to network client.
         * Called when yield mode is cleared due to end condition.
         */
        void syncYieldModeToClient(PlayerView player, YieldMode mode);
    }

    private final YieldCallback callback;

    // Legacy auto-pass tracking
    private final Set<PlayerView> autoPassUntilEndOfTurn = Sets.newHashSet();

    /**
     * Consolidated yield state for a player.
     * Tracks mode and all mode-specific timing data.
     */
    private static class YieldState {
        YieldMode mode;
        Integer startTurn;                          // For UNTIL_END_OF_TURN, UNTIL_BEFORE_COMBAT, UNTIL_END_STEP
        Boolean startedAtOrAfterPhase;              // For UNTIL_BEFORE_COMBAT and UNTIL_END_STEP
        forge.game.phase.PhaseType startPhase;      // For UNTIL_NEXT_PHASE
        Boolean startedDuringOurTurn;               // For UNTIL_YOUR_NEXT_TURN

        YieldState(YieldMode mode) {
            this.mode = mode;
        }
    }

    // Extended yield mode tracking (experimental feature)
    private final Map<PlayerView, YieldState> yieldStates = Maps.newHashMap();

    // Smart suggestion decline tracking (reset each turn)
    private final Map<PlayerView, Set<String>> declinedSuggestionsThisTurn = Maps.newHashMap();
    private final Map<PlayerView, Integer> declinedSuggestionsTurn = Maps.newHashMap();

    // Track when yield just ended this priority (to suppress suggestions)
    private final Set<PlayerView> yieldJustEnded = Sets.newHashSet();

    /**
     * Create a new YieldController with the given callback for GUI updates.
     * @param callback the callback interface for GUI operations
     */
    public YieldController(YieldCallback callback) {
        this.callback = callback;
    }

    /**
     * Automatically pass priority until reaching the Cleanup phase of the current turn.
     * This is the legacy auto-pass behavior.
     */
    public void autoPassUntilEndOfTurn(PlayerView player) {
        player = TrackableTypes.PlayerViewType.lookup(player); // ensure consistent PlayerView instance
        autoPassUntilEndOfTurn.add(player);
    }

    /**
     * Cancel auto-pass for the given player.
     */
    public void autoPassCancel(PlayerView player) {
        player = TrackableTypes.PlayerViewType.lookup(player); // ensure consistent PlayerView instance
        if (!autoPassUntilEndOfTurn.remove(player)) {
            return;
        }

        // Prevent prompt getting stuck on yielding message while actually waiting for next input opportunity
        callback.showPromptMessage(player, "");
        callback.updateButtons(player, false, false, false);
        callback.awaitNextInput();
    }

    /**
     * Check if auto-pass is active for the given player (legacy or experimental).
     */
    public boolean mayAutoPass(PlayerView player) {
        player = TrackableTypes.PlayerViewType.lookup(player);
        // Check legacy auto-pass first
        if (autoPassUntilEndOfTurn.contains(player)) {
            return true;
        }
        // Check experimental yield system
        return shouldAutoYieldForPlayer(player);
    }

    /**
     * Update the prompt message to show current yield status.
     */
    public void updateAutoPassPrompt(PlayerView player) {
        player = TrackableTypes.PlayerViewType.lookup(player);

        // Check legacy auto-pass first
        if (autoPassUntilEndOfTurn.contains(player)) {
            callback.cancelAwaitNextInput();
            String cancelKey = getCancelShortcutDisplayText();
            callback.showPromptMessage(player, Localizer.getInstance().getMessage("lblYieldingUntilEndOfTurn", cancelKey));
            callback.updateButtons(player, false, true, false);
            return;
        }

        // Check experimental yield modes
        YieldState state = yieldStates.get(player);
        if (state != null && state.mode != null && state.mode != YieldMode.NONE) {
            YieldMode mode = state.mode;
            callback.cancelAwaitNextInput();
            Localizer loc = Localizer.getInstance();
            String cancelKey = getCancelShortcutDisplayText();
            String message = switch (mode) {
                case UNTIL_NEXT_PHASE -> loc.getMessage("lblYieldingUntilNextPhase", cancelKey);
                case UNTIL_STACK_CLEARS -> loc.getMessage("lblYieldingUntilStackClears", cancelKey);
                case UNTIL_END_OF_TURN -> loc.getMessage("lblYieldingUntilEndOfTurn", cancelKey);
                case UNTIL_YOUR_NEXT_TURN -> loc.getMessage("lblYieldingUntilYourNextTurn", cancelKey);
                case UNTIL_BEFORE_COMBAT -> loc.getMessage("lblYieldingUntilBeforeCombat", cancelKey);
                case UNTIL_END_STEP -> loc.getMessage("lblYieldingUntilEndStep", cancelKey);
                default -> "";
            };
            callback.showPromptMessage(player, message);
            callback.updateButtons(player, false, true, false);
        }
    }

    /**
     * Set the yield mode for a player.
     */
    public void setYieldMode(PlayerView player, final YieldMode mode) {
        player = TrackableTypes.PlayerViewType.lookup(player); // ensure we use the correct player instance
        if (!isYieldExperimentalEnabled()) {
            // Fall back to legacy behavior for UNTIL_END_OF_TURN
            if (mode == YieldMode.UNTIL_END_OF_TURN) {
                autoPassUntilEndOfTurn.add(player);
            }
            return;
        }

        if (mode == YieldMode.NONE) {
            clearYieldMode(player);
            return;
        }

        // Clear any legacy auto-pass state to prevent interference
        // (legacy check in shouldAutoYieldForPlayer runs first and would override experimental mode)
        autoPassUntilEndOfTurn.remove(player);

        YieldState state = new YieldState(mode);
        yieldStates.put(player, state);

        GameView gameView = callback.getGameView();

        // Use network-safe GameView properties instead of gameView.getGame()
        // This ensures proper operation for non-host players in multiplayer
        if (gameView == null) {
            return;
        }

        forge.game.phase.PhaseType phase = gameView.getPhase();
        int currentTurn = gameView.getTurn();
        PlayerView currentPlayerTurn = gameView.getPlayerTurn();

        // Track mode-specific state
        switch (mode) {
            case UNTIL_NEXT_PHASE:
                state.startPhase = phase;
                break;
            case UNTIL_END_OF_TURN:
                state.startTurn = currentTurn;
                break;
            case UNTIL_BEFORE_COMBAT:
                state.startTurn = currentTurn;
                state.startedAtOrAfterPhase = isAtOrAfterCombat(phase);
                break;
            case UNTIL_END_STEP:
                state.startTurn = currentTurn;
                state.startedAtOrAfterPhase = isAtOrAfterEndStep(phase);
                break;
            case UNTIL_YOUR_NEXT_TURN:
                state.startedDuringOurTurn = currentPlayerTurn != null && currentPlayerTurn.equals(player);
                break;
            default:
                break;
        }
    }

    /**
     * Clear yield mode for a player.
     */
    public void clearYieldMode(PlayerView player) {
        player = TrackableTypes.PlayerViewType.lookup(player); // ensure we use the correct player instance
        clearYieldModeInternal(player);

        callback.showPromptMessage(player, "");
        callback.updateButtons(player, false, false, false);
        callback.awaitNextInput();

        // Notify client to update its local yield state (for network play)
        callback.syncYieldModeToClient(player, YieldMode.NONE);
    }

    /**
     * Set yield mode silently without triggering callbacks.
     * Used when receiving sync from server to avoid recursive loops.
     * Only sets the mode itself - server manages the detailed tracking state.
     */
    public void setYieldModeSilent(PlayerView player, YieldMode mode) {
        player = TrackableTypes.PlayerViewType.lookup(player);
        if (mode == null || mode == YieldMode.NONE) {
            clearYieldModeInternal(player);
            return;
        }
        // Clear legacy auto-pass to prevent interference
        autoPassUntilEndOfTurn.remove(player);
        // Just set the mode - detailed tracking is managed by server
        yieldStates.put(player, new YieldState(mode));
    }

    /**
     * Internal method to clear yield state without callbacks.
     */
    private void clearYieldModeInternal(PlayerView player) {
        yieldStates.remove(player);
        autoPassUntilEndOfTurn.remove(player); // Legacy compatibility
    }

    /**
     * Get the current yield mode for a player.
     */
    public YieldMode getYieldMode(PlayerView player) {
        player = TrackableTypes.PlayerViewType.lookup(player); // ensure we use the correct player instance
        // Check legacy auto-pass first
        if (autoPassUntilEndOfTurn.contains(player)) {
            return YieldMode.UNTIL_END_OF_TURN;
        }
        YieldState state = yieldStates.get(player);
        return state != null && state.mode != null ? state.mode : YieldMode.NONE;
    }

    /**
     * Check if the player's yield just ended this priority pass (due to end condition or interrupt).
     * Used to suppress smart suggestions immediately after a yield ends.
     * This method clears the flag after checking.
     */
    public boolean didYieldJustEnd(PlayerView player) {
        player = TrackableTypes.PlayerViewType.lookup(player);
        return yieldJustEnded.remove(player);
    }

    /**
     * Check if auto-yield should be active for a player based on current game state.
     * Uses network-safe GameView properties to work correctly for non-host players in multiplayer.
     */
    public boolean shouldAutoYieldForPlayer(PlayerView player) {
        player = TrackableTypes.PlayerViewType.lookup(player); // ensure we use the correct player instance
        // Check legacy system first
        if (autoPassUntilEndOfTurn.contains(player)) {
            return true;
        }

        if (!isYieldExperimentalEnabled()) {
            return false;
        }

        YieldState state = yieldStates.get(player);
        if (state == null || state.mode == null || state.mode == YieldMode.NONE) {
            return false;
        }

        // Check interrupt conditions
        if (shouldInterruptYield(player)) {
            clearYieldMode(player);
            yieldJustEnded.add(player); // Track that yield just ended
            return false;
        }

        GameView gameView = callback.getGameView();
        if (gameView == null) {
            return false;
        }

        // Use network-safe GameView properties instead of gameView.getGame()
        forge.game.phase.PhaseType currentPhase = gameView.getPhase();
        int currentTurn = gameView.getTurn();
        PlayerView currentPlayerTurn = gameView.getPlayerTurn();

        return switch (state.mode) {
            case UNTIL_NEXT_PHASE -> {
                if (state.startPhase == null) {
                    // startPhase wasn't set in setYieldMode (gameView was null or timing issue).
                    // Set it now, but only continue if we're in a "starting" phase.
                    // If we appear to be past the starting point (e.g., in M2 when we
                    // probably started in M1), end the yield to avoid skipping too far.
                    state.startPhase = currentPhase;

                    // Safety check: if this is the second main phase and we just set
                    // startPhase, we likely missed our stop point due to timing
                    if (currentPhase == forge.game.phase.PhaseType.MAIN2) {
                        clearYieldMode(player);
                        yieldJustEnded.add(player);
                        yield false;
                    }
                    yield true;
                }
                if (currentPhase != state.startPhase) {
                    clearYieldMode(player);
                    yieldJustEnded.add(player);
                    yield false;
                }
                yield true;
            }
            case UNTIL_STACK_CLEARS -> {
                // Use GameView.getStack() which is network-synchronized
                boolean stackEmpty = gameView.getStack() == null || gameView.getStack().isEmpty();
                if (stackEmpty) {
                    clearYieldMode(player);
                    yieldJustEnded.add(player);
                    yield false;
                }
                yield true;
            }
            case UNTIL_END_OF_TURN -> {
                // Yield until end of the turn when yield was set - clear when turn number changes
                if (state.startTurn == null) {
                    // Turn wasn't tracked when yield was set - track it now
                    state.startTurn = currentTurn;
                    yield true;
                }
                if (currentTurn > state.startTurn) {
                    clearYieldMode(player);
                    yieldJustEnded.add(player);
                    yield false;
                }
                yield true;
            }
            case UNTIL_YOUR_NEXT_TURN -> {
                // Yield until our turn starts - use PlayerView comparison (network-safe)
                boolean isOurTurn = currentPlayerTurn != null && currentPlayerTurn.equals(player);

                if (state.startedDuringOurTurn == null) {
                    // Tracking wasn't set - initialize it now
                    state.startedDuringOurTurn = isOurTurn;
                }

                if (isOurTurn) {
                    // If we started during our turn, we need to wait until it's our turn AGAIN
                    // (i.e., we left our turn and came back)
                    // If we started during opponent's turn, stop when we reach our turn
                    if (!Boolean.TRUE.equals(state.startedDuringOurTurn)) {
                        clearYieldMode(player);
                        yieldJustEnded.add(player);
                        yield false;
                    }
                } else {
                    // Not our turn - if we started during our turn, mark that we've left it
                    if (Boolean.TRUE.equals(state.startedDuringOurTurn)) {
                        // We've left our turn, now waiting for it to come back
                        state.startedDuringOurTurn = false;
                    }
                }
                yield true;
            }
            case UNTIL_BEFORE_COMBAT -> {
                if (state.startTurn == null) {
                    // Tracking wasn't set - initialize it now
                    state.startTurn = currentTurn;
                    state.startedAtOrAfterPhase = isAtOrAfterCombat(currentPhase);
                }

                // Check if we should stop: we're at or past combat on a DIFFERENT turn than when we started,
                // OR we're at combat on the SAME turn but we started BEFORE combat
                if (isAtOrAfterCombat(currentPhase)) {
                    boolean differentTurn = currentTurn > state.startTurn;
                    boolean sameTurnButStartedBeforeCombat = (currentTurn == state.startTurn.intValue()) && !Boolean.TRUE.equals(state.startedAtOrAfterPhase);

                    if (differentTurn || sameTurnButStartedBeforeCombat) {
                        clearYieldMode(player);
                        yieldJustEnded.add(player);
                        yield false;
                    }
                }
                yield true;
            }
            case UNTIL_END_STEP -> {
                if (state.startTurn == null) {
                    // Tracking wasn't set - initialize it now
                    state.startTurn = currentTurn;
                    state.startedAtOrAfterPhase = isAtOrAfterEndStep(currentPhase);
                }

                // Check if we should stop: we're at or past end step on a DIFFERENT turn than when we started,
                // OR we're at end step on the SAME turn but we started BEFORE end step
                if (isAtOrAfterEndStep(currentPhase)) {
                    boolean differentTurn = currentTurn > state.startTurn;
                    boolean sameTurnButStartedBeforeEndStep = (currentTurn == state.startTurn.intValue()) && !Boolean.TRUE.equals(state.startedAtOrAfterPhase);

                    if (differentTurn || sameTurnButStartedBeforeEndStep) {
                        clearYieldMode(player);
                        yieldJustEnded.add(player);
                        yield false;
                    }
                }
                yield true;
            }
            default -> false;
        };
    }

    /**
     * Check if yield should be interrupted based on game conditions.
     * Uses network-safe GameView properties to work correctly for non-host players in multiplayer.
     */
    private boolean shouldInterruptYield(final PlayerView player) {
        GameView gameView = callback.getGameView();
        if (gameView == null) {
            return false;
        }

        ForgePreferences prefs = FModel.getPreferences();
        forge.game.phase.PhaseType phase = gameView.getPhase();
        PlayerView currentPlayerTurn = gameView.getPlayerTurn();
        forge.game.combat.CombatView combatView = gameView.getCombat();

        if (prefs.getPrefBoolean(ForgePreferences.FPref.YIELD_INTERRUPT_ON_ATTACKERS)) {
            // Only interrupt if there are creatures attacking THIS player or their planeswalkers/battles
            if (phase == forge.game.phase.PhaseType.COMBAT_DECLARE_ATTACKERS &&
                combatView != null && isBeingAttacked(combatView, player)) {
                return true;
            }
        }

        if (prefs.getPrefBoolean(ForgePreferences.FPref.YIELD_INTERRUPT_ON_BLOCKERS)) {
            // Only interrupt if there are creatures attacking THIS player or their planeswalkers/battles
            if (phase == forge.game.phase.PhaseType.COMBAT_DECLARE_BLOCKERS &&
                combatView != null && isBeingAttacked(combatView, player)) {
                return true;
            }
        }

        if (prefs.getPrefBoolean(ForgePreferences.FPref.YIELD_INTERRUPT_ON_TARGETING)) {
            forge.util.collect.FCollectionView<forge.game.spellability.StackItemView> stack = gameView.getStack();
            if (stack != null) {
                for (forge.game.spellability.StackItemView si : stack) {
                    if (targetsPlayerOrPermanents(si, player)) {
                        return true;
                    }
                }
            }
        }

        if (prefs.getPrefBoolean(ForgePreferences.FPref.YIELD_INTERRUPT_ON_OPPONENT_SPELL)) {
            // Use network-safe stack access via GameView
            forge.game.spellability.StackItemView topItem = gameView.peekStack();
            if (topItem != null) {
                PlayerView activatingPlayer = topItem.getActivatingPlayer();
                boolean isOpponent = activatingPlayer != null && !activatingPlayer.equals(player);

                // Interrupt for any opponent spell/ability that targets player or their permanents
                if (isOpponent && targetsPlayerOrPermanents(topItem, player)) {
                    return true;
                }
            }
        }

        if (prefs.getPrefBoolean(ForgePreferences.FPref.YIELD_INTERRUPT_ON_COMBAT)) {
            if (phase == forge.game.phase.PhaseType.COMBAT_BEGIN) {
                YieldState state = yieldStates.get(player);
                YieldMode mode = state != null ? state.mode : null;
                // Don't interrupt UNTIL_END_OF_TURN on our own turn
                boolean isOurTurn = currentPlayerTurn != null && currentPlayerTurn.equals(player);
                if (!(mode == YieldMode.UNTIL_END_OF_TURN && isOurTurn)) {
                    return true;
                }
            }
        }

        if (prefs.getPrefBoolean(ForgePreferences.FPref.YIELD_INTERRUPT_ON_MASS_REMOVAL)) {
            // Use network-safe StackItemView.getApiType() for mass removal detection
            if (hasMassRemovalOnStack(gameView, player)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if the player is being attacked (directly or via planeswalkers/battles).
     * Uses network-safe CombatView instead of Combat.
     */
    private boolean isBeingAttacked(forge.game.combat.CombatView combatView, PlayerView player) {
        if (combatView == null) {
            return false;
        }

        // Check if player is being attacked directly (player as defender)
        forge.util.collect.FCollection<CardView> attackersOfPlayer = combatView.getAttackersOf(player);
        if (attackersOfPlayer != null && !attackersOfPlayer.isEmpty()) {
            return true;
        }

        // Check if any planeswalkers or battles controlled by the player are being attacked
        for (forge.game.GameEntityView defender : combatView.getDefenders()) {
            if (defender instanceof CardView) {
                CardView cardDefender = (CardView) defender;
                PlayerView controller = cardDefender.getController();
                if (controller != null && controller.equals(player)) {
                    forge.util.collect.FCollection<CardView> attackers = combatView.getAttackersOf(defender);
                    if (attackers != null && !attackers.isEmpty()) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Check if a stack item targets the player or their permanents.
     * Recursively checks sub-instances to handle abilities with targeting in sub-abilities
     * (e.g., Oona, Queen of the Fae whose targeting is in a sub-ability).
     * Uses network-safe PlayerView comparisons.
     */
    private boolean targetsPlayerOrPermanents(forge.game.spellability.StackItemView si, PlayerView player) {
        forge.util.collect.FCollectionView<PlayerView> targetPlayers = si.getTargetPlayers();
        if (targetPlayers != null) {
            for (PlayerView target : targetPlayers) {
                if (target.equals(player)) return true;
            }
        }

        forge.util.collect.FCollectionView<CardView> targetCards = si.getTargetCards();
        if (targetCards != null) {
            for (CardView target : targetCards) {
                if (target.getController() != null && target.getController().equals(player)) {
                    return true;
                }
            }
        }

        // Recursively check sub-instances for targeting (handles abilities like Oona)
        forge.game.spellability.StackItemView subInstance = si.getSubInstance();
        if (subInstance != null && targetsPlayerOrPermanents(subInstance, player)) {
            return true;
        }

        return false;
    }

    /**
     * Check if there's a mass removal spell on the stack that could affect the player's permanents.
     * Uses network-safe StackItemView.getApiType() for detection.
     * Only interrupts if the spell was cast by an opponent.
     */
    private boolean hasMassRemovalOnStack(GameView gameView, PlayerView player) {
        forge.util.collect.FCollectionView<forge.game.spellability.StackItemView> stack = gameView.getStack();
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        for (forge.game.spellability.StackItemView si : stack) {
            PlayerView activatingPlayer = si.getActivatingPlayer();

            // Only interrupt for opponent's spells
            if (activatingPlayer == null || activatingPlayer.equals(player)) {
                continue;
            }

            // Check if this is a mass removal spell type (including sub-instances)
            if (isMassRemovalStackItem(si)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determine if a stack item is a mass removal effect.
     * Recursively checks sub-instances for modal spells like Farewell.
     */
    private boolean isMassRemovalStackItem(forge.game.spellability.StackItemView si) {
        // Check the main ability
        if (isMassRemovalApiType(si.getApiType())) {
            return true;
        }

        // Check sub-instances for modal spells like Farewell
        forge.game.spellability.StackItemView subInstance = si.getSubInstance();
        if (subInstance != null && isMassRemovalStackItem(subInstance)) {
            return true;
        }

        return false;
    }

    /**
     * Check if an API type name represents a mass removal effect.
     */
    private boolean isMassRemovalApiType(String apiType) {
        if (apiType == null) {
            return false;
        }

        // DestroyAll - Wrath of God, Day of Judgment, Damnation
        // DamageAll - Blasphemous Act, Chain Reaction
        // SacrificeAll - All Is Dust, Bane of Progress
        // ChangeZoneAll - Farewell, Merciless Eviction (covers exile/bounce effects)
        return "DestroyAll".equals(apiType) ||
               "DamageAll".equals(apiType) ||
               "SacrificeAll".equals(apiType) ||
               "ChangeZoneAll".equals(apiType);
    }

    /**
     * Check if experimental yield options are enabled in preferences.
     */
    private boolean isYieldExperimentalEnabled() {
        return FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.YIELD_EXPERIMENTAL_OPTIONS);
    }

    /**
     * Check if the phase is at or after the beginning of combat.
     */
    private boolean isAtOrAfterCombat(forge.game.phase.PhaseType phase) {
        return phase != null &&
            (phase == forge.game.phase.PhaseType.COMBAT_BEGIN || phase.isAfter(forge.game.phase.PhaseType.COMBAT_BEGIN));
    }

    /**
     * Check if the phase is at or after the end step.
     */
    private boolean isAtOrAfterEndStep(forge.game.phase.PhaseType phase) {
        return phase != null &&
            (phase == forge.game.phase.PhaseType.END_OF_TURN || phase == forge.game.phase.PhaseType.CLEANUP);
    }

    /**
     * Get the total number of players in the game.
     * Uses network-safe GameView.getPlayers() instead of Game.getPlayers().
     */
    public int getPlayerCount() {
        GameView gameView = callback.getGameView();
        if (gameView == null) {
            return 0;
        }
        forge.util.collect.FCollectionView<PlayerView> players = gameView.getPlayers();
        return players != null ? players.size() : 0;
    }

    /**
     * Mark a suggestion as declined for the current turn.
     * Uses network-safe GameView.getTurn() instead of Game.getPhaseHandler().getTurn().
     */
    public void declineSuggestion(PlayerView player, String suggestionType) {
        player = TrackableTypes.PlayerViewType.lookup(player); // ensure we use the correct player instance
        GameView gameView = callback.getGameView();
        if (gameView == null) return;

        int currentTurn = gameView.getTurn();
        Integer storedTurn = declinedSuggestionsTurn.get(player);

        // Reset if turn changed
        if (storedTurn == null || storedTurn != currentTurn) {
            declinedSuggestionsThisTurn.put(player, Sets.newHashSet());
            declinedSuggestionsTurn.put(player, currentTurn);
        }

        declinedSuggestionsThisTurn.get(player).add(suggestionType);
    }

    /**
     * Check if a suggestion has been declined for the current turn.
     * Uses network-safe GameView.getTurn() instead of Game.getPhaseHandler().getTurn().
     */
    public boolean isSuggestionDeclined(PlayerView player, String suggestionType) {
        player = TrackableTypes.PlayerViewType.lookup(player); // ensure we use the correct player instance
        GameView gameView = callback.getGameView();
        if (gameView == null) return false;

        int currentTurn = gameView.getTurn();
        Integer storedTurn = declinedSuggestionsTurn.get(player);

        if (storedTurn == null || storedTurn != currentTurn) {
            return false; // Turn changed, reset
        }

        Set<String> declined = declinedSuggestionsThisTurn.get(player);
        return declined != null && declined.contains(suggestionType);
    }

    /**
     * Remove a player from legacy auto-pass (for AbstractGuiGame internal use).
     */
    public void removeFromLegacyAutoPass(PlayerView player) {
        autoPassUntilEndOfTurn.remove(player);
    }

    /**
     * Convert a keyboard shortcut preference string to display text.
     * @param codeString Space-separated key codes (e.g., "17 67" for Ctrl+C)
     * @return Human-readable shortcut text (e.g., "Ctrl+C")
     */
    public static String formatShortcutDisplayText(String codeString) {
        if (codeString == null || codeString.isEmpty()) {
            return "";
        }
        List<String> codes = new ArrayList<>(Arrays.asList(codeString.trim().split(" ")));
        List<String> displayText = new ArrayList<>();
        for (String s : codes) {
            if (!s.isEmpty()) {
                try {
                    displayText.add(KeyEvent.getKeyText(Integer.parseInt(s)));
                } catch (NumberFormatException e) {
                    displayText.add(s);
                }
            }
        }
        return String.join("+", displayText);
    }

    /**
     * Get the display text for the yield cancel keyboard shortcut.
     * @return Human-readable shortcut text, e.g., "Escape" or "Ctrl+Escape"
     */
    public String getCancelShortcutDisplayText() {
        return formatShortcutDisplayText(FModel.getPreferences().getPref(FPref.SHORTCUT_YIELD_CANCEL));
    }
}
