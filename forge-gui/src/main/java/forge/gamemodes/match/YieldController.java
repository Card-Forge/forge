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
import forge.gui.interfaces.IGuiGame;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.trackable.TrackableTypes;
import forge.util.Localizer;

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

    private final IGuiGame gui;

    // Legacy auto-pass tracking
    private final Set<PlayerView> autoPassUntilEndOfTurn = Sets.newConcurrentHashSet();

    /**
     * Consolidated yield state for a player. Immutable so that ConcurrentHashMap
     * publication of the value reference is sufficient — readers on the game thread
     * see a consistent snapshot even when writers on the Netty thread replace the
     * map entry via setYieldModeSilent.
     */
    private static final class YieldState {
        final YieldMode mode;
        final Integer startTurn;                          // For UNTIL_END_OF_TURN, UNTIL_BEFORE_COMBAT, UNTIL_END_STEP
        final Boolean startedAtOrAfterPhase;              // For UNTIL_BEFORE_COMBAT and UNTIL_END_STEP
        final forge.game.phase.PhaseType startPhase;      // For UNTIL_NEXT_PHASE
        final Boolean startedDuringOurTurn;               // For UNTIL_YOUR_NEXT_TURN

        private YieldState(YieldMode mode, Integer startTurn, Boolean startedAtOrAfterPhase,
                           forge.game.phase.PhaseType startPhase, Boolean startedDuringOurTurn) {
            this.mode = mode;
            this.startTurn = startTurn;
            this.startedAtOrAfterPhase = startedAtOrAfterPhase;
            this.startPhase = startPhase;
            this.startedDuringOurTurn = startedDuringOurTurn;
        }

        static YieldState of(YieldMode mode) {
            return new YieldState(mode, null, null, null, null);
        }

        YieldState withStartTurn(Integer v) {
            return new YieldState(mode, v, startedAtOrAfterPhase, startPhase, startedDuringOurTurn);
        }
        YieldState withStartedAtOrAfterPhase(Boolean v) {
            return new YieldState(mode, startTurn, v, startPhase, startedDuringOurTurn);
        }
        YieldState withStartPhase(forge.game.phase.PhaseType v) {
            return new YieldState(mode, startTurn, startedAtOrAfterPhase, v, startedDuringOurTurn);
        }
        YieldState withStartedDuringOurTurn(Boolean v) {
            return new YieldState(mode, startTurn, startedAtOrAfterPhase, startPhase, v);
        }
    }

    // Extended yield mode tracking (experimental feature)
    private final Map<PlayerView, YieldState> yieldStates = Maps.newConcurrentMap();

    /**
     * Create a new YieldController with the given GUI game for updates and state access.
     * @param gui the GUI game interface
     */
    public YieldController(IGuiGame gui) {
        this.gui = gui;
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
        gui.showPromptMessage(player, "");
        gui.updateButtons(player, false, false, false);
        gui.awaitNextInput();
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
        // Check persistent auto-pass when no actions available
        if (isAutoPassingNoActions(player)) {
            return true;
        }
        // Check experimental yield system
        return shouldAutoYieldForPlayer(player);
    }

    /**
     * Check if auto-pass should fire because the player has no available actions.
     * This is a persistent preference toggle, not a one-shot yield mode.
     * Reads YIELD_AUTO_PASS_NO_ACTIONS from the active player's source — host's
     * local prefs for the host player, the remote client's stored snapshot for
     * remote players.
     */
    public boolean isAutoPassingNoActions(PlayerView player) {
        if (!isYieldExperimentalEnabled()) {
            return false;
        }
        boolean prefValue = getInterruptPref(ForgePreferences.FPref.YIELD_AUTO_PASS_NO_ACTIONS);
        if (!prefValue) {
            return false;
        }
        // Interrupt conditions still break through (attackers, targeting, etc.)
        if (shouldInterruptYield(player)) {
            return false;
        }
        // Respect phase-skip settings: pass through unmarked phases even if
        // the player has actions. Auto-pass should be additive to phase-skip,
        // not cause stops at phases the user explicitly set to skip.
        GameView gv = gui.getGameView();
        if (gv != null && gv.getStack() != null && gv.getStack().isEmpty()) {
            PlayerView turnPlayer = gv.getPlayerTurn();
            forge.game.phase.PhaseType phase = gv.getPhase();
            if (turnPlayer != null && phase != null
                    && gui.isUiSetToSkipPhase(turnPlayer, phase)) {
                return true;
            }
        }
        return !player.hasAvailableActions();
    }

    /**
     * Look up a yield interrupt preference for the player this YieldController serves.
     * For the host's own GUI (CMatchUI), reads FModel.getPreferences().
     * For a remote-player proxy GUI (NetGuiGame), reads the YieldPrefs snapshot
     * stored on the proxy by notifyYieldStateChanged. Falls back to the Forge
     * default value if no snapshot has arrived yet — the race window between
     * game start and the client's first send is microseconds; a one-pass
     * fallback is acceptable.
     */
    private boolean getInterruptPref(ForgePreferences.FPref pref) {
        if (gui.isRemoteGuiProxy()) {
            YieldPrefs remote = gui.getRemoteYieldPrefs();
            return remote != null
                ? remote.getInterrupt(pref)
                : "true".equals(pref.getDefault());
        }
        return FModel.getPreferences().getPrefBoolean(pref);
    }

    /**
     * Update the prompt message to show current yield status.
     */
    public void updateAutoPassPrompt(PlayerView player) {
        player = TrackableTypes.PlayerViewType.lookup(player);

        // Check legacy auto-pass first
        if (autoPassUntilEndOfTurn.contains(player)) {
            gui.cancelAwaitNextInput();
            gui.showPromptMessage(player, Localizer.getInstance().getMessage("lblYieldingUntilEndOfTurn"));
            gui.updateButtons(player, false, true, false);
            return;
        }

        // Check experimental yield modes
        YieldState state = yieldStates.get(player);
        if (state != null && state.mode != null && state.mode != YieldMode.NONE) {
            YieldMode mode = state.mode;
            gui.cancelAwaitNextInput();
            Localizer loc = Localizer.getInstance();
            String message = switch (mode) {
                case UNTIL_NEXT_PHASE -> loc.getMessage("lblYieldingUntilNextPhase");
                case UNTIL_STACK_CLEARS -> loc.getMessage("lblYieldingUntilStackClears");
                case UNTIL_END_OF_TURN -> loc.getMessage("lblYieldingUntilEndOfTurn");
                case UNTIL_YOUR_NEXT_TURN -> loc.getMessage("lblYieldingUntilYourNextTurn");
                case UNTIL_BEFORE_COMBAT -> loc.getMessage("lblYieldingUntilBeforeCombat");
                case UNTIL_END_STEP -> loc.getMessage("lblYieldingUntilEndStep");
                case UNTIL_END_STEP_BEFORE_YOUR_TURN -> loc.getMessage("lblYieldingUntilBeforeYourTurn");
                default -> "";
            };
            gui.showPromptMessage(player, message);
            gui.updateButtons(player, false, true, false);
            return;
        }

        // No yield mode active — but mayAutoPass may still be true via the
        // persistent YIELD_AUTO_PASS_NO_ACTIONS pref. In that case clear the
        // stale prompt left over from the previous input (e.g. a Pay Mana Cost
        // prompt) so the user isn't shown a misleading message.
        if (isAutoPassingNoActions(player)) {
            gui.cancelAwaitNextInput();
            gui.showPromptMessage(player, Localizer.getInstance().getMessage("lblAutoPassingNoActions"));
            gui.updateButtons(player, false, false, false);
        }
    }

    /**
     * Set the yield mode for a player.
     * @return true if a new yield mode was activated; false otherwise (cleared, rejected, or feature disabled)
     */
    public boolean setYieldMode(PlayerView player, final YieldMode mode) {
        player = TrackableTypes.PlayerViewType.lookup(player); // ensure we use the correct player instance
        if (!isYieldExperimentalEnabled()) {
            // Fall back to legacy behavior for UNTIL_END_OF_TURN
            if (mode == YieldMode.UNTIL_END_OF_TURN) {
                autoPassUntilEndOfTurn.add(player);
                return true;
            }
            return false;
        }

        if (mode == null || mode == YieldMode.NONE) {
            clearYieldMode(player);
            return false;
        }

        GameView gameView = gui.getGameView();

        // Reject UNTIL_STACK_CLEARS when the stack is already empty — must check
        // before mutating any state so a rejected request leaves the player's
        // existing yield/auto-pass state untouched
        if (mode == YieldMode.UNTIL_STACK_CLEARS && gameView != null
                && (gameView.getStack() == null || gameView.getStack().isEmpty())) {
            return false;
        }

        // Clear any legacy auto-pass state to prevent interference
        // (legacy check in shouldAutoYieldForPlayer runs first and would override experimental mode)
        autoPassUntilEndOfTurn.remove(player);

        // If gameView is unavailable at set time, fall back to a bare state — the lazy-init
        // paths in shouldAutoYieldForPlayer will populate the timing fields on the next pass.
        if (gameView == null) {
            yieldStates.put(player, YieldState.of(mode));
            return true;
        }

        forge.game.phase.PhaseType phase = gameView.getPhase();
        int currentTurn = gameView.getTurn();
        PlayerView currentPlayerTurn = gameView.getPlayerTurn();

        // Build the initial state for this mode
        YieldState state = switch (mode) {
            case UNTIL_NEXT_PHASE -> YieldState.of(mode).withStartPhase(phase);
            case UNTIL_END_OF_TURN -> YieldState.of(mode).withStartTurn(currentTurn);
            case UNTIL_BEFORE_COMBAT -> YieldState.of(mode)
                .withStartTurn(currentTurn)
                .withStartedAtOrAfterPhase(isAtOrAfterCombat(phase));
            case UNTIL_END_STEP -> YieldState.of(mode)
                .withStartTurn(currentTurn)
                .withStartedAtOrAfterPhase(isAtOrAfterEndStep(phase));
            case UNTIL_YOUR_NEXT_TURN -> YieldState.of(mode)
                .withStartedDuringOurTurn(currentPlayerTurn != null && currentPlayerTurn.equals(player));
            case UNTIL_END_STEP_BEFORE_YOUR_TURN -> YieldState.of(mode)
                .withStartTurn(currentTurn)
                .withStartedAtOrAfterPhase(isAtOrAfterEndStep(phase));
            default -> YieldState.of(mode);
        };
        yieldStates.put(player, state);
        return true;
    }

    /**
     * Clear yield mode for a player.
     */
    public void clearYieldMode(PlayerView player) {
        player = TrackableTypes.PlayerViewType.lookup(player); // ensure we use the correct player instance
        clearYieldModeInternal(player);

        gui.showPromptMessage(player, "");
        gui.updateButtons(player, false, false, false);
        gui.awaitNextInput();

        // Notify client to update its local yield state (for network play)
        gui.syncYieldMode(player, YieldMode.NONE);
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
        yieldStates.put(player, YieldState.of(mode));
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

        // Interrupts apply uniformly: host prefs for the host player, remote
        // client's stored prefs for remote players (via getInterruptPref).
        if (shouldInterruptYield(player)) {
            clearYieldMode(player);
            return false;
        }

        GameView gameView = gui.getGameView();
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
                    yieldStates.put(player, state.withStartPhase(currentPhase));

                    // Safety check: if this is the second main phase and we just set
                    // startPhase, we likely missed our stop point due to timing
                    if (currentPhase == forge.game.phase.PhaseType.MAIN2) {
                        clearYieldMode(player);
                        yield false;
                    }
                    yield true;
                }
                if (currentPhase != state.startPhase) {
                    clearYieldMode(player);
                    yield false;
                }
                yield true;
            }
            case UNTIL_STACK_CLEARS -> {
                // Use GameView.getStack() which is network-synchronized
                boolean stackEmpty = gameView.getStack() == null || gameView.getStack().isEmpty();
                if (stackEmpty) {
                    clearYieldMode(player);
                    yield false;
                }
                yield true;
            }
            case UNTIL_END_OF_TURN -> {
                // Yield until end of the turn when yield was set - clear when turn number changes
                if (state.startTurn == null) {
                    // Turn wasn't tracked when yield was set - track it now
                    yieldStates.put(player, state.withStartTurn(currentTurn));
                    yield true;
                }
                if (currentTurn > state.startTurn) {
                    clearYieldMode(player);
                    yield false;
                }
                yield true;
            }
            case UNTIL_YOUR_NEXT_TURN -> {
                // Yield until our turn starts - use PlayerView comparison (network-safe)
                boolean isOurTurn = currentPlayerTurn != null && currentPlayerTurn.equals(player);

                if (state.startedDuringOurTurn == null) {
                    // Tracking wasn't set - initialize it now
                    state = state.withStartedDuringOurTurn(isOurTurn);
                    yieldStates.put(player, state);
                }

                if (isOurTurn) {
                    // If we started during our turn, we need to wait until it's our turn AGAIN
                    // (i.e., we left our turn and came back)
                    // If we started during opponent's turn, stop when we reach our turn
                    if (!Boolean.TRUE.equals(state.startedDuringOurTurn)) {
                        clearYieldMode(player);
                        yield false;
                    }
                } else {
                    // Not our turn - if we started during our turn, mark that we've left it
                    if (Boolean.TRUE.equals(state.startedDuringOurTurn)) {
                        // We've left our turn, now waiting for it to come back
                        yieldStates.put(player, state.withStartedDuringOurTurn(false));
                    }
                }
                yield true;
            }
            case UNTIL_BEFORE_COMBAT -> {
                if (state.startTurn == null) {
                    // Tracking wasn't set - initialize it now
                    state = state.withStartTurn(currentTurn)
                                 .withStartedAtOrAfterPhase(isAtOrAfterCombat(currentPhase));
                    yieldStates.put(player, state);
                }

                // Check if we should stop: we're at or past combat on a DIFFERENT turn than when we started,
                // OR we're at combat on the SAME turn but we started BEFORE combat
                if (isAtOrAfterCombat(currentPhase)) {
                    boolean differentTurn = currentTurn > state.startTurn;
                    boolean sameTurnButStartedBeforeCombat = (currentTurn == state.startTurn.intValue()) && !Boolean.TRUE.equals(state.startedAtOrAfterPhase);

                    if (differentTurn || sameTurnButStartedBeforeCombat) {
                        clearYieldMode(player);
                        yield false;
                    }
                }
                yield true;
            }
            case UNTIL_END_STEP -> {
                if (state.startTurn == null) {
                    // Tracking wasn't set - initialize it now
                    state = state.withStartTurn(currentTurn)
                                 .withStartedAtOrAfterPhase(isAtOrAfterEndStep(currentPhase));
                    yieldStates.put(player, state);
                }

                // Check if we should stop: we're at or past end step on a DIFFERENT turn than when we started,
                // OR we're at end step on the SAME turn but we started BEFORE end step
                if (isAtOrAfterEndStep(currentPhase)) {
                    boolean differentTurn = currentTurn > state.startTurn;
                    boolean sameTurnButStartedBeforeEndStep = (currentTurn == state.startTurn.intValue()) && !Boolean.TRUE.equals(state.startedAtOrAfterPhase);

                    if (differentTurn || sameTurnButStartedBeforeEndStep) {
                        clearYieldMode(player);
                        yield false;
                    }
                }
                yield true;
            }
            case UNTIL_END_STEP_BEFORE_YOUR_TURN -> {
                if (state.startTurn == null) {
                    yieldStates.put(player, state.withStartTurn(currentTurn)
                        .withStartedAtOrAfterPhase(isAtOrAfterEndStep(currentPhase)));
                    yield true;
                }

                // Stop at the end step of the player immediately before us in turn order.
                if (isAtOrAfterEndStep(currentPhase)) {
                    boolean differentTurn = currentTurn > state.startTurn;
                    boolean sameTurnButStartedBeforeEndStep = (currentTurn == state.startTurn.intValue())
                        && !Boolean.TRUE.equals(state.startedAtOrAfterPhase);

                    if (differentTurn || sameTurnButStartedBeforeEndStep) {
                        if (isPlayerBeforeUs(currentPlayerTurn, player, gameView)) {
                            clearYieldMode(player);
                            yield false;
                        }
                    }
                }
                yield true;
            }
            default -> false;
        };
    }

    /**
     * Check if yield should be interrupted based on game conditions.
     * Reads interrupt prefs through getInterruptPref so remote players honour
     * their own client-side prefs (forwarded by notifyYieldStateChanged) rather
     * than the host's.
     */
    private boolean shouldInterruptYield(final PlayerView player) {
        GameView gameView = gui.getGameView();
        if (gameView == null) {
            return false;
        }

        forge.game.phase.PhaseType phase = gameView.getPhase();
        PlayerView currentPlayerTurn = gameView.getPlayerTurn();
        forge.game.combat.CombatView combatView = gameView.getCombat();

        if (getInterruptPref(ForgePreferences.FPref.YIELD_INTERRUPT_ON_ATTACKERS)) {
            // Only interrupt if there are creatures attacking THIS player or their planeswalkers/battles
            if (phase == forge.game.phase.PhaseType.COMBAT_DECLARE_ATTACKERS &&
                combatView != null && isBeingAttacked(combatView, player)) {
                return true;
            }
        }

        if (getInterruptPref(ForgePreferences.FPref.YIELD_INTERRUPT_ON_TARGETING)) {
            forge.util.collect.FCollectionView<forge.game.spellability.StackItemView> stack = gameView.getStack();
            if (stack != null) {
                for (forge.game.spellability.StackItemView si : stack) {
                    if (targetsPlayerOrPermanents(si, player)) {
                        return true;
                    }
                }
            }
        }

        if (getInterruptPref(ForgePreferences.FPref.YIELD_INTERRUPT_ON_OPPONENT_SPELL)) {
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

        if (getInterruptPref(ForgePreferences.FPref.YIELD_INTERRUPT_ON_MASS_REMOVAL)) {
            if (hasMassRemovalOnStack(gameView, player)) {
                return true;
            }
        }

        if (getInterruptPref(ForgePreferences.FPref.YIELD_INTERRUPT_ON_TRIGGERS)) {
            forge.util.collect.FCollectionView<forge.game.spellability.StackItemView> stack = gameView.getStack();
            if (stack != null) {
                for (forge.game.spellability.StackItemView si : stack) {
                    if (si.isTrigger()) {
                        return true;
                    }
                }
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
     * Walks the live engine stack via gameView.getGame() — YieldController only ever runs on
     * the host process, where gameView.getGame() is non-null. Only interrupts for opponent spells.
     */
    private boolean hasMassRemovalOnStack(GameView gameView, PlayerView player) {
        forge.game.Game game = gameView.getGame();
        if (game == null) {
            return false; // host-only path; defensive
        }
        for (forge.game.spellability.SpellAbilityStackInstance si : game.getStack()) {
            forge.game.player.Player activator = si.getActivatingPlayer();
            if (activator == null || activator.getView().equals(player)) {
                continue;
            }
            if (isMassRemovalInstance(si)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determine if a stack instance is a mass removal effect.
     * Recursively checks sub-instances for modal spells like Farewell.
     */
    private boolean isMassRemovalInstance(forge.game.spellability.SpellAbilityStackInstance si) {
        forge.game.spellability.SpellAbility sa = si.getSpellAbility();
        if (sa != null && isMassRemovalApi(sa.getApi())) {
            return true;
        }
        forge.game.spellability.SpellAbilityStackInstance subInstance = si.getSubInstance();
        if (subInstance != null && isMassRemovalInstance(subInstance)) {
            return true;
        }
        return false;
    }

    /**
     * Check if an ApiType represents a mass removal effect.
     *
     * - DestroyAll: Wrath of God, Day of Judgment, Damnation
     * - DamageAll: Blasphemous Act, Chain Reaction
     * - SacrificeAll: All Is Dust, Bane of Progress
     * - ChangeZoneAll: Farewell, Merciless Eviction (covers exile/bounce effects)
     */
    private boolean isMassRemovalApi(forge.game.ability.ApiType api) {
        return api == forge.game.ability.ApiType.DestroyAll
            || api == forge.game.ability.ApiType.DamageAll
            || api == forge.game.ability.ApiType.SacrificeAll
            || api == forge.game.ability.ApiType.ChangeZoneAll;
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
     * Check if the current player's turn belongs to the player immediately before us in turn order.
     * In a 4-player game [A, B, C, D], if we are C, returns true only for B.
     * Handles wraparound: if we are A, returns true for D.
     */
    private boolean isPlayerBeforeUs(PlayerView currentPlayerTurn, PlayerView us, GameView gameView) {
        if (currentPlayerTurn == null || us == null) {
            return true; // fallback: stop yielding if we can't determine
        }

        java.util.List<PlayerView> players = new java.util.ArrayList<>(gameView.getPlayers());
        if (players.size() < 2) {
            return true;
        }

        int ourIndex = -1;
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).equals(us)) {
                ourIndex = i;
                break;
            }
        }
        if (ourIndex < 0) {
            return true; // fallback
        }

        // The player before us is at (ourIndex - 1 + size) % size
        int prevIndex = (ourIndex - 1 + players.size()) % players.size();
        return players.get(prevIndex).equals(currentPlayerTurn);
    }

    /**
     * Remove a player from legacy auto-pass (for AbstractGuiGame internal use).
     */
    public void removeFromLegacyAutoPass(PlayerView player) {
        autoPassUntilEndOfTurn.remove(player);
    }

}
